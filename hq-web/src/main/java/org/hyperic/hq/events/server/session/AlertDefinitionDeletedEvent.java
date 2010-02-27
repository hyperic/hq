package org.hyperic.hq.events.server.session;

/**
 * Indicates that an alert definition is deleted
 * 
 * @author jhickey
 */
public class AlertDefinitionDeletedEvent
    extends AlertDefinitionApplicationEvent {

    public AlertDefinitionDeletedEvent(AlertDefinition alertDefinition) {
        super(alertDefinition);
    }

}
