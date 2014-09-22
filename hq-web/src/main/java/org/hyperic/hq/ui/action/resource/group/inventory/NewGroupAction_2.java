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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.grouping.shared.GroupDuplicateNameException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction_2;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class NewGroupAction_2
    extends BaseAction_2 implements Preparable ,ModelDriven<GroupForm>{

    /**
	 * 
	 */
	private static final long serialVersionUID = -2623114659250512241L;
	/**
	 * 
	 */
	
	private final Log log = LogFactory.getLog(NewGroupAction_2.class.getName());
    private AppdefBoss appdefBoss;
    private GroupForm model;// = new GroupForm();

    @Autowired
    public NewGroupAction_2(AppdefBoss appdefBoss) {
        super();
        this.appdefBoss = appdefBoss;
    }
    
    public String ppp() {
    	System.out.println("PPPPPPP");
    	return SUCCESS;
    }
    /**
     * Create the group with the attributes specified in the given
     * <code>GroupForm</code>.
     */
    public String execute( HttpServletRequest request, HttpServletResponse response) throws Exception {
    	System.out.println("SSSSS");
/*        final GroupForm groupForm = (GroupForm) form; */

        // Clean up after ourselves first
        HttpSession session = request.getSession();
        session.removeAttribute(Constants.ENTITY_IDS_ATTR);
        session.removeAttribute(Constants.RESOURCE_TYPE_ATTR);

    
        try {

            Integer sessionId = RequestUtils.getSessionId(request);
            ResourceGroup newGroup;

            final Integer entType = model.getEntityTypeId();

            // Append username to private groups
            if (model.isPrivateGroup()) {
                final WebUser user = RequestUtils.getWebUser(session);
                final String privateName = RequestUtils.message(request, "resource.group.name.private",
                    new Object[] { user.getName() });
                model.setName(model.getName() + " " + privateName);
            }

            if (model.getGroupType() == Constants.APPDEF_TYPE_GROUP_COMPAT) {
                newGroup = appdefBoss.createGroup(sessionId, entType, model.getResourceTypeId(), model.getName(),
                    model.getDescription(), model.getLocation(), model.getEntityIds(), model.isPrivateGroup());
            } else {
                // Constants.APPDEF_TYPE_GROUP_ADHOC
                if (entType == AppdefEntityConstants.APPDEF_TYPE_APPLICATION ||
                    entType == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
                    newGroup = appdefBoss.createGroup(sessionId, entType, model.getName(), model.getDescription(),
                        model.getLocation(), model.getEntityIds(), model.isPrivateGroup());
                } else {
                    // otherwise, create a mixed group
                    newGroup = appdefBoss.createGroup(sessionId, model.getName(), model.getDescription(), model
                        .getLocation(), model.getEntityIds(), model.isPrivateGroup());
                }
            }

            log.trace("creating group [" + model.getName() + "] with attributes " + model);

            HashMap<String, Object> forwardParams = new HashMap<String, Object>(2);
            forwardParams.put(Constants.RESOURCE_PARAM, newGroup.getId());
            forwardParams.put(Constants.RESOURCE_TYPE_ID_PARAM, AppdefEntityConstants.APPDEF_TYPE_GROUP);

            model.setRid(newGroup.getId());

            RequestUtils.setConfirmation(request, "resource.group.inventory.confirm.CreateGroup", model.getName());

            return "SUCCESS";
        } catch (GroupDuplicateNameException ex) {
            log.debug("group creation failed:", ex);
            RequestUtils.setError(request, "resource.group.inventory.error.DuplicateGroupName");
            return "ERROR";
        }
    }

	public GroupForm getModel() {
		
		return model;
	}

	public void setModel(GroupForm groupForm) {
		this.model = groupForm;
	}

	public void prepare() throws Exception {
	  // groupForm.validate(null, _request);
		
	}
}
