package org.hyperic.bootstrap;

import java.util.Properties;

public interface ServerConfigurator {
    void configure() throws Exception;
    Properties getServerProps();
}
