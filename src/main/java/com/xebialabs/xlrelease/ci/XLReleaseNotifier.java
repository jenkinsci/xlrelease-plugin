/**
 * Copyright (c) 2014, XebiaLabs B.V., All rights reserved.
 *
 *
 * The XL Release plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs Libraries.
 * There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
 * this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/xlrelease-plugin/blob/master/LICENSE>.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
 * Floor, Boston, MA 02110-1301  USA
 */

package com.xebialabs.xlrelease.ci;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerConnector;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerFactory;
import com.xebialabs.xlrelease.ci.util.JenkinsReleaseListener;
import com.xebialabs.xlrelease.ci.util.Release;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static hudson.util.FormValidation.*;

public class XLReleaseNotifier extends Notifier {

    public final String credential;

    public final String template;
    public final String version;

    public List<NameValuePair> variables;
    public final boolean startRelease;

    @DataBoundConstructor
    public XLReleaseNotifier(String credential, String template, String version,  List<NameValuePair> variables, boolean startRelease) {
        this.credential = credential;
        this.template = template;
        this.version = version;
        this.variables = variables;
        this.startRelease = startRelease;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        return executeRelease(build.getEnvironment(listener),listener);
    }

    public boolean executeRelease (EnvVars envVars, TaskListener listener) {
        final JenkinsReleaseListener deploymentListener = new JenkinsReleaseListener(listener);

        String resolvedVersion = envVars.expand(version);
        List<NameValuePair> resolvedVariables = new ArrayList<NameValuePair>();
        if (CollectionUtils.isNotEmpty(variables)) {
            for (NameValuePair nameValuePair : variables) {
                resolvedVariables.add(new NameValuePair(nameValuePair.propertyName, envVars.expand(nameValuePair.propertyValue)));
            }
        }

        // createRelease
        Release release = createRelease(template, resolvedVersion, resolvedVariables);
        deploymentListener.info(Messages.XLReleaseNotifier_createRelease(template, resolvedVersion, release.getId()));

        // startRelease
        if (startRelease) {
            deploymentListener.info(Messages.XLReleaseNotifier_startRelease(template, resolvedVersion, release.getId()));
            startRelease(release);
        }
        String releaseUrl = getXLReleaseServer().getServerURL() + release.getReleaseURL();
        deploymentListener.info(Messages.XLReleaseNotifier_releaseLink(releaseUrl));
        return true;

    }

    private Release createRelease(final String resolvedTemplate, final String resolvedVersion, final List<NameValuePair> resolvedVariables) {
        // create a new release instance
        Release release = getXLReleaseServer().createRelease(resolvedTemplate, resolvedVersion, resolvedVariables);
        return release;
    }

    private void startRelease(final Release release) {
        // start the release
        getXLReleaseServer().startRelease(release.getInternalId());
    }

    private XLReleaseServerConnector getXLReleaseServer() {
        XLReleaseServerConnector connector = getDescriptor().getXLReleaseServer(credential);
        if (connector == null)
            throw new RuntimeException(Messages.XLReleaseNotifier_credentialNotFound(credential));
        return connector;
    }

    @Override
    public XLReleaseDescriptor getDescriptor() {
        XLReleaseDescriptor descriptor = (XLReleaseDescriptor) super.getDescriptor();
        descriptor.load();
        return descriptor;
    }

    @Extension
    public static final class XLReleaseDescriptor extends BuildStepDescriptor<Publisher> {

        // ************ SERIALIZED GLOBAL PROPERTIES *********** //

        private String xlReleaseServerUrl;

        private String xlReleaseClientProxyUrl;

        private List<Credential> credentials = newArrayList();

        // ************ OTHER NON-SERIALIZABLE PROPERTIES *********** //

        private final transient Map<String,XLReleaseServerConnector> credentialServerMap = newHashMap();
        private transient static XLReleaseServerFactory xlReleaseServerFactory = new XLReleaseServerFactory();
        public transient String lastCredential;
        private Release release;

        public XLReleaseDescriptor() {
            load();  //deserialize from xml
        }

