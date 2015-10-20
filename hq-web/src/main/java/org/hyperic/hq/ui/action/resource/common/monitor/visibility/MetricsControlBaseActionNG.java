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

/**
 * A <code>BaseAction</code> that handles metrics control form submissions.
 */
public class MetricsControlBaseActionNG extends BaseActionNG {

	private final Log log = LogFactory.getLog(MetricsControlBaseActionNG.class
			.getName());

	protected MetricsControlFormNG controlForm = new MetricsControlFormNG();

	protected String eid;
	protected String ctype;
	
	@Autowired
	protected AuthzBoss authzBoss;

	/**
	 * Modify the metrics summary display as specified in the given
	 * <code>MetricsControlForm</code>.
	 */
	public String fillMetrics() throws Exception {
		return doExecute(getServletRequest());
	}

	public String doExecute(HttpServletRequest request) throws ServletException, ApplicationException,
			SessionTimeoutException, SessionNotFoundException {
		HttpSession session = request.getSession();
		WebUser user = SessionUtils.getWebUser(session);

		Integer sessionId = user.getSessionId();

		if(request.getParameter("a") != null){
			controlForm.setA(Integer.parseInt((String)request.getParameter("a")));
		}
		if(request.getParameter("rn") != null){
			try{
				controlForm.setRn(Integer.parseInt((String)request.getParameter("rn")));
			}catch(Exception e){
				addCustomActionErrorMessages( getText("resource.common.monitor.error.LastNInteger"));
				return INPUT;
			}
		}
		if(request.getParameter("ru") != null){
			controlForm.setRu(Integer.parseInt((String)request.getParameter("ru")));
		}
		Map<String, Object> forwardParams = controlForm.getForwardParams();
		if(forwardParams != null ){
			if(forwardParams.containsKey("eid")){
				eid = forwardParams.get("eid").toString();
			}
			if(forwardParams.containsKey("ctype")){
				ctype = forwardParams.get("ctype").toString();
			}
		}
		if (controlForm.isEditRangeClicked()) {
			return Constants.EDIT_RANGE_URL;
			// return returnEditRange(request, mapping, forwardParams);
		} else {
			Date begin = (controlForm.getStartYear() ==  null) ? null :controlForm.getStartDate();
			Date end = (controlForm.getEndYear() ==  null) ? null : controlForm.getEndDate();
			long beginTime = (begin == null)? 0 : begin.getTime();
			long endTime = (end == null)? 0 :  end.getTime();
			
			if (controlForm.isPrevRangeClicked()
					|| controlForm.isNextRangeClicked() 
					|| controlForm.getPrevBtnClicked()
					|| controlForm.getNextBtnClicked()) {
				// Figure out what it's currently set to and go
				// backwards/forwards
				long diff = endTime - beginTime;
				List<Long> range = new ArrayList<Long>();
				if (controlForm.isPrevRangeClicked() || controlForm.getPrevBtnClicked()) {
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
				if(lastN == null || lastN < 1 || lastN > 9999){
					addCustomActionErrorMessages( getText("resource.common.monitor.error.LastNInteger"));
					return INPUT;
				}
				Integer unit = controlForm.getRu();

				log.trace("updating metric display .. lastN [" + lastN
						+ "] .. unit [" + unit + "]");
				user.setPreference(WebUser.PREF_METRIC_RANGE_LASTN, lastN);
				user.setPreference(WebUser.PREF_METRIC_RANGE_UNIT, unit);
				user.setPreference(WebUser.PREF_METRIC_RANGE, null);

				// set simple mode
				user.setPreference(WebUser.PREF_METRIC_RANGE_RO, Boolean.FALSE);

				if (controlForm.getRangeBtnClicked() && log.isDebugEnabled()) {
					log.debug("updating metric display .. lastN [" + lastN
							+ "] .. unit [" + unit + "]");
					LogFactory.getLog("user.preferences").debug(
							"Invoking setUserPrefs in MetricsControlAction "
									+ " for " + user.getId() + " at "
									+ System.currentTimeMillis()
									+ " user.prefs = " + user.getPreferences());
				}
			}

			if (controlForm.isAdvancedClicked() || controlForm.getAdvancedBtnClicked()) {
				if (controlForm.isDateRangeSelected()) {
					if(beginTime == 0){
						if(controlForm.getStartHour() == null || controlForm.getStartHour().equals("")){
							addCustomActionErrorMessages(getText("errors.invalid.StartHour",new String[]{"Date Range"}));
							return INPUT;
						}else {
							try {
								Integer.parseInt(controlForm.getStartHour());
							} catch (NumberFormatException e) {
								addCustomActionErrorMessages(getText("errors.integer",new String[]{"Start hour"}));
								return INPUT;
							}
						}
						if(controlForm.getStartMin() == null || controlForm.getStartMin().equals("")){
							addCustomActionErrorMessages( getText("errors.invalid.StartMinute",new String[]{"Date Range"}));
							return INPUT;
						}else {
							try {
								Integer.parseInt(controlForm.getStartMin());
							} catch (NumberFormatException e) {
								addCustomActionErrorMessages(getText("errors.integer",new String[]{"Start minute"}));
								return INPUT;
							}
						}
						
					}
					if(endTime == 0){
						if(controlForm.getEndHour() == null || controlForm.getEndHour().equals("")){
							addCustomActionErrorMessages( getText("errors.invalid.EndHour",new String[]{"Date Range"}));
							return INPUT;
						}else {
							try {
								Integer.parseInt(controlForm.getEndHour());
							} catch (NumberFormatException e) {
								addCustomActionErrorMessages(getText("errors.integer",new String[]{"End hour"}));
								return INPUT;
							}
						}
						if(controlForm.getEndMin() == null || controlForm.getEndMin().equals("")){
							addCustomActionErrorMessages( getText("errors.invalid.EndMin",new String[]{"Date Range"}));
							return INPUT;
						}else {
							try {
								Integer.parseInt(controlForm.getEndMin());
							} catch (NumberFormatException e) {
								addCustomActionErrorMessages(getText("errors.integer",new String[]{"End minute"}));
								return INPUT;
							}
						}
						
					}
					
					if(endTime < beginTime){
						addCustomActionErrorMessages( getText("resource.common.monitor.error.FromEarlierThanTo"));
						return INPUT;
					}
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

		getServletRequest().setAttribute("MetricsControlForm", controlForm);
		// assume the return path has been set- don't use forwardParams
		return Constants.SUCCESS_URL;
	}

	public MetricsControlFormNG getControlForm() {
		return controlForm;
	}

	public void setControlForm(MetricsControlFormNG controlForm) {
		this.controlForm = controlForm;
	}

	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	public String getCtype() {
		return ctype;
	}

	public void setCtype(String ctype) {
		this.ctype = ctype;
	}
	
}

