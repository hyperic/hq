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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.ServletActionContext;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SaveChartToDashboardUtil;
import org.hyperic.hq.ui.util.SaveChartToDashboardUtil.ResultCode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;


/**
 * View a chart for a metric.
 */

@Component("viewChartActionNG")
@Scope("prototype")
public class ViewChartActionNG extends BaseActionNG implements ModelDriven<ViewChartFormNG> {
	
    protected final Log log = LogFactory.getLog(ViewChartActionNG.class);
    @Resource
    private DashboardManager dashboardManager;
    
    protected ViewChartFormNG chartForm = new ViewChartFormNG();

	private String rid;
	private String type;
	private String ctype;
	private String eid;
	private String resourceTypeName;
	private String name;
	private String appdefTypeId;
	private String mode= "chartSingleMetricSingleResource";
	protected Integer[] origM;

	/**
     * Modify the metric chart as specified in the given <code>@{link
     * ViewActionForm}</code>.
     */
    public String execute() throws Exception {
        AppdefEntityID adeId = new AppdefEntityID(chartForm.getType().intValue(), chartForm.getRid());
        Map<String, Object> forwardParams = new HashMap<String, Object>(3);

        request.getSession().setAttribute("whole_chart", chartForm);
        
        request.setAttribute(Constants.RESOURCE_PARAM, chartForm.getRid());
        rid = chartForm.getRid() +"";
        request.setAttribute("origM", chartForm.getOrigM());
        origM = chartForm.getOrigM() ;
        request.getSession().setAttribute("chartForm_origM", origM);
        
        request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, chartForm.getType());
        type = chartForm.getType() +"";

        if(chartForm.getMode() != null && !"".equals(chartForm.getMode())){
        	mode = chartForm.getMode();
        }
        
        if(origM != null && origM.length > 1){
        	mode = "chartMultiMetricSingleResource";
        	chartForm.setMode(mode);
        }
        request.getSession().setAttribute("chartForm_showValues", chartForm.getShowValues());
        request.getSession().setAttribute("chartForm_showPeak", chartForm.getShowPeak());
        request.getSession().setAttribute("chartForm_showAverage", chartForm.getShowAverage());
        request.getSession().setAttribute("chartForm_showLow", chartForm.getShowLow());
        request.getSession().setAttribute("chartForm_showBaseline", chartForm.getShowBaseline());
        request.getSession().setAttribute("chartForm_showEvents", chartForm.getShowEvents());
        request.getSession().setAttribute("chartForm_showLowRange", chartForm.getShowLowRange());
        request.getSession().setAttribute("chartForm_showHighRange", chartForm.getShowHighRange());
        request.getSession().setAttribute("chartForm_resourceIds", request.getParameterValues("resourceIds"));
        
        // The autogroup metrics pages pass the ctype to us, and we
        // need to pass it back. If this happens, we don't need the
        // extra "mode" parameter. See bug #7501. (2003/06/24 -- JW)
        if (null != chartForm.getCtype() && !chartForm.getCtype().equals(ViewChartFormNG.NO_CHILD_TYPE)) {
        	request.setAttribute(Constants.CHILD_RESOURCE_TYPE_ID_PARAM, chartForm.getCtype());
        	ctype = chartForm.getCtype();
        } else {
        	request.setAttribute(Constants.MODE_PARAM, chartForm.getMode());
        	
        }

