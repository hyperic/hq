package org.hyperic.hq.ui.action.resource.group.control;

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


import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.ui.action.resource.common.control.NewActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
/**
 * A <code>BaseAction</code> subclass that creates a group control action in the
 * BizApp.
 */


@Component("newGroupContorlActionNG")
@Scope(value = "prototype")
public class NewGroupActionNG extends NewActionNG {

	
	public NewGroupActionNG () {
		this.cForm = new GroupControlFormNG();
		this.log = LogFactory.getLog(NewGroupActionNG.class.getName());
		this.runBackendSave=false;
	}
	
	
	public String save() throws Exception {
		
		int sessionId = RequestUtils.getSessionId(request).intValue();
		AppdefEntityID appdefId = RequestUtils.getEntityId(request);
		
		String outcome = super.save();
		
		if (outcome.equals(INPUT)) {
			 request = getServletRequest();
			 String parallel=request.getParameter("inParallel");
				if (parallel==null || parallel.equals("true")) {
					((GroupControlFormNG) cForm).setInParallel(Boolean.TRUE);
				} else {
					((GroupControlFormNG) cForm).setInParallel(Boolean.FALSE);
				}
			
			return outcome;
		}
		
		ScheduleValue sv = covertToScheduleValue();
        Integer[] orderSpec = null;
        int[] newOrderSpec = null;
        if ( ((GroupControlFormNG)  cForm).getInParallel().booleanValue() == GroupControlFormNG.IN_ORDER.booleanValue()) {
            orderSpec = ((GroupControlFormNG)  cForm).getResourceOrdering();
            newOrderSpec = new int[orderSpec.length];
            for (int i = 0; i < orderSpec.length; i++) {
                newOrderSpec[i] = orderSpec[i].intValue();
            }
        }
        try{
        if (cForm.getStartTime().equals(GroupControlFormNG.START_NOW)) {
            controlBoss.doGroupAction(sessionId, appdefId, cForm.getControlAction(), (String) null, newOrderSpec);
        } else {
            controlBoss.doGroupAction(sessionId, appdefId, cForm.getControlAction(), newOrderSpec, sv);
        }
        }catch (Exception e){
        	addActionError(getText("resource.common.error.ControlNotEnabled"));
        	return INPUT;
        }
        addActionMessage(getText("resource.common.scheduled.Confirmation"));
		return outcome;
	}
	
	
	
	
}
