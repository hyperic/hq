package org.hyperic.bootstrap;

import java.util.Properties;
/**
 * Responsible for performing configuration-related tasks on server startup
 * @author jhickey
 *
 */
public interface ServerConfigurator {
    /**
     * Configure the server
     * @throws Exception
     */
    void configure() throws Exception;
    /**
     * 
     * @return Server configuration properties
     */
    Properties getServerProps();
}
