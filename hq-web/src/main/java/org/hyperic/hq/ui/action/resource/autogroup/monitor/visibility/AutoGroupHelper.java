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

package org.hyperic.hq.ui.action.resource.autogroup.monitor.visibility;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.MetricsDisplayFormPrepareAction;
import org.hyperic.hq.ui.util.ContextUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A <code>MetricsDisplayFormPrepareAction</code> that retrieves data
 * from the Bizapp to be displayed on an <code>AutoGroup
 * Metrics</code> page.
 */
public final class AutoGroupHelper {
    private static Log log =
        LogFactory.getLog( AutoGroupHelper.class.getName() );

    public static List getAutoGroupResourceHealths(ServletContext ctx,
                                                   Integer sessionId,
                                                   AppdefEntityID[] entityIds,
                                                   AppdefEntityTypeID childTypeId)
        throws Exception {
        MeasurementBoss boss = ContextUtils.getMeasurementBoss(ctx);

        if (null == entityIds) {
            // auto-group of platforms
            log.trace("finding current health for autogrouped platforms " +
                      "of type " + childTypeId);
            return boss.findAGPlatformsCurrentHealthByType(sessionId.intValue(),
                                                           childTypeId.getId());
        } else {
            // auto-group of servers or services
            switch (childTypeId.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                return boss.findAGServersCurrentHealthByType(sessionId.intValue(),
                                                             entityIds, 
                                                             childTypeId.getId());
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    log.trace("finding current health for autogrouped services " +
                              "of type " + childTypeId + " for resources " +
                              Arrays.asList(entityIds));
                    return boss.findAGServicesCurrentHealthByType(sessionId.intValue(),
                                                                  entityIds,
                                                                  childTypeId.getId());
            default:
                log.trace("finding current health for autogrouped services " +
                          "of type " + childTypeId + " for resources " +
                          Arrays.asList(entityIds));
                return boss.findAGServicesCurrentHealthByType(sessionId.intValue(),
                                                              entityIds,
                                                              childTypeId.getId());
            }
        }
    }
}

// EOF