        private void mapCredentialsByName() {
            for (Credential credential : credentials) {
                String serverUrl = credential.resolveServerUrl(xlReleaseServerUrl);
                String proxyUrl = credential.resolveProxyUrl(xlReleaseClientProxyUrl);
                if (credential.useGlobalCredential) {
                    StandardUsernamePasswordCredentials cred =  Credential.lookupSystemCredentials(credential.credentialsId);
                    credentialServerMap.put(credential.name, xlReleaseServerFactory.newInstance(serverUrl, proxyUrl,
                            cred.getUsername(), cred.getPassword() != null ? cred.getPassword().getPlainText() : ""));
                } else {
                    credentialServerMap.put(credential.name, xlReleaseServerFactory.newInstance(serverUrl, proxyUrl,
                            credential.username, credential.password != null ? credential.password.getPlainText() : ""));
                }
            }
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            //this method is called when the global form is submitted.
            xlReleaseServerUrl = json.get("xlReleaseServerUrl").toString();
            xlReleaseClientProxyUrl = json.get("xlReleaseClientProxyUrl").toString();
            credentials = req.bindJSONToList(Credential.class, json.get("credentials"));
            save();  //serialize to xml
            mapCredentialsByName();
            return true;
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return Messages.XLReleaseNotifier_displayName();
        }

        private FormValidation validateOptionalUrl(String url) {
            try {
                if (!Strings.isNullOrEmpty(url)) {
                    new URL(url);
                }
            } catch (MalformedURLException e) {
                return error("%s is not a valid URL.",url);
            }
            return ok();

        }

        @Override
        public synchronized void load() {
            super.load();
            mapCredentialsByName();
        }

        public FormValidation doCheckXLReleaseServerUrl(@QueryParameter String xlReleaseServerUrl) {
            if (Strings.isNullOrEmpty(xlReleaseServerUrl)) {
                return error("Url required.");
            }
            return validateOptionalUrl(xlReleaseServerUrl);
        }

        public FormValidation doCheckXLReleaseClientProxyUrl(@QueryParameter String xlReleaseClientProxyUrl) {
            return validateOptionalUrl(xlReleaseClientProxyUrl);
        }

        public FormValidation doValidateTemplate(@QueryParameter String credential, @QueryParameter final String template) {
            try {

                this.release = getTemplate(credential,template);
                if(this.release != null && !"folder".equals(release.getStatus())) {
                    return warning("Changing template may unintentionally change your variables");
                }
                return error("Template does not exist.");
            } catch (UniformInterfaceException exp){
                return error("Template does not exist.");
            }catch (Exception exp) {
                return warning("Failed to communicate with XL Release server");
            }
        }

        private Release getTemplate(String credential, String value) {
            List<Release> candidates = getXLReleaseServer(credential).searchTemplates(value);
            for (Release candidate : candidates) {
                if (candidate.getTitle().equals(value)) {
                    candidate.setVariableValues(getVariables(credential, candidate));
                    return candidate;
                }
            }
            return null;

        }

        private Map<String, String> getVariables(String credential, Release release) {
            List<TemplateVariable> variables = getXLReleaseServer(credential).getVariables(release.getInternalId());
            return TemplateVariable.toMap(variables);
        }

        public AutoCompletionCandidates doAutoCompleteTemplate(@QueryParameter final String value) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            List<Release> releases = getXLReleaseServer(lastCredential).searchTemplates(value);
            for (Release release:releases) {
                candidates.add(release.getTitle());
            }
            return candidates;
        }

        public List<Credential> getCredentials() {
            return credentials;
        }

        public String getXlReleaseServerUrl() {
            return xlReleaseServerUrl;
        }

        public String getXlReleaseClientProxyUrl() {
            return xlReleaseClientProxyUrl;
        }

        public ListBoxModel doFillCredentialItems() {

            ListBoxModel m = new ListBoxModel();
            m.add("-- Please Select --","");
            for (Credential c : credentials)
                m.add(c.name, c.name);
            return m;
        }

        public FormValidation doCheckCredential(@QueryParameter String credential) {
            lastCredential = credential;
            if (StringUtils.isEmpty(credential)) {
               return error("Please select a valid credential");
            }
            return warning("Changing credentials may unintentionally change your available templates");
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }


        private XLReleaseServerConnector getXLReleaseServer(String credential) {
            checkNotNull(credential);
            return credentialServerMap.get(credential);
        }


        public Map<String, String> getVariablesOf(final String credential, final String template) {
            release = getTemplate(credential, template);
            if (release == null) {
                return Collections.emptyMap();
            }
            return release.getVariableValues();
        }

        public int getNumberOfVariables(@QueryParameter String credential, @QueryParameter String template) {
            if (credential != null) {
                Map<String, String> variables = getVariablesOf(credential,template);
                if (variables != null) {
                    return variables.size();
                }
            }
            return 0;
        }

        @VisibleForTesting
        public static void setXlReleaseServerFactory(final XLReleaseServerFactory xlReleaseServerFactory) {
            XLReleaseDescriptor.xlReleaseServerFactory = xlReleaseServerFactory;
        }
    }
}