        if (chartForm.getSaveChart()) {
            // isEE == false, bc this is the .org version of this action
            return saveChartToDashboard(false);
        } else if (chartForm.isPrevPageClicked()) {
            return SUCCESS;
        } else {
            // If prev or next buttons were clicked, the dates
            // caused by those clicks will override what's
            // actually in the form, so we must update the form as
            // appropriate.
        	setParameters();
            if (chartForm.isNextRangeClicked() || chartForm.isPrevRangeClicked()) {
                MetricRange range = new MetricRange();
                if (chartForm.isNextRangeClicked()) {
                    long newBegin = chartForm.getEndDate().getTime();
                    long diff = newBegin - chartForm.getStartDate().getTime();
                    long newEnd = newBegin + diff;

                    range.setBegin(new Long(newBegin));
                    range.setEnd(new Long(newEnd));
                } else if (chartForm.isPrevRangeClicked()) {
                    long newEnd = chartForm.getStartDate().getTime();
                    long diff = chartForm.getEndDate().getTime() - newEnd;
                    long newBegin = newEnd - diff;

                    range.setBegin(new Long(newBegin));
                    range.setEnd(new Long(newEnd));
                }
                chartForm.setA(MetricDisplayRangeFormNG.ACTION_DATE_RANGE);
                chartForm.populateStartDate(new Date(range.getBegin().longValue()), request.getLocale());
                chartForm.populateEndDate(new Date(range.getEnd().longValue()), request.getLocale());
                range.shiftNow();
                request.setAttribute(Constants.METRIC_RANGE, range);
            }

            // update metric display range
            String retVal = this.redrawChart( );
            if (retVal.equals(SUCCESS)) {
                return SUCCESS;
            } else {
                if (log.isTraceEnabled()) {
                    log.trace("returning " + retVal);
                }
                return retVal;
            }
        }
    }

    public String saveChartToDashboard( boolean isEE) throws Exception {
    	
    	ServletContext context = ServletActionContext.getServletContext();
        
        String theUrl = request.getHeader("Referer");
        AppdefEntityID adeId = new AppdefEntityID(chartForm.getType().intValue(), chartForm.getRid());

        ResultCode result = SaveChartToDashboardUtil.saveChartToDashboard(context, request, theUrl, chartForm, adeId, chartForm.getChartName(), isEE , dashboardManager);

        switch (result) {
            case DUPLICATE:
                addCustomActionErrorMessages(getText("resource.common.monitor.visibility.chart.error.ChartDuplicated"));
                break;

            case ERROR:
            	addCustomActionErrorMessages(getText( "resource.common.monitor.visibility.chart.error.ChartNotSaved"));
                break;

            case SUCCESS:
            	addCustomActionErrorMessages(getText( "resource.common.monitor.visibility.chart.confirm.ChartSaved"));
        }

        return SUCCESS;
    }
   
    
    public String redrawChart() throws Exception {

		// Redirect user back to where they came if cancelled
		if (chartForm.isCancelClicked()) {
			return SUCCESS;
		}

		String forward = checkSubmit(chartForm);
		if (forward != null) {
			return forward;
		}

		WebUser user = RequestUtils.getWebUser(request);
		Integer sessionId = user.getSessionId();

		if (chartForm.isLastnSelected()) {
			Integer lastN = chartForm.getRn();
			Integer unit = chartForm.getRu();

			log.trace("updating metric display .. lastN [" + lastN
					+ "] .. unit [" + unit + "]");
			user.setPreference(WebUser.PREF_METRIC_RANGE_LASTN, lastN);
			user.setPreference(WebUser.PREF_METRIC_RANGE_UNIT, unit);
			user.setPreference(WebUser.PREF_METRIC_RANGE, null);

			// set simple mode
			user.setPreference(WebUser.PREF_METRIC_RANGE_RO, Boolean.FALSE);
		} else if (chartForm.isDateRangeSelected()) {
			Date begin = chartForm.getStartDate();
			Date end = chartForm.getEndDate();

			if(chartForm.getStartHour() == null || chartForm.getStartHour().equals("")){
				addCustomActionErrorMessages(getText("errors.invalid.StartHour",new String[]{"Date Range"}));
				return INPUT;
			}else {
				try {
					Integer.parseInt(chartForm.getStartHour());
				} catch (NumberFormatException e) {
					addCustomActionErrorMessages(getText("errors.integer",new String[]{"Start hour"}));
					return INPUT;
				}
			}
			if(chartForm.getStartMin() == null || chartForm.getStartMin().equals("")){
				addCustomActionErrorMessages( getText("errors.invalid.StartMinute",new String[]{"Date Range"}));
				return INPUT;
			}else {
				try {
					Integer.parseInt(chartForm.getStartMin());
				} catch (NumberFormatException e) {
					addCustomActionErrorMessages(getText("errors.integer",new String[]{"Start minute"}));
					return INPUT;
				}
			}
			
			if(chartForm.getEndHour() == null || chartForm.getEndHour().equals("")){
				addCustomActionErrorMessages( getText("errors.invalid.EndHour",new String[]{"Date Range"}));
				return INPUT;
			}else {
				try {
					Integer.parseInt(chartForm.getEndHour());
				} catch (NumberFormatException e) {
					addCustomActionErrorMessages(getText("errors.integer",new String[]{"End hour"}));
					return INPUT;
				}
			}
			if(chartForm.getEndMin() == null || chartForm.getEndMin().equals("")){
				addCustomActionErrorMessages( getText("errors.invalid.EndMin",new String[]{"Date Range"}));
				return INPUT;
			}else {
				try {
					Integer.parseInt(chartForm.getEndMin());
				} catch (NumberFormatException e) {
					addCustomActionErrorMessages(getText("errors.integer",new String[]{"End minute"}));
					return INPUT;
				}
			}
			if(begin.getTime() > end.getTime()){
				addCustomActionErrorMessages( getText("resource.common.monitor.error.FromEarlierThanTo"));
				return INPUT;
			}
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
					+ chartForm.getA() + "] selected");
		}

		log.trace("Invoking setUserPrefs" + " in MetricDisplayRangeAction "
				+ " for " + user.getId() + " at " + System.currentTimeMillis()
				+ " user.prefs = " + user.getPreferences());
		authzBoss.setUserPrefs(sessionId, user.getId(), user.getPreferences());

		// XXX: assume return path is set, don't use forward params
		return SUCCESS;
	}
	
	private void setParameters() {
		rid = chartForm.getRid().toString();
		if (chartForm.getType() != null) {
			type = chartForm.getType().toString();
			resourceTypeName = calculateResourceName(new Integer(type));
		}
		ctype = chartForm.getCtype();
		eid = chartForm.getEid().toString();
		
		if (request.getParameterValues("r") != null) {
			getServletRequest().getSession().setAttribute(
					"displayMetrics_r",
					getServletRequest().getParameterValues("r"));
		}
		
		if (request.getParameter("Resource") != null) {
			name= getServletRequest().getParameter("Resource");
		}
		if (request.getParameter("appdefType") != null) {
			appdefTypeId = getServletRequest().getParameter("appdefType");
		}
		if (resourceTypeName == null) {
			resourceTypeName = "Autogroup";
			if (eid != null
					&& AppdefEntityConstants.APPDEF_TYPE_GROUP == Integer
							.parseInt(eid.split(":")[0])) {
				resourceTypeName = "CompatGroup";
			}
		}

	}
	
	private String calculateResourceName(int type) {
		if (AppdefEntityConstants.APPDEF_TYPE_PLATFORM == type) {
			return "Platform";
		} else if (AppdefEntityConstants.APPDEF_TYPE_SERVER == type) {
			return "Server";
		} else if (AppdefEntityConstants.APPDEF_TYPE_SERVICE == type) {
			return "Service";
		} else if (AppdefEntityConstants.APPDEF_TYPE_APPLICATION == type) {
			return "Application";
		} else if (AppdefEntityConstants.APPDEF_TYPE_GROUP == type) {
			return "Group";
		} else if (AppdefEntityConstants.APPDEF_TYPE_AUTOGROUP == type) {
			return "Autogroup";
		} else {
			return "Platform";
		}

	}

	public ViewChartFormNG getModel() {
		return chartForm;
	}
	
    public ViewChartFormNG getChartForm() {
		return chartForm;
	}

	public void setChartForm(ViewChartFormNG chartForm) {
		this.chartForm = chartForm;
	}
	
	public String getRid() {
		return rid;
	}

	public void setRid(String rid) {
		this.rid = rid;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCtype() {
		return ctype;
	}

	public void setCtype(String ctype) {
		this.ctype = ctype;
	}

	public String getEid() {
		return eid;
	}

	public void setEid(String eid) {
		this.eid = eid;
	}

	public String getResourceTypeName() {
		return resourceTypeName;
	}

	public void setResourceTypeName(String resourceTypeName) {
		this.resourceTypeName = resourceTypeName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAppdefTypeId() {
		return appdefTypeId;
	}

	public void setAppdefTypeId(String appdefTypeId) {
		this.appdefTypeId = appdefTypeId;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public Integer[] getOrigM() {
		return origM;
	}

	public void setOrigM(Integer[] origM) {
		this.origM = origM;
	}


}
