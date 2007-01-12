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

import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import java.util.List;
import java.util.Iterator;

/**
 * A ZeventListener that listens for Resource update events, rescheduling
 * it's metrics with the new configuration.  If no metrics are found for the
 * resource the default set of metrics are enabled.
 */
class MeasurementReschedulerZeventListener implements ZeventListener {

    private static Log _log =
        LogFactory.getLog(MeasurementReschedulerZeventListener.class);

    private static final String _method = "refreshSchedule";
    private static ObjectName _object;

    static {
        try {
            _object = new ObjectName("hyperic.jmx:type=Service,name=MeasurementSchedule");
        } catch (MalformedObjectNameException e) {
            throw new SystemException(e);
        }
    }

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
                    MeasurementEnabler.enableDefaultMetrics(subject, id);
                } else {
                    // Reschedule
                    _log.info("Rescheduling metric schedule for " + id);

                    MBeanServer server = MBeanUtil.getMBeanServer();
                    server.invoke(_object, _method,
                                  new Object[] { id },
                                  new String[] { AppdefEntityID.class.getName()});
                }
            } catch (Exception e) {
                _log.error("Unable to refresh schedule for id=" + id, e);
            }
        }
    }
}
