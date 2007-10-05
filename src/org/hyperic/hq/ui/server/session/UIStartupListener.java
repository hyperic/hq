package org.hyperic.hq.ui.server.session;

import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;

public class UIStartupListener implements StartupListener {

    public void hqStarted() {

        ZeventManager.getInstance().addListener(ResourceDeletedZevent.class,
                                                ResourceDeleteWatcher.getInstance());
    }
}
