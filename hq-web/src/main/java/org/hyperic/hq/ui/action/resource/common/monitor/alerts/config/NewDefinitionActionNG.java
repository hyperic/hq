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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.EventsBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * Create a new alert definition.
 * 
 */
@Component("newAlertDefinitionActionNG")
@Scope("prototype")
public class NewDefinitionActionNG extends BaseActionNG implements ModelDriven<DefinitionFormNG>{

    private final Log log = LogFactory.getLog(NewDefinitionActionNG.class.getName());
    @Autowired
    private EventsBoss eventsBoss;
    @Autowired
    private MeasurementBoss measurementBoss;

    private DefinitionFormNG defForm = new DefinitionFormNG();
    private String eid;
    private String aetid;
    private Integer ad;

    public String execute() throws Exception {

        AppdefEntityID adeId;
        if (defForm.getRid() != null) {
            adeId = new AppdefEntityID(defForm.getType().intValue(), defForm.getRid());
            eid = adeId.getAppdefKey();
        } else {
            adeId = new AppdefEntityTypeID(defForm.getType().intValue(), defForm.getResourceType());
            aetid = adeId.getAppdefKey();
        }

        String forward = checkSubmit(defForm);
        if (forward != null) {
            log.trace("returning " + forward);
            return forward;
        }

        int sessionID = RequestUtils.getSessionId(request).intValue();

        AlertDefinitionValue adv = new AlertDefinitionValue();
        defForm.exportProperties(adv);
        defForm.exportConditionsEnablement(adv, request, sessionID, measurementBoss,
            adeId instanceof AppdefEntityTypeID);
        adv.setAppdefType(adeId.getType());
        adv.setAppdefId(adeId.getId());
        log.trace("adv=" + adv);

        if (adeId instanceof AppdefEntityTypeID)
            try {
                adv = eventsBoss.createResourceTypeAlertDefinition(sessionID, (AppdefEntityTypeID) adeId, adv);
            } catch (Exception e) {
                return "failure";
            }
        else
            adv = eventsBoss.createAlertDefinition(sessionID, adv);

        ad = adv.getId();

        if (areAnyMetricsDisabled(adv, adeId, sessionID)) {
            addActionError(getText("resource.common.monitor.alert.config.error.SomeMetricsDisabled"));
        } else {
            addActionMessage(getText("resource.common.monitor.alert.config.confirm.Create"));
        }
        return SUCCESS;
    }

    private boolean areAnyMetricsDisabled(AlertDefinitionValue adv, AppdefEntityID adeId, int sessionID)
        throws SessionNotFoundException, SessionTimeoutException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, PermissionException, RemoteException {
        // create a map of metricId --> enabled for this resource
        List metrics = measurementBoss.findMeasurements(sessionID, adeId, PageControl.PAGE_ALL);
        Map<Integer, Boolean> metricEnabledFlags = new HashMap<Integer, Boolean>(metrics.size());
        for (Iterator it = metrics.iterator(); it.hasNext();) {
            // Groups are handled differently here. The list of
            // metrics that will be returned for a group will be
            // GroupMetricDisplaySummary beans instead of
            // DerivedMeasurementValue beans. We cannot check the
            // enabled status of these measurements for groups, so
            // don't do anything here.
            try {
                Measurement m = (Measurement) it.next();
                metricEnabledFlags.put(m.getId(), new Boolean(m.isEnabled()));
            } catch (ClassCastException e) {

            }
        }

        // iterate over alert conditions and see if any of the metrics
        // being used are disabled
        for (int i = 0; i < adv.getConditions().length; ++i) {
            if (adv.getConditions()[i].measurementIdHasBeenSet()) {
                Integer mid = new Integer(adv.getConditions()[i].getMeasurementId());
                Boolean metricEnabled = (Boolean) metricEnabledFlags.get(mid);
                if (null != metricEnabled) {
                    return metricEnabled.equals(Boolean.FALSE);
                }
            }
        }

        return false;
    }

	public DefinitionFormNG getModel() {
		return defForm;
	}

	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	public String getAetid() {
		return aetid;
	}

	public void setAetid(String aetid) {
		this.aetid = aetid;
	}

	public Integer getAd() {
		return ad;
	}

	public void setAd(Integer ad) {
		this.ad = ad;
	}

	public DefinitionFormNG getDefForm() {
		return defForm;
	}

	public void setDefForm(DefinitionFormNG defForm) {
		this.defForm = defForm;
	}
}
