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

import java.text.StringCharacterIterator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.DashboardManagerEJBImpl;
import org.hyperic.hq.ui.util.ActionUtils;
import org.hyperic.hq.ui.util.ConfigurationProxy;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.DashboardUtils;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.util.SessionUtils;
import org.hyperic.util.config.ConfigResponse;

/**
 * View a chart for a metric.
 */
public class ViewChartAction extends MetricDisplayRangeAction {
    protected static Log log =
        LogFactory.getLog( ViewChartAction.class.getName() );

    /**
     * Modify the metric chart as specified in the given <code>@{link
     * ViewActionForm}</code>.
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
    
        ViewChartForm chartForm = (ViewChartForm)form;

        AppdefEntityID adeId =
            new AppdefEntityID( chartForm.getType().intValue(),
                                chartForm.getRid());

        HashMap forwardParams = new HashMap(3);
        forwardParams.put( Constants.RESOURCE_PARAM, chartForm.getRid() );
        forwardParams.put( Constants.RESOURCE_TYPE_ID_PARAM, chartForm.getType() );
        // The autogroup metrics pages pass the ctype to us, and we
        // need to pass it back.  If this happens, we don't need the
        // extra "mode" parameter.  See bug #7501. (2003/06/24 -- JW)
        if ( null != chartForm.getCtype() &&
             !chartForm.getCtype().equals(ViewChartForm.NO_CHILD_TYPE) ) {
            forwardParams.put( Constants.CHILD_RESOURCE_TYPE_ID_PARAM, chartForm.getCtype() );
        } else {
            forwardParams.put( Constants.MODE_PARAM, chartForm.getMode() );
        }

        if ( chartForm.getSaveChart() ) {
            ActionForward success = returnRedraw(request, mapping, forwardParams);

            // build the chart URL
            HashMap chartParams = new HashMap();
            chartParams.put("m", chartForm.getM());
            chartParams.put("showPeak", new Boolean(chartForm.getShowPeak()));
            chartParams.put("showValues", new Boolean(chartForm.getShowValues()));
            chartParams.put("showAverage", new Boolean(chartForm.getShowAverage()));
            chartParams.put("showLow", new Boolean(chartForm.getShowLow()));
            chartParams.put("threshold", chartForm.getThreshold());

            if(adeId.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP){
                chartParams.put( "mode", chartForm.getMode() );                
                chartParams.put( "r", chartForm.getResourceIds() );
            }
            
            if (chartForm.getCtype() != null &&
                chartForm.getCtype().length() > 0) {
                chartParams.put( "mode", chartForm.getMode() );                
                chartParams.put( "ctype", chartForm.getCtype() );                
            }
            
            String url = ActionUtils.changeUrl(success.getPath(), chartParams);
            _saveUserChart(url, chartForm.getChartName(), request);

            if ( log.isDebugEnabled() ) {
                log.debug("Saving chart to dashboard ...\n\tchartName="+chartForm.getChartName()+"\n\turl="+url);
            }

            return success;
        } else if ( chartForm.isPrevPageClicked() ) {
            return returnSuccess(request, mapping, forwardParams);
        } else {
            // If prev or next buttons were clicked, the dates
            // caused by those clicks will override what's
            // actually in the form, so we must update the form as
            // appropriate.
            if ( chartForm.isNextRangeClicked() || chartForm.isPrevRangeClicked() ) {
                MetricRange range = new MetricRange();
                if ( chartForm.isNextRangeClicked() ) {
                    long newBegin = chartForm.getEndDate().getTime();
                    long diff = newBegin - chartForm.getStartDate().getTime();
                    long newEnd = newBegin + diff;

                    range.setBegin( new Long(newBegin) );
                    range.setEnd( new Long(newEnd) );
                } else if ( chartForm.isPrevRangeClicked() ) {
                    long newEnd = chartForm.getStartDate().getTime();
                    long diff = chartForm.getEndDate().getTime() - newEnd;
                    long newBegin = newEnd - diff;

                    range.setBegin( new Long(newBegin) );
                    range.setEnd( new Long(newEnd) );
                }
                chartForm.setA(chartForm.ACTION_DATE_RANGE);
                chartForm.populateStartDate( new Date( range.getBegin().longValue() ), request.getLocale() );
                chartForm.populateEndDate( new Date( range.getEnd().longValue() ), request.getLocale() );
                range.shiftNow();
                request.setAttribute(Constants.METRIC_RANGE, range);
            }

            // update metric display range
            ActionForward retVal = super.execute(mapping, form, request, response);
            if ( retVal.getName().equals(Constants.SUCCESS_URL) ) {
                return returnRedraw(request, mapping, forwardParams);
            } else {
                if ( log.isTraceEnabled() ) {
                    log.trace( "returning " + retVal.getName() );
                }
                return retVal;
            }
        }

    }
        
    protected ActionForward returnRedraw(HttpServletRequest request,
                                         ActionMapping mapping, Map params)
    throws Exception {
        return constructForward(request, mapping, Constants.REDRAW_URL, 
                                params, false);
    }

    //--------------------------------------------------------------------------------
    //-- private helpers
    //--------------------------------------------------------------------------------
    //forHTMLTag is copy-n-pasted from: http://www.javapractices.com/Topic96.cjp
    //used to be in our util.StringUtil, we should really use jakarta's
    //StringEscapeUtils.escapeHTML()
    /**
     * Replace characters having special meaning <em>inside</em> HTML tags
     * with their escaped equivalents, using character entities such as
     * <tt>'&amp;'</tt>.
     * 
     * <P>
     * The escaped characters are :
     * <ul>
     * <li><
     * <li>>
     * <li>"
     * <li>'
     * <li>\
     * <li>&
     * </ul>
     * 
     * <P>
     * This method ensures that arbitrary text appearing inside a tag does not
     * "confuse" the tag. For example, <tt>HREF='Blah.do?Page=1&Sort=ASC'</tt>
     * does not comply with strict HTML because of the ampersand, and should be
     * changed to <tt>HREF='Blah.do?Page=1&amp;Sort=ASC'</tt>. This is
     * commonly seen in building query strings. (In JSTL, the c:url tag performs
     * this task automatically.)
     */
    private static String forHTMLTag(String aTagFragment) {
        final StringBuffer result = new StringBuffer();

        final StringCharacterIterator iterator =
            new StringCharacterIterator(aTagFragment);
        
        for (char character = iterator.current();
             character != StringCharacterIterator.DONE;
             character = iterator.next()) {
            switch (character) {
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '\"':
                    result.append("&quot;");
                    break;
                case '\'':
                    result.append("&#039;");
                    break;
                case '\\':
                    result.append("&#092;");
                    break;
                case '&':
                    result.append("&amp;");
                    break;
                case '|':
                    result.append("&#124;");
                    break;
                case ',':
                    result.append("&#44;");
                    break;
                default:
                    //the char is not a special one add it to the result as is
                    result.append(character);
                    break;
            }
        }
        return result.toString();
    }

