package org.hyperic.hq.bizapp.server.session;

import org.hyperic.hq.application.StartupListener;

public class BossStartupListener 
    implements StartupListener
{
    public void hqStarted() {
        EventsBossEJBImpl.getOne().startup();
        UpdateBossEJBImpl.getOne().startup();
        ProductBossEJBImpl.getOne().preload();
        SystemAudit.createUpAudit();
    }
}
