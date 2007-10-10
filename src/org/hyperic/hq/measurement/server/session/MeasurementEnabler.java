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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.TrackerManagerLocal;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;

class MeasurementEnabler 
    implements ZeventListener
{
    private static Log _log = LogFactory.getLog(MeasurementEnabler.class);

    public void processEvents(List events) {
        DerivedMeasurementManagerLocal dm =
            DerivedMeasurementManagerEJBImpl.getOne();
        ConfigManagerLocal cm = ConfigManagerEJBImpl.getOne();
        TrackerManagerLocal tm = TrackerManagerEJBImpl.getOne();

        for (Iterator i=events.iterator(); i.hasNext(); ) {
            ResourceZevent z = (ResourceZevent)i.next();
            AuthzSubjectValue subject = z.getAuthzSubjectValue();
            AppdefEntityID id = z.getAppdefEntityID();
            boolean isCreate, isUpdate, isRefresh;

            isCreate = z instanceof ResourceCreatedZevent;
            isUpdate = z instanceof ResourceUpdatedZevent;
            isRefresh = z instanceof ResourceRefreshZevent;

            try {
                // Handle reschedules for when agents are updated.
                if (isRefresh) {
                    _log.info("Refreshing metric schedule for [" + id + "]");
                    dm.reschedule(id);
                    continue;
                }

                // For either create or update events, schedule the default
                // metrics
                if (dm.getEnabledMetricsCount(subject, id) == 0) {
                    _log.info("Enabling default metrics for [" + id + "]");
                    dm.enableDefaultMetrics(subject, id);
                }

                if (isCreate) {
                    // On initial creation of the service check if log or config
                    // tracking is enabled.  If so, enable it.  We don't auto
                    // enable log or config tracking for update events since
                    // in the callback we don't know if that flag has changed.
                    ConfigResponse c =
                        cm.getMergedConfigResponse(subject,
                                                   ProductPlugin.TYPE_MEASUREMENT,
                                                   id, true);
                    tm.enableTrackers(subject, id, c);
                }

            } catch(Exception e) {
                _log.warn("Unable to enable default metrics", e);
            }
        }
    }
}
