package org.hyperic.hq.events.server.session;

import org.hyperic.hq.application.StartupListener;

public class EventsStartupListener 
    implements StartupListener
{
   public void hqStarted() {
       // Make sure the escalation enumeration is loaded and registered so 
       // that the escalations run
       ClassicEscalationAlertType.class.getClass();
   }
}
