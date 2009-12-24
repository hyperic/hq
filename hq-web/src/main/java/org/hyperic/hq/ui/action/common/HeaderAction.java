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

package org.hyperic.hq.ui.action.common;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.tiles.ComponentContext;
import org.apache.struts.tiles.actions.TilesAction;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.AttachType;
import org.hyperic.hq.hqu.server.session.AttachmentMasthead;
import org.hyperic.hq.hqu.server.session.ViewMastheadCategory;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class HeaderAction
    extends TilesAction {

    private ProductBoss productBoss;

    @Autowired
    public HeaderAction(ProductBoss productBoss) {
        super();
        this.productBoss = productBoss;
    }

    public ActionForward execute(ComponentContext context, ActionMapping mapping, ActionForm form,
                                 HttpServletRequest request, HttpServletResponse response) throws ServletException,
        RemoteException, SessionException {

        Integer sessionId = RequestUtils.getSessionId(request);
        Collection<AttachmentDescriptor> mastheadAttachments = productBoss.findAttachments(sessionId.intValue(),
            AttachType.MASTHEAD);

        ArrayList<AttachmentDescriptor> resourceAttachments = new ArrayList<AttachmentDescriptor>();
        ArrayList<AttachmentDescriptor> trackerAttachments = new ArrayList<AttachmentDescriptor>();
        for (AttachmentDescriptor d : mastheadAttachments) {
            AttachmentMasthead attachment = (AttachmentMasthead) d.getAttachment();
            if (attachment.getCategory().equals(ViewMastheadCategory.RESOURCE)) {
                resourceAttachments.add(d);
            } else if (attachment.getCategory().equals(ViewMastheadCategory.TRACKER)) {
                trackerAttachments.add(d);
            }
        }

        request.setAttribute("mastheadResourceAttachments", resourceAttachments);
        request.setAttribute("mastheadTrackerAttachments", trackerAttachments);
        return null;
    }
}
