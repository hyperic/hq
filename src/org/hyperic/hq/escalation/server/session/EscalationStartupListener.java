package org.hyperic.hq.escalation.server.session;

import org.hyperic.hq.application.StartupListener;

public class EscalationStartupListener 
    implements StartupListener
{
    public void hqStarted() {
        EscalationManagerEJBImpl.getOne().startup();
    }
}
