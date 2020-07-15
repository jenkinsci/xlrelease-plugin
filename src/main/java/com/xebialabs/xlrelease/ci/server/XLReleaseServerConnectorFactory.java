package com.xebialabs.xlrelease.ci.server;

import com.xebialabs.xlrelease.ci.Credential;

import java.util.Map;
import java.util.logging.Logger;

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
        	
        	//XLINT-458 and 706, the key used to look up the XLReleaseServerConnector is not unique
        	//For an example: XLR servers traditionally have username:password setup as admin:admin
        	//the key was set as username + ":" + password.getPlainText() + "@" + name + ":" + credentialsId + ":"
        	//will return the first stored server connector when the key is matched. Without considering the URL, it ends up job to be executed in wrong server.
        	//added server URL as part of key to distinguish them.
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
