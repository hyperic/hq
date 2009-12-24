/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work". Copyright (C) [2004,
 * 2005, 2006], Hyperic, Inc. This file is part of HQ. HQ is free software; you
 * can redistribute it and/or modify it under the terms version 2 of the GNU
 * General Public License as published by the Free Software Foundation. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.ui.action.resource.common.monitor.visibility;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.bizapp.shared.uibeans.ProblemMetricSummary;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A portlet for problem metrics
 */
public class ProblemMetricsDisplayAction
    extends TilesAction {

    private static final String RESOURCE_NAMES = "ProblemMetricsDisplayAction_resourceNames";

    private AppdefBoss appdefBoss;
    private MeasurementBoss measurementBoss;

    @Autowired
    public ProblemMetricsDisplayAction(AppdefBoss appdefBoss, MeasurementBoss measurementBoss) {
        super();
        this.appdefBoss = appdefBoss;
        this.measurementBoss = measurementBoss;
    }

    /**
     * Fetch the list of problem metrics for a resource
     */
    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws Exception {

        WebUser user = RequestUtils.getWebUser(request);
        int sessionId = user.getSessionId().intValue();
        AppdefEntityID aeid = RequestUtils.getEntityId(request);

        // Now fetch the display range
        Map<String, Object> range = user.getMetricRangePreference();
        long begin = ((Long) range.get(MonitorUtils.BEGIN)).longValue();
        long end = ((Long) range.get(MonitorUtils.END)).longValue();
        ProblemMetricsDisplayForm probForm = (ProblemMetricsDisplayForm) form;
        String[] resStrs;
        AppdefEntityID[] hosts = null;
        Map<String, String> resourceNames = new HashMap<String, String>();

        request.getSession().removeAttribute(RESOURCE_NAMES);

        if ((resStrs = probForm.getHost()).length > 0) {
            hosts = new AppdefEntityID[resStrs.length];

            for (int i = 0; i < resStrs.length; i++) {
                hosts[i] = new AppdefEntityID(resStrs[i]);
            }

            PageList<AppdefResourceValue> resources = appdefBoss.findByIds(sessionId, hosts, null);

            for (Iterator<AppdefResourceValue> i = resources.iterator(); i.hasNext();) {
                AppdefResourceValue resource = i.next();

                resourceNames.put(resource.getEntityId().getAppdefKey(), resource.getName());
            }

            if (!resourceNames.isEmpty()) {
                request.getSession().setAttribute(RESOURCE_NAMES, resourceNames);
            }
        }

        AppdefEntityTypeID childTypeID = null;

        if (probForm.getCtype() != null && probForm.getCtype().length() > 0) {
            // Autogroup
            childTypeID = new AppdefEntityTypeID(probForm.getCtype());
        }

        List<ProblemMetricSummary> problems;

        if ((resStrs = probForm.getEids()).length > 0) {
            AppdefEntityID[] entities = new AppdefEntityID[resStrs.length];

            for (int i = 0; i < resStrs.length; i++) {
                entities[i] = new AppdefEntityID(resStrs[i]);
            }

            // Autogroups go by specific entities
            AppdefEntityTypeID[] childTypes = null;

            if (childTypeID != null) {
                childTypes = new AppdefEntityTypeID[] { childTypeID };
            }

            problems = measurementBoss.findAllMetrics(sessionId, aeid, hosts, childTypes, entities, begin, end);
            // Set the resources for the view
            request.getSession().setAttribute(aeid.getAppdefKey() + ".entities", entities);
        } else if (childTypeID != null) { // Autogroup
            if (probForm.getHost().length > 0) {
                // Host selected, make ctype the children array
                AppdefEntityTypeID[] children = new AppdefEntityTypeID[] { childTypeID };

                problems = measurementBoss.findAllMetrics(sessionId, aeid, null, children, begin, end);
            } else {
                problems = measurementBoss.findAllMetrics(sessionId, aeid, childTypeID, begin, end);
            }
        } else {
            AppdefEntityTypeID[] children = null;

            if ((resStrs = probForm.getChild()).length > 0) {
                children = new AppdefEntityTypeID[resStrs.length];

                for (int i = 0; i < resStrs.length; i++) {
                    children[i] = new AppdefEntityTypeID(resStrs[i]);
                }
            }

            problems = measurementBoss.findAllMetrics(sessionId, aeid, hosts, children, begin, end);
            // Clear the resources for the view
            request.getSession().removeAttribute(aeid.getAppdefKey() + ".entities");
        }

        context.putAttribute("problems", problems);

        return null;
    }
}