/**
 * Copyright (c) 2014, XebiaLabs B.V., All rights reserved.
 * <p/>
 * <p/>
 * The XL Release plugin for Jenkins is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs Libraries.
 * There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
 * this software, see the FLOSS License Exception
 * <https://github.com/jenkinsci/xlrelease-plugin/blob/master/LICENSE>.
 * <p/>
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; version 2
 * of the License.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
 * Floor, Boston, MA 02110-1301  USA
 */

package com.xebialabs.xlrelease.ci;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

import com.xebialabs.xlrelease.ci.server.XLReleaseServerConnector;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerFactory;
import com.xebialabs.xlrelease.ci.server.XLReleaseServerImplMock;

import hudson.model.FreeStyleProject;

public class XLReleaseNotifierFormTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @BeforeClass
    public static void setUp() {
        XLReleaseNotifier.XLReleaseDescriptor.setXlReleaseServerFactory(new XLReleaseServerFactory() {
            @Override
            public XLReleaseServerConnector newInstance(final String serverUrl, final String proxyUrl, final String username, final String password) {
                return new XLReleaseServerImplMock();
            }
        });
    }

    @Test
    @LocalData
    public void testXLReleaseForm() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        XLReleaseNotifier before = new XLReleaseNotifier("admin", "atemplate", "1.0", null, false, null);
        p.getPublishersList().add(before);

        j.submit(j.createWebClient().getPage(p, "configure").getFormByName("config"));

        XLReleaseNotifier after = p.getPublishersList().get(XLReleaseNotifier.class);

        j.assertEqualBeans(before, after, "credential,template,version");
    }

}
