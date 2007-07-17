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

package org.hyperic.hq.ui.action.admin.user;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.WebUser;

/**
 * An Action that retrieves a specific user from the BizApp to
 * facilitate display of the <em>Edit Role</em> form.
 */
public class EditUserFormPrepareAction extends TilesAction {

    // ---------------------------------------------------- Public Methods

    /**
     * Retrieve User data and store it in the specified request
     * parameters.
     */
    public ActionForward execute(ComponentContext context,
                                 ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {
        Log log = LogFactory.getLog(EditUserFormPrepareAction.class.getName());
        EditForm userForm = (EditForm)form;

        WebUser user = (WebUser) request.getAttribute(Constants.USER_ATTR);

        if (userForm.getFirstName() == null)
            userForm.setFirstName( user.getFirstName() );
        if(userForm.getLastName() == null)
            userForm.setLastName( user.getLastName() );
        if(userForm.getDepartment() == null)
            userForm.setDepartment( user.getDepartment() );
        if(userForm.getName() == null)
            userForm.setName( user.getName() );
        if(userForm.getEmailAddress() == null)
            userForm.setEmailAddress( user.getEmailAddress() );
        if(userForm.getPhoneNumber() == null)
            userForm.setPhoneNumber( user.getPhoneNumber() );
        if(userForm.getSmsAddress() == null)
            userForm.setSmsAddress( user.getSmsaddress());    

        userForm.setHtmlEmail(user.isHtmlEmail());
        if ( user.getActive() ) {
            userForm.setEnableLogin("yes");
        } else {
            userForm.setEnableLogin("no");
        }

        return null;
    }
}
