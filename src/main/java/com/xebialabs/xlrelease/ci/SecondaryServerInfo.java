package com.xebialabs.xlrelease.ci;

import org.kohsuke.stapler.DataBoundConstructor;
import com.google.common.base.Strings;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class SecondaryServerInfo extends AbstractDescribableImpl<SecondaryServerInfo> {

    public final String secondaryServerUrl;
    public final String secondaryProxyUrl;

    @DataBoundConstructor
    public SecondaryServerInfo(String secondaryServerUrl, String secondaryProxyUrl) {
        this.secondaryServerUrl = secondaryServerUrl;
        this.secondaryProxyUrl = secondaryProxyUrl;
    }

    public boolean showSecondaryServerSettings() {
        return !Strings.isNullOrEmpty(secondaryServerUrl) || !Strings.isNullOrEmpty(secondaryProxyUrl);
    }

    public String resolveServerUrl(String defaultUrl) {
        if (!Strings.isNullOrEmpty(secondaryServerUrl)) {
            return secondaryServerUrl;
        }
        return defaultUrl;
    }

    public String resolveProxyUrl(String defaultUrl) {
        if (!Strings.isNullOrEmpty(secondaryProxyUrl)) {
            return secondaryProxyUrl;
        }
        return defaultUrl;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final SecondaryServerInfo that = (SecondaryServerInfo) o;

        if (secondaryProxyUrl == null && that.secondaryProxyUrl != null) return false;
        if (secondaryProxyUrl != null && !secondaryProxyUrl.equals(that.secondaryProxyUrl)) return false;
        if (secondaryServerUrl == null && that.secondaryServerUrl != null) return false;
        if (secondaryServerUrl != null && !secondaryServerUrl.equals(that.secondaryServerUrl)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = secondaryServerUrl != null ? secondaryServerUrl.hashCode() : 0;
        result = 31 * result + (secondaryProxyUrl != null ? secondaryProxyUrl.hashCode() : 0);
        return result;
    }

    @Extension
    public static class SecondaryServerInfoDescriptor extends Descriptor<SecondaryServerInfo> {

        @Override
        public String getDisplayName() {
            return null;
        }
    }
}
