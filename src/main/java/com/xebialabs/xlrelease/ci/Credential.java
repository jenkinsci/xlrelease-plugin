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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.SchemeRequirement;
import hudson.model.Project;
import hudson.security.ACL;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import com.google.common.base.Function;
import com.google.common.base.Strings;

import com.xebialabs.xlrelease.ci.server.XLReleaseServerConnector;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerFactory;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;
import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;

public class Credential extends AbstractDescribableImpl<Credential> {

    private static final SchemeRequirement HTTP_SCHEME = new SchemeRequirement("http");
    private static final SchemeRequirement HTTPS_SCHEME = new SchemeRequirement("https");

    public static final Function<Credential, String> CREDENTIAL_INDEX = new Function<Credential, String>() {
        public String apply(Credential input) {
            return input.getName();
        }
    };
    public final String name;
    public final String username;
    public final Secret password;
    public final String credentialsId;
    private final SecondaryServerInfo secondaryServerInfo;
    public final boolean useGlobalCredential;

    @DataBoundConstructor
    public Credential(String name, String username, Secret password, String credentialsId, boolean useGlobalCredential , SecondaryServerInfo secondaryServerInfo) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.secondaryServerInfo = secondaryServerInfo;
        this.credentialsId = credentialsId;
        this.useGlobalCredential = useGlobalCredential;
    }

    public String getName() {
        return name;
    }

    public  String getSecondaryServerUrl() {
        if (secondaryServerInfo != null) {
            return secondaryServerInfo.secondaryServerUrl;
        }
        return null;
    }

    public  String getSecondaryProxyUrl() {
        if (secondaryServerInfo != null) {
            return secondaryServerInfo.secondaryProxyUrl;
        }
        return null;
    }

    public String resolveServerUrl(String defaultUrl) {
        if (secondaryServerInfo != null) {
            return secondaryServerInfo.resolveServerUrl(defaultUrl);
        }
        return defaultUrl;
    }

    public String resolveProxyUrl(String defaultUrl) {
        if (secondaryServerInfo != null) {
            return secondaryServerInfo.resolveProxyUrl(defaultUrl);
        }
        return defaultUrl;
    }

    public boolean showSecondaryServerSettings() {
        return secondaryServerInfo!= null && secondaryServerInfo.showSecondaryServerSettings();
    }

    public boolean showGolbalCredentials() {
        return useGlobalCredential;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Credential that = (Credential) o;

        if (!name.equals(that.name)) return false;
        if (!password.equals(that.password)) return false;
        if (secondaryServerInfo == null && that.secondaryServerInfo != null) return false;
        if (secondaryServerInfo != null && !secondaryServerInfo.equals(that.secondaryServerInfo)) return false;
        if (useGlobalCredential && that.useGlobalCredential && !credentialsId.equals(that.credentialsId)) return false;
        return username.equals(that.username);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (useGlobalCredential && credentialsId != null ? credentialsId.hashCode() :0);
        result = 31 * result + (secondaryServerInfo != null ? secondaryServerInfo.hashCode() : 0);
        return result;
    }

    public static StandardUsernamePasswordCredentials lookupSystemCredentials(String credentialsId) {
        return CredentialsMatchers.firstOrNull(
                lookupCredentials(StandardUsernamePasswordCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
                        HTTP_SCHEME, HTTPS_SCHEME),
                CredentialsMatchers.withId(credentialsId)
        );
    }

    @Extension
    public static final class CredentialDescriptor extends Descriptor<Credential> {
        @Override
        public String getDisplayName() {
            return "Credential";
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

        public FormValidation doCheckSecondaryServerUrl(@QueryParameter String secondaryServerUrl) {
            return validateOptionalUrl(secondaryServerUrl);
        }

        public FormValidation doCheckSecondaryProxyUrl(@QueryParameter String secondaryProxyUrl) {
            return validateOptionalUrl(secondaryProxyUrl);
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Project context) {
            // TODO: also add requirement on host derived from URL ?
            List<StandardUsernamePasswordCredentials> creds = lookupCredentials(StandardUsernamePasswordCredentials.class, context,
                    ACL.SYSTEM,
                    HTTP_SCHEME, HTTPS_SCHEME);

            return new StandardUsernameListBoxModel().withAll(creds);
        }

        public FormValidation doValidateUserNamePassword(@QueryParameter String xlReleaseServerUrl, @QueryParameter String xlReleaseClientProxyUrl, @QueryParameter String username,
                                         @QueryParameter Secret password, @QueryParameter String secondaryServerUrl, @QueryParameter String secondaryProxyUrl) throws IOException {
            try {
                String serverUrl = Strings.isNullOrEmpty(secondaryServerUrl) ? xlReleaseServerUrl : secondaryServerUrl;
                String proxyUrl = Strings.isNullOrEmpty(secondaryProxyUrl) ? xlReleaseClientProxyUrl : secondaryProxyUrl;

                if (Strings.isNullOrEmpty(serverUrl)) {
                    return FormValidation.error("No server URL specified");
                }

                XLReleaseServerConnector xlReleaseServerConnector = validateConnection(serverUrl,proxyUrl,username,password.getPlainText());
                return FormValidation.ok("Your XL Release instance [%s] version %s is alive, and your credentials are valid!", serverUrl, xlReleaseServerConnector.getVersion());
            } catch(IllegalStateException e) {
                return FormValidation.error(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                return FormValidation.error("XL Release configuration is not valid! %s", e.getMessage());
            }
        }

        public FormValidation doValidateCredential(@QueryParameter String xlReleaseServerUrl, @QueryParameter String xlReleaseClientProxyUrl, @QueryParameter String secondaryServerUrl, @QueryParameter String secondaryProxyUrl, @QueryParameter String credentialsId) throws IOException {
            try {
                String serverUrl = Strings.isNullOrEmpty(secondaryServerUrl) ? xlReleaseServerUrl : secondaryServerUrl;
                String proxyUrl = Strings.isNullOrEmpty(secondaryProxyUrl) ? xlReleaseClientProxyUrl : secondaryProxyUrl;

                if (Strings.isNullOrEmpty(credentialsId)) {
                    return FormValidation.error("No credentials specified");
                }
                StandardUsernamePasswordCredentials credentials = lookupSystemCredentials(credentialsId);
                if (credentials == null) {
                    return FormValidation.error(String.format("Could not find credential with id '%s'", credentialsId));
                }
                if (Strings.isNullOrEmpty(serverUrl)) {
                    return FormValidation.error("No server URL specified");
                }

                XLReleaseServerConnector xlReleaseServerConnector = validateConnection(serverUrl,proxyUrl,credentials.getUsername(), credentials.getPassword().getPlainText());
                return FormValidation.ok("Your XL Release instance [%s] version %s is alive, and your credentials are valid!", serverUrl, xlReleaseServerConnector.getVersion());
            } catch(IllegalStateException e) {
                return FormValidation.error(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                return FormValidation.error("XL Release configuration is not valid! %s", e.getMessage());
            }
        }

        private XLReleaseServerConnector validateConnection(String serverUrl,String proxyUrl,String username,String password) throws Exception {
            XLReleaseServerFactory factory = new XLReleaseServerFactory();
            XLReleaseServerConnector xlReleaseServerConnector = factory.newInstance(serverUrl, proxyUrl, username, password);
            xlReleaseServerConnector.testConnection(); // throws IllegalStateException if creds invalid
            return xlReleaseServerConnector;
        }
    }

}
