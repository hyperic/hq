
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

package org.hyperic.hq.ui.action.resource.common.control;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.control.server.session.ControlSchedule;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.ScheduleFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.springframework.stereotype.Component;

/**
 * This populates the EditForm associated with a server control action.
 */
@Component("editFormPrepareActionNG")
public class EditFormPrepareActionNG extends
ResourceControlControllerNG implements ViewPreparer {
	private final Log log = LogFactory.getLog(EditFormPrepareActionNG.class.getName());
	@Resource
    private ControlBoss controlBoss;
    /**
     * Retrieve server action data and store it in the specified request
     * parameters.
     */
    public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {

        log.trace("Preparing to modify server control properties action.");

        ControlFormNG cForm = new ControlFormNG();
        request = getServletRequest();
		clearErrorsAndMessages();
        // populate the control form from that ControlActionSchedule.

        try {
        	
            int sessionId = RequestUtils.getSessionId(request).intValue();
            setResource();
            Portal portal = Portal.createPortal(
					"resource.server.Control.PageTitle.Edit",
					"resource.server.Control.PageTitle.Edit");
			portal.setDialog(true);
			request.setAttribute(Constants.PORTAL_KEY, portal);
			
			
            Integer trigger = RequestUtils.getIntParameter(request, Constants.CONTROL_BATCH_ID_PARAM);

            ControlSchedule job = controlBoss.getControlJob(sessionId, trigger);

            cForm.populateFromSchedule(job.getScheduleValue(), request.getLocale());
            cForm.setControlAction(job.getAction());
            cForm.setDescription(job.getScheduleValue().getDescription());
            AppdefEntityID appdefId = RequestUtils.getEntityId(request);
            List<String> actions = controlBoss.getActions(sessionId, appdefId);
            List<String> options= new ArrayList<String>();
			for (String action : actions) {
				 String value = action;
			     String label = StringUtil.capitalize(value);
			     options.add(label);
			}
			cForm.setControlActions(options);
			cForm.setNumControlActions(new Integer(options.size()));
           
			request.setAttribute("type",appdefId.getType());
	        request.setAttribute("rid",appdefId.getId());
	        if(cForm.getStartTime().equals("1")) {
	        	  request.setAttribute("immediately","true");
	        } 
	        if(cForm.getRecurrenceFrequencyDaily()!=null) {
	        	  request.setAttribute("every","true");
	        } 
	        if((cForm.getRecurInterval().equals(ScheduleFormNG.RECUR_MONTHLY))&&cForm.getRecurrenceFrequencyMonthly().equals(ScheduleFormNG.ON_EACH)) {
	        	  request.setAttribute("each","true");
	        } 
	        if(cForm.getEndTime().equals("none")) {
	        	  request.setAttribute("noEnd","true");
	        } 
	        if (cForm.getRecurrenceDay()!=null&& (cForm.getRecurInterval().equals(ScheduleFormNG.RECUR_WEEKLY))){
	        	 String[] days = {"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
	        	 List<String> daysIAlreadyPutInRequest=new ArrayList<String>();
	             Integer [] recurrenceDay= cForm.getRecurrenceDay();
	        	
	        	for (int i = 0; i < recurrenceDay.length; i++) {
	        		request.setAttribute(days[recurrenceDay[i]-1], days[recurrenceDay[i]-1]);
	        		daysIAlreadyPutInRequest.add(days[recurrenceDay[i]-1]);
	        	}
	        	for (String day : days) {
	        		if (!(daysIAlreadyPutInRequest.contains(day))) {
	        			request.setAttribute(day,"");
					} 
	        	}
	        	        
	        }
            Calendar calendar = GregorianCalendar.getInstance();
	        Date date = new Date(); 
	        calendar.setTime(date); 
	        request.setAttribute("startYearr", Integer.parseInt(calendar.get(Calendar.YEAR)+""));
	        if ( null==cForm.getNumDays()  || cForm.getNumDays().length()==0) {
	         cForm.setNumDays("1");
	        }
	        if ( null==cForm.getNumWeeks()  || cForm.getNumWeeks().length()==0) {
	        cForm.setNumWeeks("1");
	        }
	        if ( null==cForm.getNumMonths()  || cForm.getNumMonths().length()==0) {
	        cForm.setNumMonths("1");
	        }
	        request.setAttribute("cForm",cForm);

        } catch (PluginNotFoundException pnfe) {
            log.trace("no plugin available", pnfe);
            this.addActionError(getText(  "resource.common.error.PluginNotFound"));
            
        } catch (PluginException cpe) {
            log.trace("could not find trigger", cpe);
            this.addActionError(getText("resource.common.error.ControlNotEnabled"));
            
        }
        catch (Exception ex) {
			log.error(ex,ex);
		}
    }
}
