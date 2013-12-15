package com.xebialabs.xlrelease.ci.server;

import java.util.List;

import com.xebialabs.xlrelease.ci.JenkinsCreateRelease;
import com.xebialabs.xlrelease.ci.util.ReleaseFullView;

public interface XLReleaseServer {
    void newCommunicator();

    Object getVersion();

    List<ReleaseFullView> searchTemplates(String s);


    ReleaseFullView createRelease(String resolvedTemplate, String resolvedVersion, JenkinsCreateRelease createRelease);

    void startRelease(String releaseId);
}
