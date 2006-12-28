package org.hyperic.hq.galerts.server.session;

import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.galerts.shared.GalertManagerUtil;

public class GalertStartupListener 
    implements StartupListener
{
    public void hqStarted() {
        try {
            GalertManagerUtil.getLocalHome().create().startup();
        } catch (Exception e) {
            throw new SystemException(e);           
        }
    }
}
