package com.xebialabs.xlrelease.ci;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.collect.Ordering;
import com.xebialabs.xlrelease.ci.XLReleaseNotifier.XLReleaseDescriptor;
import com.xebialabs.xlrelease.ci.Credential.SecondaryServerInfo;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerConnector;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Job;
import hudson.util.Secret;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.List;

public class RepositoryUtils {

    public static XLReleaseServerConnector getXLreleaseServer(String credentialName, Credential overridingCredential, Job<?,?> project) {
        Credential credential = findCredential(credentialName);
        if (null != credential && null != overridingCredential) {
            credential = retrieveOverridingCredential(credential, overridingCredential.getCredentialsId(),
                    credential.getName(), overridingCredential.getUsername(), overridingCredential.getPassword(),
                    overridingCredential.isUseGlobalCredential());
        }
        XLReleaseDescriptor descriptor = getXLreleaseDescriptor();
        return descriptor.getXLReleaseServer(credential, project);

    }

    public static Credential retrieveOverridingCredential(Credential credential, String credentialId, String name,
                                                          String username, Secret password, boolean useGlobalCredential) {
        XLReleaseDescriptor descriptor = getXLreleaseDescriptor();
        String secondaryProxyUrl = credential.resolveProxyUrl(descriptor.getXlReleaseClientProxyUrl());
        String secondaryServerUrl = credential.resolveServerUrl(descriptor.getXlReleaseServerUrl());
        SecondaryServerInfo serverInfo = new SecondaryServerInfo(secondaryServerUrl, secondaryProxyUrl);
        credential = new Credential(name, username, password, credentialId, useGlobalCredential, serverInfo);
        return credential;
    }

    public static XLReleaseServerConnector getXLreleaseServerFromCredentialsId(String serverCredentialName, String credentialId, Job<?,?> project) {
        Credential credential = findCredential(serverCredentialName);
        if (null != credential && null != credentialId) {
            StandardUsernamePasswordCredentials cred = Credential.lookupSystemCredentials(credentialId, project.getParent());
            if ( null == cred )
            {
                throw new IllegalArgumentException(Messages.XLReleaseNotifier_credentialNotFound(credentialId));
            }
            credential = retrieveOverridingCredential(credential, credentialId, credential.getName(),
                    cred.getUsername(), cred.getPassword(), true);
        }
        XLReleaseDescriptor descriptor = getXLreleaseDescriptor();
        return descriptor.getXLReleaseServer(credential, project);
    }

    private static List<Credential> getGlobalCredentials() {
        return getXLreleaseDescriptor().getCredentials();
    }

    public static Credential findCredential(String credentialName) {
        for (Credential credential : getGlobalCredentials()) {
            if (credentialName.equals(credential.getName())) {
                return credential;
            }
        }
        throw new IllegalArgumentException(Messages.XLReleaseNotifier_credentialNotFound(credentialName));
    }

    public static Credential retrieveOverridingCredentialFromProject(AbstractProject<?,?> project) {
        Credential overridingCredential = null;
        XLReleaseNotifier notifier = retrieveXLreleaseNotifierFromProject(project);
        if (null != notifier) {
            overridingCredential = notifier.getOverridingCredential();
            if (null != overridingCredential && StringUtils.isEmpty(overridingCredential.getUsername())
                    && null != overridingCredential.getCredentialsId()) {
                XLReleaseDescriptor descriptor = notifier.getDescriptor();
                String secondaryProxyUrl = overridingCredential.resolveProxyUrl(descriptor.getXlReleaseClientProxyUrl());
                String secondaryServerUrl = overridingCredential.resolveServerUrl(descriptor.getXlReleaseServerUrl());
                SecondaryServerInfo serverInfo = new SecondaryServerInfo(secondaryServerUrl, secondaryProxyUrl);

                StandardUsernamePasswordCredentials cred = Credential.lookupSystemCredentials(overridingCredential.getCredentialsId(), project.getParent());
                if (null != cred) {
                    overridingCredential = new Credential(overridingCredential.getName(), cred.getUsername(), cred.getPassword(),
                            overridingCredential.getCredentialsId(), false, serverInfo );
                }
            }
        }
        return overridingCredential;
    }

    public static XLReleaseNotifier retrieveXLreleaseNotifierFromProject(AbstractProject<?,?> project)
    {
        XLReleaseNotifier notifier = null;
        XLReleaseDescriptor descriptor = getXLreleaseDescriptor();
        if ( null != project )
        {
            notifier = (XLReleaseNotifier) project.getPublishersList().get(descriptor);
        }
        return notifier;
    }

    public static XLReleaseDescriptor getXLreleaseDescriptor(){
        return (XLReleaseDescriptor) Hudson.getInstance().getDescriptor(XLReleaseNotifier.class);
    }
}
