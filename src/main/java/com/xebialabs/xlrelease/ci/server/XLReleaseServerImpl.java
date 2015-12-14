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

import java.text.SimpleDateFormat;
import java.util.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;

import com.xebialabs.xlrelease.ci.NameValuePair;
import com.xebialabs.xlrelease.ci.util.*;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class XLReleaseServerImpl implements XLReleaseServer {

    private static final Logger logger = LoggerFactory.getLogger(XLReleaseServerImpl.class);

    private static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();
    private static final int INTERNAL_SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    private String user;
    private String password;
    private String proxyUrl;
    private String serverUrl;

    XLReleaseServerImpl(String serverUrl, String proxyUrl, String username, String password) {
        this.user = username;
        this.password = password;
        this.proxyUrl = proxyUrl;
        this.serverUrl = serverUrl;
    }

    @Override
    public void newCommunicator() {
        logger.info("Check that XL Release is running");
        WebResource service = buildWebResource();
        ClientResponse response = service.path("profile").accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
        if (response.getStatusInfo().getFamily() != SUCCESSFUL) {
            throw new IllegalStateException(response.getStatusInfo().getReasonPhrase());
        }
        String xlrelease = response.toString();
        logger.info("Response: {}", xlrelease);
    }

    @Override
    public Object getVersion() {
        return serverUrl;
    }

    @Override
    public List<Release> searchTemplates(final String s) {
        List<Release> templates = getAllTemplates();

        CollectionUtils.filter(templates, new Predicate() {
            public boolean evaluate(Object o) {
                return ((Release) o).getTitle().contains(s);
            }
        });
        logger.info(templates + "\n");

        return templates;
    }

    @Override
    public List<Release> getAllTemplates() {
        logger.info("Get all the templates");
        WebResource service = buildWebResource();
        GenericType<List<Release>> genericType = new GenericType<List<Release>>() {
        };
        return service
                .path("api")
                .path("v1")
                .path("templates")
                .accept(MediaType.APPLICATION_JSON)
                .get(genericType);
    }

    @Override
    public List<TemplateVariable> getVariables(String templateId) {
        WebResource service = buildWebResource();
        logger.info("Get variables for {}", templateId);

        // Try first the internal API in this case, as the public API would return 400 instead of 404
        ClientResponse response = service
                .path("releases")
                .path(templateId)
                .path("updatable-variables")
                .accept(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);


        if (isEndpointMissing(response)) {
            response = service
                    .path("api/v1/templates/Applications")
                    .path(templateId)
                    .path("variables")
                    .accept(MediaType.APPLICATION_JSON)
                    .get(ClientResponse.class);
        }

        failIfUnsuccessful(response);

        GenericType<List<TemplateVariable>> genericType = new GenericType<List<TemplateVariable>>() {
        };

        List<TemplateVariable> variableList = response.getEntity(genericType);
        CollectionUtils.filter(variableList, new Predicate() {
            public boolean evaluate(Object o) {
                if (o instanceof TemplateVariable) {
                    List<String> acceptedTypes = new ArrayList<String>();
                    // 4.7- internal API types
                    acceptedTypes.add("DEFAULT");
                    acceptedTypes.add("PASSWORD");
                    acceptedTypes.add("DEPLOYIT_ENVIRONMENT");
                    acceptedTypes.add("DEPLOYIT_PACKAGE");
                    // 4.8+ public API types
                    acceptedTypes.add("xlrelease.StringVariable");
                    acceptedTypes.add("xlrelease.XLDeployPackageVariable");
                    acceptedTypes.add("xlrelease.XLDeployEnvironmentVariable");
                    return acceptedTypes.contains(((TemplateVariable) o).getType());
                }
                return false;
            }
        });
        return variableList;

    }

    @Override
    public Release createRelease(final String resolvedTemplate, final String resolvedVersion, final List<NameValuePair> variables) {
        logger.info("Create a release for {} ", resolvedTemplate);
        WebResource service = buildWebResource();

        final String templateInternalId = getTemplateInternalId(resolvedTemplate);
        GenericType<Release> genericType = new GenericType<Release>() {
        };

        CreateReleasePublicForm createReleasePublicForm = new CreateReleasePublicForm(resolvedVersion, convertToVariablesMap(variables));
        ClientResponse response = service
                .path("api/v1/templates/Applications")
                .path(templateInternalId)
                .path("create")
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, createReleasePublicForm);

        if (isEndpointMissing(response)) {
            // Try the pre-4.8.x internal API
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            String scheduledStartDate = format.format(Calendar.getInstance().getTime());

            Calendar dueDate = Calendar.getInstance();
            dueDate.add(Calendar.DATE, 1);
            String scheduledDueDate = format.format(dueDate.getTime());
            CreateReleaseInternalForm createReleaseInternalForm = new CreateReleaseInternalForm(
                    templateInternalId,
                    resolvedVersion,
                    convertToVariablesList(variables),
                    scheduledDueDate,
                    scheduledStartDate);

            response = service
                    .path("releases")
                    .type(MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class, createReleaseInternalForm);
        }

        failIfUnsuccessful(response);

        return response.getEntity(genericType);
    }

    @Override
    public void startRelease(final String releaseId) {
        logger.info("Start the release for: {}", releaseId);
        WebResource service = buildWebResource();
        ClientResponse response = service
                .path("api/v1/releases/Applications")
                .path(releaseId)
                .path("start")
                .accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class);
        if (isEndpointMissing(response)) {
            logger.info("New version of XL-Release not found trying use old internal API for releaseId = {}", releaseId);
            service.path("releases")
                    .path(releaseId)
                    .path("start")
                    .type(MediaType.APPLICATION_JSON)
                    .post();
        }

        failIfUnsuccessful(response);
    }

    private String getTemplateInternalId(final String resolvedTemplate) {
        List<Release> templates = searchTemplates(resolvedTemplate);
        CollectionUtils.filter(templates, new Predicate() {
            public boolean evaluate(Object o) {
                return ((Release) o).getTitle().equals(resolvedTemplate);
            }
        });
        return templates.get(0).getInternalId();
    }

    private List<TemplateVariable> convertToVariablesList(final List<NameValuePair> variables) {
        List<TemplateVariable> result = new ArrayList<TemplateVariable>();
        for (NameValuePair variable : variables) {
            result.add(new TemplateVariable(variable.propertyName, variable.propertyValue));
        }
        return result;
    }

    private Map<String, String> convertToVariablesMap(final List<NameValuePair> variables) {
        Map<String, String> variablesMap = new HashMap<String, String>();
        for (NameValuePair variable : variables) {
            variablesMap.put(variable.propertyName, variable.propertyValue);
        }
        return variablesMap;
    }

    private WebResource buildWebResource() {
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.setMapper((new ObjectMapperProvider()).getMapper());
        config.getSingletons().add(jacksonProvider);

        Client client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter(user, password));
        return client.resource(serverUrl);
    }

    private boolean isEndpointMissing(final ClientResponse response) {
        if (response.getStatusInfo().getStatusCode() == NOT_FOUND) {
            return true;
        }
        if (response.getStatusInfo().getStatusCode() == INTERNAL_SERVER_ERROR) {
            String message = response.getEntity(String.class);
            // 405 Not Allowed is returned as 500 for some reason
            return message != null && message.contains("405") && message.contains("Allow");
        }
        return false;
    }

    private void failIfUnsuccessful(final ClientResponse response) {
        if (response.getStatusInfo().getFamily() != SUCCESSFUL) {
            String errorReason = response.getEntity(String.class);
            throw new IllegalStateException(errorReason);
        }
    }
}
