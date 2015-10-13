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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * An Action that removes an alert
 */
@Component("alertsRemoveActionNG")
@Scope("prototype")
public class RemoveActionNG
    extends BaseActionNG implements ModelDriven<RemoveFormNG>{
	
	@Autowired
    private EventsBoss eventsBoss;
    private final Log log = LogFactory.getLog(RemoveActionNG.class.getName());

    private RemoveFormNG nwForm = new RemoveFormNG();
	
    private String eid;

    /**
     * removes alerts
     */
    public String execute() throws Exception {

        request = getServletRequest();
        log.debug("entering removeAlertsAction");
        Integer type = nwForm.getType();
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(Constants.ENTITY_ID_PARAM, nwForm.getEid());

        eid = nwForm.getEid();
        String forward = checkSubmit( nwForm);
        // if the remove button was clicked, we are coming from
        // the alerts list page and just want to continue
        // processing ...
        if (forward != null && !forward.equals(Constants.REMOVE_URL)) {
            log.trace("returning " + forward);
            // if there is no resource type, there is probably no
            // resource -- go to dashboard on cancel
            if (forward.equals(Constants.CANCEL_URL) && type.intValue() == 0) {
                return returnNoResource();
            }
            return forward;
        }

        Integer[] alertIds = nwForm.getAlerts();
        String[] escalatables = nwForm.getEalerts();

        if (log.isDebugEnabled()) {
            if (alertIds != null) {
                log.debug("acting on alerts: " + Arrays.asList(alertIds));
            }
            if (escalatables != null) {
                log.debug("acting on ealerts: " + Arrays.asList(escalatables));
            }
        }

        if ((alertIds == null || alertIds.length == 0) && (escalatables == null || escalatables.length == 0)) {
            return SUCCESS;
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        if (nwForm.isDeleteClicked()) {
            log.debug("!!!!!!!!!!!!!!!! removing alerts!!!!!!!!!!!!");
            eventsBoss.deleteAlerts(sessionId.intValue(), alertIds);
        } else if (nwForm.getButtonAction() != null) {
            if ("ACKNOWLEDGE".equals(nwForm.getButtonAction())) {
                log.debug("Acknowledge alerts");

                if (alertIds != null) {
                    for (int i = 0; i < alertIds.length; i++) {
                        // XXX: This only works for classic alert types ATM
                        eventsBoss.acknowledgeAlert(sessionId.intValue(), ClassicEscalationAlertType.CLASSIC,
                            alertIds[i], nwForm.getPauseTime(), nwForm.getAckNote());
                    }
                }

                if (escalatables != null) {
                    log.debug("Escalatable alerts");
                    for (int i = 0; i < escalatables.length; i++) {
                        StringTokenizer st = new StringTokenizer(escalatables[i], ":");

                        int code = Integer.parseInt(st.nextToken());
                        Integer alert = Integer.valueOf(st.nextToken());

                        eventsBoss.acknowledgeAlert(sessionId.intValue(), EscalationAlertType.findByCode(code), alert,
                            nwForm.getPauseTime(), nwForm.getAckNote());
                    }
                }
            } else if ("FIXED".equals(nwForm.getButtonAction())) {
                log.debug("Fixed alerts");

                if (alertIds != null) {
                    for (int i = 0; i < alertIds.length; i++) {
                        // This only works for classic alert types
                        eventsBoss.fixAlert(sessionId.intValue(), ClassicEscalationAlertType.CLASSIC, alertIds[i],
                            nwForm.getFixedNote(), nwForm.isFixAll());
                    }
                }

                if (escalatables != null) {
                    log.debug("Escalatable alerts");
                    for (int i = 0; i < escalatables.length; i++) {
                        StringTokenizer st = new StringTokenizer(escalatables[i], ":");

                        int code = Integer.parseInt(st.nextToken());
                        Integer alert = Integer.valueOf(st.nextToken());

                        eventsBoss.fixAlert(sessionId.intValue(), EscalationAlertType.findByCode(code), alert, nwForm
                            .getFixedNote(), nwForm.isFixAll());
                    }
                }
            }
        }

         if (nwForm.getEid() == null) {
            return returnNoResource();
        } else {
            return SUCCESS;
        }

    }

    protected String returnNoResource() throws Exception {
        return  "noresource";
    }

    
	public RemoveFormNG getNwForm() {
		return nwForm;
	}

	public void setNwForm(RemoveFormNG nwForm) {
		this.nwForm = nwForm;
	}

	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	public RemoveFormNG getModel() {
		
		return nwForm;
	}
}
