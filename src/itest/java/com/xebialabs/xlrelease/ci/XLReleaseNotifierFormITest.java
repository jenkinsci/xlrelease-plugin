// :copyright: (c) 2019 by XebiaLabs BV.
// :license: GPLv2, see LICENSE for more details.

package com.xebialabs.xlrelease.ci;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.xebialabs.xlrelease.ci.util.Release;
import hudson.model.Descriptor;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.util.Secret;
import net.sf.json.JSONObject;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import org.kohsuke.stapler.StaplerRequest;

import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class XLReleaseNotifierFormITest {

    private static final String USER_VARIABLE = "user";
    private static final String TEMPLATE_NAME = "Samples & Tutorials/Welcome to XL Release!";
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

    @ClassRule 
    public static BuildWatcher bw = new BuildWatcher();

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
        Credential overridingCredential = new Credential(ADMIN_CREDENTIAL, username, Secret.fromString(password), null, true, null);
        XLReleaseNotifier before = reconfigureWithEnvSettings(
            new XLReleaseNotifier(
                ADMIN_CREDENTIAL, 
                TEMPLATE_NAME, 
                RELEASE_TITLE, 
                null, 
                true,
                overridingCredential));

        project.getPublishersList().add(before);
        HtmlForm xlrForm = jenkins.createWebClient().getPage(project, "configure").getFormByName("config");
        HtmlInput templateSelect = xlrForm.getInputsByName("_.template").get(0);

        /*System.out.println("=========================================");
        System.out.println("XLR Form: "+xlrForm.toString());
        this.printDomElement(xlrForm.getChildElements(), 1);*/

        assertThat(templateSelect.asText(), equalTo(TEMPLATE_NAME));
        assertThat(xlrForm.getInputByName("_.version").asText(), equalTo(RELEASE_TITLE));
        assertThat(xlrForm.getSelectByName("_.credential").getSelectedOptions().get(0).asText(), equalTo(ADMIN_CREDENTIAL));
    }

    @Test
    @LocalData
    public void shouldStartRelease() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        Credential overridingCredential = new Credential(ADMIN_CREDENTIAL, null, null, null, true, null);
        XLReleaseNotifier before = reconfigureWithEnvSettings(
            new XLReleaseNotifier(
                ADMIN_CREDENTIAL, 
                TEMPLATE_NAME, 
                RELEASE_TITLE, 
                newArrayList(new NameValuePair(USER_VARIABLE, "jenkins")), 
                true,
                overridingCredential));

        project.getPublishersList().add(before);
        FreeStyleBuild freeStyleBuild = jenkins.buildAndAssertSuccess(project);
        String releaseId = findReleaseId(freeStyleBuild.getLog(20));
        assertThat(releaseId, notNullValue());

        waitForReleaseStarted(releaseId);
    }

    @Test
    @LocalData
    public void shouldStartReleaseWithJenkinsFile() throws Exception {
        System.out.println("shouldStartReleaseWithJenkinsFile: begin");
        String jenkinsfile = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("JenkinsFile"), Charsets.UTF_8);
        System.out.println("shouldStartReleaseWithJenkinsFile: got jenkinsfile");

        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "workflow");
        job.setDefinition(new CpsFlowDefinition(jenkinsfile, true));
        System.out.println("shouldStartReleaseWithJenkinsFile: got job");

        WorkflowRun run = jenkins.buildAndAssertSuccess(job);
        System.out.println("got run: "+run.toString());
        String releaseId = findReleaseId(run.getLog(20));
        System.out.println("got releaseId: "+releaseId);
        assertThat(releaseId, notNullValue());
        waitForReleaseStarted(releaseId);
    }

    private void waitForReleaseStarted(final String releaseId) throws InterruptedException {
        ClientConfig config = new DefaultClientConfig();
        Client client = Client.create(config);
        client.addFilter(new HTTPBasicAuthFilter(username, password));
        WebResource service = client.resource(host);

        GenericType<Release> genericType = new GenericType<Release>() {};

        int maxNumberOfRetry = 45;
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

        fail("Release " + releaseId + " was not started within 45 seconds");
    }

    private String findReleaseId(List<String> log) {
        for (String line : log) {
            Matcher matcher = Pattern.compile(".*\"(Applications/.*Release[^/-]+).*\"").matcher(line);
            if (matcher.matches()) {
                return matcher.group(1);
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

    // Helper method to print xml elements.  Useful for debugging.
    private void printDomElement(Iterable<DomElement> el, int depth)
    {
        for (DomElement cel : el )
        {
            System.out.print("                                                                    ".substring(0, depth*2));
            System.out.println(cel.toString());
            if ( cel.getChildElementCount() > 0 ) 
            {
                printDomElement(cel.getChildElements(), depth+1);
            }
        }
    }
}
