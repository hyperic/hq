package org.hyperic.hq.events.server.session;

import java.util.List;

import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.zevents.ZeventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Receives AlertConditionSatisfiedZEvents and forwards them to the AlertManager
 * for processing
 * @author jhickey
 * 
 */
@Component
public class AlertConditionsSatisfiedListener implements ZeventListener<AlertConditionsSatisfiedZEvent> {
    private AlertManager alertManager;

    @Autowired
    public AlertConditionsSatisfiedListener(AlertManager alertManager) {
        this.alertManager = alertManager;
    }

    public void processEvents(List<AlertConditionsSatisfiedZEvent> events) {
        for (AlertConditionsSatisfiedZEvent z : events) {
            alertManager.fireAlert(z);
        }
    }

}
