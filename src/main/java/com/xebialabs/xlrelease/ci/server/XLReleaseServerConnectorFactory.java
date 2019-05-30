package com.xebialabs.xlrelease.ci.server;

import com.xebialabs.xlrelease.ci.Credential;

import java.util.Map;
import java.util.logging.Logger;

import static com.google.common.collect.Maps.newHashMap;

public class XLReleaseServerConnectorFactory {
    private final static Logger LOGGER = Logger.getLogger(XLReleaseServerConnectorFactory.class.getName());
    private String serverUrl;
    private String proxyUrl;
    private transient static XLReleaseServerFactory xlReleaseServerFactory = new XLReleaseServerFactory();


    public void load(String serverUrl, String proxyUrl) {
        this.serverUrl = serverUrl;
        this.proxyUrl = proxyUrl;
    }

    public XLReleaseServerConnector getXLReleaseServerConnector(Credential credential, Map<String, XLReleaseServerConnector> credentialServerMap) {
        XLReleaseServerConnector xlReleaseServer = null;
        if (null != credential) {

            XLReleaseServerConnector xlReleaseServerConnectorServerRef = credentialServerMap.get(credential.getKey());

            if (null != xlReleaseServerConnectorServerRef) {
                xlReleaseServer = xlReleaseServerConnectorServerRef;
            }

            if (null == xlReleaseServer) {
                synchronized (this) {
                    xlReleaseServer = xlReleaseServerFactory.newInstance(serverUrl, proxyUrl, credential);
                    credentialServerMap.put(credential.getKey(), xlReleaseServerFactory.newInstance(serverUrl, proxyUrl,
                            credential.getUsername(), credential.getPassword() != null ? credential.getPassword().getPlainText() : ""));
                }
            }
        }
        // no credential - no server
        return xlReleaseServer;

    }

    public static void setXLReleaseServerFactory(XLReleaseServerFactory xlReleaseServerFactory) {
        XLReleaseServerConnectorFactory.xlReleaseServerFactory = xlReleaseServerFactory;
    }
}
