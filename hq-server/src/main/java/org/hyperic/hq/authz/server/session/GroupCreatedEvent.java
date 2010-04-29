package org.hyperic.hq.authz.server.session;

import org.springframework.context.ApplicationEvent;

/**
 * Indicates that a group has been created
 * @author jhickey
 */
public class GroupCreatedEvent extends ApplicationEvent {

    public GroupCreatedEvent(ResourceGroup group) {
        super(group);
    }
    
    public ResourceGroup getGroup() {
        return (ResourceGroup) getSource();
    }

}
