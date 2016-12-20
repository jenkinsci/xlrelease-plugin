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

package com.xebialabs.xlrelease.ci.util;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.xebialabs.xlrelease.ci.NameValuePair.VARIABLE_PREFIX;
import static com.xebialabs.xlrelease.ci.NameValuePair.VARIABLE_SUFFIX;

@XmlRootElement
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateVariable {

    private String key;
    private String type;
    private Object value;

    public TemplateVariable() {
    }

    public TemplateVariable(final String key, final String value) {
        this.key = key;
        this.value = value;
    }

    public static Map<String, String> toMap(Collection<? extends TemplateVariable> variables) {
        Map<String, String> result = new HashMap<String, String>();
        for (TemplateVariable variable : variables) {
            result.put(getVariableName(variable.getKey()), variable.getValue() == null ? null : variable.getValue().toString());
        }
        return result;
    }

    private static String getVariableName(String variable) {
        if (isVariable(variable)) {
            variable = variable.substring(VARIABLE_PREFIX.length(), variable.length() - VARIABLE_SUFFIX.length());
        }
        return variable;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public static boolean isVariable (String string){
        return string.startsWith(VARIABLE_PREFIX) && string.endsWith(VARIABLE_SUFFIX);
    }
}
