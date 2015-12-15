package com.xebialabs.xlrelease.ci.util;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "server-info")
@XmlAccessorType(XmlAccessType.FIELD)
public class ServerInfo {

    @XmlElement(name = "version")
    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }
}
