package com.xebialabs.xlrelease.ci.workflow;

import com.google.inject.Inject;
import com.xebialabs.xlrelease.ci.*;
import com.xebialabs.xlrelease.ci.Messages;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerConnector;
import com.xebialabs.xlrelease.ci.util.JenkinsReleaseListener;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import org.kohsuke.stapler.AncestorInPath;
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
    public String overrideCredentialId;

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
    public void setOverrideCredentialId(String overrideCredentialId) {
        this.overrideCredentialId = overrideCredentialId;
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

        public AutoCompletionCandidates doAutoCompleteTemplate(@QueryParameter final String value, @AncestorInPath AbstractProject project) {
            return getXLReleaseDescriptor().doAutoCompleteTemplate(value, project);
        }

        public FormValidation doValidateTemplate(@QueryParameter String serverCredentials, @QueryParameter boolean overridingCredential, @QueryParameter final String template, @AncestorInPath AbstractProject project) {
            return getXLReleaseDescriptor().doValidateTemplate(serverCredentials, overridingCredential, template, project);
        }

        public ListBoxModel doFillServerCredentialsItems() {
            return getXLReleaseDescriptor().doFillCredentialItems();
        }

        public Map<String, String> getVariablesOf(final String credential, final String template) {
            return getXLReleaseDescriptor().getVariablesOf(credential, null, template);
        }

        public FormValidation doCheckServerCredentials(@QueryParameter String serverCredentials) {
            return getXLReleaseDescriptor().doCheckCredential(serverCredentials);
        }

        public int getNumberOfVariables(@QueryParameter String serverCredentials,@QueryParameter boolean overridingCredential, @QueryParameter String username
                , @QueryParameter String password, @QueryParameter boolean useGlobalCredential, @QueryParameter String credentialsId, @QueryParameter String template) {
            return getXLReleaseDescriptor().getNumberOfVariables(serverCredentials, overridingCredential, username, password, useGlobalCredential, credentialsId ,template);
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

        @StepContextParameter
        private transient Run<?,?> run;

        @Override
        protected Void run() throws Exception {
            if (StringUtils.isNotEmpty(step.version)) {
                JenkinsReleaseListener deploymentListener = new JenkinsReleaseListener(listener);
                deploymentListener.info(Messages._XLReleaseStep_versionDeprecated());
            }
            Job<?,?> job = this.run.getParent();
            //Credential
            XLReleaseNotifier releaseNotifier = new XLReleaseNotifier(step.serverCredentials, step.template, (step.releaseTitle != null) ? step.releaseTitle : step.version, step.variables, step.startRelease, getOverridingCredential());
            XLReleaseServerConnector xlReleaseServerConnector = RepositoryUtils.getXLreleaseServerFromCredentialsId(
                    step.serverCredentials, step.overrideCredentialId, job);
            releaseNotifier.executeRelease(envVars, listener,xlReleaseServerConnector);
            return null;
        }

        private Credential getOverridingCredential() {
            if (StringUtils.isNotEmpty(step.overrideCredentialId)) {
                Credential credential =  new Credential("Overriding", "", Secret.fromString(""), step.overrideCredentialId, true, null);
                return  credential;
            } else
                return null;

    }
}}
