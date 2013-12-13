package com.xebialabs.xlrelease.ci.server;

public interface XLReleaseServer {
    void newCommunicator();

    Object getVersion();
}
