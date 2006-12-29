package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupListener;

public class GroupingStartupListener 
    implements StartupListener
{
    private static final Object LOCK = new Object();
    
    private static GroupChangeCallback _callbacks;
    
    public void hqStarted() {
        HQApp app = HQApp.getInstance();

        synchronized (LOCK) {
            _callbacks = (GroupChangeCallback)
                app.registerCallbackCaller(GroupChangeCallback.class);
        }
    }
    
    static GroupChangeCallback getCallbackObj() {
        synchronized (LOCK) {
            return _callbacks;
        }
    }
}
