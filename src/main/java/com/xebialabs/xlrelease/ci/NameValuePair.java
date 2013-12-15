package com.xebialabs.xlrelease.ci;


import java.util.Collection;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.xebialabs.xlrelease.ci.util.ListBoxModels;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

import static com.xebialabs.xlrelease.ci.XLReleaseNotifier.XLReleaseDescriptor;

public class NameValuePair extends AbstractDescribableImpl<NameValuePair> {

    public String propertyName;
    public String propertyValue;

    @DataBoundConstructor
    public NameValuePair(String propertyName, String propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    @Extension
    public static final class NameValuePairDescriptor extends Descriptor<NameValuePair> {
        @Override
        public String getDisplayName() {
            return "NameValuePair";
        }

        public ListBoxModel doFillPropertyNameItems(
                @QueryParameter @RelativePath(value = "..") String template) {
            Collection<TemplateVariable> properties = getXLReleaseDescriptor().getVariablesOf(template);
            Collection<String> keys = CollectionUtils.collect(properties, new Transformer() {
                @Override
                public Object transform(final Object input) {
                    return ((TemplateVariable)input).getKey();
                }
            });
            return ListBoxModels.of(keys);
        }

        protected XLReleaseDescriptor getXLReleaseDescriptor() {
            return (XLReleaseDescriptor) Jenkins.getInstance().getDescriptorOrDie(XLReleaseNotifier.class);
        }

    }
}
