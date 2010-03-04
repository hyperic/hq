package org.hyperic.bootstrap;

import java.io.IOException;

import org.hyperic.sigar.SigarException;
/**
 * Controls the lifecycle of the Embedded HQ Database (if should be used)
 * @author jhickey
 *
 */
public interface EmbeddedDatabaseController {

    /**
     * Is the server even installed with the built-in DB?
     * @return true if built-in DB should be used
     */
    boolean shouldUse();
    
    /**
     * Starts the built-in DB
     * @return true if successfully started
     * @throws SigarException
     * @throws IOException
     */
    boolean startBuiltInDB() throws SigarException, IOException;
    
    /**
     * Stops the built-in DB
     * @return true if successfully stopped
     * @throws SigarException
     * @throws IOException
     */
    boolean stopBuiltInDB() throws SigarException, IOException;
    
}
