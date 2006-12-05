package org.hyperic.hq.galerts.server.session;

import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.galerts.processor.Gtrigger;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;


public interface GtriggerType {
    ConfigSchema getSchema();
    
    
    Gtrigger createTrigger(ConfigResponse cfg);

    /**
     * Determine if a trigger is valid for a specified group.  This is used
     * to determine whether or not a trigger should be an option when creating
     * an alert definition.
     */
    boolean validForGroup(ResourceGroup g);
}
