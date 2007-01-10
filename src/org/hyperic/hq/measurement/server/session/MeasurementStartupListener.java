package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.shared.SRNManagerUtil;
import org.hyperic.hq.zevents.ZeventManager;

public class MeasurementStartupListener
    implements StartupListener
{
    public void hqStarted() {

        /**
         * Initialize the SRN cache
         */
        try {
            SRNManagerUtil.getLocalHome().create().initializeCache();
        } catch(Exception e) {
            throw new SystemException(e);
        }

        /**
         * Add measurement enabler listener to enable metrics for newly
         * created resources.
         */
        ZeventManager.getInstance().
            addListener(ResourceCreatedZevent.class,
                        new MeasurementEnablerZeventListener());
    }
}
