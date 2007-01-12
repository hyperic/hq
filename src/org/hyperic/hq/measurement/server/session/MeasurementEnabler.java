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

import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.util.config.ConfigResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MeasurementEnabler {

    private static Log _log = LogFactory.getLog(MeasurementEnabler.class);

    private static String getMonitorableType(AuthzSubjectValue subject,
                                        AppdefEntityID id)
        throws AppdefEntityNotFoundException, PermissionException {

        AppdefEntityValue av;
        String mtype = null;
        switch(id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                av = new AppdefEntityValue(id, subject);
                mtype = av.getMonitorableType();
                break;
            default:
                break;
        }
        return mtype;
    }

    public static void enableDefaultMetrics(AuthzSubjectValue subject,
                                            AppdefEntityID id) {
        ConfigManagerLocal configManager = ConfigManagerEJBImpl.getOne();
        RawMeasurementManagerLocal rmManager =
            RawMeasurementManagerEJBImpl.getOne();
        DerivedMeasurementManagerLocal dmManager =
            DerivedMeasurementManagerEJBImpl.getOne();

        String mtype;
        ConfigResponse config;
        try {
            mtype = getMonitorableType(subject, id);
            // No monitorable type
            if (mtype == null) return;

            config = ConfigManagerEJBImpl.getOne().
                getMergedConfigResponse(subject,
                                        ProductPlugin.TYPE_MEASUREMENT,
                                        id, true);
        } catch (Exception e) {
            _log.error("Unable to enable default metrics for id=" + id +
                ": " + e.getMessage(), e);
            return;
        }

        // Check the configuration
        try {
            rmManager.checkConfiguration(subject, id, config);
        } catch (
            InvalidConfigException e) {
            _log.warn("Error turning on default metrics, configuration (" +
                config + ") " + "couldn't be validated: " +
                e.getMessage());
            configManager.setValidationError(subject, id, e.getMessage());
        } catch (Exception e) {
            _log.warn("Error turning on default metrics, " +
                "error in validation: " + e.getMessage());
            configManager.setValidationError(subject, id, e.getMessage());
        }

        // Enable the metrics
        try {
            dmManager.createDefaultMeasurements(subject, id, mtype, config);
            configManager.clearValidationError(subject, id);

            //XXX: Send new metric event!

        } catch (Exception e) {
            _log.warn("Unable to enable default metrics for id=" + id +
                ": " + e.getMessage(), e);
        }
    }
}

