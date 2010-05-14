package org.hyperic.hq.events.server.session;

import org.springframework.context.ApplicationEvent;
/**
 * Abstract class for any events related to alert definitions
 * @author jhickey
 *
 */
public class AlertDefinitionApplicationEvent extends ApplicationEvent {

    public AlertDefinitionApplicationEvent(AlertDefinition alertDefinition) {
        super(alertDefinition);
    }
    
    public AlertDefinition getAlertDefinition() {
        return (AlertDefinition) getSource();
    }

}
