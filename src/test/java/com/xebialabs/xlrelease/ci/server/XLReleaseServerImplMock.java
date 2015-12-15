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

package com.xebialabs.xlrelease.ci.server;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xebialabs.xlrelease.ci.NameValuePair;
import com.xebialabs.xlrelease.ci.util.Release;
import com.xebialabs.xlrelease.ci.util.TemplateVariable;

public class XLReleaseServerImplMock implements XLReleaseServer {

    @Override
    public void testConnection() {
    }

    @Override
    public String getVersion() {
        return "test version";
    }

    @Override
    public List<Release> searchTemplates(final String s) {
        List<Release> templates = getAllTemplates();

        CollectionUtils.filter(templates, new Predicate() {
            public boolean evaluate(Object o) {
                return ((Release) o).getTitle().contains(s);
            }
        });
        LoggerFactory.getLogger(this.getClass()).info(templates + "\n");

        return templates;
    }

    @Override
    public List<Release> getAllTemplates() {
        List<Release> result = new ArrayList<Release>();
        result.add(new Release("someid", "atemplate", null));

        return result;
    }

    @Override
    public List<TemplateVariable> getVariables(String templateId) {
        return new ArrayList<TemplateVariable>();
    }

    @Override
    public Release createRelease(final String resolvedTemplate, final String resolvedVersion, final List<NameValuePair> variables) {
        return new Release("someid", "atemplate", null);
    }

    @Override
    public void startRelease(final String releaseId) {

    }
}
