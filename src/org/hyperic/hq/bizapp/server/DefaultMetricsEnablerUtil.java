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

package org.hyperic.hq.bizapp.server;

import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.bizapp.server.session.BizappSessionEJB;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A singleton that can be used to enable default metrics
 * and runtime-AI.
 *
 * This way, both the MeasurementBoss session bean and
 * the DefaultMetricsEnabler MDB can share the code.
 */
public class DefaultMetricsEnablerUtil {

    private static final Log log
        = LogFactory.getLog(DefaultMetricsEnablerUtil.class.getName());

    private static final DefaultMetricsEnablerUtil _instance 
        = new DefaultMetricsEnablerUtil();
    public static DefaultMetricsEnablerUtil instance () { return _instance; }
    private DefaultMetricsEnablerUtil () {}

    public static final MetricEnablerSemaphore metricEnabler
        = MetricEnablerSemaphore.getInstance();

    private static final Object RUNTIME_AI_MUTEX = new Object();

    protected String getMonitorableType(AuthzSubjectValue subject,
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
    
    public boolean enableDefaultMetrics (AuthzSubjectValue subject,
                                         AppdefEntityID id,
                                         BizappSessionEJB caller,
                                         boolean isCreate) 
        throws AppdefEntityNotFoundException, TemplateNotFoundException,
               PermissionException, ConfigFetchException, 
               EncodingException, MeasurementCreateException,
               InvalidConfigException, 
               SessionTimeoutException, SessionNotFoundException {
               
        String mtype = this.getMonitorableType(subject, id);

        // No monitorable type
        if (mtype == null) return false;

        // This will throw an error if something needs to be
        // configured
        ConfigResponse mergedCR;
        try {
            mergedCR =
                caller.getConfigManager()
                .getMergedConfigResponse(subject,
                                         ProductPlugin.TYPE_MEASUREMENT,
                                         id, true);
        } catch (ConfigFetchException cfe) {
            // This is expected for resources that are not fully configured
            log.info(cfe.getMessage());
            return false;
        }
        
        // Is the config OK?
        try {
            caller.getRawMeasurementManager().checkConfiguration(subject, 
                                                                 id,
                                                                 mergedCR);
        } catch (InvalidConfigException e) {
            log.warn("Error turning on default metrics, configuration (" +
                     mergedCR + ") " + "couldn't be validated: " + e.getMessage());
                caller.getConfigManager().setValidationError(subject, id, 
                                                             e.getMessage());
                throw e;
        } catch (Exception e) {
            log.warn("Error turning on default metrics, " +
                     "error in validation: " + e.getMessage());
            caller.getConfigManager().setValidationError(subject, id, 
                                                         e.getMessage());
            // wrap it just so the user knows something bad happened
            throw new InvalidConfigException(e.getMessage());
        }

        metricEnabler.enableDefaultMetrics(subject, id, mtype, mergedCR);
        caller.getConfigManager().clearValidationError(subject, id);

        return true;
    }
}
