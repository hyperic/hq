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

package org.hyperic.hq.ui.action.resource.platform.autodiscovery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.autoinventory.ScanConfiguration;
import org.hyperic.hq.autoinventory.ScanMethod;
import org.hyperic.hq.autoinventory.ScanMethodConfig;
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.bizapp.shared.AIBoss;
import org.hyperic.util.config.ConfigResponse;

public class EditAutoDiscoveryAction extends NewAutoDiscoveryAction {
    
    private static final Log log
        = LogFactory.getLog(EditAutoDiscoveryAction.class.getName());

    /**
     * load the AIScheduleValue if needed
     */
    private AIScheduleValue getAIScheduleValue(AIBoss aiboss,
                                               int sessionId, 
                                               Integer scheduleId)
        throws Exception {
        return aiboss.findScheduledJobById(sessionId, scheduleId);
    }
    
    /**
     * assume there are only one ScanMethodConfig
     * 
     * @return a ConfigResponse to a ScanMethodConfig
     */
    protected ConfigResponse getConfigResponse(AIBoss aiboss,
                                               int sessionId,
                                               Integer scheduleId,
                                               ScanConfiguration scanConfig,
                                               ScanMethod scanMethod )
        throws Exception {

        AIScheduleValue sched
            = getAIScheduleValue(aiboss, sessionId, scheduleId);
        ScanMethodConfig[] configs = sched.getConfigObj().getScanMethodConfigs();
        if (configs.length > 0)
            return configs[0].getConfig();

        return null;
    }
}
