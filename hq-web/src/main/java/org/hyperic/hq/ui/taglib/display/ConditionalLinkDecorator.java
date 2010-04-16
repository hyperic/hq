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
