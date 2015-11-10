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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.ScheduleFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.joda.time.LocalDate;
import org.quartz.SchedulerException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * An <code>Action</code> subclass that creates a control action associated with
 * a server.
 */
@Component("newActionNG")
@Scope(value = "prototype")
public class NewActionNG extends BaseActionNG implements
		ModelDriven<ControlFormNG> {

	@Resource
	protected ControlBoss controlBoss;
	protected Log log = LogFactory.getLog(NewActionNG.class.getName());
	protected ControlFormNG cForm = new ControlFormNG();
	protected String internalEid;
	protected String internalRid;
	protected String internalType;
	
	protected boolean runBackendSave=true;

	/**
	 * Create the control action and associate it with the server.
	 * <code>NewForm</code> and save it into the session attribute
	 * <code>Constants.ACTION_ATTR</code>.
	 */
	// @SkipValidation
	// public String start() throws Exception {
	//
	// // log.trace("preparing new server control action");
	// // request=getServletRequest();
	// // int sessionId = RequestUtils.getSessionId(request).intValue();
	// //
	// // AppdefEntityID appdefId = RequestUtils.getEntityId(request);
	// //
	// // List<String> actions = controlBoss.getActions(sessionId, appdefId);
	// //
	// // Map options= new LinkedHashMap<String, String>();
	// // for (String action : actions) {
	// // String value = action;
	// // String label = StringUtil.capitalize(value);
	// // options.put(value,label);
	// // }
	// // cForm.setControlActions(options);
	// // cForm.setNumControlActions(new Integer(options.size()));
	// // if (cForm.getStartHour() == null) {
	// // Date date = new Date();
	// // Calendar calendar = GregorianCalendar.getInstance();
	// // calendar.setTime(date);
	// // int hours=calendar.get(Calendar.HOUR);
	// // cForm.setStartHour(hours+"");
	// // }
	// // if (cForm.getStartMin()==null) {
	// // DateTime dt = new DateTime();
	// // int minutes = dt.getMinuteOfHour();
	// // cForm.setStartMin(minutes+"");
	// // }
	// // if (cForm.getStartAmPm()==null) {
	// // Date date = new Date();
	// // Calendar calendar = GregorianCalendar.getInstance();
	// // calendar.setTime(date);
	// // int ampm=calendar.get(Calendar.AM_PM);
	// // if (ampm==0) {
	// // cForm.setStartAmPm("AM");
	// // } else {
	// // cForm.setStartAmPm("PM");
	// // }
	// // }
	// return "formNewActionLoad";
	// }
	public String save() throws Exception {

		log.trace("creating new action");
		try {
			request = getServletRequest();
			int sessionId = RequestUtils.getSessionId(request).intValue();
			AppdefEntityID appdefId = RequestUtils.getEntityId(request);
			request.setAttribute(Constants.RESOURCE_PARAM, appdefId.getId());
			request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(
					appdefId.getType()));
			boolean validationFailed=false;
			String des = this.cForm.getDescription();
			if (des != null && des.length() > 100) {
				this.addFieldError("description",
						getText("ng.errors.maxlength", new String[] { "100" }));
				
				validationFailed=true;
			}
			if (!(cForm.getStartTime().equals("1"))) {
			 try {
		            int tmph = Integer.parseInt(cForm.getStartHour());
		            if (tmph < 0 || tmph > 12) {
		                this.addFieldError("startHour",getText("errors.range", new String[] {tmph+"","1","12"}));
		                validationFailed=true;
		            }
		        } catch (NumberFormatException nfe) {
		        	this.addFieldError("startHour", getText("errors.invalid.StartHour",new String [] {cForm.getStartHour()+""}));
		        	validationFailed=true;
		        }

		        try {
		            int tmpmin = Integer.parseInt(cForm.getStartMin());
		            if (tmpmin > 59 || tmpmin < 0) {
		            	  this.addFieldError("startMin",getText("errors.range", new String[] {tmpmin+"","0","59"}));
		            	  validationFailed=true;
		            }
		        } catch (NumberFormatException nfe) {
		        	this.addFieldError("startMin", getText("errors.invalid.StartMin",new String [] {cForm.getStartMin()+""}));
		        	validationFailed=true;
		        }
		        
		}
		        
			String cAction = this.cForm.getControlAction();
			if (cAction != null && cAction.equals("")) {
				this.addFieldError("controlAction",getText("errors.required",new String[] { getText("dash.home.TableHeader.ControlAction") }));
				validationFailed=true;
			}
			// make sure that the ControlAction is valid.
			String action = cForm.getControlAction();
			List<String> validActions = controlBoss.getActions(sessionId,
					appdefId);
			if (!validActions.contains(action)) {
				this.addActionError(getText("resource.common.control.error.ControlActionNotValid"));
				validationFailed=true;
			}
			if (!(cForm.getStartTime().equals("1"))) {
				cForm.setStartTime(ScheduleFormNG.START_ON_DATE);	
			
			if (cForm.getStartTime().equals(ScheduleFormNG.START_ON_DATE) && cForm.getRecurInterval().equals(ScheduleFormNG.RECUR_WEEKLY))  {
				Integer tmpNumWeeks = null;
				try {
                    tmpNumWeeks = new Integer(Integer.parseInt(cForm.getNumWeeks()));
                } catch (NumberFormatException nfe) {
                	this.addFieldError("numWeeks",getText("resource.autodiscovery.ScheduleTab.error.numWeeks",new String [] {cForm.getNumWeeks()}));
                	validationFailed=true;
                }
				if (null == cForm.getRecurrenceDay() || cForm.getRecurrenceDay().length == 0) {
					this.addFieldError("recurrenceDay",getText("resource.autodiscovery.ScheduleTab.error.recurrenceDay"));
					validationFailed=true;
					
				}
			}
			if (cForm.getStartTime().equals(ScheduleFormNG.START_ON_DATE) && cForm.getRecurInterval().equals(ScheduleFormNG.RECUR_DAILY))  {
				Integer tmpNumDays = null;
				try {
					tmpNumDays = new Integer(Integer.parseInt(cForm.getNumDays()));
                } catch (NumberFormatException nfe) {
                	this.addFieldError("numDays",getText("resource.autodiscovery.ScheduleTab.error.numDays",new String [] {cForm.getNumDays()}));
                	validationFailed=true;
                }
			}
			if (cForm.getStartTime().equals(ScheduleFormNG.START_ON_DATE) && cForm.getRecurInterval().equals(ScheduleFormNG.RECUR_MONTHLY))  {
				Integer tmpNumMonths = null;
				try {
					tmpNumMonths = new Integer(Integer.parseInt(cForm.getNumMonths()));
                } catch (NumberFormatException nfe) {
                	this.addFieldError("numMonths",getText("resource.autodiscovery.ScheduleTab.error.numDays",new String [] {cForm.getNumMonths()}));
                	validationFailed=true;
                }
			}
			
			LocalDate localDate = new LocalDate(cForm.getEndYear(),cForm.getEndMonth() + 1, cForm.getEndDay());
			Date endDate = new Date();
			if (cForm.getRecurInterval().equals("recurNever")&& (cForm.getEndTime().equals("1") || cForm.getEndTime().equals(null))) {
				endDate = null;
			} else {
				endDate = java.sql.Date.valueOf(localDate + "");
			}
			if ((!(cForm.getRecurInterval().equals(ScheduleFormNG.RECUR_NEVER))&&(cForm.getStartDate() != null&& !(cForm.getEndTime().equals("1"))))) {
				Date tmpStartDate = cForm.getStartDate();
				Date tmpEndDate = endDate;
				if (tmpStartDate.after(tmpEndDate)) {
					this.addFieldError("startMonth",getText("resource.common.monitor.error.FromEarlierThanTo"));
					validationFailed=true;
				}
			}
			 Date d= new Date(); 
			 GregorianCalendar cal = new GregorianCalendar();
		     cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
		     cal.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH));
		     cal.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
             cal.set(Calendar.HOUR_OF_DAY, Calendar.getInstance().get(Calendar.HOUR_OF_DAY));
	         cal.set(Calendar.MINUTE, Calendar.getInstance().get(Calendar.MINUTE));
	         cal.set(Calendar.SECOND, 0);
	         cal.set(Calendar.MILLISECOND,0);
		     d=cal.getTime();
		       
			if (d.after(cForm.getStartDate())){
				this.addFieldError("startYear",getText("resource.common.control.error.ScheduleInvalid"));
				validationFailed=true;	
			}
		}else{
			cForm.setStartTime(ScheduleFormNG.START_NOW);
		}
			request.setAttribute("recurrenceDay", cForm.getRecurrenceDay());
			if (validationFailed) {
				return INPUT;
			}
			if (cForm.getStartTime().equals(ScheduleFormNG.START_NOW)) {
				if (runBackendSave) {
					controlBoss
							.doAction(sessionId, appdefId, action, (String) null);
				}
			} else {

				// create the new action to schedule
				
				if (runBackendSave) {
					controlBoss.doAction(sessionId, appdefId, action, covertToScheduleValue());
				}
			}

			// set confirmation message 
			if (runBackendSave) {
				addActionMessage(getText("resource.common.scheduled.Confirmation"));
			}
			internalRid = appdefId.getId().toString();
			internalType = String.valueOf(appdefId.getType());
			internalEid = internalType + ":" + internalRid;
			return SUCCESS;
		} catch (PluginNotFoundException pnfe) {
			log.trace("no plugin available", pnfe);
			this.addActionError(getText("resource.common.control.PluginNotFound"));
			return INPUT;
		} catch (PluginException cpe) {
			log.trace("control not enabled", cpe);
			this.addActionError(getText("resource.common.error.ControlNotEnabled"));
			return INPUT;
		} catch (PermissionException pe) {
			this.addActionError(getText("resource.common.control.error.NewPermission"));
			return INPUT;
		} catch (SchedulerException se) {
			this.addActionError(getText("resource.common.control.error.ScheduleInvalid"));
			return INPUT;
		}
		
	}

	@SkipValidation
	public String cancel() throws Exception {
		setHeaderResources();
		clearErrorsAndMessages();
		AppdefEntityID appdefId = RequestUtils.getEntityId(request);
		if (appdefId != null) {
			internalEid = appdefId.getId() + ":" + appdefId.getType();
		}
		internalRid = appdefId.getId().toString();
		internalType = String.valueOf(appdefId.getType());
		return "cancel";
	}

	@SkipValidation
	public String reset() throws Exception {
		setHeaderResources();
		cForm.reset();
		clearErrorsAndMessages();
		AppdefEntityID appdefId = RequestUtils.getEntityId(request);
		if (appdefId != null) {
			internalEid = appdefId.getId() + ":" + appdefId.getType();
			;
		}
		internalRid = appdefId.getId().toString();
		internalType = String.valueOf(appdefId.getType());
		return "reset";
	}

	public ControlFormNG getModel() {
		// TODO Auto-generated method stub
		return cForm;
	}

	public String getInternalRid() {
		return internalRid;
	}

	public void setInternalRid(String internalRid) {
		this.internalRid = internalRid;
	}

	public String getInternalType() {
		return internalType;
	}

	public void setInternalType(String internalType) {
		this.internalType = internalType;
	}

	public ControlFormNG getcForm() {
		return cForm;
	}

	public void setcForm(ControlFormNG cForm) {
		this.cForm = cForm;
	}

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}
	
	protected ScheduleValue covertToScheduleValue(){
		if (!(cForm.getEndTime().equals("1"))) {
			cForm.setEndTime(ScheduleFormNG.END_ON_DATE);
			cForm.setEndMin(cForm.getStartMin());
			cForm.setEndHour(cForm.getStartHour());
			cForm.setEndAmPm(cForm.getStartAmPm());
		} else {
			cForm.setEndTime(ScheduleFormNG.END_NEVER);
		}
		if (!(cForm.getRecurInterval().equals(ScheduleFormNG.RECUR_NEVER))) {
			if (cForm.getRecurInterval().equals(
					ScheduleFormNG.RECUR_DAILY)) {
				if (cForm.getRecurrenceFrequencyDaily().equals("1")) {
					cForm.setRecurrenceFrequencyDaily(ScheduleFormNG.EVERY_DAY);

				} else {
					cForm.setRecurrenceFrequencyDaily(ScheduleFormNG.EVERY_WEEKDAY);
				}
			}
			if (cForm.getRecurInterval().equals(ScheduleFormNG.RECUR_MONTHLY)) {
				if (cForm.getRecurrenceFrequencyMonthly().equals("1")) {
					cForm.setRecurrenceFrequencyMonthly(ScheduleFormNG.ON_EACH);

				} else {
					cForm.setRecurrenceFrequencyMonthly(ScheduleFormNG.ON_DAY);
				}
			}
		}
		ScheduleValue sv = cForm.createSchedule();
		sv.setDescription(cForm.getDescription());
		return sv;
	}

}
