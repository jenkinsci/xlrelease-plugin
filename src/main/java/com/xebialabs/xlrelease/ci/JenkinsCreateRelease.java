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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

public class JenkinsCreateRelease implements Describable<JenkinsCreateRelease> {

    private final List<NameValuePair> variables;

    @DataBoundConstructor
    public JenkinsCreateRelease(List<NameValuePair> variables) {
        this.variables = variables;
    }

    public Descriptor<JenkinsCreateRelease> getDescriptor() {
        return Jenkins.getInstance().getDescriptorOrDie(getClass());
    }

    public List<NameValuePair> getVariables() {
        return variables;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<JenkinsCreateRelease> {
        @Override
        public String getDisplayName() {
            return "JenkinsCreateRelease";
        }

        @Override
        public JenkinsCreateRelease newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return super.newInstance(req, formData);
        }
    }
}


