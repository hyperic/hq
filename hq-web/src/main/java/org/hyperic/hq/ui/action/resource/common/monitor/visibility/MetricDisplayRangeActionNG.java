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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * A <code>BaseAction</code> that handles metrics-specific form gestures.
 */
@Component("metricDisplayRangeActionNG")
public class MetricDisplayRangeActionNG extends BaseActionNG implements
		ModelDriven<MetricDisplayRangeFormNG> {

	private final Log log = LogFactory.getLog(MetricDisplayRangeActionNG.class
			.getName());
	@Autowired
	private AuthzBoss authzBoss;

	private MetricDisplayRangeFormNG displayForm = new MetricDisplayRangeFormNG();

	/**
	 * Modify the metrics summary display as specified in the given
	 * <code>MetricDisplayRangeForm</code>.
	 */
	public String execute() throws Exception {

		// Redirect user back to where they came if cancelled
		if (displayForm.isCancelClicked()) {
			return SUCCESS;
		}

		String forward = checkSubmit(displayForm);
		if (forward != null) {
			return forward;
		}

		WebUser user = RequestUtils.getWebUser(request);
		Integer sessionId = user.getSessionId();

		if (displayForm.isLastnSelected()) {
			Integer lastN = displayForm.getRn();
			Integer unit = displayForm.getRu();

			log.trace("updating metric display .. lastN [" + lastN
					+ "] .. unit [" + unit + "]");
			user.setPreference(WebUser.PREF_METRIC_RANGE_LASTN, lastN);
			user.setPreference(WebUser.PREF_METRIC_RANGE_UNIT, unit);
			user.setPreference(WebUser.PREF_METRIC_RANGE, null);

			// set simple mode
			user.setPreference(WebUser.PREF_METRIC_RANGE_RO, Boolean.FALSE);
		} else if (displayForm.isDateRangeSelected()) {
			Date begin = displayForm.getStartDate();
			Date end = displayForm.getEndDate();

			List<Long> range = new ArrayList<Long>();
			range.add(new Long(begin.getTime()));
			range.add(new Long(end.getTime()));

			log.trace("updating metric display date range [" + begin + ":"
					+ end + "]");
			user.setPreference(WebUser.PREF_METRIC_RANGE, range);
			user.setPreference(WebUser.PREF_METRIC_RANGE_LASTN, null);
			user.setPreference(WebUser.PREF_METRIC_RANGE_UNIT, null);

			// set advanced mode
			user.setPreference(WebUser.PREF_METRIC_RANGE_RO, Boolean.TRUE);
		} else {
			throw new ServletException("invalid date range action ["
					+ displayForm.getA() + "] selected");
		}

		log.trace("Invoking setUserPrefs" + " in MetricDisplayRangeAction "
				+ " for " + user.getId() + " at " + System.currentTimeMillis()
				+ " user.prefs = " + user.getPreferences());
		authzBoss.setUserPrefs(sessionId, user.getId(), user.getPreferences());

		// XXX: assume return path is set, don't use forward params
		return SUCCESS;
	}

	public MetricDisplayRangeFormNG getModel() {
		return displayForm;
	}

	public MetricDisplayRangeFormNG getDisplayForm() {
		return displayForm;
	}

	public void setDisplayForm(MetricDisplayRangeFormNG displayForm) {
		this.displayForm = displayForm;
	}

}
