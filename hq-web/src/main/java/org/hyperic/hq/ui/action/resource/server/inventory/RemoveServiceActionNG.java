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

package org.hyperic.hq.ui.action.resource.server.inventory;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.action.resource.RemoveResourceFormNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("RemoveServerActionNG")
@Scope("prototype")
public class RemoveServiceActionNG extends BaseActionNG implements
	ModelDriven<RemoveResourceFormNG> {
	private final Log log = LogFactory.getLog(RemoveServiceActionNG.class);

	@Resource
    private AppdefBoss appdefBoss;
	
	private RemoveResourceFormNG nwForm = new RemoveResourceFormNG() ;
	
	private String internalEid;
	
	public String execute() throws Exception {
		

        AppdefEntityID aeid = new AppdefEntityID(nwForm.getEid());
        Integer[] resources = nwForm.getResources();
        Map<String, Object> params = new HashMap<String, Object>(2);
        params.put(Constants.ENTITY_ID_PARAM, aeid);

        if (aeid.isPlatform()) {
            params.put(Constants.ACCORDION_PARAM, "3");
        } else {
            params.put(Constants.ACCORDION_PARAM, "1");
        }

        if (resources == null || resources.length == 0) {
            return SUCCESS;
        }

        Integer sessionId = RequestUtils.getSessionId(request);

        log.trace("removing resource");

        for (int i = 0; i < resources.length; i++) {
            appdefBoss.removeAppdefEntity(sessionId.intValue(), AppdefEntityID.newServiceID(resources[i]), false);
        }

        internalEid = nwForm.getEid();
        
        return SUCCESS;
	}

	public RemoveResourceFormNG getModel() {
		return nwForm;
	}

	public RemoveResourceFormNG getPForm() {
		return nwForm;
	}

	public void setPForm(RemoveResourceFormNG nwForm) {
		this.nwForm = nwForm;
	}

	public String getInternalEid() {
		return internalEid;
	}

	public void setInternalEid(String internalEid) {
		this.internalEid = internalEid;
	}
}
