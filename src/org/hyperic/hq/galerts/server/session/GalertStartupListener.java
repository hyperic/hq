package org.hyperic.hq.galerts.server.session;

import org.hyperic.hq.application.StartupListener;

public class GalertStartupListener 
    implements StartupListener
{
    public void hqStarted() {
        GalertManagerEJBImpl.getOne().startup();
    }
}
