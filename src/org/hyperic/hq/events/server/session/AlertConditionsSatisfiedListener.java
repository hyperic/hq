package org.hyperic.hq.events.server.session;

import java.util.List;

import org.hyperic.hq.zevents.ZeventListener;

/**
 * Receives AlertConditionSatisfiedZEvents and forwards them to the AlertManager
 * for processing
 * @author jhickey
 *
 */
public class AlertConditionsSatisfiedListener implements ZeventListener {

    public void processEvents(List events) {
        AlertManagerEJBImpl.getOne().handleAlertConditionsSatisfiedEvents(events);

    }

}
