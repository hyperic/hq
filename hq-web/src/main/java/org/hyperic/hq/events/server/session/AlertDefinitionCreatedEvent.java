package org.hyperic.hq.events.server.session;


/**
 * Indicates that an alert definition is created
 * 
 *
 */
public class AlertDefinitionCreatedEvent
    extends AlertDefinitionApplicationEvent {

    public AlertDefinitionCreatedEvent(AlertDefinition alertDefinition) {
        super(alertDefinition);
    }

}
