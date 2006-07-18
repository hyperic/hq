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

import javax.ejb.EJBLocalObject;

import org.hyperic.hq.measurement.shared.BaselineLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.DerivedMeasurementPK;
import org.hyperic.hq.measurement.shared.MeasurementTemplateLocal;
import org.hyperic.hq.measurement.server.session.DMValueCache;
import org.hyperic.util.pager.PagerProcessor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PagerProcessor_measurement implements PagerProcessor {
    private static final Log log =
        LogFactory.getLog(PagerProcessor_measurement.class);
    private static final boolean debug = log.isDebugEnabled();
    private DMValueCache cache;

    public PagerProcessor_measurement() {
        cache = DMValueCache.getInstance();
    }

    public Object processElement(Object o) {
        if (debug) {
            log.debug("PagerProcessor_dm: processElement starting");
        }
        if (o == null) {
            if (debug) {
                log.debug(
                    "PagerProcessor_dm: processElement returning null for element");
            }
            return null;
        }
        if (!(o instanceof EJBLocalObject)) {
            return o;
        }
        
        // EJB Local object processing
        if (o instanceof DerivedMeasurementLocal) {
            if (debug) {
                log.debug(
                    "PagerProcessor_dm: processElement converting DerivedMeasurementLocal to value object");
            }
            try {
                DerivedMeasurementLocal dmEjb = (DerivedMeasurementLocal)o;
                DerivedMeasurementPK pk = (DerivedMeasurementPK)dmEjb.getPrimaryKey();
                DerivedMeasurementValue dmv = cache.get(pk.getId());
                if(dmv == null) {
                    dmv = dmEjb.getDerivedMeasurementValue();
                    cache.put(dmv);
                }
                return dmv;
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Error converting to DerivedMeasurementValue: " + e);
            }
        }
        if (o instanceof MeasurementTemplateLocal) {
            if (debug) {
                log.debug(
                    "PagerProcessor_dm: processElement converting MeasurementTemplateLocal to value object");
            }
            try {
                return ((MeasurementTemplateLocal) o)
                    .getMeasurementTemplateValue();
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Error converting to MeasurementTemplateValue: " + e);
            }
        }
        if (o instanceof BaselineLocal) {
            if (debug) {
                log.debug(
                    "PagerProcessor_bl: processElement converting BaselineLocal to value object");
            }
            try {
                return ((BaselineLocal) o).getBaselineValue();
            } catch (Exception e) {
                if (log.isDebugEnabled())
                    log.debug("Error converting to BaselineValue", e);
                throw new IllegalStateException(
                    "Error converting to BaselineValue: " + e);
            }
        }

        if (debug) {
            log.debug("PagerProcessor_dm: processElement not processing object " +
                      o.getClass());
        }
        return o;
    }
}
