package org.hyperic.hq.ha;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Utility class for HA operations.  TODO remove this once we have proper DI for HQU controllers
 * @author jhickey
 *
 */
public abstract class HAUtil {

    private static HAService HA_SERVICE;
    
    private static Log LOG = LogFactory.getLog(HAUtil.class);
    
    public static boolean isMasterNode() {
        if(HA_SERVICE == null) {
            LOG.warn("Received request for primary status before HA services initialized. Returning true.");
            return true;
        }
        return HA_SERVICE.isMasterNode();
    }
    
    public static void setHAService(HAService service) {
        HAUtil.HA_SERVICE = service;
    }
}
