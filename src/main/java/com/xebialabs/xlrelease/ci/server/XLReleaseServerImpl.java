package com.xebialabs.xlrelease.ci.server;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class XLReleaseServerImpl implements XLReleaseServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(XLReleaseServerImpl.class);

    private XLReleaseDescriptorRegistry descriptorRegistry;
    private String user;
    private String password;
    private String proxyUrl;
    private String serverUrl;

    XLReleaseServerImpl(String serverUrl, String proxyUrl, String username, String password) {
        this.user=username;
        this.password=password;
        this.proxyUrl=proxyUrl;
        this.serverUrl=serverUrl;
    }


    @Override
    public void newCommunicator() {
        // setup REST-Client
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.addFilter( new HTTPBasicAuthFilter(user, password) );
        WebResource service = client.resource( serverUrl);

        LoggerFactory.getLogger(this.getClass()).info("Check that XL Release is running");
        String xlrelease = service.path("releases").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class).toString();
        LoggerFactory.getLogger(this.getClass()).info(xlrelease + "\n");

    }

    @Override
    public Object getVersion() {
        return serverUrl;
    }

    @Override
    public List<String> searchTemplates(final String s) {
        // setup REST-Client
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.addFilter( new HTTPBasicAuthFilter(user, password) );
        WebResource service = client.resource( serverUrl);

        LoggerFactory.getLogger(this.getClass()).info("Get all the templates");
        String xlrelease = service.path("releases").path("templates").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class).toString();
        LoggerFactory.getLogger(this.getClass()).info(xlrelease + "\n");

        return new ArrayList<String>();
    }
}
