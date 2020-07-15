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

import javax.management.RuntimeErrorException;
import javax.ws.rs.core.MediaType;

import com.google.gson.Gson;

import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import java.text.SimpleDateFormat;
import java.text.ParseException;

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
        System.out.println("AbstractXLReleaseConnector<init>: " + serverUrl + ", " + proxyUrl + ", " + username);
    }

    @Override
    public String getVersion() {
        WebResource service = buildWebResource();
        ServerInfo serverInfo = service.path("server")
                .path("info")
                .accept(MediaType.APPLICATION_XML_TYPE)
                .get(ServerInfo.class);
        String version = serverInfo.getVersion();
        System.out.println("XLR Version: "+version);
        return version;
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
        return client.resource(serverUrl.replace("\"", ""));
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

    protected Map<String, Object> convertToVariablesMap(final List<NameValuePair> variables, final Map<String, TemplateVariable> templateVariables) {
        Map<String, Object> variablesMap = new HashMap<String, Object>();
        for (NameValuePair variable : variables) {
            String varType = templateVariables.get(variable.getPropertyName()).getType();
            variablesMap.put(getVariableName(variable.getPropertyName()), castVariable(variable.propertyValue, varType));
        }
        return variablesMap;
    }

    private String getVariableName (String variable) {
        if (!isVariable(variable)) {
            return VARIABLE_PREFIX + variable + VARIABLE_SUFFIX;
        }
        return variable;
    }

    private Object castVariable (final String variable, String type) {
        switch (type) {
            case "xlrelease.BooleanVariable":
                return Boolean.valueOf(variable);
            case "xlrelease.IntegerVariable":
                return Integer.valueOf(variable);
            case "xlrelease.DateVariable":
                return parseDate(variable);
            case "xlrelease.ListStringVariable":
                return parseList(variable);
            case "xlrelease.MapStringStringVariable":
                return parseMap(variable);
            case "xlrelease.SetStringVariable":
                return parseSet(variable);

            // Commented cases fall under default case
            // case "xlrelease.StringVariable":
            // case "xlrelease.PasswordStringVariable":

            // List Box doesn't enforce possible values via API
            // May want to introduce validation and/or autocompletion
            // case "xlrelease.XLDeployPackageVariable":
            // case "xlrelease.XLDeployEnvironmentVariable":
            // case "xlrelease.ListOfStringValueProviderConfiguration":

            default:
                return variable;
        }
    }


    private Date parseDate(final String s) {
        List<SimpleDateFormat> formats = new ArrayList<SimpleDateFormat>();

        formats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        formats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"));
        formats.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"));
        formats.add(new SimpleDateFormat("yyyy-MM-dd"));
        formats.add(new SimpleDateFormat("dd-MMM-yyyy"));
        formats.add(new SimpleDateFormat("dd/MM/yyyy"));

        for (SimpleDateFormat format : formats){
            try {
                return new Date(format.parse(s).getTime());
            } catch (ParseException e) {
                // Hitting this exception is fine, we have multiple formats.
            }
        }

        // None of the formats worked.
        throw new RuntimeException("Date '" + s +  "' is invalid." +
                "\nSupported date formats:\nyyyy-MM-dd'T'HH:mm:ss.SSSZ" +
                "\nyyyy-MM-dd'T'HH:mm:ssXXX\nyyyy-MM-dd'T'HH:mm:ssZ\nyyyy-MM-dd\ndd-MMM-yyyy\ndd/MM/yyyy" +
                "\nEx. 2019-08-28T11:23:18Z");
    }

    private List<String> parseList(final String s) {
        List<String> list = Arrays.asList(s.split(","));
        list.replaceAll(String::trim);
        return list;
    }

    private Map<String, String> parseMap(final String s) {
        Gson g = new Gson();
        Map<String, String> map = g.fromJson(s, Map.class);
        return map;
    }

    private Set<String> parseSet(final String s) {
        List<String> list = parseList(s);
        Set<String> set = list.stream().collect(Collectors.toSet());
        return set;
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
