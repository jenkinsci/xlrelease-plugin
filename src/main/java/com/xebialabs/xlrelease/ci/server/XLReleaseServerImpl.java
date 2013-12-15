package com.xebialabs.xlrelease.ci.server;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;

import com.xebialabs.xlrelease.ci.JenkinsCreateRelease;
import com.xebialabs.xlrelease.ci.NameValuePair;
import com.xebialabs.xlrelease.ci.util.CreateReleaseView;
import com.xebialabs.xlrelease.ci.util.ReleaseFullView;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;

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
        WebResource service = client.resource(serverUrl);

        LoggerFactory.getLogger(this.getClass()).info("Check that XL Release is running");
        String xlrelease = service.path("releases").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class).toString();
        LoggerFactory.getLogger(this.getClass()).info(xlrelease + "\n");

    }

    @Override
    public Object getVersion() {
        return serverUrl;
    }

    @Override
    public List<ReleaseFullView> searchTemplates(final String s) {
        // setup REST-Client
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(config);
        client.addFilter( new HTTPBasicAuthFilter(user, password) );
        WebResource service = client.resource(serverUrl);

        LoggerFactory.getLogger(this.getClass()).info("Get all the templates");
        GenericType<List<ReleaseFullView>> genericType =
                new GenericType<List<ReleaseFullView>>() {};
        List<ReleaseFullView> templates = service.path("releases").path("templates").accept(MediaType.APPLICATION_JSON).get(genericType);
        CollectionUtils.filter(templates, new Predicate() {
            public boolean evaluate(Object o) {
               if (((ReleaseFullView)o).getTitle().contains(s))
                   return true;
               return false;
            }
        });
        LoggerFactory.getLogger(this.getClass()).info(templates + "\n");

        return templates;
    }

    @Override
    public ReleaseFullView createRelease(final String resolvedTemplate, final String resolvedVersion, final JenkinsCreateRelease createRelease) {
        // POST /releases/
        LoggerFactory.getLogger(this.getClass()).info("Create a release for " + resolvedTemplate);
        // setup REST-Client
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(config);
        client.addFilter( new HTTPBasicAuthFilter(user, password) );
        WebResource service = client.resource(serverUrl);

        GenericType<ReleaseFullView> genericType =
                new GenericType<ReleaseFullView>() {};


        CreateReleaseView createReleaseView = new CreateReleaseView(getTemplateId(resolvedTemplate), resolvedVersion, convertToTemplateVariables(createRelease.getVariables()));

        ReleaseFullView result = service.path("releases").type(MediaType.APPLICATION_JSON).post(genericType, createReleaseView);

        return result;
    }

    private String getTemplateId(final String resolvedTemplate) {
        List<ReleaseFullView> templates = searchTemplates(resolvedTemplate);
        CollectionUtils.filter(templates, new Predicate() {
            public boolean evaluate(Object o) {
                if (((ReleaseFullView)o).getTitle().equals(resolvedTemplate))
                    return true;
                return false;
            }
        });

        return templates.get(0).getId();
    }

    private List<TemplateVariable> convertToTemplateVariables(final List<NameValuePair> variables) {
        List<TemplateVariable> result = new ArrayList<TemplateVariable>();
        for (NameValuePair variable : variables) {
            result.add(new TemplateVariable(variable.propertyName,variable.propertyValue));
        }

        return result;
    }

    @Override
    public void startRelease(final String releaseId) {
        //POST /releases/{releaseId}/start
        LoggerFactory.getLogger(this.getClass()).info("Start the release for: " + releaseId);

        // setup REST-Client
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(config);
        client.addFilter( new HTTPBasicAuthFilter(user, password) );
        WebResource service = client.resource(serverUrl);

        service.path("releases").path(releaseId).path("start").type(MediaType.APPLICATION_JSON).post();
    }
}
