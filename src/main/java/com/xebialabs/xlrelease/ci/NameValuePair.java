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


import java.util.Map;

import com.xebialabs.xlrelease.ci.util.TemplateVariable;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.xebialabs.xlrelease.ci.util.ListBoxModels;

import hudson.Extension;
import hudson.RelativePath;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

import static com.xebialabs.xlrelease.ci.XLReleaseNotifier.XLReleaseDescriptor;
import static com.xebialabs.xlrelease.ci.util.ListBoxModels.emptyModel;
import static com.xebialabs.xlrelease.ci.util.TemplateVariable.isVariable;

public class NameValuePair extends AbstractDescribableImpl<NameValuePair> {

    public String propertyName;
    public String propertyValue;

    public static String VARIABLE_PREFIX = "${";
    public static String VARIABLE_SUFFIX = "}";

    @DataBoundConstructor
    public NameValuePair(String propertyName, String propertyValue) {
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
        if (!isVariable(propertyName)) {
            this.propertyName = VARIABLE_PREFIX + propertyName + VARIABLE_SUFFIX;
        }

    }

    @Extension
    public static final class NameValuePairDescriptor extends Descriptor<NameValuePair> {
        @Override
        public String getDisplayName() {
            return "NameValuePair";
        }

        public ListBoxModel doFillPropertyNameItems(@QueryParameter @RelativePath(value = "..") String credential,
                @QueryParameter @RelativePath(value = "..") String template) {
            Map<String, String> variables = getXLReleaseDescriptor().getVariablesOf(credential, template);
            if (variables == null) {
                return emptyModel();
            }
            return ListBoxModels.of(variables.keySet());
        }

        protected XLReleaseDescriptor getXLReleaseDescriptor() {
            return (XLReleaseDescriptor) Jenkins.getInstance().getDescriptorOrDie(XLReleaseNotifier.class);
        }

    }
}
