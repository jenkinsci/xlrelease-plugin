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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import hudson.PluginWrapper;
import jenkins.model.Jenkins;

public class PluginFirstClassloaderInvocationHandler implements InvocationHandler {

    private static final Object[] NO_ARGS = {};
    private Object target;

    public PluginFirstClassloaderInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args == null) {
            args = NO_ARGS;
        }

        //Classloader magic required to bootstrap resteasy.
        final Thread currentThread = Thread.currentThread();
        final ClassLoader origClassLoader = currentThread.getContextClassLoader();
        try {
            PluginWrapper plugin = Jenkins.getInstance().getPluginManager().getPlugin("xlrelease-plugin");
            if (plugin != null) {
                final ClassLoader pluginClassLoader = plugin.classLoader;
                currentThread.setContextClassLoader(pluginClassLoader);
            }
            return doInvoke(method, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } finally {
            currentThread.setContextClassLoader(origClassLoader);
        }
    }

    protected Object doInvoke(Method method, Object[] args) throws Throwable {
        return method.invoke(target, args);
    }
}
