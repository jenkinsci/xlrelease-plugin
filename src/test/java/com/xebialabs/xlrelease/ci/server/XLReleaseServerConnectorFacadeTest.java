package com.xebialabs.xlrelease.ci.server;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class XLReleaseServerConnectorFacadeTest {

    @Test
    public void should_choose_correct_connector_by_version() {
        XLReleaseServerConnectorFacade server = new XLReleaseServerConnectorFacade(null, null, null, null);
        assertThat(server.isVersionPre48("4.7.2"), is(true));
        assertThat(server.isVersionPre48("4.7.3-SNAPSHOT"), is(true));
        assertThat(server.isVersionPre48("4.6.0"), is(true));
        assertThat(server.isVersionPre48("3.0.0"), is(true));

        assertThat(server.isVersionPre48("0.0.0"), is(false));
        assertThat(server.isVersionPre48("4.8.0"), is(false));
        assertThat(server.isVersionPre48("4.8.1"), is(false));
        assertThat(server.isVersionPre48("4.9.2"), is(false));
        assertThat(server.isVersionPre48("5.0.0"), is(false));
        assertThat(server.isVersionPre48("4.08.0"), is(false));
        assertThat(server.isVersionPre48("4.80.0"), is(false));
        assertThat(server.isVersionPre48("@package.version@"), is(false));
        assertThat(server.isVersionPre48(""), is(false));
        assertThat(server.isVersionPre48(null), is(false));
    }

}
