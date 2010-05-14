package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.springframework.context.ApplicationEvent;

/**
 * Indicates that metrics have been 
 * enabled for a given appdef resource. 
 * @author jhickey
 */
public class MetricsEnabledEvent extends ApplicationEvent {

    public MetricsEnabledEvent(AppdefEntityID entityID) {
        super(entityID);
    }
    
    public AppdefEntityID getEntityId() {
        return (AppdefEntityID) getSource();
    }

}
