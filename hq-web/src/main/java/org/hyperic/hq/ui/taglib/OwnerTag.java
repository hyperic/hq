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
import javax.servlet.jsp.JspTagException;

import org.apache.struts.util.ResponseUtils;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.ui.util.BizappUtils;

/**
 * A JSP tag that formats and prints the "owner information" commonly displayed
 * as part of the attributes of a resource.
 */
public class OwnerTag extends VarSetterBaseTag {
	private static final long serialVersionUID = 1L;

	/* the name of the scoped attribute that holds our user */
	private Object owner = null;

	/**
	 * Set the name of the attribute in page, request, session or application
	 * scope that holds the <code>OperationOwner</code> object.
	 * 
	 * @param owner
	 *            the name of the scoped attribute
	 */
	public void setOwner(Object owner) {
		this.owner = owner;
	}

	public Object getOwner() {
		return owner;
	}

	/**
	 * Process the tag, generating and printing the owner information.
	 * 
	 * @exception JspException
	 *                if the scripting variable can not be found or if there is
	 *                an error processing the tag
	 */
	public final int doStartTag() throws JspException {
		try {
			Object owner = getOwner();

			/*
			 * XXX: would be nice if WebUser and AuthzSubject implemented a
			 * common interface or something
			 */
			String username;
			String email;
			String full;

			if (owner instanceof WebUser) {
				WebUser webUser = (WebUser) owner;
				username = webUser.getUsername();
				email = webUser.getEmailAddress();
				full = BizappUtils.makeSubjectFullName(webUser.getFirstName(),
						webUser.getLastName());
			} else if (owner instanceof AuthzSubjectValue) {
				AuthzSubjectValue subject = (AuthzSubjectValue) owner;
				username = subject.getName();
				email = subject.getEmailAddress();
				full = BizappUtils.makeSubjectFullName(subject.getFirstName(),
						subject.getLastName());
			} else {
				AuthzSubject subject = (AuthzSubject) owner;
				username = subject.getName();
				email = subject.getEmailAddress();
				full = BizappUtils.makeSubjectFullName(subject.getFirstName(),
						subject.getLastName());
			}

			// if we have an email address:
			// if we have a username, display full name and linked username
			// else display linked full name
			// else
			// display the full name
			// if we have a username, display the username

			StringBuffer output = new StringBuffer();

			if (email != null && !email.equals("")) {
				if (username != null && username.length() > 0) {
					output.append(ResponseUtils.filter(full));
					output.append(" (<a href=\"mailto:");
					output.append(ResponseUtils.filter(email));
					output.append("\">");
					output.append(ResponseUtils.filter(username));
					output.append("</a>)");
				} else {
					output.append("<a href=\"mailto:");
					output.append(ResponseUtils.filter(email));
					output.append("\">");
					output.append(ResponseUtils.filter(full));
					output.append("</a>");
				}
			} else {
				output.append(ResponseUtils.filter(full));
				if (username != null && username.length() > 0) {
					output.append(" (");
					output.append(ResponseUtils.filter(username));
					output.append(")");
				}
			}

			setScopedVariable(output.toString());
		} catch (NullPointerException npe) {
			throw new JspTagException("Owner attribute value is null", npe);
		}

		return SKIP_BODY;
	}

	/**
	 * Release tag state.
	 * 
	 */
	public void release() {
		owner = null;
		super.release();
	}
}
