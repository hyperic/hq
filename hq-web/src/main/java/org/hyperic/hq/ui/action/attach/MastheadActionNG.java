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

package org.hyperic.hq.ui.action.attach;

import javax.annotation.Resource;

import org.hyperic.hq.bizapp.shared.ProductBoss;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.Portal;
import org.hyperic.hq.ui.action.BaseActionNG;
import org.hyperic.hq.ui.util.RequestUtils;
import org.springframework.stereotype.Component;

@Component(value = "mastheadActionNG")
public class MastheadActionNG extends BaseActionNG {
	@Resource
	private ProductBoss productBoss;

	
	public String execute() throws Exception {
		setHeaderResources();

		// Look up the id
		Integer id = RequestUtils.getIntParameter(request, "typeId");

		int sessionId = RequestUtils.getSessionIdInt(request);
		AttachmentDescriptor attachDesc = productBoss.findAttachment(sessionId,
				id);
		if (attachDesc != null) {
			Attachment attachment = attachDesc.getAttachment();
			String title = attachDesc.getHTML();
			request.setAttribute(Constants.TITLE_PARAM_ATTR, title);

			request.setAttribute("attachment", productBoss.findViewById(
					sessionId, attachment.getView().getId()));

			request.setAttribute(Constants.PAGE_TITLE_KEY,
					attachDesc.getHelpTag());
			Portal portal = Portal.createPortal("attachment.title", "");
			request.setAttribute(Constants.PORTAL_KEY, portal);

		}

		return SUCCESS;
	}

}
