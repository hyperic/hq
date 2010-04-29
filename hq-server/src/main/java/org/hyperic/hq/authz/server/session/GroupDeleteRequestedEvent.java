package org.hyperic.hq.authz.server.session;

import org.springframework.context.ApplicationEvent;

/**
 * Sent before a group is deleted
 * @author jhickey
 */
public class GroupDeleteRequestedEvent extends ApplicationEvent {

    public GroupDeleteRequestedEvent(ResourceGroup group) {
        super(group);
    }

    public ResourceGroup getGroup() {
        return (ResourceGroup) getSource();
    }

}
