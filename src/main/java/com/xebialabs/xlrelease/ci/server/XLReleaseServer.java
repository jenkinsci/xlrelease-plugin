package com.xebialabs.xlrelease.ci.server;

import java.util.List;

public interface XLReleaseServer {
    void newCommunicator();

    Object getVersion();

    List<String> searchTemplates(String s);


}
