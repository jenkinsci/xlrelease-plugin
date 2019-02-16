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


package com.xebialabs.xlrelease.ci.server;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.xebialabs.xlrelease.ci.Credential;
import hudson.util.Secret;


public class XLReleaseServerFactory {

    public boolean validConnection(String serverUrl, String proxyUrl, String username, String password) throws IllegalStateException {
        newInstance(serverUrl, proxyUrl, username, password).testConnection();  //throws IllegalStateException on failure.
        return true;
    }

    public XLReleaseServerConnector newInstance(String serverUrl, String proxyUrl, String username, String password) {
        return newInstance(serverUrl,proxyUrl,new Credential(username, username, Secret.fromString(password), null, false, null));
    }

    public XLReleaseServerConnector newInstance(String serverUrl, String proxyUrl, Credential credential) {
        String userName = credential.getUsername();
        String password = credential.getPassword().getPlainText();
        if (credential.isUseGlobalCredential()) {
            StandardUsernamePasswordCredentials cred =  Credential.lookupSystemCredentials(credential.getCredentialsId());
            userName =  cred.getUsername();
            password = cred.getPassword().getPlainText();
        }

        XLReleaseServerConnectorFacade server = new XLReleaseServerConnectorFacade(serverUrl, proxyUrl, userName, password);
        return newProxy(XLReleaseServerConnector.class, new PluginFirstClassloaderInvocationHandler(server));
    }

    public static String getNameFromId(String id) {
        String[] nameParts = id.split("/");
        return nameParts[nameParts.length - 1];
    }

    public static String getParentId(String id) {
        String[] nameParts = id.split("/");
        List<String> list = Lists.newArrayList(nameParts);
        if (list.size() > 1) {
            list.remove(nameParts.length - 1);
        }
        return Joiner.on("/").join(list);
    }

    public static <T> T newProxy(
            Class<T> interfaceType, InvocationHandler handler) {
        checkNotNull(interfaceType);
        checkNotNull(handler);
        checkArgument(interfaceType.isInterface());
        Object object = Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class<?>[] { interfaceType },
                handler);
        return interfaceType.cast(object);
    }

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static void checkArgument(boolean expression) {
        if (!expression) {
            throw new IllegalArgumentException();
        }
    }
}