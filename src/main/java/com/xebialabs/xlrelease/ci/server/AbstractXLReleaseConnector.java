package com.xebialabs.xlrelease.ci.server;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.json.JSONConfiguration;
import com.xebialabs.xlrelease.ci.NameValuePair;
import com.xebialabs.xlrelease.ci.util.ObjectMapperProvider;
import com.xebialabs.xlrelease.ci.util.Release;
import com.xebialabs.xlrelease.ci.util.ServerInfo;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.xebialabs.xlrelease.ci.NameValuePair.VARIABLE_PREFIX;
import static com.xebialabs.xlrelease.ci.NameValuePair.VARIABLE_SUFFIX;
import static com.xebialabs.xlrelease.ci.util.TemplateVariable.isVariable;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public abstract class AbstractXLReleaseConnector implements XLReleaseServerConnector {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractXLReleaseConnector.class);

    private String user;
    private String password;
    private String proxyUrl;
    private String serverUrl;


    protected AbstractXLReleaseConnector(String serverUrl, String proxyUrl, String username, String password) {
        this.user = username;
        this.password = password;
        this.proxyUrl = proxyUrl;
        this.serverUrl = serverUrl;
    }

    @Override
    public String getVersion() {
        WebResource service = buildWebResource();
        ServerInfo serverInfo = service.path("server")
                .path("info")
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(ServerInfo.class);
        return serverInfo.getVersion();
    }

    @Override
    public void testConnection() {
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
    public List<Release> searchTemplates(final String filter) {
        List<Release> templates = getAllTemplates();

        CollectionUtils.filter(templates, new Predicate() {
            public boolean evaluate(Object o) {
                return ((Release) o).getTitle().toLowerCase().startsWith(filter.toLowerCase());
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
        logger.info("Getting variables from xl-release");
        ClientResponse response = getVariablesResponse(templateId);
        failIfUnsuccessful(response);
        GenericType<List<TemplateVariable>> genericType = new GenericType<List<TemplateVariable>>() {
        };
        List<TemplateVariable> variableList = response.getEntity(genericType);
        return filterVariables(variableList);
    }

    @Override
    public Release createRelease(final String resolvedTemplate, final String resolvedVersion, final List<NameValuePair> variables) {
        logger.info("Creating Release with template {}", resolvedTemplate);
        ClientResponse response = createReleaseResponse(resolvedTemplate, resolvedVersion, variables);
        failIfUnsuccessful(response);
        GenericType<Release> genericType = new GenericType<Release>() {
        };
        return response.getEntity(genericType);

    }

    @Override
    public void startRelease(final String releaseId) {
        logger.info("Starting release {}", releaseId);
        ClientResponse response = startReleaseResponse(releaseId);
        failIfUnsuccessful(response);

    }

    private void failIfUnsuccessful(final ClientResponse response) {
        if (response.getStatusInfo().getFamily() != SUCCESSFUL) {
            String errorReason = response.getEntity(String.class);
            throw new IllegalStateException(errorReason);
        }
    }

    protected WebResource buildWebResource() {
        ClientConfig config = new DefaultClientConfig();
        config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.setMapper((new ObjectMapperProvider()).getMapper());
        config.getSingletons().add(jacksonProvider);

        Client client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter(user, password));
        return client.resource(serverUrl);
    }

    protected String getTemplateInternalId(final String templateTitle) {
        List<Release> templates = searchTemplates(templateTitle);
        CollectionUtils.filter(templates, new Predicate() {
            public boolean evaluate(Object o) {
                return ((Release) o).getTitle().equals(templateTitle);
            }
        });
        if (templates.size() == 0) {
            throw new RuntimeException("No template found for template id : " + templateTitle);
        }
        return templates.get(0).getInternalId();
    }

    protected List<TemplateVariable> convertToVariablesList(final List<NameValuePair> variables) {
        List<TemplateVariable> result = new ArrayList<TemplateVariable>();
        for (NameValuePair variable : variables) {
            result.add(new TemplateVariable(variable.propertyName, variable.propertyValue));
        }
        return result;
    }

    protected Map<String, String> convertToVariablesMap(final List<NameValuePair> variables) {
        Map<String, String> variablesMap = new HashMap<String, String>();
        for (NameValuePair variable : variables) {
            variablesMap.put(getVariableName(variable.getPropertyName()), variable.propertyValue);
        }
        return variablesMap;
    }

    private String getVariableName (String variable) {
        if (!isVariable(variable)) {
            return VARIABLE_PREFIX + variable + VARIABLE_SUFFIX;
        }
        return variable;
    }

    @Override
    public String getServerURL() {
        return serverUrl;
    }

    protected abstract ClientResponse getVariablesResponse(String templateId);

    protected abstract ClientResponse createReleaseResponse(String templateTitle, String releaseTitle, List<NameValuePair> variables);

    protected abstract ClientResponse startReleaseResponse(String releaseId);

    protected abstract List<TemplateVariable> filterVariables(List<TemplateVariable> variables);
}
