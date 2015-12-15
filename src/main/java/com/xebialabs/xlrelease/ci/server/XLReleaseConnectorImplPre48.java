package com.xebialabs.xlrelease.ci.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.core.MediaType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import com.xebialabs.xlrelease.ci.NameValuePair;
import com.xebialabs.xlrelease.ci.util.CreateReleaseInternalForm;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;

public class XLReleaseConnectorImplPre48 extends AbstractXLReleaseConnector {

    protected XLReleaseConnectorImplPre48(final String serverUrl, final String proxyUrl, final String username, final String password) {
        super(serverUrl, proxyUrl, username, password);
    }

    @Override
    public ClientResponse getVariablesResponse(final String templateId) {
        WebResource service = buildWebResource();
        return service
                .path("releases")
                .path(templateId)
                .path("updatable-variables")
                .accept(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
    }

    @Override
    public ClientResponse createReleaseResponse(final String templateTitle, final String releaseTitle, final List<NameValuePair> variables) {
        WebResource service = buildWebResource();

        final String templateInternalId = getTemplateInternalId(templateTitle);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String scheduledStartDate = format.format(Calendar.getInstance().getTime());

        Calendar dueDate = Calendar.getInstance();
        dueDate.add(Calendar.DATE, 1);
        String scheduledDueDate = format.format(dueDate.getTime());
        CreateReleaseInternalForm createReleaseInternalForm = new CreateReleaseInternalForm(
                templateInternalId,
                releaseTitle,
                convertToVariablesList(variables),
                scheduledDueDate,
                scheduledStartDate);

        return service
                .path("releases")
                .type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class, createReleaseInternalForm);
    }

    @Override
    public ClientResponse startReleaseResponse(final String releaseId) {
        WebResource service = buildWebResource();
        return service.path("releases")
                .path(releaseId)
                .path("start")
                .type(MediaType.APPLICATION_JSON)
                .post(ClientResponse.class);
    }

    @Override
    public List<TemplateVariable> filterVariables(final List<TemplateVariable> variables) {
        CollectionUtils.filter(variables, new Predicate() {
            public boolean evaluate(Object o) {
                if (o instanceof TemplateVariable) {
                    List<String> acceptedTypes = new ArrayList<String>();
                    acceptedTypes.add("DEFAULT");
                    acceptedTypes.add("DEPLOYIT_ENVIRONMENT");
                    acceptedTypes.add("DEPLOYIT_PACKAGE");
                    return acceptedTypes.contains(((TemplateVariable) o).getType());
                }
                return false;
            }
        });
        return variables;
    }
}
