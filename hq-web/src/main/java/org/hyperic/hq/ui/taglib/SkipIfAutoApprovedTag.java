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

package org.hyperic.hq.ui.taglib;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.hq.ui.util.SessionUtils;

public class SkipIfAutoApprovedTag extends BodyTagSupport {
	private static final long serialVersionUID = 1L;

	private AIServerValue aiserver = null;

	public int doStartTag() throws JspException {
		try {
			
			AppdefBoss appdefBoss = Bootstrap.getBean(AppdefBoss.class);
			WebUser user = SessionUtils
					.getWebUser(((HttpServletRequest) pageContext.getRequest())
							.getSession());
			int sessionId = user.getSessionId().intValue();
			AIServerValue aiServer = getAiserver();
	
			if (BizappUtils.isAutoApprovedServer(sessionId, appdefBoss, aiServer)) {
				return SKIP_BODY;
			}
		} catch (NullPointerException npe) {
			throw new JspTagException(npe);
		}

		return EVAL_BODY_INCLUDE;
	}

	public AIServerValue getAiserver() {
		return this.aiserver;
	}

	public void setAiserver(AIServerValue aiserver) {
		this.aiserver = aiserver;
	}
}
