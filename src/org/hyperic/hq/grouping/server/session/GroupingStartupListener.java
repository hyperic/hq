package org.hyperic.hq.grouping.server.session;

import org.hyperic.hq.application.StartupListener;

public class GroupingStartupListener 
    implements StartupListener
{
    public void hqStarted() {
        System.out.println("HQ HAS STARTED");
    }
}
