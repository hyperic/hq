package org.hyperic.hq.authz.server.session;

import org.springframework.context.ApplicationEvent;

/**
 * Sent before resource deletion
 * @author jhickey
 * 
 */
public class ResourceDeleteRequestedEvent
    extends ApplicationEvent {
    public ResourceDeleteRequestedEvent(Resource resource) {
        super(resource);
    }

    public Resource getResource() {
        return (Resource) getSource();
    }
}
