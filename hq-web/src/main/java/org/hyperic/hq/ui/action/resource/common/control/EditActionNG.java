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


/**
 * modifies the controlAction data.
 */


package org.hyperic.hq.ui.action.resource.common.control;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.ScheduleFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.joda.time.LocalDate;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;
@Component("editActionNG")
@Scope(value="prototype")
public class EditActionNG extends BaseActionNG implements ModelDriven<ControlFormNG> {

	    private final Log log = LogFactory.getLog(EditActionNG.class.getName());
	    @Resource
	    private ControlBoss controlBoss;
	    ControlFormNG cForm = new ControlFormNG();
	    private String internalEid;
	    private String internalRid;
		private String internalType;
		private String internalBid;
	   
       
	    public String save() throws Exception {

	        log.trace("modifying Control action");
	        try {
	        	request=getServletRequest();
	            int sessionId = RequestUtils.getSessionId(request).intValue();
	            AppdefEntityID appdefId = RequestUtils.getEntityId(request);
                request.setAttribute(Constants.RESOURCE_PARAM, appdefId.getId());
	            request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(appdefId.getType()));
	          
	            // XXX This is not working as an "edit" until PR: 4815 is
	            // resolved. Currently
	            // will delete that control action, and replace it with a new.
//copied from new action
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

				//till here
	            // make sure that the ControlAction is valid.
	            String action = cForm.getControlAction();
	            List<String> validActions = controlBoss.getActions(sessionId, appdefId);
	            if (!validActions.contains(action)) {
	            	this.addActionError(getText( "resource.common.control.error.ControlActionNotValid", action));
	                return INPUT;
	            }
//another copied code
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
			if (new Date().after(cForm.getStartDate())){
				this.addFieldError("startYear",getText("resource.common.control.error.ScheduleInvalid"));
				validationFailed=true;	
			}
	       }
	            else{
	    			cForm.setStartTime(ScheduleFormNG.START_NOW);
	    		}
			request.setAttribute("recurrenceDay", cForm.getRecurrenceDay());
			if (validationFailed) {
				return INPUT;
			}
//till here
	            Integer[] triggers = new Integer[] { RequestUtils
	                .getIntParameter(request, Constants.CONTROL_BATCH_ID_PARAM), };
	            controlBoss.deleteControlJob(sessionId, triggers);
	            if (cForm.getStartTime().equals(ScheduleFormNG.START_NOW)) {
					controlBoss
							.doAction(sessionId, appdefId, action, (String) null);
				} else {

					// create the new action to schedule
					
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

	            // create the new action to schedule
	            ScheduleValue sv = cForm.createSchedule();
	            sv.setDescription(cForm.getDescription());
				
	            if (cForm.getStartTime().equals(ScheduleFormNG.START_NOW)) {
	                controlBoss.doAction(sessionId, appdefId, action, (String) null);
	            } else {
	                controlBoss.doAction(sessionId, appdefId, action, sv);
	            }
				}
	            // set confirmation message
	            addActionMessage(getText("resource.common.scheduled.Confirmation"));
	            internalRid = appdefId.getId().toString();
	    		internalType = String.valueOf(appdefId.getType());
	            internalEid = internalType + ":" + internalRid;
	            return SUCCESS;
	        } catch (PluginNotFoundException pnfe) {
	            log.trace("no plugin available", pnfe);
	            this.addActionError(getText( "resource.common.error.PluginNotFound"));
	            return INPUT;
	        } catch (PluginException cpe) {
	            log.trace("control not enabled", cpe);
	            this.addActionError(getText("resource.common.error.ControlNotEnabled"));
	            return INPUT;
	        }
	    }
	    @SkipValidation
		public String cancel() throws Exception {
			setHeaderResources();
			clearErrorsAndMessages();
			AppdefEntityID appdefId = RequestUtils.getEntityId(request);
			if (appdefId!= null) {
				internalEid = appdefId.getId()+":"+appdefId.getType();
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
			if (appdefId!= null) {
				this.internalEid = appdefId.toString();
				this.internalRid = appdefId.getId()+"";
				this.internalType = appdefId.getType()+"";
				
			}
			this.setInternalBid(RequestUtils.getIntParameter(request, Constants.CONTROL_BATCH_ID_PARAM)+"");
		return "reset";
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


		public ControlFormNG getModel() {
			return cForm;
		}
		public String getInternalBid() {
			return internalBid;
		}
		public void setInternalBid(String internalBid) {
			this.internalBid = internalBid;
		}
		
	    

}
