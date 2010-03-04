package org.hyperic.bootstrap;

import java.util.List;

import org.hyperic.sigar.SigarException;

/**
 * Controls the lifecycle of the underlying HQ engine (i.e. the app server)
 * @author jhickey
 * 
 */
public interface EngineController {
    /**
     * Start the engine
     * @param javaOpts The system properties to use when starting the engine
     * @return exitCode 0 if success
     */
    int start(List<String> javaOpts);

    /**
     * Stop the engine
     * @return exitCode 0 if success
     * @throws SigarException If error finding process to stop or executing the
     *         stop behavior
     */
    int stop() throws SigarException;

    /**
     * Forcibly halt the engine
     * @throws SigarException If error finding process to stop or executing the
     *         stop behavior
     */
    void halt() throws SigarException;
}
