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
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.zevents.ZeventListener;

class MeasurementEnabler 
    implements ZeventListener
{
    private static Log _log = LogFactory.getLog(MeasurementEnabler.class);

    public void processEvents(List events) {
        DerivedMeasurementManagerLocal dm = 
            DerivedMeasurementManagerEJBImpl.getOne();

        for (Iterator i=events.iterator(); i.hasNext(); ) {
            ResourceZevent z = (ResourceZevent)i.next();
            AuthzSubjectValue subject = z.getAuthzSubjectValue();
            AppdefEntityID id = z.getAppdefEntityID();
            boolean isUpdate;
            
            isUpdate = z instanceof ResourceUpdatedZevent;
            
            try {
                if (!isUpdate || dm.getEnabledMetricsCount(subject, id) == 0) {
                    _log.info("Enabling default metrics for [" + id + "]");
                    dm.enableDefaultMetrics(subject, id);
                } else {
                    _log.info("Rescheduling metric schedule for [" + id + "]");
                    dm.reschedule(id);
                }
            } catch(Exception e) {
                _log.warn("Unable to enable default metrics", e);
            }
        }
    }
}
