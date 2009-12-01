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

package org.hyperic.hq.ui.action.resource.group.monitor.visibility;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.ResourceMetricsFormPrepareAction;
import org.hyperic.hq.ui.util.RequestUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is for the "Group Metrics" page
 * 
 * Everything about ResourceMetricsFormPrepareAction is fine for groups,
 * except with groups we override getShowNumberCollecting()
 */
public class GroupMetricsFormPrepareAction
    extends ResourceMetricsFormPrepareAction {

    protected static Log log =
        LogFactory.getLog(GroupMetricsFormPrepareAction.class.getName());

    // ---------------------------------------------------- Protected Methods

    /* (non-Javadoc)
     * @see org.hyperic.hq.ui.action.resource.common.monitor.visibility.MetricsDisplayFormPrepareAction#getShowNumberCollecting()
     */
    protected Boolean getShowNumberCollecting() {
        return Boolean.TRUE;
    }
    
    
    /* (non-Javadoc)
     * @see org.hyperic.hq.ui.action.resource.common.monitor.visibility.MetricsDisplayFormPrepareAction#getMetrics(javax.servlet.http.HttpServletRequest, org.hyperic.hq.appdef.shared.AppdefEntityID, java.lang.Long, java.lang.Long)
     */
    protected Map getMetrics(HttpServletRequest request, long filters,
                             String keyword, AppdefEntityID entityId,
                             Long begin, Long end, boolean showAll)
        throws Exception{
                
        AppdefGroupValue group =
            (AppdefGroupValue) RequestUtils.getResource(request);
        request.setAttribute(Constants.CHILD_RESOURCE_TYPE_ATTR,
                             group.getAppdefResourceTypeValue());
        return super.getMetrics(request, entityId, filters, keyword, begin,
                                end, showAll);
    }

}
