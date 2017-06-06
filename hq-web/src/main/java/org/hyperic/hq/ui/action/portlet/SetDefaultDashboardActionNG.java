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

package org.hyperic.hq.ui.action.portlet;

import java.io.InputStream;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.json.JSONResult;
import org.hyperic.hq.ui.json.action.JsonActionContextNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.json.JSONObject;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opensymphony.xwork2.ModelDriven;

@Component("setDefaultDashboardActionNG")
@Scope(value = "prototype")
public class SetDefaultDashboardActionNG extends BaseActionNG implements ModelDriven<DashboardFormNG>{

	@Resource
    private AuthzBoss authzBoss;

	private DashboardFormNG dForm= new DashboardFormNG();
	
	private InputStream inputStream;

	public InputStream getInputStream() {
		return inputStream;
	}	
	
    public String execute() throws Exception {

    	JsonActionContextNG ctx = this.setJSONContext();
    	
        HttpSession session = request.getSession();
        WebUser user = RequestUtils.getWebUser(session);
        String currentDefaultDashboardId = user.getPreference(Constants.DEFAULT_DASHBOARD_ID, null);
        String submittedDefaultDashboardId = dForm.getDefaultDashboard();

        // Compare the incoming default dashboard id with the one we had in our
        // user preferences
        // If they aren't equal it means the user is changing it, so update
        if (!submittedDefaultDashboardId.equals(currentDefaultDashboardId)) {
            user.setPreference(Constants.DEFAULT_DASHBOARD_ID, dForm.getDefaultDashboard());
            session.setAttribute(Constants.SELECTED_DASHBOARD_ID, new Integer(dForm.getDefaultDashboard()));
            authzBoss.setUserPrefs(user.getSessionId(), user.getSubject().getId(), user.getPreferences());
        }

        //return mapping.findForward(Constants.AJAX_URL);
        
        JSONObject setDefualt = new JSONObject();
        setDefualt.put("success", "success");
        JSONResult jsonRes = new JSONResult(setDefualt);
        ctx.setJSONResult(jsonRes);
		
        inputStream = this.streamJSONResult(ctx);
        
        return SUCCESS;
    }

	public DashboardFormNG getdForm() {
		return dForm;
	}

	public void setdForm(DashboardFormNG dForm) {
		this.dForm = dForm;
	}

	public DashboardFormNG getModel() {
		return dForm;
	}

}
