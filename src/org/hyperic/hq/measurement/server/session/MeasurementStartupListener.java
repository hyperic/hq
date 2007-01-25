/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.measurement.server.session;

import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.shared.SRNManagerUtil;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.product.server.MBeanUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanServer;
import java.util.List;
import java.util.Iterator;

public class MeasurementStartupListener
    implements StartupListener
{
    private static Log _log =
        LogFactory.getLog(MeasurementStartupListener.class);

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
                        new MeasurementEnablerListener());

        /**
         * Add measurement rescheduler to reschedule metrics when resources
         * are updated.
         */
        ZeventManager.getInstance().
            addListener(ResourceUpdatedZevent.class,
                        new MeasurementReschedulerListener());
    }

    /**
     * A ZeventListener that listens for Resource create events, enabling the
     * default set of metrics
     */
    private class MeasurementEnablerListener implements ZeventListener {

        public void processEvents(List events) {
            for (Iterator i = events.iterator(); i.hasNext();) {
                ResourceCreatedZevent z= (ResourceCreatedZevent) i.next();

                MeasurementEnabler.getInstance().enableDefaultMetrics(z);
            }
        }
    }

    /**
     * A ZeventListener that listens for Resource update events, rescheduling
     * it's metrics with the new configuration.  If no metrics are found for the
     * resource the default set of metrics are enabled.
     */
    private class MeasurementReschedulerListener implements ZeventListener {
        private static final String SCHEDULER_OBJ =
            "hyperic.jmx:type=Service,name=MeasurementSchedule";
        private static final String _method = "refreshSchedule";

        public void processEvents(List events)
        {
            for (Iterator i = events.iterator(); i.hasNext(); ) {
                ResourceUpdatedZevent zevent = (ResourceUpdatedZevent)i.next();
                AppdefEntityID id = zevent.getAppdefEntityID();
                AuthzSubjectValue subject = zevent.getAuthzSubjectValue();
                DerivedMeasurementManagerLocal dmManager =
                    DerivedMeasurementManagerEJBImpl.getOne();

                try {
                    int count = dmManager.getEnabledMetricsCount(subject, id);
                    if (count == 0) {
                        // No enabled metrics, schedule the default metrics
                        _log.info("Enabling default metrics for " + id);
                        MeasurementEnabler.getInstance()
                                          .enableDefaultMetrics(zevent);
                    } else {
                        // Reschedule
                        _log.info("Rescheduling metric schedule for " + id);

                        MBeanServer server = MBeanUtil.getMBeanServer();
                        ObjectName obj = new ObjectName(SCHEDULER_OBJ);
                        server.invoke(obj, _method,
                                      new Object[] { id },
                                      new String[] { AppdefEntityID.class.getName() });
                    }
                } catch (Exception e) {
                    _log.error("Unable to refresh schedule for id=" + id, e);
                }
            }
        }
    }
}

