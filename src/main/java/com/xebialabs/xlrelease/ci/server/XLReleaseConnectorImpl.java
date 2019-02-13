package com.xebialabs.xlrelease.ci.server;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import com.xebialabs.xlrelease.ci.NameValuePair;
import com.xebialabs.xlrelease.ci.util.CreateReleasePublicForm;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;

public class XLReleaseConnectorImpl extends AbstractXLReleaseConnector {
    public XLReleaseConnectorImpl(final String serverUrl, final String proxyUrl, final String username, final String password) {
        super(serverUrl, proxyUrl, username, password);
    }

    @Override
    public ClientResponse getVariablesResponse(final String templateId) {
        WebResource service = buildWebResource();
        return service
                .path("api/v1/templates/Applications")
                .path(templateId)
                .path("variables")
                .accept(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
    }

    @Override
    public ClientResponse createReleaseResponse(final String resolvedTemplate, final String resolvedVersion, final List<NameValuePair> variables) {
        WebResource service = buildWebResource();

        final String templateInternalId = getTemplateInternalId(resolvedTemplate);

        CreateReleasePublicForm createReleasePublicForm = new CreateReleasePublicForm(resolvedVersion, convertToVariablesMap(variables));
        return service
                .path("api/v1/templates/Applications")
                .path(templateInternalId)
                .path("create")
                .type(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, createReleasePublicForm);
    }

    @Override
    public ClientResponse startReleaseResponse(final String releaseId) {
        WebResource service = buildWebResource();
        return service
                .path("api/v1/releases/Applications")
                .path(releaseId)
                .path("start")
                .accept(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class);
    }

    @Override
    public List<TemplateVariable> filterVariables(final List<TemplateVariable> variables) {
        CollectionUtils.filter(variables, new Predicate() {
            public boolean evaluate(Object o) {
                if (o instanceof TemplateVariable) {
                    List<String> acceptedTypes = new ArrayList<String>();
                    acceptedTypes.add("xlrelease.StringVariable");
                    acceptedTypes.add("xlrelease.XLDeployPackageVariable");
                    acceptedTypes.add("xlrelease.XLDeployEnvironmentVariable");
                    acceptedTypes.add("xlrelease.BooleanVariable");
                    acceptedTypes.add("xlrelease.DateVariable");
                    acceptedTypes.add("xlrelease.IntegerVariable");
                    acceptedTypes.add("xlrelease.ListStringVariable");
                    acceptedTypes.add("xlrelease.MapStringStringVariable");
                    acceptedTypes.add("xlrelease.PasswordStringVariable");
                    acceptedTypes.add("xlrelease.SetStringVariable");
                    acceptedTypes.add("xlrelease.ListOfStringValueProviderConfiguration.java");
                    return acceptedTypes.contains(((TemplateVariable) o).getType());
                }
                return false;
            }
        });
        return variables;
    }
}
