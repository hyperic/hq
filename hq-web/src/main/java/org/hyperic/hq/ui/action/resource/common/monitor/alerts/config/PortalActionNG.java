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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

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
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.Portlet;
import org.hyperic.hq.ui.action.resource.ResourceControllerNG;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

/**
 * A dispatcher for the alerts portal.
 * 
 */
@Component("alertsConfigPortalActionNG")
@Scope("prototype")
public class PortalActionNG extends ResourceControllerNG implements
		ModelDriven<DefinitionFormNG>, Preparable {
	private final Log log = LogFactory.getLog(PortalActionNG.class.getName());

	protected final Properties keyMethodMap = new Properties();

	@Autowired
	protected EventsBoss eventsBoss;

	@Autowired
	private MeasurementBoss measurementBoss;
	
	private DefinitionFormNG defForm = new DefinitionFormNG();
	
	private String eid;
	private String aetid;
	private String alertDefId;
	
	protected Properties getKeyMethodMap() {
		return keyMethodMap;
	}

	private void initKeyMethodMap() {
		keyMethodMap.setProperty(Constants.MODE_LIST, "listDefinitions");
		keyMethodMap.setProperty(Constants.MODE_NEW, "newDefinition");
		keyMethodMap.setProperty(Constants.MODE_VIEW, "listDefinitions");
		keyMethodMap.setProperty("viewDefinition", "viewEscalation");
	}

	/**
	 * We override this in case the resource has been deleted ... simply ignore
	 * that fact.
	 */
	protected AppdefEntityID setResource() throws Exception {
		try {
			return super.setResource();
		} catch (ParameterNotFoundException e) {
			log.warn("No resource found.");
		}
		return null;
	}

	private void setTitle(HttpServletRequest request, Portal portal,
			String titleName) throws Exception {

		AppdefEntityID aeid;
		try {
			aeid = RequestUtils.getEntityTypeId(request);
		} catch (ParameterNotFoundException e) {
			aeid = RequestUtils.getEntityId(request);
		}

		titleName = BizappUtilsNG.replacePlatform(titleName, aeid);
		portal.setName(titleName);

		// if there's an alert definition available, set our second
		// title parameter to its name
		try {
			int sessionId = RequestUtils.getSessionId(request).intValue();

			AlertDefinitionValue adv = AlertDefUtil.getAlertDefinition(request,
					sessionId, eventsBoss);
			request.setAttribute(Constants.TITLE_PARAM2_ATTR, adv.getName());
		} catch (ParameterNotFoundException e) {
			// it's okay
			log.trace("couldn't find alert definition: " + e.getMessage());
		}
	}

	public String newDefinition() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal,
				"alert.config.platform.edit.NewAlertDef.Title");
		portal.addPortlet(new Portlet(".events.config.new"), 1);
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "newDefinition";
	}

	public String editProperties() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal, "alert.config.platform.edit.page.Title");
		portal.addPortlet(new Portlet(".events.config.edit.properties"), 1);
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "editProperties";
	}

	public String editConditions() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal, "alert.config.platform.edit.condition.Title");
		portal.addPortlet(new Portlet(".events.config.edit.conditions"), 1);
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "editConditions";
	}

	public String editControlAction() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal,
				"alerts.config.platform.EditControlAction.Title");
		portal.addPortlet(new Portlet(".events.config.edit.controlaction"), 1);
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "editControlAction";
	}

	public String editSyslogAction() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal,
				"alerts.config.platform.EditSyslogAction.Title");
		portal.addPortlet(new Portlet(".events.config.edit.syslogaction"), 1);
		portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "editSyslogAction";
	}

	public String viewOthers() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal,
				"alert.config.platform.props.ViewDef.email.Title");
		portal.addPortlet(new Portlet(".events.config.view.others"), 1);
		// JW - this shouldn't be a dialog ... portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "viewOthers";
	}

	public String viewUsers() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal,
				"alert.config.platform.props.ViewDef.users.Title");
		portal.addPortlet(new Portlet(".events.config.view.users"), 1);
		// JW - this shouldn't be a dialog ... portal.setDialog(true);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "viewUsers";
	}

	public String viewEscalation() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal,
				"alert.config.platform.props.ViewDef.escalation.Title");
		portal.addPortlet(new Portlet(".events.config.view.escalation"), 1);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "viewEscalation";
	}

	public String viewOpenNMS() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal,
				"alert.config.platform.props.ViewDef.openNMS.Title");
		portal.addPortlet(new Portlet(".events.config.view.opennms"), 1);
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "viewOpenNMS";
	}

	public String monitorConfigureAlerts() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		portal.addPortlet(new Portlet(".events.config.list"), 1);
		portal.setDialog(false);

		request.setAttribute(Constants.PORTAL_KEY, portal);

		return "monitorConfigureAlerts";
	}

	public String listDefinitions() throws Exception {
		AppdefEntityID aeid = setResource();

		setNavMapLocation(Constants.ALERT_CONFIG_LOC);

		// clean out the return path
		SessionUtils.resetReturnPath(request.getSession());
		/*
		 * // set the return path try { setReturnPath(request, mapping); } catch
		 * (ParameterNotFoundException pne) { log.debug(pne); }
		 */

		Portal portal = Portal.createPortal();
		setTitle(request, portal, "alerts.config.platform.DefinitionList.Title");
		portal.setDialog(false);
		String res = "listDefinitions";

		try {
			RequestUtils.getStringParameter(request,
					Constants.APPDEF_RES_TYPE_ID);
			portal.addPortlet(new Portlet(".admin.alerts.List"), 1);
		} catch (ParameterNotFoundException e) {
			if (aeid != null && aeid.isGroup()) {
				portal.addPortlet(new Portlet(".events.group.config.list"), 1);
				res = "listGroupDefinitions";
			} else {
				portal.addPortlet(new Portlet(".events.config.list"), 1);
			}
		}
		request.setAttribute(Constants.PORTAL_KEY, portal);

		return res;

	}

	public String addUsers() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal,
				"alerts.config.platform.AssignUsersToAlertDefinition.Title");
		portal.addPortlet(new Portlet(".events.config.addusers"), 1);
		portal.setDialog(false);

		request.setAttribute(Constants.PORTAL_KEY, portal);
		return "addUsers";
	}

	public String addOthers() throws Exception {
		setResource();
		Portal portal = Portal.createPortal();
		setTitle(request, portal,
				"alerts.config.platform.AssignOthersToAlertDefinition.Title");
		portal.addPortlet(new Portlet(".events.config.addothers"), 1);
		portal.setDialog(false);

		request.setAttribute(Constants.PORTAL_KEY, portal);
		return "addOthers";
	}


	public String save() throws Exception {
		
		request = getServletRequest();
		request.getSession().setAttribute("defForm",defForm);
		fillCondition();
		/*if (defForm.getName() == null || "".equals(defForm.getName())) {
			addFieldError(
					"name",
					getText("errors.required",
							new String[] { "name" }));
			return INPUT;
		}*/
		if (defForm.getName().length() > 255) {
			addFieldError(
					"name",
					getText("errors.maxlength", new String[] {
							"name", "255" }));
			return INPUT;
		}
		if (defForm.getDescription() != null
				&& defForm.getDescription().length() > 250) {
			addFieldError(
					"description",
					getText("errors.maxlength", new String[] {
							"description", "250" }));
			return INPUT;
		}
		if (defForm.getConditions() != null
				&& defForm.getConditions().length > 0) {
			Map<String, String> validationResults = defForm.validate(request,
					new HashMap<String, String>());
			if (!validationResults.isEmpty()) {
				for (String key : validationResults.keySet()) {
					addFieldError(key, validationResults.get(key));
				}
				request.setAttribute("defForm", defForm);
				return INPUT;
			}
		}
		Map<String, Object> params = new HashMap<String, Object>();
		AppdefEntityID adeId;
		if (defForm.getRid() != null) {
			adeId = new AppdefEntityID(defForm.getType().intValue(),
					defForm.getRid());
			params.put(Constants.ENTITY_ID_PARAM, adeId.getAppdefKey());
			eid = adeId.getAppdefKey();
		} else {
			adeId = new AppdefEntityTypeID(defForm.getType().intValue(),
					defForm.getResourceType());
			params.put(Constants.APPDEF_RES_TYPE_ID, adeId.getAppdefKey());
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
		defForm.exportConditionsEnablement(adv, request, sessionID,
				measurementBoss, adeId instanceof AppdefEntityTypeID);
		adv.setAppdefType(adeId.getType());
		adv.setAppdefId(adeId.getId());
		log.trace("adv=" + adv);

		if (adeId instanceof AppdefEntityTypeID)
			try {
				adv = eventsBoss.createResourceTypeAlertDefinition(sessionID,
						(AppdefEntityTypeID) adeId, adv);
			} catch (Exception e) {
				return "failure";
			}
		else
			adv = eventsBoss.createAlertDefinition(sessionID, adv);

		params.put(Constants.ALERT_DEFINITION_PARAM, adv.getId());
		alertDefId = adv.getId() + "";
		if (areAnyMetricsDisabled(adv, adeId, sessionID)) {
			addActionError(getText("resource.common.monitor.alert.config.error.SomeMetricsDisabled"));
		} else {
			addActionMessage(getText("resource.common.monitor.alert.config.confirm.Create"));
		}
		
		return SUCCESS;
	}

	public String reset() throws Exception {
		doCancel();
		return RESET;
	}
	
	public String newReset() throws Exception {
		doCancel();
		return "resetNew";
	}
	

	public String cancel() throws Exception {
		doCancel();
		return CANCELED;
	}
	
	public String newCancel() throws Exception {
		doCancel();
		return "cenceledNew";
	}

	public void doCancel() {
		if (defForm.getAd() != null) {
			alertDefId = defForm.getAd().toString();
		} else {
			alertDefId = getServletRequest().getParameter("ad");
		}
		if (defForm.getEid() != null) {
			eid = defForm.getEid();
		} else {
			eid = getServletRequest().getParameter("eid");
		}
		if (defForm.getAetid() != null) {
			aetid = defForm.getAetid();
		} else {
			aetid = getServletRequest().getParameter("aetid");
		}
	}

	private void fillCondition() {
		ConditionBeanNG conditionBean = new ConditionBeanNG();
		if (request.getParameter("getCondition(0).absoluteComparator") != null) {
			conditionBean.setAbsoluteComparator(request
					.getParameter("getCondition(0).absoluteComparator"));
		}
		if (request.getParameter("getCondition(0).absoluteValue") != null) {
			conditionBean.setAbsoluteValue(request
					.getParameter("getCondition(0).absoluteValue"));
		}
		if (request.getParameter("getCondition(0).controlAction") != null) {
			conditionBean.setControlAction(request
					.getParameter("getCondition(0).controlAction"));
		}
		if (request.getParameter("getCondition(0).controlActionStatus") != null) {
			conditionBean.setControlActionStatus(request
					.getParameter("getCondition(0).controlActionStatus"));
		}
		if (request.getParameter("getCondition(0).customProperty") != null) {
			conditionBean.setCustomProperty(request
					.getParameter("getCondition(0).customProperty"));
		}
		if (request.getParameter("getCondition(0).fileMatch") != null) {
			conditionBean.setFileMatch(request
					.getParameter("getCondition(0).fileMatch"));
		}
		if (request.getParameter("getCondition(0).logLevel") != null) {
			conditionBean.setLogLevel(Integer.parseInt(request
					.getParameter("getCondition(0).logLevel")));
		}
		if (request.getParameter("getCondition(0).logMatch") != null) {
			conditionBean.setLogMatch(request
					.getParameter("getCondition(0).logMatch"));
		}
		if (request.getParameter("getCondition(0).metricId") != null) {
			conditionBean.setMetricId(Integer.parseInt(request
					.getParameter("getCondition(0).metricId")));
		}
		if (request.getParameter("getCondition(0).metricName") != null) {
			conditionBean.setMetricName(request
					.getParameter("getCondition(0).metricName"));
		}
		if (request.getParameter("getCondition(0).thresholdType") != null) {
			conditionBean.setThresholdType(request
					.getParameter("getCondition(0).thresholdType"));
		}
		if (request.getParameter("getCondition(0).trigger") != null) {
			conditionBean.setTrigger(request
					.getParameter("getCondition(0).trigger"));
		}
		defForm.setConditions(Collections.singletonList(conditionBean));
	}

	private boolean areAnyMetricsDisabled(AlertDefinitionValue adv,
			AppdefEntityID adeId, int sessionID)
			throws SessionNotFoundException, SessionTimeoutException,
			AppdefEntityNotFoundException, GroupNotCompatibleException,
			PermissionException, RemoteException {
		// create a map of metricId --> enabled for this resource
		List metrics = measurementBoss.findMeasurements(sessionID, adeId,
				PageControl.PAGE_ALL);
		Map<Integer, Boolean> metricEnabledFlags = new HashMap<Integer, Boolean>(
				metrics.size());
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
				Integer mid = new Integer(
						adv.getConditions()[i].getMeasurementId());
				Boolean metricEnabled = (Boolean) metricEnabledFlags.get(mid);
				if (null != metricEnabled) {
					return metricEnabled.equals(Boolean.FALSE);
				}
			}
		}

		return false;
	}

	public DefinitionFormNG getDefForm() {
		return defForm;
	}

	public void setDefForm(DefinitionFormNG defForm) {
		this.defForm = defForm;
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

	public String getAlertDefId() {
		return alertDefId;
	}

	public void setAlertDefId(String alertDefId) {
		this.alertDefId = alertDefId;
	}

	public DefinitionFormNG getModel() {
		
		return defForm;
	}

	public void prepare() throws Exception {
		Portal portal = Portal.createPortal();
		setTitle(request, portal,
				"alert.config.platform.edit.NewAlertDef.Title");
		setResource();
		doCancel();

	}
}
