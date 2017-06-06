/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import java.util.Collection;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tiles.AttributeContext;
import org.apache.tiles.context.TilesRequestContext;
import org.apache.tiles.preparer.ViewPreparer;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.ViewResourceCategory;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("tabBodyActionNG")
public class TabBodyActionNG extends BaseActionNG implements ViewPreparer {

	private final Log log = LogFactory.getLog(TabBodyActionNG.class);
	
	@Autowired
	private ProductBoss productBoss;


	public void execute(TilesRequestContext tilesContext,
			AttributeContext attributeContext) {
		request = getServletRequest();
		// Look up the id
		Integer id;
		try {
			id = RequestUtils.getIntParameter(request, "id");
		} catch (Exception e) {
			id = null;
		}
		AppdefEntityID eid = RequestUtils.getEntityId(request);

		int sessionId;
		try {
			sessionId = RequestUtils.getSessionIdInt(request);
		
		Collection<AttachmentDescriptor> availAttachents = productBoss
				.findAttachments(sessionId, eid, ViewResourceCategory.VIEWS);

		// Set the list of avail attachments
		request.setAttribute("resourceViewTabAttachments", availAttachents);
		for (AttachmentDescriptor attach : availAttachents) {
			Attachment a = attach.getAttachment();
			if (a.getId().equals(id)) {
				// Set the requested view
				String title = attach.getHTML();
				request.setAttribute(Constants.TITLE_PARAM_ATTR, title);
				request.setAttribute("resourceViewTabAttachment", productBoss
						.findViewById(RequestUtils.getSessionId(request)
								.intValue(), a.getView().getId()));
				request.setAttribute(Constants.PAGE_TITLE_KEY,
						attach.getHelpTag());
				break;
			}
		}
		Portal portal = Portal.createPortal("attachment.title", "");
		request.setAttribute(Constants.PORTAL_KEY, portal);
		} catch (ServletException e) {
			log.error(e);
		} catch (SessionException e) {
			log.error(e);
		}
	}
}