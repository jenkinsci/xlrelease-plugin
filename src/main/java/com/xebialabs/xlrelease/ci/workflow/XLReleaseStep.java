package com.xebialabs.xlrelease.ci.workflow;

import com.google.inject.Inject;
import com.sun.istack.NotNull;
import com.xebialabs.xlrelease.ci.NameValuePair;
import com.xebialabs.xlrelease.ci.XLReleaseNotifier;
import com.xebialabs.xlrelease.ci.util.Release;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.xebialabs.xlrelease.ci.XLReleaseNotifier.XLReleaseDescriptor;

public class XLReleaseStep extends AbstractStepImpl {

    public String credential = null;
    public String template = null;
    public String version = null;
    public List<NameValuePair> variables = null;
    public boolean startRelease = false;

    @DataBoundConstructor
    public XLReleaseStep(String credential, String template, String version, List<NameValuePair> variables, boolean startRelease) {
        this.credential = credential;
        this.template = template;
        this.version = version;
        this.variables = variables;
        this.startRelease = startRelease;
    }

    @DataBoundSetter
    public void setCredential(@NotNull String credential) {
        this.credential = Util.fixEmptyAndTrim(credential);
    }

    @DataBoundSetter
    public void setTemplate(String template) {
        this.template = Util.fixEmptyAndTrim(template);
    }

    @DataBoundSetter
    public void setVersion(String version) {
        this.version = Util.fixEmptyAndTrim(version);
    }

    @DataBoundSetter
    public void setVariables(List<NameValuePair> variables) {
        this.variables = variables;
    }

    @DataBoundSetter
    public void setStartRelease(boolean startRelease) {
        this.startRelease = startRelease;
    }

    @Override
    public XLReleaseStepDescriptor getDescriptor() {
        return (XLReleaseStepDescriptor) super.getDescriptor();
    }

    @Extension
    public static final class XLReleaseStepDescriptor extends AbstractStepDescriptorImpl {

        private final XLReleaseDescriptor descriptor;

        public XLReleaseStepDescriptor() {
            super(XLReleaseExecution.class);
            descriptor = new XLReleaseDescriptor();
        }

        @Override
        public String getFunctionName() {
            return "xlrCreateRelease";
        }

        @Override
        public String getDisplayName() {
            return "Invoke a XLR Release";
        }

        public ListBoxModel doFillTemplateItems(@QueryParameter String credential) {
            return getXLReleaseDescriptor().doFillTemplateItems(credential);
        }

        public ListBoxModel doFillCredentialItems() {
            return getXLReleaseDescriptor().doFillCredentialItems();
        }


        public Map<String, String> getVariablesOf(final String credential, final String template) {
            return getXLReleaseDescriptor().getVariablesOf(credential, template);
        }

        public FormValidation doCheckCredential(@QueryParameter String credential) {
            if (StringUtils.isEmpty(credential)) {
                return FormValidation.error("Please select a valid credential");
            }
            return null;
        }

        public int getNumberOfVariables(@QueryParameter String credential, @QueryParameter String template) {
            return getXLReleaseDescriptor().getNumberOfVariables(credential, template);
        }

        private XLReleaseDescriptor getXLReleaseDescriptor() {
            descriptor.load();
            return descriptor;
        }

    }

    public static final class XLReleaseExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

        @Inject
        private transient XLReleaseStep step;

        @StepContextParameter
        private transient EnvVars envVars;

        @StepContextParameter
        private transient TaskListener listener;

        @Override
        protected Void run() throws Exception {
            XLReleaseNotifier releaseNotifier = new XLReleaseNotifier(step.credential, step.template, step.version, step.variables, step.startRelease);
            String resolvedVersion = envVars.expand(step.version);
            List<NameValuePair> resolvedVariables = new ArrayList<NameValuePair>();
            if (CollectionUtils.isNotEmpty(step.variables)) {
                for (NameValuePair nameValuePair : step.variables) {
                    resolvedVariables.add(new NameValuePair(nameValuePair.propertyName, envVars.expand(nameValuePair.propertyValue)));
                }
            }

            Release release = releaseNotifier.createRelease(step.template, resolvedVersion, resolvedVariables);
            listener.getLogger().println("Create a new release from template " + step.template + " with name " + resolvedVersion + " and ID " + release.getId());

            if (step.startRelease) {
                releaseNotifier.startRelease(release);
                listener.getLogger().println("Start a release from template " + step.template + " with name " + resolvedVersion + " and ID " + release.getId());
            }
            return null;
        }
    }
}
