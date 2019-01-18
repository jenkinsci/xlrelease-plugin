package com.xebialabs.xlrelease.ci.workflow;

import com.google.inject.Inject;
import com.xebialabs.xlrelease.ci.Messages;
import com.xebialabs.xlrelease.ci.NameValuePair;
import com.xebialabs.xlrelease.ci.XLReleaseNotifier;
import com.xebialabs.xlrelease.ci.util.JenkinsReleaseListener;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.AutoCompletionCandidates;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.List;
import java.util.Map;

import static com.xebialabs.xlrelease.ci.XLReleaseNotifier.XLReleaseDescriptor;

public class XLReleaseStep extends AbstractStepImpl {

    public final String serverCredentials;
    public final String template;
    public final String releaseTitle;
    @Deprecated
    public String version;
    public List<NameValuePair> variables = null;
    public boolean startRelease = false;

    @DataBoundConstructor
    public XLReleaseStep(String serverCredentials, String template, String version, List<NameValuePair> variables, boolean startRelease, String releaseTitle) {
        this.serverCredentials = serverCredentials;
        this.template = template;
        this.version = version;
        this.variables = variables;
        this.startRelease = startRelease;
        this.releaseTitle = releaseTitle;
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
            return "Create and invoke a XLR release";
        }

        public AutoCompletionCandidates doAutoCompleteTemplate(@QueryParameter final String value) {
            return getXLReleaseDescriptor().doAutoCompleteTemplate(value);
        }

        public FormValidation doValidateTemplate(@QueryParameter String serverCredentials, @QueryParameter final String template) {
            return getXLReleaseDescriptor().doValidateTemplate(serverCredentials, template);
        }

        public ListBoxModel doFillServerCredentialsItems() {
            return getXLReleaseDescriptor().doFillCredentialItems();
        }


        public Map<String, String> getVariablesOf(final String credential, final String template) {
            return getXLReleaseDescriptor().getVariablesOf(credential, template);
        }

        public FormValidation doCheckServerCredentials(@QueryParameter String serverCredentials) {
            return getXLReleaseDescriptor().doCheckCredential(serverCredentials);
        }

        public int getNumberOfVariables(@QueryParameter String serverCredentials, @QueryParameter String template) {
            return getXLReleaseDescriptor().getNumberOfVariables(serverCredentials, template);
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
            if (StringUtils.isNotEmpty(step.version)) {
                JenkinsReleaseListener deploymentListener = new JenkinsReleaseListener(listener);
                deploymentListener.info(Messages._XLReleaseStep_versionDeprecated());
            }
            XLReleaseNotifier releaseNotifier = new XLReleaseNotifier(step.serverCredentials, step.template, (step.releaseTitle != null) ? step.releaseTitle : step.version, step.variables, step.startRelease);
            releaseNotifier.executeRelease(envVars, listener);
            return null;
        }
    }
}
