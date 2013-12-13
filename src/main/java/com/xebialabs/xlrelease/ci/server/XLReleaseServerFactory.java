package com.xebialabs.xlrelease.ci.server;

import java.util.List;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.reflect.Reflection;


public class XLReleaseServerFactory {
    public static boolean validConnection(String serverUrl, String proxyUrl, String username, String password) throws IllegalStateException {
        newInstance(serverUrl, proxyUrl, username, password).newCommunicator();  //throws IllegalStateException on failure.
        return true;
    }


    public static XLReleaseServer newInstance(String serverUrl, String proxyUrl, String username, String password) {
        XLReleaseServerImpl server = new XLReleaseServerImpl(serverUrl, proxyUrl, username, password);
        return Reflection.newProxy(XLReleaseServer.class, new PluginFirstClassloaderInvocationHandler(server));
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
}