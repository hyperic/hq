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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;

/**
 * This class is a two in one tag that that creates a row of quicknav icons for
 * the resource hub.
 */
public class QuicknavTag extends TagSupport {
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(QuicknavTag.class.getName());

	private AppdefResourceValue resource;
	private PageContext context;

	public AppdefResourceValue getResource() {
		return resource;
	}

	public void setResource(AppdefResourceValue s) {
		resource = s;
	}

	public String decorate(Object obj) throws Exception {
		AppdefResourceValue rv = getResource();

		if (rv == null) {
			log.debug("Resource attribute value is null");

			return QuicknavUtil.getNA();
		}

		if (rv.getEntityId() == null) {
			log.debug("Resource entityId value is null");

			return QuicknavUtil.getNA();
		}

		return QuicknavUtil.getOutput(rv, context);
	}

	public int doStartTag() throws JspException {
		try {
			String d = decorate(this);

			context.getOut().write(d);
		} catch (Exception e) {
			log.error("Error while displaying nav icons.", e);

			throw new JspException(e);
		}

		return SKIP_BODY;
	}

	public int doEndTag() {
		release();

		return EVAL_PAGE;
	}

	public void setPageContext(PageContext pc) {
		context = pc;
	}

	public void release() {
		context = null;
		resource = null;
	}
}