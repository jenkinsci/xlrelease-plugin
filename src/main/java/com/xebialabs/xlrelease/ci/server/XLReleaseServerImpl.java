/**
 * Copyright (c) 2014, XebiaLabs B.V., All rights reserved.
 *
 *
 * The XL Release plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs Libraries.
 * There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
 * this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/xlrelease-plugin/blob/master/LICENSE>.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
 * Floor, Boston, MA 02110-1301  USA
 */

package com.xebialabs.xlrelease.ci.server;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;

import com.xebialabs.xlrelease.ci.util.ObjectMapperProvider;
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

import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;

import com.xebialabs.xlrelease.ci.NameValuePair;
import com.xebialabs.xlrelease.ci.util.CreateReleaseView;
import com.xebialabs.xlrelease.ci.util.ReleaseFullView;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;

public class XLReleaseServerImpl implements XLReleaseServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(XLReleaseServerImpl.class);

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
        List<ReleaseFullView> templates = getAllTemplates();

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
    public List<ReleaseFullView> getAllTemplates() {
        // setup REST-Client
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(config);
        client.addFilter( new HTTPBasicAuthFilter(user, password) );
        WebResource service = client.resource(serverUrl);

        LoggerFactory.getLogger(this.getClass()).info("Get all the templates");
        GenericType<List<ReleaseFullView>> genericType =
                new GenericType<List<ReleaseFullView>>() {};
        return service.path("releases").path("templates").accept(MediaType.APPLICATION_JSON).get(genericType);
    }

    @Override
    public ReleaseFullView createRelease(final String resolvedTemplate, final String resolvedVersion, final List<NameValuePair> variables) {
        // POST /releases/
        LoggerFactory.getLogger(this.getClass()).info("Create a release for " + resolvedTemplate);
        // setup REST-Client
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.setMapper((new ObjectMapperProvider()).getMapper());
        config.getSingletons().add(jacksonProvider);

        Client client = Client.create(config);
        client.addFilter( new HTTPBasicAuthFilter(user, password) );
        WebResource service = client.resource(serverUrl);

        GenericType<ReleaseFullView> genericType =
                new GenericType<ReleaseFullView>() {};


        CreateReleaseView createReleaseView = new CreateReleaseView(getTemplateId(resolvedTemplate), resolvedVersion, convertToTemplateVariables(variables), "2014-09-29T15:00:00.000Z", "2014-09-04T15:00:00.000Z");

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
