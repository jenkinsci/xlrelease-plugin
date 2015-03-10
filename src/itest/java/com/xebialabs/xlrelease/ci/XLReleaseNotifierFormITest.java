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

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlSelect;

import hudson.model.FreeStyleProject;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

public class XLReleaseNotifierFormITest {

    private static final String USER_VARIABLE = "${user}";
    private static final String TEMPLATE_NAME = "Welcome to XL Release!";
    private static final String RELEASE_TITLE = "Release created with jenkins plugin ${BUILD_NUMBER}";

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    @LocalData
    public void shouldShowListOfTemplatesWithSavedAsPreSelected() throws Exception {
        FreeStyleProject project = jenkins.createFreeStyleProject();
        XLReleaseNotifier before = new XLReleaseNotifier("admin_credential", TEMPLATE_NAME, RELEASE_TITLE, null, true);
        project.getPublishersList().add(before);

        HtmlForm xlrForm = jenkins.createWebClient().getPage(project, "configure").getFormByName("config");
        HtmlSelect templateSelect = xlrForm.getSelectByName("_.template");

        assertThat(templateSelect.getSelectedOptions().get(0).asText(), equalTo(TEMPLATE_NAME));
        assertThat(templateSelect.getOptionSize(), greaterThan(1));
        assertThat(xlrForm.getInputByName("_.version").asText(), equalTo(RELEASE_TITLE));
        assertThat(xlrForm.getSelectByName("_.propertyName").getSelectedOptions().get(0).asText(), equalTo(USER_VARIABLE));
    }

}
