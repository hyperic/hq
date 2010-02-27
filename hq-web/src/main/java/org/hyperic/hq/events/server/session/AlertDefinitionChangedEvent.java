package org.hyperic.hq.events.server.session;

/**
 * Indicates than an alert definition is changed
 * 
 *@author jhickey
 */
public class AlertDefinitionChangedEvent
    extends AlertDefinitionApplicationEvent {

    public AlertDefinitionChangedEvent(AlertDefinition alertDefinition) {
        super(alertDefinition);
    }

}
