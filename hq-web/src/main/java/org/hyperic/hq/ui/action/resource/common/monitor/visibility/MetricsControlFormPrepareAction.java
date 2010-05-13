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

import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.timer.StopWatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.actions.TilesAction;

/**
 * A base class for <code>Action</code>s that prepare pages containing the
 * metrics control form.
 */
public class MetricsControlFormPrepareAction
    extends TilesAction {

    protected final Log log = LogFactory.getLog(MetricsControlFormPrepareAction.class.getName());

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {
        StopWatch watch = new StopWatch();

        MetricsControlForm controlForm = (MetricsControlForm) form;

        AppdefResourceValue resource = RequestUtils.getResource(request);
        if (resource == null) {
            return null;
        }
        AppdefEntityID entityId = resource.getEntityId();

        controlForm.setRid(resource.getId());
        controlForm.setType(new Integer(entityId.getType()));
        prepareForm(request, controlForm);

        if (log.isDebugEnabled()) {
            log.debug("IndicatorChartsAction.fresh: " + watch);
        }
        
        return null;
    }

    // ---------------------------------------------------- Protected Methods

    protected void prepareForm(HttpServletRequest request, MetricsControlForm form, MetricRange range)
        throws InvalidOptionException {
        WebUser user = SessionUtils.getWebUser(request.getSession());

        // set metric range defaults
        Map<String, Object> pref = user.getMetricRangePreference(true);
        form.setReadOnly((Boolean) pref.get(MonitorUtils.RO));
        form.setRn((Integer) pref.get(MonitorUtils.LASTN));
        form.setRu((Integer) pref.get(MonitorUtils.UNIT));

        Long begin, end;

        if (range != null) {
            begin = range.getBegin();
            end = range.getEnd();
        } else {
            begin = (Long) pref.get(MonitorUtils.BEGIN);
            end = (Long) pref.get(MonitorUtils.END);
        }

        form.setRb(begin);
        form.setRe(end);

        form.populateStartDate(new Date(begin.longValue()), request.getLocale());
        form.populateEndDate(new Date(end.longValue()), request.getLocale());

        Boolean readOnly = (Boolean) pref.get(MonitorUtils.RO);
        if (readOnly.booleanValue()) {
            form.setA(MetricDisplayRangeForm.ACTION_DATE_RANGE);
        } else {
            form.setA(MetricDisplayRangeForm.ACTION_LASTN);
        }
    }

    protected void prepareForm(HttpServletRequest request, MetricsControlForm form) throws InvalidOptionException {
        prepareForm(request, form, null);
    }
}
