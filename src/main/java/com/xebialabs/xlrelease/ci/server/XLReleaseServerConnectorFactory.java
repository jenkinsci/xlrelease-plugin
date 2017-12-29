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

    private String xlReleaseServerUrl;
    private String xlReleaseClientProxyUrl;
    private List<Credential> credentials = newArrayList();

    private final transient static Map<String,XLReleaseServerConnector> credentialServerMap = new WeakHashMap<>();
    private transient static XLReleaseServerFactory xlReleaseServerFactory = new XLReleaseServerFactory();

    public XLReleaseServerConnectorFactory(){

    }

    public void load(String xlReleaseServerUrl, String xlReleaseClientProxyUrl, List<Credential> credentials){
        this.xlReleaseServerUrl=xlReleaseServerUrl;
        this.xlReleaseClientProxyUrl=xlReleaseClientProxyUrl;
        this.credentials=credentials;
        initMap();
    }

    private void initMap() {
        for (Credential credential : credentials) {
            getXLReleaseServerConnector(credential);
        }
    }

    public XLReleaseServerConnector getXLReleaseServerConnector(Credential credential) {
        XLReleaseServerConnector xlReleaseServerConnector = null;
        if (null != credential) {
            xlReleaseServerConnector = credentialServerMap.get(credential.getKey());
            if (null == xlReleaseServerConnector) {
                synchronized (this) {
                    xlReleaseServerConnector = xlReleaseServerFactory.newInstance(xlReleaseServerUrl, xlReleaseClientProxyUrl, credential);
                    credentialServerMap.put(credential.getKey(), xlReleaseServerConnector);
                }
            }
        }
        return xlReleaseServerConnector;
    }



    public static void setXLReleaseServerFactory(XLReleaseServerFactory xlReleaseServerFactory){
        XLReleaseServerConnectorFactory.xlReleaseServerFactory=xlReleaseServerFactory;
    }

}
