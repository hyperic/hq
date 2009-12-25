/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.hyperic.hq.appdef.shared.AppdefResourcePermissions;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.ui.util.RequestUtils;

/**
 * Check to see if the current user has a specific set of permissions relative
 * to the current resource in scope.
 */
public class UserResourcePermission extends TagSupport {
	private static final long serialVersionUID = 1L;

	/** Holds value of property resource. */
	private AppdefResourceValue resource;
	/** Holds value of property debug. */
	private Boolean debug;

	/**
	 * evaluate the user, resource and required permission. to determine if the
	 * user had rights to perform permission on resource
	 * 
	 * populate scoped variable with true or false.
	 * 
	 * @throws JspException
	 * @return
	 */
	public final int doStartTag() throws JspException {
		// make sure these attributes aren't already in the request.

		try {
			HttpServletRequest request = (HttpServletRequest) pageContext
					.getRequest();
		
			AppdefBoss appdefBoss = Bootstrap.getBean(AppdefBoss.class);
			Boolean debug = getDebug();
			AppdefResourceValue resource = getResource();
			int sessionId = RequestUtils.getSessionId(request).intValue();
			AppdefResourcePermissions permissions = appdefBoss
					.getResourcePermissions(sessionId, resource.getEntityId());

			// put each of them in a request scoped variable.
			request.setAttribute("canControl", new Boolean(permissions
					.canControl()));
			request.setAttribute("canCreateChild", new Boolean(permissions
					.canCreateChild()));
			request.setAttribute("canMeasure", new Boolean(permissions
					.canMeasure()));
			request.setAttribute("canModify", new Boolean(permissions
					.canModify()));
			request.setAttribute("canRemove", new Boolean(permissions
					.canRemove()));
			request.setAttribute("canView", new Boolean(permissions.canView()));
			request.setAttribute("canAlert",
					new Boolean(permissions.canAlert()));

			// have a debug option to output the values as html.
			if (debug) {
				JspWriter output = pageContext.getOut();
				String br = "<br>";
				String eq = "&nbsp;=&nbsp;";

				output.print("<table>");
				output.print("<tr>");
				output.print("<td>");
				output.print("canControl" + eq + permissions.canControl());
				output.print(br);
				output.print("canCreateChild" + eq
						+ new Boolean(permissions.canCreateChild()));
				output.print(br);
				output.print("canMeasure" + eq
						+ new Boolean(permissions.canMeasure()));
				output.print(br);
				output.print("canModify" + eq
						+ new Boolean(permissions.canModify()));
				output.print(br);
				output.print("canRemove" + eq
						+ new Boolean(permissions.canRemove()));
				output.print(br);
				output.print("canView" + eq
						+ new Boolean(permissions.canView()));
				output.print(br);
				output.print("</td>");
				output.print("</tr>");
				output.print("</table>");
			}
		} catch (NullPointerException npe) {
			throw new JspTagException(npe);
		} catch (Exception e) {
			throw new JspException(e);
		}

		return SKIP_BODY;
	}

	/**
	 * @return
	 * @throws JspException
	 */
	public int doEndTag() throws JspException {
		release();
		return EVAL_PAGE;
	}

	/**
	 * Getter for property resource.
	 * 
	 * @return Value of property resource.
	 * 
	 */
	public AppdefResourceValue getResource() {
		return this.resource;
	}

	/**
	 * Setter for property resource.
	 * 
	 * @param resource
	 *            New value of property resource.
	 * 
	 */
	public void setResource(AppdefResourceValue resource) {
		this.resource = resource;
	}

	/**
	 * Getter for property debug.
	 * 
	 * @return Value of property debug.
	 * 
	 */
	public Boolean getDebug() {
		return this.debug;
	}

	/**
	 * Setter for property debug.
	 * 
	 * @param debug
	 *            New value of property debug.
	 * 
	 */
	public void setDebug(Boolean debug) {
		this.debug = debug;
	}
}
