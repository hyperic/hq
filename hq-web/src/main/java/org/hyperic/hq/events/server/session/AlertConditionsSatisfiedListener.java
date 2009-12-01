package org.hyperic.hq.events.server.session;

import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.zevents.ZeventListener;

/**
 * Receives AlertConditionSatisfiedZEvents and forwards them to the AlertManager
 * for processing
 * @author jhickey
 *
 */
public class AlertConditionsSatisfiedListener implements ZeventListener {

    public void processEvents(List events) {
        AlertManager am = AlertManagerImpl.getOne();
        
        for (Iterator i=events.iterator(); i.hasNext(); ) {
            AlertConditionsSatisfiedZEvent z = (AlertConditionsSatisfiedZEvent)i.next();
            am.fireAlert(z);
        }
    }

}
