package com.xebialabs.xlrelease.ci.server;

import java.util.List;
import com.sun.jersey.api.client.ClientResponse;

import com.xebialabs.xlrelease.ci.NameValuePair;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;

public interface XLReleaseConnector {

    ClientResponse getVariablesResponse(String templateId);

    ClientResponse createReleaseResponse(String templateTitle, String releaseTitle, List<NameValuePair> variables);

    ClientResponse startReleaseResponse(String releaseId);

    List<TemplateVariable> filterVariables(List<TemplateVariable> variables);
}
