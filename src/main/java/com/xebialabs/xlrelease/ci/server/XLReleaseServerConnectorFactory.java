package com.xebialabs.xlrelease.ci.server;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.xebialabs.xlrelease.ci.Credential;
import com.xebialabs.xlrelease.ci.Messages;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

public class XLReleaseServerConnectorFactory {

    private String serverUrl;
    private String proxyUrl;
    
    private transient static XLReleaseServerFactory xlReleaseServerFactory = new XLReleaseServerFactory();

    public XLReleaseServerConnectorFactory(){}

    public void load(String serverUrl, String proxyUrl)
    {
        this.serverUrl = serverUrl;
        this.proxyUrl = proxyUrl;
    }

    public XLReleaseServerConnector getXLReleaseServerConnector(Credential credential) {
        if ( credential == null ) return null;
        return xlReleaseServerFactory.newInstance(serverUrl, proxyUrl, credential);
    }

    public static void setXLReleaseServerFactory(XLReleaseServerFactory xlReleaseServerFactory){
        XLReleaseServerConnectorFactory.xlReleaseServerFactory=xlReleaseServerFactory;
    }
}
