package org.hyperic.hq.ha;

import org.hyperic.hq.common.ProductProperties;

/**
 * Utility class for HA operations. TODO remove this once we have proper DI for
 * HQU controllers
 * @author jhickey
 * 
 */
public abstract class HAUtil {

    public static boolean isMasterNode() {
        return ((HAService) ProductProperties.getPropertyInstance("hyperic.hq.ha.service"))
            .isMasterNode();
    }

}
