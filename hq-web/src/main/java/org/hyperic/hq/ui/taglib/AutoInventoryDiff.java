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

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;

/**
 * Generates a String representing the diff for an AIPlatform
 */
public class AutoInventoryDiff extends TagSupport {
	private static final long serialVersionUID = 1L;
	
	private AIPlatformValue resource;

	public int doStartTag() throws JspException {
		try {
			JspWriter output = pageContext.getOut();
			AIPlatformValue platformValue = getResource();
			String diffString = AIQueueConstants.getPlatformDiffString(
					platformValue.getQueueStatus(), platformValue.getDiff());

			output.print(diffString);
		} catch (NullPointerException npe) {
			throw new JspTagException("Resource attribute value is null", npe);
		} catch (IOException e) {
			throw new JspException(e);
		}

		return SKIP_BODY;
	}

	public AIPlatformValue getResource() {
		return this.resource;
	}

	public void setResource(AIPlatformValue resource) {
		this.resource = resource;
	}
}
