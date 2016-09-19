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

package com.xebialabs.xlrelease.ci;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.core.MediaType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.kohsuke.stapler.StaplerRequest;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;
import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

import com.xebialabs.xlrelease.ci.util.Release;

import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import net.sf.json.JSONObject;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XLReleaseNotifierFormITest {

    private static final String USER_VARIABLE = "${user}";
    private static final String TEMPLATE_NAME = "Welcome to XL Release!";
    private static final String RELEASE_TITLE = "Release created with jenkins plugin ${BUILD_NUMBER}";
    private static final String ADMIN_CREDENTIAL = "admin_credential";

    private static final String ENV_XLR_HOST = "xlReleaseIntegration.host";
    private static final String ENV_XLR_USERNAME = "xlReleaseIntegration.username";
    private static final String ENV_XLR_PASSWORD = "xlReleaseIntegration.password";

    private static final String DEFAULT_HOST = "http://localhost:5516";
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin";

    private static String host;
    private static String username;
    private static String password;

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void before() {
        host = System.getProperty(ENV_XLR_HOST) != null ? System.getProperty(ENV_XLR_HOST) : DEFAULT_HOST;
        username = System.getProperty(ENV_XLR_USERNAME) != null ? System.getProperty(ENV_XLR_USERNAME) : DEFAULT_USERNAME;
        password = System.getProperty(ENV_XLR_PASSWORD) != null ? System.getProperty(ENV_XLR_PASSWORD) : DEFAULT_PASSWORD;
    }

    @Test
    @LocalData
    public void shouldShowListOfTemplatesWithSavedAsPreSelected() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        XLReleaseNotifier before = reconfigureWithEnvSettings(
                new XLReleaseNotifier(ADMIN_CREDENTIAL, TEMPLATE_NAME, RELEASE_TITLE, null, true));
        project.getPublishersList().add(before);

        HtmlForm xlrForm = jenkins.createWebClient().getPage(project, "configure").getFormByName("config");
        HtmlSelect templateSelect = xlrForm.getSelectByName("_.template");

        assertThat(templateSelect.getOptionSize(), greaterThan(1));
        assertThat(templateSelect.getSelectedOptions().get(0).asText(), equalTo(TEMPLATE_NAME));
        assertThat(xlrForm.getInputByName("_.version").asText(), equalTo(RELEASE_TITLE));
        assertThat(xlrForm.getSelectByName("_.propertyName").getSelectedOptions().get(0).asText(), equalTo(USER_VARIABLE));
    }

    @Test
    @LocalData
    public void shouldStartRelease() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        XLReleaseNotifier before = reconfigureWithEnvSettings(new XLReleaseNotifier(
                ADMIN_CREDENTIAL, TEMPLATE_NAME, RELEASE_TITLE,
                newArrayList(new NameValuePair(USER_VARIABLE, "jenkins")), true));
        project.getPublishersList().add(before);
        FreeStyleBuild freeStyleBuild = jenkins.buildAndAssertSuccess(project);
        String releaseId = findReleaseId(freeStyleBuild.getLog(20));
        assertThat(releaseId, notNullValue());

        waitForReleaseStarted(releaseId);
    }

    private void waitForReleaseStarted(final String releaseId) throws InterruptedException {
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter(username, password));
        WebResource service = client.resource(host);

        GenericType<Release> genericType = new GenericType<Release>() {};

        int maxNumberOfRetry = 30;
        while (maxNumberOfRetry-- > 0) {
            try {
                Release release = service.path("api").path("v1").path("releases").path(releaseId).accept(MediaType.APPLICATION_JSON).get(genericType);
                if ("IN_PROGRESS".equals(release.getStatus())) {
                    return;
                }
            } catch (UniformInterfaceException exception) {
                if (exception.getResponse().getStatus() != 404) {
                    throw exception;
                }
            }
            Thread.sleep(1000);
        }

        fail("Release " + releaseId + " was not started within 30 seconds");
    }

    private String findReleaseId(List<String> log) {
        for (String line : log) {
            Matcher matcher = Pattern.compile(".*(Release\\d+).*").matcher(line);
            if (matcher.matches()) {
                return "Applications/" + matcher.group(1);
            }
        }
        return null;
    }

    private XLReleaseNotifier reconfigureWithEnvSettings(XLReleaseNotifier xlReleaseNotifier) throws Descriptor.FormException {
        StaplerRequest request = mock(StaplerRequest.class);
        when(request.bindJSONToList(eq(Credential.class), anyObject())).thenReturn(newArrayList(
                new Credential(ADMIN_CREDENTIAL, username, Secret.fromString(password), null, false, null)));
        JSONObject json = new JSONObject();
        json.put("xlReleaseServerUrl", host);
        json.put("xlReleaseClientProxyUrl", "");

        xlReleaseNotifier.getDescriptor().configure(request, json);

        return xlReleaseNotifier;
    }

}
