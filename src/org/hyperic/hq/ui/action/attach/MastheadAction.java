/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007], Hyperic, Inc.
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

package org.hyperic.hq.ui.action.attach;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl;
import org.hyperic.hq.hqu.shared.UIPluginManagerLocal;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseAction;
import org.hyperic.hq.ui.util.ContextUtils;
import org.hyperic.hq.ui.util.RequestUtils;

public class MastheadAction extends BaseAction {

    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
        throws Exception {
        // Look up the id
        Integer id = RequestUtils.getIntParameter(request, "typeId");
        UIPluginManagerLocal pluginManager = UIPluginManagerEJBImpl.getOne();
        AttachmentDescriptor attachDesc = pluginManager.findAttachmentDescriptorById(id);
        if(attachDesc != null){
        	Attachment attachment = attachDesc.getAttachment();
        	String title = attachDesc.getHTML();
            request.setAttribute(Constants.TITLE_PARAM_ATTR, title);
            ServletContext ctx = getServlet().getServletContext();
            ProductBoss pBoss = ContextUtils.getProductBoss(ctx );
            request.setAttribute("attachment",
                pBoss.findViewById(
                    RequestUtils.getSessionId(request).intValue(),
                    attachment.getView().getId()));
            
            request.setAttribute(Constants.PAGE_TITLE_KEY, 
            		attachDesc.getHelpTag());
            Portal portal = Portal.createPortal("attachment.title", "");
            request.setAttribute(Constants.PORTAL_KEY, portal);

        }
        
        return null;
    }

}