    protected void _saveUserChart(String url, String name,
                                HttpServletRequest request) 
        throws Exception {
        ServletContext ctx = getServlet().getServletContext();
        AuthzBoss boss = ContextUtils.getAuthzBoss(ctx);
        HttpSession session = request.getSession();
        WebUser user = SessionUtils.getWebUser(request.getSession());
        AuthzBoss aBoss = ContextUtils.getAuthzBoss(ctx);
        DashboardConfig dashConfig = DashboardUtils
				.findDashboard((Integer) session
						.getAttribute(Constants.SELECTED_DASHBOARD_ID), 
						user, aBoss);
		if (dashConfig == null) {
			AuthzSubject me = boss.findSubjectById(user.getSessionId(), user
					.getSubject().getId());
			dashConfig = DashboardManagerEJBImpl.getOne().getUserDashboard(me, me);
		}
		ConfigResponse dashPrefs = dashConfig.getConfig();
        String charts = dashPrefs.getValue(Constants.USER_DASHBOARD_CHARTS, "");
        
        // the name might be generated by user input, we need to make sure
        // they don't use our delimiters when it's serialized into the preference
        // system
        name = forHTMLTag(name);
        String origname = name;
        
        // make sure its not a duplicate chart
        if (charts.indexOf(origname + "," + url) > -1) {
            RequestUtils.setConfirmation(request,
                "resource.common.monitor.visibility.chart.error.ChartDuplicated");
            return;
        }
        
        // Now see if the name is already used
        String chartname = origname;
        for (int i = 2; charts.indexOf(chartname + ",") > -1; i++) {
            // Hard-code name to be a number in parenthesis to differentiate
            chartname = origname + " (" + i + ")";
        }
        
        // If chart already exists, don't add it again
        charts += Constants.DASHBOARD_DELIMITER + chartname + "," + url;

        dashPrefs.setValue(Constants.USER_DASHBOARD_CHARTS, charts);
        log.debug("ViewChartAction - saving chart: " + charts);
        ConfigurationProxy.getInstance().setUserDashboardPreferences(dashPrefs, boss, user );
        
        RequestUtils.setConfirmation(request,
            "resource.common.monitor.visibility.chart.confirm.ChartSaved");
    }
}
