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

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import com.google.common.base.Function;
import com.google.common.base.Strings;

import com.xebialabs.xlrelease.ci.server.XLReleaseServer;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerFactory;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;

import static hudson.util.FormValidation.error;
import static hudson.util.FormValidation.ok;

public class Credential extends AbstractDescribableImpl<Credential> {

    public static final Function<Credential, String> CREDENTIAL_INDEX = new Function<Credential, String>() {
        public String apply(Credential input) {
            return input.getName();
        }
    };
    public final String name;
    public final String username;
    public final Secret password;
    private final SecondaryServerInfo secondaryServerInfo;

    @DataBoundConstructor
    public Credential(String name, String username, Secret password, SecondaryServerInfo secondaryServerInfo) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.secondaryServerInfo = secondaryServerInfo;
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

        return username.equals(that.username);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + username.hashCode();
        result = 31 * result + password.hashCode();
        result = 31 * result + (secondaryServerInfo != null ? secondaryServerInfo.hashCode() : 0);
        return result;
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


        public FormValidation doValidate(@QueryParameter String xlReleaseServerUrl, @QueryParameter String xlReleaseClientProxyUrl, @QueryParameter String username,
                                         @QueryParameter Secret password, @QueryParameter String secondaryServerUrl, @QueryParameter String secondaryProxyUrl) throws IOException {
            try {
                String serverUrl = Strings.isNullOrEmpty(secondaryServerUrl) ? xlReleaseServerUrl : secondaryServerUrl;
                String proxyUrl = Strings.isNullOrEmpty(secondaryProxyUrl) ? xlReleaseClientProxyUrl : secondaryProxyUrl;

                XLReleaseServer xlReleaseServer = XLReleaseServerFactory.newInstance(serverUrl, proxyUrl, username, password.getPlainText());
                xlReleaseServer.newCommunicator(); // throws IllegalStateException if creds invalid
                return FormValidation.ok("Your XL Release instance [%s] is alive, and your credentials are valid!", xlReleaseServer.getVersion());
            } catch(IllegalStateException e) {
                return FormValidation.error(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                return FormValidation.error("XL Release configuration is not valid! %s", e.getMessage());
            }
        }
    }

}
