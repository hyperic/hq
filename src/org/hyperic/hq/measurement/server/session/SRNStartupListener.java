package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.shared.SRNManagerUtil;

public class SRNStartupListener 
    implements StartupListener
{
    public void hqStarted() {
        try {
            SRNManagerUtil.getLocalHome().create().initializeCache();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
}
