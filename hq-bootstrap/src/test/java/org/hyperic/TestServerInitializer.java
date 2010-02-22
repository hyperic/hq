package org.hyperic;

import org.junit.Test;

/**
 * Small utility for testing the ServerInitializer. Not meant to run as a
 * regular unit test, as it requires a fully installed HQ server to point to
 * @author jhickey
 * 
 */
public class TestServerInitializer {

    @Test
    public void testInitialize() throws Exception {
        ServerInitializer initializer = new ServerInitializer(
            "/Applications/Evolution/server-5.0.0",
            "/Applications/Evolution/server-5.0.0/hq-engine");
        initializer.initialize();
    }
}
