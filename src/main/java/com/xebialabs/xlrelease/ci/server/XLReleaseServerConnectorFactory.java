package com.xebialabs.xlrelease.ci.server;

import com.xebialabs.xlrelease.ci.Credential;
import com.xebialabs.xlrelease.ci.XLReleaseNotifier.XLReleaseDescriptor;

import java.util.Map;
import java.util.logging.Logger;

import static com.google.common.collect.Maps.newHashMap;

public class XLReleaseServerConnectorFactory {
    private String serverUrl;
    private String proxyUrl;
    private transient static XLReleaseServerFactory xlReleaseServerFactory = new XLReleaseServerFactory();
    private final static Logger LOGGER = Logger.getLogger(XLReleaseServerConnectorFactory.class.getName());


    public void load(String serverUrl, String proxyUrl) {
        this.serverUrl = serverUrl;
        this.proxyUrl = proxyUrl;
    }

    public XLReleaseServerConnector getXLReleaseServerConnector(Credential credential, Map<String, XLReleaseServerConnector> credentialServerMap) {
        XLReleaseServerConnector xlReleaseServer = null;
        
        if (null != credential) {
        	String _serverUrl = credential.showSecondaryServerSettings()?credential.getSecondaryServerUrl():serverUrl;
        	String _proxyUrl = credential.showSecondaryServerSettings()?credential.getSecondaryProxyUrl():proxyUrl;
        	String credKey = credential.getKey()+":"+_serverUrl;

            XLReleaseServerConnector xlReleaseServerConnectorServerRef = credentialServerMap.get(credKey);

            if (null != xlReleaseServerConnectorServerRef) {
                xlReleaseServer = xlReleaseServerConnectorServerRef;
                LOGGER.info("XLReleaseServerConnector found in the HashMap using key for username=" + credential.getUsername()+ ", server url=" + _serverUrl);
            }
            if(credential.showSecondaryServerSettings()) {
            	 LOGGER.info("non-default server URL=" + credential.getSecondaryServerUrl());
            	 LOGGER.info("non-default proxy server URL=" + credential.getSecondaryProxyUrl());
            }
            else {
		            LOGGER.info("default server URL=" + serverUrl);
		            LOGGER.info("default proxy server URL=" + proxyUrl);
            }

            if (null == xlReleaseServer) {
                synchronized (this) {
                	LOGGER.info("XLReleaseServerConnector not found in the HashMap....create a new instance using key for username=" + credential.getUsername()+ ", server url=" + _serverUrl);
                    xlReleaseServer = xlReleaseServerFactory.newInstance(_serverUrl, _proxyUrl, credential);
                    credentialServerMap.put(credKey, xlReleaseServer);
                }
            }
        }
        // no credential - no server
        return xlReleaseServer;

    }

}
