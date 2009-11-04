package org.hyperic.hq.events.server.session;

import java.util.List;

import org.hyperic.hq.zevents.ZeventListener;
/**
 * Listens for trigger creation events
 * @author jhickey
 *
 */
public class TriggersCreatedListener implements ZeventListener{


    public void processEvents(List events) {
       RegisteredTriggerManagerEJBImpl.getOne().handleTriggerCreatedEvents(events);
    }

}
