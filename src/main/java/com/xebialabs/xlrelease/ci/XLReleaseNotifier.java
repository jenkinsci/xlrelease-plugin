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

import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerConnector;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerConnectorFactory;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerFactory;
import com.xebialabs.xlrelease.ci.util.JenkinsReleaseListener;
import com.xebialabs.xlrelease.ci.util.ListBoxModels;
import com.xebialabs.xlrelease.ci.util.Release;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.verb.POST;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
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

    public Credential overridingCredential;

    @DataBoundConstructor
    public XLReleaseNotifier(String credential, String template, String version,  List<NameValuePair> variables, boolean startRelease, Credential overridingCredential) {
        this.credential = credential;
        this.template = template;
        this.version = version;
        this.variables = variables;
        this.startRelease = startRelease;
        this.overridingCredential = overridingCredential;
}

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        String cred = credential;
        Credential credential = RepositoryUtils.findCredential(cred);

        if (null != credential && null != overridingCredential) {
            credential = RepositoryUtils.retrieveOverridingCredential(credential, overridingCredential.getCredentialsId(),
                    credential.getName(), overridingCredential.getUsername(), overridingCredential.getPassword(),
                    overridingCredential.isUseGlobalCredential());
        }

        XLReleaseDescriptor descriptor = RepositoryUtils.getXLreleaseDescriptor();
        XLReleaseServerConnector xlReleaseServerConnector = descriptor.getXLReleaseServer(credential, build.getProject());

        return executeRelease(build.getEnvironment(listener),listener, xlReleaseServerConnector);
    }

    public boolean executeRelease (EnvVars envVars, TaskListener listener, XLReleaseServerConnector xlReleaseServerConnector) {
        final JenkinsReleaseListener deploymentListener = new JenkinsReleaseListener(listener);

        String resolvedVersion = envVars.expand(version);
        List<NameValuePair> resolvedVariables = new ArrayList<NameValuePair>();
        if (CollectionUtils.isNotEmpty(variables)) {
            for (NameValuePair nameValuePair : variables) {
                resolvedVariables.add(new NameValuePair(nameValuePair.propertyName, envVars.expand(nameValuePair.propertyValue)));
            }
        }

        // createRelease
        Release release = createRelease(template, resolvedVersion, resolvedVariables, xlReleaseServerConnector);
        deploymentListener.info(Messages.XLReleaseNotifier_createRelease(template, resolvedVersion, release.getId()));

        // startRelease
        if (startRelease) {
            deploymentListener.info(Messages.XLReleaseNotifier_startRelease(template, resolvedVersion, release.getId()));
            startRelease(release, xlReleaseServerConnector);
        }
        String releaseUrl = xlReleaseServerConnector.getServerURL() + release.getReleaseURL();
        deploymentListener.info(Messages.XLReleaseNotifier_releaseLink(releaseUrl));
        return true;

    }

    private Release createRelease(final String resolvedTemplate, final String resolvedVersion, final List<NameValuePair> resolvedVariables, XLReleaseServerConnector xlReleaseServerConnector) {
        // create a new release instance
        Release release = xlReleaseServerConnector.createRelease(resolvedTemplate, resolvedVersion, resolvedVariables);
        return release;
    }

    private void startRelease(final Release release, XLReleaseServerConnector xlReleaseServerConnector) {
        // start the release
        xlReleaseServerConnector.startRelease(release.getInternalId());
    }

    @Override
    public XLReleaseDescriptor getDescriptor() {
        XLReleaseDescriptor descriptor = (XLReleaseDescriptor) super.getDescriptor();
        descriptor.load();
        return descriptor;
    }

    public boolean showGolbalCredentials() {
        return overridingCredential.isUseGlobalCredential();
    }

    public Credential getOverridingCredential() {
        return this.overridingCredential;
    }

    @Extension
    public static final class XLReleaseDescriptor extends BuildStepDescriptor<Publisher> {

        // ************ SERIALIZED GLOBAL PROPERTIES *********** //

        private String xlReleaseServerUrl;

        private String xlReleaseClientProxyUrl;

        private List<Credential> credentials = newArrayList();

        private static final SchemeRequirement HTTP_SCHEME = new SchemeRequirement("http");
        private static final SchemeRequirement HTTPS_SCHEME = new SchemeRequirement("https");

        // ************ OTHER NON-SERIALIZABLE PROPERTIES *********** //

        private final XLReleaseServerConnectorFactory connectorHolder = new XLReleaseServerConnectorFactory();

        private final transient Map<String,XLReleaseServerConnector> xlrCredentialServerMap = newHashMap();
        private final transient Map<String, SoftReference<XLReleaseServerConnector>> credentialServerMap = new HashMap<String, SoftReference<XLReleaseServerConnector>>();
        private transient static XLReleaseServerFactory xlReleaseServerFactory = new XLReleaseServerFactory();
        public transient String lastCredential;
        public transient Credential lastOverridingCredential;
        private Release release;

        public XLReleaseDescriptor() {
            load();  //deserialize from xml
        }

        private XLReleaseServerConnector newXLreleaseServer(Credential credential, ItemGroup<?> itemGroup) {
            String serverUrl = credential.resolveServerUrl(xlReleaseServerUrl);
            String proxyUrl = credential.resolveProxyUrl(xlReleaseClientProxyUrl);

            String userName = credential.getUsername();
            String password = credential.getPassword().getPlainText();
            if (credential.isUseGlobalCredential()) {
                StandardUsernamePasswordCredentials cred = Credential.lookupSystemCredentials(credential.getCredentialsId(), itemGroup);
                if (cred == null) {
                    throw new IllegalArgumentException(String.format("Credentials for '%s' not found.", credential.getCredentialsId()));
                }
                userName = cred.getUsername();
                password = cred.getPassword().getPlainText();
            }
            return xlReleaseServerFactory.newInstance(serverUrl, proxyUrl, userName, password);
        }

        public XLReleaseServerConnector getXLReleaseServer(Credential credential, Job<?, ?> project) {
            XLReleaseServerConnector xlReleaseServerConnector = null;
            if (null != credential) {
                SoftReference<XLReleaseServerConnector> xlreleaseServerRef = credentialServerMap.get(credential.getKey());

                if (null != xlreleaseServerRef) {
                    xlReleaseServerConnector = xlreleaseServerRef.get();
                }

                if (null == xlReleaseServerConnector) {
                    synchronized (this) {
                        xlReleaseServerConnector = newXLreleaseServer(credential, project.getParent());
                        credentialServerMap.put(credential.getKey(), new SoftReference<XLReleaseServerConnector>(xlReleaseServerConnector));
                    }
                }
            }
            // no credential - no server
            return xlReleaseServerConnector;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            //this method is called when the global form is submitted.
            xlReleaseServerUrl = json.get("xlReleaseServerUrl").toString();
            xlReleaseClientProxyUrl = json.get("xlReleaseClientProxyUrl").toString();
            credentials = req.bindJSONToList(Credential.class, json.get("credentials"));
            save();  //serialize to xml
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
            connectorHolder.load(xlReleaseServerUrl, xlReleaseClientProxyUrl);
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
        @POST
        public FormValidation doValidateTemplate(
            @QueryParameter String credential, 
            @QueryParameter boolean overridingCredential, 
            @QueryParameter final String template,
            @AncestorInPath AbstractProject project, @AncestorInPath Item item)
        {
            if (item == null) {
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
            }
            item.checkPermission(Item.CONFIGURE);
            try {
                Credential overridingCredentialTemp = null;
                if ( overridingCredential ) 
                {
                    overridingCredentialTemp = RepositoryUtils.retrieveOverridingCredentialFromProject(project);
                }
                XLReleaseServerConnector xlreleaseServer = RepositoryUtils.getXLreleaseServer(credential, overridingCredentialTemp, project);

                this.release = getTemplate(xlreleaseServer, template);
                if ( this.release != null && !"folder".equals(release.getStatus()) ) 
                {
                    return ok("%s is valid template.", template);
                }

                return error("Template does not exist.");
            } 
            catch (UniformInterfaceException exp){
                return error("Template does not exist.");
            }
            catch (Exception exp) {
                exp.printStackTrace();
                return warning("Failed to communicate with XL Release server: "+exp.getMessage());
            }
        }

        private Release getTemplate(XLReleaseServerConnector xlreleaseServer, String value) {
            List<Release> candidates = xlreleaseServer.searchTemplates(value);
            for (Release candidate : candidates) {
                if (candidate.getTitle().equals(value)) {
                    candidate.setVariableValues(getVariables(xlreleaseServer, candidate));
                    return candidate;
                }
            }
            return null;
        }

        private Map<String, String> getVariables(XLReleaseServerConnector xlreleaseServer, Release release) {
            List<TemplateVariable> variables = xlreleaseServer.getVariables(release.getInternalId());
            return TemplateVariable.toMap(variables);
        }

        public AutoCompletionCandidates doAutoCompleteTemplate(@QueryParameter final String value, @AncestorInPath AbstractProject project, @AncestorInPath Item item) {
            AutoCompletionCandidates candidates = new AutoCompletionCandidates();
            if (item == null) {
                return candidates;
            }
            item.checkPermission(Item.CONFIGURE);
            XLReleaseNotifier xlReleaseNotifier = RepositoryUtils.retrieveXLreleaseNotifierFromProject(project);
            if (xlReleaseNotifier != null) {
                Credential overridingcredential = RepositoryUtils.retrieveOverridingCredentialFromProject(project);

                XLReleaseServerConnector xlReleaseServerConnector = RepositoryUtils.getXLreleaseServer(xlReleaseNotifier.credential, overridingcredential, project);
                List<Release> releases = xlReleaseServerConnector.searchTemplates(value);
                for (Release release:releases) {
                    candidates.add(release.getTitle());
                }

            }
            return candidates;
        }
        @POST
        public FormValidation doReloadTemplates(@QueryParameter String credential,@QueryParameter boolean overridingCredential, @AncestorInPath AbstractProject project, @AncestorInPath Item item)
        {
            if (item == null) {
                return FormValidation.ok();
            }
            item.checkPermission(Item.CONFIGURE);
            if (overridingCredential) {
                XLReleaseNotifier notifier = (XLReleaseNotifier) project.getPublishersList().get(this);
                this.lastOverridingCredential = notifier.overridingCredential;
            }
            else {
                this.lastOverridingCredential=null;
            }

            this.lastCredential=credential;
            return ok();
        }

        public List<Credential> getCredentials() {
            return credentials;
        }
        public void setCredentials(List<Credential> credentials) {
            this.credentials = credentials;
        }

        public String getXlReleaseServerUrl() {
            return xlReleaseServerUrl;
        }
        public void setXlReleaseServerUrl(String url) {
            this.xlReleaseServerUrl = url;
        }

        public String getXlReleaseClientProxyUrl() {
            return xlReleaseClientProxyUrl;
        }
        public void setXlReleaseClientProxyUrl(String url) {
            this.xlReleaseClientProxyUrl = url;
        }

        public ListBoxModel doFillCredentialItems(@AncestorInPath Item item) {
            if (item == null) {
                return ListBoxModels.emptyModel();
            }
            item.checkPermission(Item.CONFIGURE);
            ListBoxModel m = new ListBoxModel();
            m.add("-- Please Select --","");
            for (Credential c : credentials)
                m.add(c.name, c.name);
            return m;
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @AncestorInPath Item item) {
            if (item == null) {
                return ListBoxModels.emptyModel();
            }
            item.checkPermission(Item.CONFIGURE);
            List<StandardUsernamePasswordCredentials> creds = lookupCredentials(StandardUsernamePasswordCredentials.class, context,
                    ACL.SYSTEM,
                    HTTP_SCHEME, HTTPS_SCHEME);

            return new StandardUsernameListBoxModel().withAll(creds);
        }
        @POST
        public FormValidation doCheckCredential(@QueryParameter String credential, @AncestorInPath Item item) {
            if (item == null) {
                return FormValidation.ok();
            }
            item.checkPermission(Item.CONFIGURE);
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

        private XLReleaseServerConnector getXLReleaseServer(String credentialName, Credential overridingCredential) {
            return connectorHolder.getXLReleaseServerConnector(getCombinedCredential(credentialName, overridingCredential),xlrCredentialServerMap);
        }

        public Credential getCombinedCredential(String credentialName, Credential overridingCredential) {
            checkNotNull(credentialName);

            Credential credential = findCredential(credentialName);
            if ( credential == null )
            {
                throw new IllegalArgumentException("credential '"+credentialName+"' not found");
            }

            if ( null != overridingCredential && overridingCredential.getUsername() != null ) {
                credential = new Credential(credential.getName(), 
                    overridingCredential.getUsername(), 
                    overridingCredential.getPassword(), 
                    overridingCredential.getCredentialsId(), 
                    overridingCredential.isUseGlobalCredential(), 
                    overridingCredential.getSecondaryServerInfo());
            }
            return credential;
        }

        public Credential findCredential(String credentialName) {
            for (Credential credential : credentials) {
                if ( credentialName.equals(credential.getName()) ) {
                    return credential;
                }
            }
            throw new IllegalArgumentException(Messages.XLReleaseNotifier_credentialNotFound(credentialName));
        }

        public Map<String, String> getVariablesOf(final String credential, final Credential overridingCredential, final String template) {
            try
            {
                XLReleaseServerConnector xlreleaseServer = getXLReleaseServer(credential, overridingCredential);
                release = getTemplate(xlreleaseServer, template);

                if ( release != null ) {
                    return release.getVariableValues();
                }
            }
            catch (UniformInterfaceException ex)
            {
                // hack? clever? you decide!
                Map<String,String> errmsg = new HashMap<String,String>();
                errmsg.put("ERROR: "+ex.getMessage(), "");
                return errmsg;
            }
            return Collections.emptyMap();
        }

        public int getNumberOfVariables(@QueryParameter String credential, 
            @QueryParameter boolean overridingCredential, 
            @QueryParameter String username, 
            @QueryParameter String password, 
            @QueryParameter boolean useGlobalCredential,
            @QueryParameter String credentialsId,
            @QueryParameter String template) 
        {
            if (credential != null) {
                Credential overridingCredentialTemp=null;
                if(overridingCredential)
                    overridingCredentialTemp=new Credential(credential, username, Secret.fromString(password), credentialsId, useGlobalCredential, null);
                Map<String, String> variables = getVariablesOf(credential, overridingCredentialTemp, template);
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
