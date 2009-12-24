/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.MonitorUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A <code>BaseAction</code> that handles alert metrics control form
 * submissions.
 */
public class AlertMetricsControlAction
    extends MetricsControlAction {

    @Autowired
    public AlertMetricsControlAction(AuthzBoss authzBoss) {
        super(authzBoss);

    }

    /**
     * Modify the metrics summary display as specified in the given
     * <code>MetricsControlForm</code>.
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        AlertMetricsControlForm controlForm = (AlertMetricsControlForm) form;
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(session);

        if (controlForm.getAlertDefaults().equals(Boolean.FALSE)) {
            Map<String, Object> pref = user.getMetricRangePreference();
            Boolean ro = (Boolean) pref.get(MonitorUtils.RO);

            if (ro.booleanValue()) {
                controlForm.setAlertDefaults(Boolean.TRUE);
            } else {
                Integer lastN = (Integer) pref.get(MonitorUtils.LASTN);
                Integer unit = (Integer) pref.get(MonitorUtils.UNIT);
                long rangeMillis = lastN.longValue();

                if (unit.intValue() == MonitorUtils.UNIT_DAYS) {
                    rangeMillis = rangeMillis * 86400000;
                } else if (unit.intValue() == MonitorUtils.UNIT_HOURS) {
                    rangeMillis = rangeMillis * 3600000;
                } else if (unit.intValue() == MonitorUtils.UNIT_MINUTES) {
                    rangeMillis = rangeMillis * 60000;
                }
                // If the user's global metric range preference is greater than
                // 48 hours, then automatically reset their preference to the
                // default settings
                if (rangeMillis > 172800000) {
                    controlForm.setAlertDefaults(Boolean.TRUE);
                }
            }
        }

        return super.execute(mapping, form, request, response);
    }

}
