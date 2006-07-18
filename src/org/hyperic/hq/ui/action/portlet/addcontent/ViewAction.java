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

package org.hyperic.hq.ui.action.portlet.addcontent;

import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;

import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.StringUtil;

import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.Constants;

/**
 * An <code>Action</code> that loads the <code>Portal</code>
 * identified by the <code>PORTAL_PARAM</code> request parameter (or
 * the default portal, if the parameter is not specified) into the
 * <code>PORTAL_KEY</code> request attribute.
 */
public class ViewAction extends TilesAction {
    // --------------------------------------------------------- Public Methods
    
    
   public ActionForward execute(ComponentContext context,
                            ActionMapping mapping,
                            ActionForm form,
                            HttpServletRequest request,
                            HttpServletResponse response)
   throws Exception{

        List portlets = (List) context.getAttribute("portlets");
        WebUser user =  (WebUser) request.getSession().getAttribute(
        Constants.WEBUSER_SES_ATTR );

        ArrayList availablePortlets = new ArrayList();
        String userPortlets = new String();

        Boolean wide = new Boolean( (String) context.getAttribute("wide") );


        if( wide.booleanValue() ){
            userPortlets = user.getPreference( Constants.USER_PORTLETS_SECOND );
        }
        else{
            userPortlets = user.getPreference( Constants.USER_PORTLETS_FIRST );
        }

        for( Iterator i = portlets.iterator(); i.hasNext(); ){
            String portlet = (String) i.next();

            if( userPortlets.indexOf(portlet) == -1 )
                availablePortlets.add(portlet);

        }

        context.putAttribute("availablePortlets", availablePortlets);
        return null;
        
    }
    
}
