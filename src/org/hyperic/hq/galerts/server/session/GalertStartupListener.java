package org.hyperic.hq.galerts.server.session;

import org.hyperic.hq.application.StartupListener;

public class GalertStartupListener 
    implements StartupListener
{
    public void hqStarted() {
        GalertManagerEJBImpl.getOne().startup();

        // Make sure the escalation enumeration is loaded and registered so 
        // that the escalations run
        GalertEscalationAlertType.class.getClass();
    }
}
