package org.hyperic.hq.escalation.server.session;

import org.hyperic.hq.application.StartupListener;

public class MEscalationStartupListener 
    implements StartupListener
{
    public void hqStarted() {
        MEscalationManagerEJBImpl.getOne().startup();
    }
}
