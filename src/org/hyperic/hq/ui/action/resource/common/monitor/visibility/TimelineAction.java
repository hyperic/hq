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

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.EventLogBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.beans.TimelineBean;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.TimeUtil;

/**
 * 
 * Set an array for the timeline display
 */
public class TimelineAction extends TilesAction {

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.struts.action.Action#execute(org.apache.struts.action.ActionMapping,
     *      org.apache.struts.action.ActionForm,
     *      javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        WebUser user = (WebUser) request.getSession()
                .getAttribute(Constants.WEBUSER_SES_ATTR);
        Map range = user.getMetricRangePreference();
        long begin = ((Long) range.get(MonitorUtils.BEGIN)).longValue();
        long end = ((Long) range.get(MonitorUtils.END)).longValue();
        long[] intervals = new long[Constants.DEFAULT_CHART_POINTS];

        // Get the events count
        ServletContext ctx = getServlet().getServletContext();
        EventLogBoss boss = ContextUtils.getEventLogBoss(ctx);
        AppdefEntityID aeid = RequestUtils.getEntityId(request);

        boolean[] logsExist = boss.logsExistPerInterval(user.getSessionId().intValue(),
                                                       aeid, begin, end,
                                                       intervals.length);

        // Create the time intervals beans
        TimelineBean[] beans = new TimelineBean[intervals.length];
        long interval = TimeUtil.getInterval(begin, end,
                                             Constants.DEFAULT_CHART_POINTS);
        for (int i = 0; i < intervals.length; i++) {
            beans[i] = new TimelineBean(begin + (interval * i),logsExist[i]);
        }

        request.setAttribute(Constants.TIME_INTERVALS_ATTR, beans);

        return null;
    }
}
