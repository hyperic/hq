package org.hyperic.hq.bizapp.server.session;

import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupFinishedCallback;
import org.hyperic.hq.application.StartupListener;

public class BossStartupListener 
    implements StartupListener, StartupFinishedCallback
{
    private static final Object LOCK = new Object();
    private static UpdateReportAppender _updateCallback; 
    
    public void hqStarted() {
        EventsBossImpl.getOne().startup();
        EventLogBossImpl.getOne().startup();
        AuthBossImpl.getOne().startup();
        AppdefBossImpl.getOne().startup();
        ProductBossImpl.getOne().preload();
       
        
        synchronized (LOCK) {
            _updateCallback = (UpdateReportAppender)HQApp.getInstance()
                 .registerCallbackCaller(UpdateReportAppender.class);
        }
        
        HQApp.getInstance().registerCallbackListener(StartupFinishedCallback.class,
                                                     this);
    }
    
    public void startupFinished() {
        UpdateBossImpl.getOne().startup();
    }

    static UpdateReportAppender getUpdateReportAppender() {
        synchronized(LOCK) {
            return _updateCallback;
        }
    }
    
   
}
