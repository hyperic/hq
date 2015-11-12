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

package org.hyperic.hq.ui.action.resource.common.monitor.config;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * modifies the metrics data.
 */
@Component("configMetricsActionNG")
@Scope("prototype")
public class ConfigMetricsActionNG extends BaseActionNG implements
		ModelDriven<MonitoringConfigFormNG> {

	private final Log log = LogFactory.getLog(ConfigMetricsActionNG.class
			.getName());
	@Autowired
	protected MeasurementBoss measurementBoss;

	@Autowired
	private TransactionRetry transactionRetry;

	MonitoringConfigFormNG mForm = new MonitoringConfigFormNG();

	private String savedId;
	private String rid;
	private int type;
	private String resourceTypeName;

	public String execute() throws Exception {
		clearErrorsAndMessages();
		log.trace("modifying metrics action");

		request = getServletRequest();
		HashMap<String, Object> parms = new HashMap<String, Object>(2);

		final int sessionId = RequestUtils.getSessionId(request).intValue();

		// this action will be passed an entityTypeId OR an entityId
		AppdefEntityTypeID aetid = null;
		AppdefEntityID appdefId = null;
		try {
			// check for appdef entity
			appdefId = RequestUtils.getEntityId(request);

			parms.put(Constants.RESOURCE_PARAM, appdefId.getId());
			rid = appdefId.getId().toString();
			parms.put(Constants.RESOURCE_TYPE_ID_PARAM,
					new Integer(appdefId.getType()));
			type = appdefId.getType();

		} catch (ParameterNotFoundException e) {
			// we better have a entityTypeId or this will throw an
			// uncaught ParameterNotFOundException
			aetid = new AppdefEntityTypeID(RequestUtils.getStringParameter(
					request, Constants.APPDEF_RES_TYPE_ID));
			parms.put("aetid", aetid.getAppdefKey());
			savedId = aetid.getAppdefKey();
		}
		if (appdefId != null) {
			resourceTypeName = calculatePlatformName(appdefId.getType());
		} else if (aetid != null) {
			resourceTypeName = calculatePlatformName(aetid.getType());
		}

		final Integer[] midsToUpdate = mForm.getMids();

		

		if ("remove".equals(mForm.getClickedType())) {

			// don't make any back-end call if user has not selected any
			// metrics.
			if (midsToUpdate.length == 0)
				return SUCCESS;

			measurementBoss.disableMeasurements(sessionId, appdefId,
					midsToUpdate);
			addActionMessage(getText("resource.common.monitor.visibility.config.RemoveMetrics.Confirmation"));
			return REMOVE;
		}

		String parameter = request.getParameter("collectionInterval");
		if(parameter != null && !"".equals(parameter)){
			try {
				mForm.setCollectionInterval(Integer.parseInt(parameter));
			} catch (Exception e) {
				addCustomActionErrorMessages(getText("errors.range",new String[]{"Collection Interval","1","9999"}));
				return "failure";
			}
		}
		// take the list of pending metric ids (mids),
		// and update them.);
		if (mForm.getCollectionInterval() == null || mForm.getCollectionInterval() == -1l ) {
			mForm.setCollectionInterval(0l);
		}
		
		
		if ("ok".equals(mForm.getClickedType()) && (mForm.getCollectionInterval() < 1l || mForm.getCollectionInterval() > 9999l) ) {
			addCustomActionErrorMessages(getText("errors.range",new String[]{"Colelction Interval","1","9999"}));
			return "failure";
		}
		
		final long interval = mForm.getIntervalTime();

		// don't make any back-end call if user has not selected any metrics.
		if (midsToUpdate.length == 0)
			return SUCCESS;

		String confirmation = "resource.common.monitor.visibility.config.ConfigMetrics.Confirmation";
		if (aetid == null) {
			if (mForm.getCollectionInterval() < 1l || mForm.getCollectionInterval() > 9999l ) {
				addCustomActionErrorMessages(getText("errors.range",new String[]{"Colelction Interval","1","9999"}));
				return "failure";
			}
			measurementBoss.updateMeasurements(sessionId, appdefId,
					midsToUpdate, interval);
		} else {
			if ("enableIndBtn".equals(mForm.getClickedType())) {
				measurementBoss.enableIndicatorMetrics(sessionId, aetid, midsToUpdate);
				confirmation = "resource.common.monitor.visibility.config.IndicatorMetrics.Confirmation";
			} else if ("disableIndBtn".equals(mForm.getClickedType())){
				measurementBoss.disableIndicatorMetrics(sessionId, aetid, midsToUpdate);
				confirmation = "resource.common.monitor.visibility.config.IndicatorMetrics.Confirmation";
			} else {
				final Runnable runner = new Runnable() {
					public void run() {
						try {
							measurementBoss.updateMetricDefaultInterval(
									sessionId, midsToUpdate, interval);
						} catch (SessionException e) {
							log.error(e, e);
						}
					}
				};
				transactionRetry.runTransaction(runner, 3, 1000);
			}
		}
		addActionMessage(getText(confirmation));

		return SUCCESS;

	}

	public String getRid() {
		return rid;
	}

	public void setRid(String rid) {
		this.rid = rid;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	private String calculatePlatformName(int type) {
		if (AppdefEntityConstants.APPDEF_TYPE_PLATFORM == type) {
			return "platform";
		} else if (AppdefEntityConstants.APPDEF_TYPE_SERVER == type) {
			return "server";
		} else if (AppdefEntityConstants.APPDEF_TYPE_SERVICE == type) {
			return "service";
		} else if (AppdefEntityConstants.APPDEF_TYPE_APPLICATION == type) {
			return "application";
		} else if (AppdefEntityConstants.APPDEF_TYPE_GROUP == type) {
			return "group";
		} else {
			return "platform";
		}

	}

	public MonitoringConfigFormNG getmForm() {
		return mForm;
	}

	public void setmForm(MonitoringConfigFormNG mForm) {
		this.mForm = mForm;
	}

	public String getSavedId() {
		return savedId;
	}

	public void setSavedId(String savedId) {
		this.savedId = savedId;
	}

	public String getResourceTypeName() {
		return resourceTypeName;
	}

	public void setResourceTypeName(String resourceTypeName) {
		this.resourceTypeName = resourceTypeName;
	}

	public MonitoringConfigFormNG getModel() {

		return mForm;
	}

}
