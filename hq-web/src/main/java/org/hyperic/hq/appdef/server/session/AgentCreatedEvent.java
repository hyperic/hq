package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.Agent;
import org.springframework.context.ApplicationEvent;
/**
 * Indicates that an agent has been created
 * @author jhickey
 *
 */
public class AgentCreatedEvent extends ApplicationEvent {

    public AgentCreatedEvent(Agent agent) {
        super(agent);
    }
    
    public Agent getAgent() {
        return (Agent)getSource();
    }

}
