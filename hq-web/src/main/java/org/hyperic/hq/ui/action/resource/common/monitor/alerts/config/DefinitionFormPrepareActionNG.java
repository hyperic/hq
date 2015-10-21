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

package org.hyperic.hq.ui.action.resource.common.monitor.alerts.config;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.server.session.CpropKey;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.common.monitor.alerts.AlertDefUtil;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Prepare the alert definition form for new / edit.
 * 
 */

public abstract class DefinitionFormPrepareActionNG extends BaseActionNG
		implements ViewPreparer {

	protected final Log log = LogFactory
			.getLog(DefinitionFormPrepareActionNG.class.getName());

	@Autowired
	protected MeasurementBoss measurementBoss;
	@Autowired
	protected ControlBoss controlBoss;
	@Autowired
	protected AppdefBoss appdefBoss;

	/**
	 * Prepare the form for a new alert definition.
	 */
	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {

		request = getServletRequest();
		int sessionID;
		try {
			sessionID = RequestUtils.getSessionId(request).intValue();

			DefinitionFormNG defForm = null;
			if (request.getSession().getAttribute("defForm") == null) {
				defForm = new DefinitionFormNG();
				defForm.reset();
			} else {
				defForm = (DefinitionFormNG) request.getSession().getAttribute(
						"defForm");

			}

			Map<String, String> prioritiesMap = new HashMap<String, String>();
			for (int priority : defForm.getPriorities()) {
				prioritiesMap.put(priority + "",
						getText("alert.config.props.PB.Priority." + priority));
			}
			defForm.setPrioritiesMap(prioritiesMap);
			setupForm(defForm, request, sessionID);

			if (!defForm.isOkClicked()) {
				// setting up form for the first time
				setupConditions(request, defForm);
			}
			request.setAttribute("defForm", defForm);
		} catch (ServletException e) {
			log.error(e);
		} catch (Exception e) {
			log.error(e);
		}
	}

	protected void setupForm(DefinitionFormNG defForm,
			HttpServletRequest request, int sessionID) throws Exception {
		request.setAttribute("enableEachTime", new Integer(
				EventConstants.FREQ_EVERYTIME));
		request.setAttribute("enableOnce",
				new Integer(EventConstants.FREQ_ONCE));
		request.setAttribute("enableNumTimesInPeriod", new Integer(
				EventConstants.FREQ_COUNTER));
		request.setAttribute("noneDeleted", new Integer(
				Constants.ALERT_CONDITION_NONE_DELETED));

		PageControl pc = PageControl.PAGE_ALL;
		List metrics, baselines = new ArrayList();

		int numMetricsEnabled = 0;
		AppdefEntityID adeId;
		boolean controlEnabled;

		try {
			adeId = RequestUtils.getEntityTypeId(request);
			metrics = measurementBoss.findMeasurementTemplates(sessionID,
					(AppdefEntityTypeID) adeId, null, pc);
			defForm.setType(new Integer(adeId.getType()));
			defForm.setResourceType(adeId.getId());
			numMetricsEnabled++;

			controlEnabled = controlBoss.isControlSupported(sessionID,
					(AppdefEntityTypeID) adeId);
		} catch (ParameterNotFoundException e) {
			adeId = RequestUtils.getEntityId(request);
			metrics = measurementBoss.findMeasurements(sessionID, adeId, pc);

			if (!adeId.isGroup()) {
				for (Iterator it = metrics.iterator(); it.hasNext();) {
					Measurement m = (Measurement) it.next();
					if (m.isEnabled())
						numMetricsEnabled++;
				}
			}

			controlEnabled = controlBoss.isControlEnabled(sessionID, adeId);

		}
		request.setAttribute("logTrackEnabled", Boolean.TRUE);

		defForm.setMetrics(metrics);

		if (metrics.size() == 0) {
			addCustomActionErrorMessages(getText("resource.common.monitor.alert.config.error.NoMetricsConfigured"));
		} else if (numMetricsEnabled == 0) {
			addCustomActionErrorMessages(getText("resource.common.monitor.alert.config.error.NoMetricsEnabled"));
		}

		// need to duplicate this for the JavaScript on the page
		request.setAttribute("baselines", baselines);

		request.setAttribute(Constants.CONTROL_ENABLED, new Boolean(
				controlEnabled));
		if (controlEnabled) {
			try {
				defForm.setControlActions(AlertDefUtil.getControlActions(
						sessionID, adeId, controlBoss));
			} catch (PluginNotFoundException e) {
				// services that defined under server
				if (adeId.getType() == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
					setControlActionsToNA(defForm);
				} else {
					throw e;
				}
			}
		} else {
			setControlActionsToNA(defForm);
		}

		Map<String, String> custProps = getCustomProperties(sessionID, adeId);
		if (custProps != null && custProps.size() > 0) {
			request.setAttribute(Constants.CUSTPROPS_AVAIL, Boolean.TRUE);
			defForm.setCustomProperties(custProps);
		}
	}

	private void setControlActionsToNA(DefinitionFormNG defForm) {
		List<String> controlActions = new ArrayList<String>(1);
		controlActions.add("(N/A)");
		defForm.setControlActions(controlActions);
	}

	protected abstract void setupConditions(HttpServletRequest request,
			DefinitionFormNG defForm) throws Exception;

	/**
	 * Returns a List of custom property keys for the passed-in resource.
	 */
	protected Map<String, String> getCustomProperties(int sessionID,
			AppdefEntityID adeId) throws SessionNotFoundException,
			SessionTimeoutException, AppdefEntityNotFoundException,
			PermissionException, RemoteException {
		List<CpropKey> custProps;

		if (adeId instanceof AppdefEntityTypeID) {
			custProps = appdefBoss.getCPropKeys(sessionID, adeId.getType(),
					adeId.getID());
		} else {
			custProps = appdefBoss.getCPropKeys(sessionID, adeId);
		}

		Map<String, String> custPropStrs = new LinkedHashMap<String, String>();
		for (CpropKey custProp : custProps) {

			custPropStrs.put(custProp.getKey(), custProp.getDescription());
		}

		return custPropStrs;
	}
}
