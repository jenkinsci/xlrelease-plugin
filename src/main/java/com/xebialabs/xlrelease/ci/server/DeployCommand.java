package com.xebialabs.xlrelease.ci.server;

import java.text.SimpleDateFormat;
import java.util.List;

import com.xebialabs.xlrelease.ci.util.JenkinsDeploymentListener;

import static com.google.common.collect.Lists.newArrayList;

public class DeployCommand {

    private JenkinsDeploymentListener listener;

    DeployCommand( JenkinsDeploymentListener listener) {
        this.listener = listener;
    }

    public void deploy(String deploymentPackage, String environment) {





        listener.debug("deploy");


    }

    private boolean executeTask(String taskId) {


        checkTaskState(taskId);
        return true;

    }

    private List<Integer> range(int end) {
        List<Integer> result = newArrayList();
        for (int i = 1; i < end; i++) {
            result.add(i);
        }
        return result;
    }

    private void checkTaskState(String taskId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");

        StringBuilder sb = new StringBuilder();
    }

    private void startTaskAndWait(String taskId) {
        // Wait until done/failed
        boolean done = false;


    }
}