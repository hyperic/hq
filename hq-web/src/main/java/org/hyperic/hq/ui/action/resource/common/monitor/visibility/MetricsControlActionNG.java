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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.SessionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * A <code>BaseAction</code> that handles metrics control form submissions.
 */
@Component("metricsControlActionNG")
public class MetricsControlActionNG extends BaseActionNG implements ModelDriven<MetricsControlFormNG>{

	private final Log log = LogFactory.getLog(MetricsControlActionNG.class
			.getName());

	protected MetricsControlFormNG controlForm = new MetricsControlFormNG();

	@Autowired
	protected AuthzBoss authzBoss;

	/**
	 * Modify the metrics summary display as specified in the given
	 * <code>MetricsControlForm</code>.
	 */
	public String execute() throws Exception {
		return execute(getServletRequest());
	}

	public String execute(HttpServletRequest request) throws ServletException, ApplicationException,
			SessionTimeoutException, SessionNotFoundException {
		HttpSession session = request.getSession();
		WebUser user = SessionUtils.getWebUser(session);

		Integer sessionId = user.getSessionId();

		Map<String, Object> forwardParams = controlForm.getForwardParams();
		if (controlForm.isEditRangeClicked()) {
			return Constants.EDIT_RANGE_URL;
			// return returnEditRange(request, mapping, forwardParams);
		} else {
			Date begin = controlForm.getStartDate();
			Date end = controlForm.getEndDate();
			long beginTime = begin.getTime();
			long endTime = end.getTime();

			if (controlForm.isPrevRangeClicked()
					|| controlForm.isNextRangeClicked()) {
				// Figure out what it's currently set to and go
				// backwards/forwards
				long diff = endTime - beginTime;
				List<Long> range = new ArrayList<Long>();
				if (controlForm.isPrevRangeClicked()) {
					range.add(new Long(beginTime - diff));
					range.add(new Long(beginTime));
				} else {
					range.add(new Long(endTime));
					range.add(new Long(endTime + diff));
				}

				log.trace("updating metric display date range [" + begin + ":"
						+ end + "]");
				user.setPreference(WebUser.PREF_METRIC_RANGE, range);
				user.setPreference(WebUser.PREF_METRIC_RANGE_LASTN, null);
				user.setPreference(WebUser.PREF_METRIC_RANGE_UNIT, null);

				// set advanced mode
				user.setPreference(WebUser.PREF_METRIC_RANGE_RO, Boolean.TRUE);
			} else if (controlForm.isLastnSelected()) {
				Integer lastN = controlForm.getRn();
				Integer unit = controlForm.getRu();

				log.trace("updating metric display .. lastN [" + lastN
						+ "] .. unit [" + unit + "]");
				user.setPreference(WebUser.PREF_METRIC_RANGE_LASTN, lastN);
				user.setPreference(WebUser.PREF_METRIC_RANGE_UNIT, unit);
				user.setPreference(WebUser.PREF_METRIC_RANGE, null);

				// set simple mode
				user.setPreference(WebUser.PREF_METRIC_RANGE_RO, Boolean.FALSE);

				if (controlForm.isRangeClicked() && log.isDebugEnabled()) {
					log.debug("updating metric display .. lastN [" + lastN
							+ "] .. unit [" + unit + "]");
					LogFactory.getLog("user.preferences").debug(
							"Invoking setUserPrefs in MetricsControlAction "
									+ " for " + user.getId() + " at "
									+ System.currentTimeMillis()
									+ " user.prefs = " + user.getPreferences());
				}
			}

			if (controlForm.isAdvancedClicked()) {
				if (controlForm.isDateRangeSelected()) {
					List<Long> range = new ArrayList<Long>();
					range.add(new Long(beginTime));
					range.add(new Long(endTime));

					log.trace("updating metric display date range [" + begin
							+ ":" + end + "]");
					user.setPreference(WebUser.PREF_METRIC_RANGE, range);
					user.setPreference(WebUser.PREF_METRIC_RANGE_LASTN, null);
					user.setPreference(WebUser.PREF_METRIC_RANGE_UNIT, null);

					// set advanced mode
					user.setPreference(WebUser.PREF_METRIC_RANGE_RO,
							Boolean.TRUE);
				} else if (!controlForm.isLastnSelected()) {
					throw new ServletException("invalid date range action ["
							+ controlForm.getA() + "] selected");
				}

				log.trace("Invoking setUserPrefs"
						+ " in MetricDisplayRangeAction " + " for "
						+ user.getId() + " at " + System.currentTimeMillis()
						+ " user.prefs = " + user.getPreferences());
			} else {
				if (controlForm.isSimpleClicked()) {
					user.setPreference(WebUser.PREF_METRIC_RANGE_RO,
							Boolean.FALSE);

					if (log.isDebugEnabled()) {
						LogFactory.getLog("user.preferences").debug(
								"Invoking setUserPrefs in MetricsControlAction "
										+ " for " + user.getId() + " at "
										+ System.currentTimeMillis()
										+ " user.prefs = "
										+ user.getPreferences());
					}
				}
			}
			authzBoss.setUserPrefs(sessionId, user.getId(),
					user.getPreferences());
		}

		// assume the return path has been set- don't use forwardParams
		return Constants.SUCCESS_URL;
	}

	public MetricsControlFormNG getModel() {
		return controlForm;
	}

	public MetricsControlFormNG getControlForm() {
		return controlForm;
	}

	public void setControlForm(MetricsControlFormNG controlForm) {
		this.controlForm = controlForm;
	}
	
	

	// ---------------------------------------------------- Private Methods

	/*
	 * private ActionForward returnEditRange(HttpServletRequest request,
	 * ActionMapping mapping, Map<String, Object> params) throws Exception {
	 * return constructForward(request, mapping, Constants.EDIT_RANGE_URL,
	 * params, NO_RETURN_PATH); }
	 */
}
