/**
 * Copyright (c) 2013, XebiaLabs B.V., All rights reserved.
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import com.google.common.base.Strings;

import com.xebialabs.xlrelease.ci.server.XLReleaseServer;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerFactory;
import com.xebialabs.xlrelease.ci.util.JenkinsReleaseListener;
import com.xebialabs.xlrelease.ci.util.ReleaseFullView;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;
import static hudson.util.FormValidation.warning;

public class XLReleaseNotifier extends Notifier {

    public final String credential;

    public final String template;
    public final String version;

    public final JenkinsCreateRelease createRelease;
    public final boolean startRelease;


    @DataBoundConstructor
    public XLReleaseNotifier(String credential, String template, String version, JenkinsCreateRelease createRelease, boolean startRelease) {
        this.credential = credential;
        this.template = template;
        this.version = version;
        this.createRelease = createRelease;
        this.startRelease = startRelease;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        final JenkinsReleaseListener deploymentListener = new JenkinsReleaseListener(listener);

        final EnvVars envVars = build.getEnvironment(listener);
        String resolvedTemplate = envVars.expand(template);
        String resolvedVersion = envVars.expand(version);

        // createRelease
        ReleaseFullView releaseFullView = null;
        if (createRelease != null || startRelease)
            releaseFullView = createRelease(resolvedTemplate,resolvedVersion, deploymentListener);

        // startRelease
        if (startRelease)
            startRelease(releaseFullView, resolvedTemplate,resolvedVersion, deploymentListener);

        return true;
    }

    private ReleaseFullView createRelease(final String resolvedTemplate, final String resolvedVersion, final JenkinsReleaseListener deploymentListener) {
        deploymentListener.info(Messages.XLReleaseNotifier_createRelease(resolvedTemplate, resolvedVersion));

        // create a new release instance
        ReleaseFullView releaseFullView = getXLReleaseServer().createRelease(resolvedTemplate, resolvedVersion, createRelease);
        return releaseFullView;

    }

    private void startRelease(final ReleaseFullView releaseFullView, final String resolvedTemplate, final String resolvedVersion, final JenkinsReleaseListener deploymentListener) {
        deploymentListener.info(Messages.XLReleaseNotifier_startRelease(resolvedTemplate, resolvedVersion));

        // start the release
        getXLReleaseServer().startRelease(releaseFullView.getId());
    }


    private XLReleaseServer getXLReleaseServer() {
        return getDescriptor().getXLReleaseServer(credential);
    }

    @Override
    public XLReleaseDescriptor getDescriptor() {
        return (XLReleaseDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class XLReleaseDescriptor extends BuildStepDescriptor<Publisher> {

        // ************ SERIALIZED GLOBAL PROPERTIES *********** //

        private String xlReleaseServerUrl;

        private String xlReleaseClientProxyUrl;

        private List<Credential> credentials = newArrayList();

        // ************ OTHER NON-SERIALIZABLE PROPERTIES *********** //

        private final transient Map<String,XLReleaseServer> credentialServerMap = newHashMap();

        private ReleaseFullView releaseFullView;

        public XLReleaseDescriptor() {
            load();  //deserialize from xml
            mapCredentialsByName();
        }

        private void mapCredentialsByName() {
            for (Credential credential : credentials) {
                String serverUrl = credential.resolveServerUrl(xlReleaseServerUrl);
                String proxyUrl = credential.resolveProxyUrl(xlReleaseClientProxyUrl);

                credentialServerMap.put(credential.name,
                        XLReleaseServerFactory.newInstance(serverUrl, proxyUrl, credential.username, credential.password.getPlainText()));
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

        public FormValidation doCheckXLReleaseServerUrl(@QueryParameter String xlReleaseServerUrl) {
            if (Strings.isNullOrEmpty(xlReleaseServerUrl)) {
                return error("Url required.");
            }
            return validateOptionalUrl(xlReleaseServerUrl);
        }

        public FormValidation doCheckXLReleaseClientProxyUrl(@QueryParameter String xlReleaseClientProxyUrl) {
            return validateOptionalUrl(xlReleaseClientProxyUrl);
        }

        public FormValidation doCheckTemplate(@QueryParameter String credential, @QueryParameter final String value, @AncestorInPath AbstractProject project) {
            if ("Templates".equals(value))
                return ok("Fill in the template name, eg Monthly release");


            List<ReleaseFullView> candidates = getXLReleaseServer(credential).searchTemplates(value);
            for (ReleaseFullView candidate : candidates) {
                if (candidate.getTitle().equals(value)) {
                    this.releaseFullView = candidate;
                    return ok();
                }
            }
            if (!candidates.isEmpty()) {
                return warning("Template doesn't exist. Did you mean to type one of the following: %s?", candidates);
            }
            return warning("Template does not exist.");
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
            for (Credential c : credentials)
                m.add(c.name, c.name);
            return m;
        }

        public FormValidation doCheckCredential(@QueryParameter String credential) {
            return warning("Changing credentials may unintentionally change your deployables' types - check the definitions afterward");
        }

        @Override
        public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }





        private XLReleaseServer getXLReleaseServer(String credential) {
            checkNotNull(credential);
            return credentialServerMap.get(credential);
        }


        private Credential getDefaultCredential() {
            if (credentials.isEmpty())
                throw new RuntimeException("No credentials defined in the system configuration");
            return credentials.iterator().next();
        }

        public Collection<TemplateVariable> getVariablesOf(final String template) {
            return releaseFullView.getVariables();
        }
    }
}
