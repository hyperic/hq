/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.ui.taglib.display;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConditionalLinkDecorator extends BaseDecorator {
	private static Log log = LogFactory.getLog(ConditionalLinkDecorator.class.getName());

	private String test;
	private String href;

	@Override
	public String decorate(Object columnValue) {
		// This is the actual text of the column taken from the column tag...
		String columnText = (String) columnValue;

		// ...now figure out if we have to render a link or not, if we can't
		// determine it, we just render the plain text as the default
		// behavior...
		String attrValue = getTest();

		// ...If we can render a link, see if we have a href and render it, if
		// not
		// we just render the default text...
		if (Boolean.parseBoolean(attrValue)) {
			String href = getHref();

			// ...at this point we should know enough to render the link or not
			if (href != null) {
				StringBuffer result = new StringBuffer();

				result.append("<a href=\"").append(href).append("\">");
				result.append(columnText);
				result.append("</a>");

				return result.toString();
			}
		}

		return columnText;
	}

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = test;
	}

	public String getHref() {
		return href;
	}

	public void setHref(String href) {
		this.href = href;
	}
}
