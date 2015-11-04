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

import java.util.HashMap;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * An Action that removes a control event from a server.
 */
@Component("removeControlJobsActionNG")
@Scope(value="prototype")
public class RemoveControlJobsActionNG
    extends BaseActionNG implements ModelDriven<RemoveControlJobsFormNG> {

    private final Log log = LogFactory.getLog(RemoveControlJobsActionNG.class.getName());
    @Resource
    private ControlBoss controlBoss;
    RemoveControlJobsFormNG rmForm = new RemoveControlJobsFormNG();
   	private Integer internalRid;
	private Integer internalType;
     /**
     * Removes control jobs from a server.
     */
    public String execute () throws Exception {

        
        request=getServletRequest();
       
           Integer[] jobs = rmForm.getControlJobs();

            AppdefEntityID aeid = RequestUtils.getEntityId(request);

            request.setAttribute(Constants.RESOURCE_PARAM, aeid.getId());
            request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(aeid.getType()));
            Integer sessionId = RequestUtils.getSessionId(request);
            controlBoss.deleteControlJob(sessionId.intValue(), jobs);
            log.trace("Removed resource control jobs.");
            addActionMessage(getText("resource.common.control.confirm.ScheduledRemoved"));
           	this.setInternalRid(aeid.getId());
    		this.setInternalType(aeid.getType());
            return SUCCESS;

             }

	public RemoveControlJobsFormNG getModel() {
		// TODO Auto-generated method stub
		return rmForm;
	}

	public Integer getInternalRid() {
		return internalRid;
	}

	public void setInternalRid(Integer internalRid) {
		this.internalRid = internalRid;
	}

	public Integer getInternalType() {
		return internalType;
	}

	public void setInternalType(Integer internalType) {
		this.internalType = internalType;
	}
}
