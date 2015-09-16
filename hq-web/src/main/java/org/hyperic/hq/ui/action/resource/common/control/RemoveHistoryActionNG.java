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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

/**
 * An Action that removes a control event from a resource.
 */
@Component("removeHistoryActionNG")
@Scope(value="prototype")
public class RemoveHistoryActionNG
    extends BaseActionNG implements ModelDriven<RemoveHistoryFormNG>  {

    private final Log log = LogFactory.getLog(RemoveHistoryActionNG.class);
    @Resource
    private ControlBoss controlBoss;

    RemoveHistoryFormNG rmForm = new RemoveHistoryFormNG();
   

    /**
     * removes controlactions from a resource
     */
    public String execute() throws Exception {

        
        request=getServletRequest();
        Integer[] actions = rmForm.getControlActions();
        AppdefEntityID aeid = RequestUtils.getEntityId(request);

        request.setAttribute(Constants.RESOURCE_PARAM, aeid.getId());
        request.setAttribute(Constants.RESOURCE_TYPE_ID_PARAM, new Integer(aeid.getType()));

        String forward = checkSubmit(rmForm);

        if (forward != null) {
            return forward;
        }

        if (actions == null || actions.length == 0) {
            return SUCCESS;
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        controlBoss.deleteJobHistory(sessionId.intValue(), actions);

        log.trace("Removed server control events.");
        addActionMessage(getText("resource.server.ControlHistory.Confirmation"));
        return SUCCESS;

    }


	public RemoveHistoryFormNG getModel() {
		return rmForm;
	}


	public RemoveHistoryFormNG getRmForm() {
		return rmForm;
	}


	public void setRmForm(RemoveHistoryFormNG rmForm) {
		this.rmForm = rmForm;
	}
	
}
