/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.ui.util;

import java.text.StringCharacterIterator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForward;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.resource.common.monitor.visibility.ViewChartForm;
import org.hyperic.hq.ui.server.session.DashboardConfig;
import org.hyperic.hq.ui.server.session.RoleDashboardConfig;
import org.hyperic.hq.ui.server.session.UserDashboardConfig;
import org.hyperic.hq.ui.shared.DashboardManager;
import org.hyperic.util.config.ConfigResponse;

abstract public class SaveChartToDashboardUtil {
	private static Log log = LogFactory.getLog(SaveChartToDashboardUtil.class.getName());
	
	
	
	private static Pattern AEID_PATTERN_A = 
	    Pattern.compile(".*[?&]type=(\\d+).*&rid=(\\d+).*",
                        Pattern.CASE_INSENSITIVE);
	private static Pattern AEID_PATTERN_B = 
	    Pattern.compile(".*[?&]rid=(\\d+).*&type=(\\d+).*",
	                    Pattern.CASE_INSENSITIVE);
	
	public enum ResultCode {
		SUCCESS,
		DUPLICATE,
		ERROR
	}
	
	// Moving this logic into a util method as it's being used by more than one class
	public static ResultCode saveChartToDashboard(ServletContext ctx, HttpServletRequest request, 
	                                              ActionForward success, ViewChartForm chartForm, 
	                                              AppdefEntityID adeId, String chartName, boolean isEE,
	                                              DashboardManager dashboardManager)
	throws Exception 
	{
        AuthzBoss boss = Bootstrap.getBean(AuthzBoss.class);
        WebUser user = RequestUtils.getWebUser(request);
        String[] dashboardIds = request.getParameterValues(Constants.DASHBOARD_ID_PARAM);
        String url = generateChartUrl(success, chartForm, adeId, isEE);
        ResultCode result = ResultCode.ERROR; // Initialize as error, so that we can be proved wrong
        
        if (dashboardIds != null) {
    		for (int x = 0; x < dashboardIds.length; x++) {
    			Integer dashId = Integer.valueOf(dashboardIds[x]);
    			DashboardConfig dashboardConfig = dashboardManager.findDashboard(dashId, user, boss);
    			
    			result = addChartToDashboard(forHTMLTag(chartName), url, dashboardConfig, boss, user, request);
    			
    			// Something's wrong, break out
    			if (result.equals(ResultCode.ERROR)) break;
    		}
    	} else {
            AuthzSubject me = boss.findSubjectById(user.getSessionId(), user.getSubject().getId());
            DashboardConfig dashboardConfig = dashboardManager.getUserDashboard(me, me);
    		
    		result = addChartToDashboard(forHTMLTag(chartName), url, dashboardConfig, boss, user, request);
    	}

        return result;
	}
	
	public static AppdefEntityID getAppdefEntityIDFromChartUrl(String url) {
        AppdefEntityID id = null;
        Matcher matcher = AEID_PATTERN_A.matcher(url);
        
        if (matcher.matches()) {
            id = new AppdefEntityID(matcher.group(1) + ':' + matcher.group(2));            
        } else {
            matcher = AEID_PATTERN_B.matcher(url);
            if (matcher.matches()) {
                id = new AppdefEntityID(matcher.group(2) + ':' + matcher.group(1));
            }
        }
        
        return id;
	}
	
	private static String generateChartUrl(ActionForward success, ViewChartForm chartForm, AppdefEntityID adeId, boolean isEE) 
	throws Exception
	{
        // build the chart URL
        Map<String, Object> chartParams = new HashMap<String, Object>();
        
        chartParams.put("m", chartForm.getM());
        chartParams.put("showPeak", new Boolean(chartForm.getShowPeak()));
        chartParams.put("showValues", new Boolean(chartForm.getShowValues()));
        chartParams.put("showAverage", new Boolean(chartForm.getShowAverage()));
        chartParams.put("showLow", new Boolean(chartForm.getShowLow()));
        chartParams.put("threshold", chartForm.getThreshold());

        if (isEE) {
            chartParams.put("showHighRange", new Boolean(chartForm.getShowHighRange()));
            chartParams.put("showLowRange", new Boolean(chartForm.getShowLowRange()));
            chartParams.put("showEvents", new Boolean(chartForm.getShowEvents()));
            chartParams.put("showBaseline", new Boolean(chartForm.getShowBaseline()));
        }
        
        if(adeId.isGroup()) {
            chartParams.put( "mode", chartForm.getMode() );                
            chartParams.put( "r", chartForm.getResourceIds() );
        }
        
        if (chartForm.getCtype() != null && chartForm.getCtype().length() > 0) {
            chartParams.put( "mode", chartForm.getMode() );                
            chartParams.put( "ctype", chartForm.getCtype() );                
        }
        
        return ActionUtils.changeUrl(success.getPath(), chartParams);
	}
	
	private static ResultCode addChartToDashboard(String name, String url, DashboardConfig dashboardConfig, AuthzBoss boss, WebUser user, HttpServletRequest request) 
	throws Exception
	{	
		ConfigResponse configResponse = dashboardConfig.getConfig();
        String charts = configResponse.getValue(Constants.USER_DASHBOARD_CHARTS, "");
        
        // make sure its not a duplicate chart
        if (charts.indexOf(name + "," + url) > -1) {
        	// If it's a dup, it's already there and our work is done
            return ResultCode.DUPLICATE;
        }
        
        // Now see if the name is already used
        String chartname = name;
        
        for (int i = 2; charts.indexOf(chartname + ",") > -1; i++) {
            // Hard-code name to be a number in parenthesis to differentiate
            chartname = name + " (" + i + ")";
        }

        charts += Constants.DASHBOARD_DELIMITER + chartname + "," + url;

        configResponse.setValue(Constants.USER_DASHBOARD_CHARTS, charts);
        
		if (dashboardConfig instanceof RoleDashboardConfig) {
			RoleDashboardConfig roleDashboardConfig = (RoleDashboardConfig) dashboardConfig;
			
			Bootstrap.getBean(ConfigurationProxy.class).setRoleDashboardPreferences(configResponse,  user, roleDashboardConfig.getRole());
		} else if (dashboardConfig instanceof UserDashboardConfig) {
		    Bootstrap.getBean(ConfigurationProxy.class).setUserDashboardPreferences(configResponse,  user);
		} else {
            // Neither role or user dashboard. This shouldn't happen, but if it somehow does, treat it as an error.
			return ResultCode.ERROR;
		}
		
        if ( log.isDebugEnabled() ) {
            log.debug("Saving chart to dashboard ...\n\tchartName="+ name +"\n\turl=" + url);
        }

		// success
		return ResultCode.SUCCESS;
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
}