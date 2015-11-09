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

package org.hyperic.hq.ui.action.resource.common.control;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.ScheduleFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.util.StringUtil;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

/**
 * An <code>Action</code> subclass that prepares to create a control action
 * associated with a server.
 */
@Component("newFormPrepareActionNG")
public class NewFormPrepareActionNG extends
ResourceControlControllerNG implements ViewPreparer  {
	@Resource
	private ControlBoss controlBoss;
	    private final Log log = LogFactory.getLog(NewFormPrepareActionNG.class.getName());
	     /**
	     * Create the control action and associate it with the server.
	     * <code>NewForm</code> and save it into the session attribute
	     * <code>Constants.ACTION_ATTR</code>.
	     */
	    public void execute(TilesRequestContext tilesContext,
				AttributeContext attributeContext) {

	        log.trace("preparing new server control action");
	        
	       
	        int sessionId;
			try {
				request = getServletRequest();
				setResource();
				Portal portal = Portal.createPortal(
						"resource.server.Control.PageTitle.New",
						"resource.server.Control.PageTitle.New");
				portal.setDialog(true);
				request.setAttribute(Constants.PORTAL_KEY, portal);
				
				

				ControlFormNG cForm = new ControlFormNG();
		        
				 cForm=buildCurrentcForm(cForm,request);
		        
		        
				sessionId = RequestUtils.getSessionId(request).intValue();
			
			clearErrorsAndMessages();

	        AppdefEntityID appdefId = RequestUtils.getEntityId(request);

	        Map<String, String> actionsForRequest=new LinkedHashMap<String, String>();
	        List<String> actions = controlBoss.getActions(sessionId, appdefId);
	        List<String> options= new ArrayList<String>();
			for (String action : actions) {
				 String value = action;
			     String label = StringUtil.capitalize(value);
			     options.add(label);
			     actionsForRequest.put(value,label);
			}
			cForm.setControlActions(options);
			request.setAttribute("availableActions", actionsForRequest);
			
	        cForm.setNumControlActions(new Integer(options.size()));
	        Calendar calendar = GregorianCalendar.getInstance();
	        
	        
	        Date date = new Date(); 
	        calendar.setTime(date); 
	        if ((cForm.getStartMin().equals(""))&&(cForm.getStartHour().equals(""))&&(cForm.getNumDays().equals(""))&&(cForm.getNumWeeks().equals(""))&&(cForm.getNumMonths().equals(""))) {//the first time the form is loaded (after hit the new button)
        	//set starttime and end time just for the first time the form is loaded
        	cForm.setStartDay(calendar.get(Calendar.DAY_OF_MONTH));
        	cForm.setStartMonth(calendar.get(Calendar.MONTH));
        	cForm.setStartYear(calendar.get(Calendar.YEAR));
        	cForm.setEndDay(calendar.get(Calendar.DAY_OF_MONTH));
        	cForm.setEndMonth(calendar.get(Calendar.MONTH));
        	cForm.setEndYear(calendar.get(Calendar.YEAR));
        	int hours=calendar.get(Calendar.HOUR);
        	if (hours>9) {
				cForm.setStartHour(hours+"");        
			} else {
				cForm.setStartHour("0"+hours);
			}
        	
		      
        	DateTime dt = new DateTime();  
        	int minutes = dt.getMinuteOfHour();
        	if (minutes>9) {
				cForm.setStartMin(minutes+"");        
			} else {
				cForm.setStartMin("0"+minutes);
			}
        	
        	calendar = GregorianCalendar.getInstance();
        	calendar.setTime(date);    
        	int ampm=calendar.get(Calendar.AM_PM);
        	if (ampm==0) {
        		cForm.setStartAmPm("am");
			} else {
				cForm.setStartAmPm("pm");
			}
	   		
	        cForm.setStartYear(calendar.get(Calendar.YEAR));
	        cForm.setNumDays("1");
	        cForm.setNumWeeks("1");
	        cForm.setNumMonths("1");
	        }
	        request.setAttribute("startYearr", Integer.parseInt(calendar.get(Calendar.YEAR)+""));
	        request.setAttribute("type",appdefId.getType());
	        request.setAttribute("rid",appdefId.getId());
	        if(cForm.getStartTime().equals("1")) {
	        	  request.setAttribute("immediately","true");
	        } 
	        if(cForm.getRecurrenceFrequencyDaily().equals("1")) {
	        	  request.setAttribute("every","true");
	        } 
	        if(cForm.getRecurrenceFrequencyMonthly().equals("1")) {
	        	  request.setAttribute("each","true");
	        } 
	        if(cForm.getEndTime().equals("1")) {
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
	       
	        request.setAttribute("cForm",cForm);
	        /*
	         			Portal portal = Portal
					.createPortal(
							"resource.platform.inventory.EditPlatformGeneralPropertiesTitle",
							".resource.platform.inventory.EditPlatformGeneralProperties");
			portal.setDialog(true);
			request.setAttribute(Constants.PORTAL_KEY, portal);
			request.setAttribute(Constants.TITLE_PARAM_ATTR,
					resourceForm.getName()); 
	         
	         */
			} catch (Exception ex) {
				log.error(ex,ex);
			}
	      }
	    public static  ControlFormNG buildCurrentcForm(ControlFormNG form,HttpServletRequest req) {
			form.setControlAction(req.getParameter("controlAction") );
			form.setDescription( req.getParameter("description") );
			String startTime=req.getParameter("startTime");
			if (startTime==null || startTime.equals("1")) {
				form.setStartTime("1");
			} else {
				form.setStartTime("0");
			}
			String recurrenceFrequencyDaily=req.getParameter("recurrenceFrequencyDaily");
			if (recurrenceFrequencyDaily==null || recurrenceFrequencyDaily.equals("1")) {
				form.setRecurrenceFrequencyDaily("1");
			} else {
				form.setRecurrenceFrequencyDaily("0");
			}
			String recurrenceFrequencyMonthly=req.getParameter("recurrenceFrequencyMonthly");
			if (recurrenceFrequencyMonthly==null || recurrenceFrequencyMonthly.equals("1")) {
				form.setRecurrenceFrequencyMonthly("1");
			} else {
				form.setRecurrenceFrequencyMonthly("0");
			}
			String endTime=req.getParameter("endTime");
			if (endTime==null || endTime.equals("1")) {
				form.setEndTime("1");
			} else {
				form.setEndTime("onDate");
			}
			
			if (req.getParameter("startMonth")!=null) {
				
				form.setStartMonth(  Integer.parseInt(req.getParameter("startMonth")) );
			}
			if (req.getParameter("startDay")!=null) {
			form.setStartDay(  Integer.parseInt(req.getParameter("startDay")) );
			}
			if (req.getParameter("startYear")!=null) {
			form.setStartYear(  Integer.parseInt(req.getParameter("startYear")));
			}
			if (req.getParameter("startHour")!=null) {
			form.setStartHour( req.getParameter("startHour"));
			}else{
				form.setStartHour("");
			}
			if (req.getParameter("startMin")!=null) {
			form.setStartMin(  req.getParameter("startMin"));
			}
			else{
				form.setStartMin("");
			}
			form.setStartAmPm( req.getParameter("startAmPm"));
			form.setRecurInterval( req.getParameter("recurInterval"));
			if (req.getParameter("numDays")!=null) {
			form.setNumDays( req.getParameter("numDays"));
			}else{
				form.setNumDays("");
			}
			if (req.getParameter("numWeeks")!=null) {
			form.setNumWeeks( req.getParameter("numWeeks"));
			}else{
				form.setNumWeeks("");
			}
			if (req.getAttribute("recurrenceDay")!=null){
				form.setRecurrenceDay((Integer[]) req.getAttribute("recurrenceDay"));
			}
			if (req.getParameter("numMonths")!=null) {
			form.setNumMonths( req.getParameter("numMonths"));
			}else{
				form.setNumMonths("");
			}
			if (req.getParameter("recurrenceWeek")!=null) {
			form.setRecurrenceWeek(Integer.parseInt(req.getParameter("recurrenceWeek")));
			}
			if (req.getParameter("monthlyRecurrenceDay")!=null) {
			form.setMonthlyRecurrenceDay(Integer.parseInt(req.getParameter("monthlyRecurrenceDay")));
			}
			if (req.getParameter("eachDay")!=null) {
			form.setEachDay(Integer.parseInt(req.getParameter("eachDay")));
			}
			if (req.getParameter("endMonth")!=null) {
			form.setEndMonth(  Integer.parseInt(req.getParameter("endMonth")) );
			}
			if (req.getParameter("endDay")!=null) {
			form.setEndDay(  Integer.parseInt(req.getParameter("endDay")) );
			}
			if (req.getParameter("endYear")!=null) {
			form.setEndYear(Integer.parseInt(req.getParameter("endYear")));
			}
			return form;
	    	
	    }
	    }
