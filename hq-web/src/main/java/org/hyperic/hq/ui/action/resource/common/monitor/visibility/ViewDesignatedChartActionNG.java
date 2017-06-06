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

import java.util.HashMap;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.exception.ParameterNotFoundException;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Forward to chart page for a designated metric.
 */
@Component("viewDesignatedChartActionNG")
public class ViewDesignatedChartActionNG extends MetricDisplayRangeActionNG {

	@Autowired
	private MeasurementBoss measurementBoss;

	private String ctypeStr;
	private String mode;
	private String m;
	private String eid;

	/**
	 * Modify the metric chart as specified in the given <code>@{link
	 * ViewActionForm}</code>.
	 */
	public String execute() throws Exception {

		HashMap<String, Object> forwardParams = new HashMap<String, Object>(4);
		AppdefEntityID aeid = RequestUtils.getEntityId(request);
//		forwardParams.put(Constants.ENTITY_ID_PARAM, aeid.getAppdefKey());
		eid = aeid.getAppdefKey();	
		int sessionId = RequestUtils.getSessionId(request).intValue();
		MeasurementTemplate mt;
		try {
			AppdefEntityTypeID ctype = RequestUtils
					.getChildResourceTypeId(request);

			// forwardParams.put(Constants.CHILD_RESOURCE_TYPE_ID_PARAM,
			// ctype.getAppdefKey());
			ctypeStr = ctype.getAppdefKey().toString();
			// forwardParams.put(Constants.MODE_PARAM,
			// Constants.MODE_MON_CHART_SMMR);
			mode = Constants.MODE_MON_CHART_SMMR;

			// Now we have to look up the designated metric template ID
			mt = measurementBoss.getAvailabilityMetricTemplate(sessionId, aeid,
					ctype);
		} catch (ParameterNotFoundException e) {
			// forwardParams.put(Constants.MODE_PARAM, aeid.isGroup() ?
			// Constants.MODE_MON_CHART_SMMR
			// : Constants.MODE_MON_CHART_SMSR);

			mode = aeid.isGroup() ? Constants.MODE_MON_CHART_SMMR
					: Constants.MODE_MON_CHART_SMSR;
			// Now we have to look up the designated metric template ID
			mt = measurementBoss.getAvailabilityMetricTemplate(sessionId, aeid);
		}

		// forwardParams.put(Constants.METRIC_PARAM, mt.getId());
		m = mt.getId().toString();
		getDisplayForm().setMode(mode);
		
		return mode;
	}

	public String getCtypeStr() {
		return ctypeStr;
	}

	public String getMode() {
		return mode;
	}

	public String getM() {
		return m;
	}

	public String getEid() {
		return eid;
	}

}
