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

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;

import org.hyperic.hq.authz.shared.OperationValue;
import org.hyperic.hq.authz.shared.AuthzConstants;

public class EditPasswordPrepareAction extends TilesAction{
    // --------------------------------------------------------- Public Methods

   public ActionForward execute(ComponentContext context,
                        ActionMapping mapping,
                        ActionForm form,
                        HttpServletRequest request,
                        HttpServletResponse response)
    throws Exception {
        
        
        WebUser user = (WebUser) request.getSession().getAttribute( 
                                                   Constants.WEBUSER_SES_ATTR );
        ServletContext ctx = getServlet().getServletContext();
        Integer sessionId = user.getSessionId();
        AuthzBoss authzBoss = ContextUtils.getAuthzBoss(ctx);
                        
        
        boolean admin = false;                

        for(Iterator i = authzBoss.getAllOperations( sessionId ).iterator();
            i.hasNext();){
            OperationValue operation = (OperationValue) i.next();            
            if( AuthzConstants.subjectOpModifySubject.equals(operation.getName()) ){
                admin = true;
                break;
            }            
        }

        if(admin)
            context.putAttribute("administrator", "true");
        
        return null;
            
    }
    
}
