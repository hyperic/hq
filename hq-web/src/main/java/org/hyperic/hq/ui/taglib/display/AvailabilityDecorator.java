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

package org.hyperic.hq.ui.taglib.display;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.Tag;
import javax.servlet.jsp.tagext.TagSupport;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.ui.Constants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.taglib.TagUtils;

/**
 * This class is a two in one decorator/tag for use within the
 * <code>TableTag</code>; it is a <code>ColumnDecorator</code> tag that that
 * creates a column of availability icons.
 * 
 * One of these days, when the whole DependencyNode thing is cleaned up, a lot
 * of this stuff should just move to it's own decorator just for DependencyNodes
 */
public class AvailabilityDecorator extends ColumnDecorator implements Tag {
	private final static String ICON_SRC = "/resource/Availability";
	private final static String ICON_WIDTH = "12";
	private final static String ICON_HEIGHT = "12";
	private final static String ICON_BORDER = "0";
	private final static String ICON_PRE = "/images/icon_available_";
	private final static String ICON_UP = ICON_PRE + "green.gif";
	private final static String ICON_DOWN = ICON_PRE + "red.gif";
	private final static String ICON_WARN = ICON_PRE + "yellow.gif";
	private final static String ICON_POWERED_OFF = ICON_PRE + "black.gif";
    private final static String ICON_PAUSED = ICON_PRE + "orange.gif";
	private final static String ICON_ERR = ICON_PRE + "error.gif";

	private static Log log = LogFactory.getLog(AvailabilityDecorator.class
			.getName());

	private AppdefResourceValue resource;
	private Integer resourceId;
	private Integer resourceTypeId;
	private Boolean monitorable; // optional attribute
	private String value; // optional attribute

	// flags
	private boolean resourceIsSet = false;
	private boolean resourceIdIsSet = false;
	private boolean resourceTypeIdIsSet = false;
	private boolean monitorableIsSet = false;
	private boolean valueIsSet = false;

	private PageContext context;
	private Tag parent;

	public AppdefResourceValue getResource() {
		return resource;
	}

	public void setResource(AppdefResourceValue s) {
		resourceIsSet = true;
		resource = s;
	}

	/**
	 * Returns the resourceId.
	 * 
	 * @return String
	 */
	public Integer getResourceId() {
		return resourceId;
	}

	/**
	 * Returns the resourceTypeId.
	 * 
	 * @return String
	 */
	public Integer getResourceTypeId() {
		return resourceTypeId;
	}

	/**
	 * Sets the resourceId.
	 * 
	 * @param resourceId
	 *            The resourceId to set
	 */
	public void setResourceId(Integer resourceId) {
		resourceIdIsSet = true;
		this.resourceId = resourceId;
	}

	/**
	 * @return String
	 */
	public Boolean getMonitorable() {
		return monitorable;
	}

	/**
	 * Sets the compatibleGroup.
	 * 
	 * @param compatibleGroup
	 *            The compatibleGroup to set
	 */
	public void setMonitorable(Boolean monitorable) {
		monitorableIsSet = true;
		this.monitorable = monitorable;
	}

	/**
	 * Sets the resourceTypeId.
	 * 
	 * @param resourceTypeId
	 *            The resourceTypeId to set
	 */
	public void setResourceTypeId(Integer resourceTypeId) {
		resourceTypeIdIsSet = true;
		this.resourceTypeId = resourceTypeId;
	}

	/**
	 * @return String
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Sets the value.
	 * 
	 * @param value
	 *            The value to set
	 */
	public void setValue(String value) {
		valueIsSet = true;
		this.value = value;
	}

	public String decorate(Object obj) throws Exception {
		if (valueIsSet) {
			return getOutputByValue();
		}

		if (resourceIdIsSet && resourceTypeIdIsSet) {
			try {
				return getOutputById();
			} catch (java.lang.NumberFormatException e) {
				log.debug("bogus params: resourceId=" + resourceId
						+ " resourceTypeId=" + resourceTypeId);

				return "";
			}
		}

		return getOutputByResource();
	}

	private String getOutputById() throws JspException {
		Integer id = getResourceId();
		Integer typeId = getResourceTypeId();

		if (id == null) {
			log.debug("ResourceId attribute value set to null");

			return getNA();
		}

		if (typeId == null) {
			log.debug("ResourceTypeId attribute value set to null");

			return getNA();
		}

		if (typeId.intValue() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
			if (monitorableIsSet) {
				Boolean monitorableFlag = getMonitorable();

				if (monitorableFlag == null) {
					monitorableFlag = Boolean.FALSE;
				}

				if (!monitorableFlag.booleanValue()) {
					return getNA();
				}
			} else {
				log.debug("don't know if group is compatible, returning n/a");

				return getNA();
			}
		}

		return getOutput(new AppdefEntityID(typeId.intValue(), id));
	}

	private String getOutputByResource() throws JspException {
		AppdefResourceValue resource = getResource();

		if (resource == null) {
			log.debug("Resource attribute value set to null");

			return getNA();
		}

		if (resource.getEntityId() == null) {
			return getNA();
		} else {
			return getOutput(resource.getEntityId());
		}
	}

	private String getOutputByValue() throws JspException {
		HttpServletRequest req = (HttpServletRequest) context.getRequest();
		StringBuffer src = new StringBuffer(req.getContextPath());

		if (monitorableIsSet) {
			Boolean monitorableFlag = getMonitorable();

			if (monitorableFlag == null) {
				monitorableFlag = Boolean.FALSE;
			}

			if (!monitorableFlag.booleanValue()) {
				if (resourceIdIsSet && resourceTypeIdIsSet) {
					Integer id = getResourceId();
					Integer typeId = getResourceTypeId();

					if (id == null) {
						log.debug("ResourceId attribute value set to null");

						return getNA();
					}

					if (typeId == null) {
						log.debug("ResourceTypeId attribute value set to null");

						return getNA();
					}

					return this.getOutput(new AppdefEntityID(typeId.intValue(),
							id));
				}

				// Just return not available
				return getNA();
			}
		}

		String availStr = getValue();

		if (availStr == null) {
			log.debug("Value attribute value set to null, setting to NaN");
			availStr = "";
		}

		double availVal;

		if (availStr.length() > 0) {
			availVal = Double.parseDouble(availStr);
		} else {
			availVal = Double.NaN;
		}

		if (availVal == MeasurementConstants.AVAIL_DOWN) {
			src.append(ICON_DOWN);
		} else if (availVal == MeasurementConstants.AVAIL_UP) {
			src.append(ICON_UP);
		} else if (availVal == MeasurementConstants.AVAIL_PAUSED) {
			src.append(ICON_PAUSED);
		} else if (availVal == MeasurementConstants.AVAIL_POWERED_OFF) {
            src.append(ICON_POWERED_OFF);
        } else if (availVal < MeasurementConstants.AVAIL_UP
				&& availVal > MeasurementConstants.AVAIL_DOWN) {
			src.append(ICON_WARN);
		} else {
			src.append(ICON_ERR);
		}

		StringBuffer buf = new StringBuffer().append("<img src=\"").append(src)
				.append("\" width=\"").append(ICON_WIDTH)
				.append("\" height=\"").append(ICON_HEIGHT).append(
						"\" alt=\"\" border=\"").append(ICON_BORDER).append(
						"\">");

		return buf.toString();
	}

	private String getOutput(AppdefEntityID aeid) {
		HttpServletRequest req = (HttpServletRequest) context.getRequest();
		StringBuffer src = new StringBuffer(req.getContextPath());

		src.append(ICON_SRC).append("?").append(Constants.ENTITY_ID_PARAM)
				.append("=").append(aeid.getAppdefKey());

		StringBuffer buf = new StringBuffer();

		buf.append("<img name=\"avail").append(aeid.getID())
				.append("\" src=\"").append(src).append("\" width=\"").append(
						ICON_WIDTH).append("\" height=\"").append(ICON_HEIGHT)
				.append("\" alt=\"\" border=\"").append(ICON_BORDER).append(
						"\">");

		return buf.toString();
	}

	private String getNA() throws JspException {
		return TagUtils.getInstance().message(this.getPageContext(), null,
				null, "common.value.notavail");
	}

	public int doStartTag() throws JspTagException {
		ColumnTag ancestorTag = (ColumnTag) TagSupport.findAncestorWithClass(
				this, ColumnTag.class);

		if (ancestorTag == null) {
			throw new JspTagException(
					"An AvailabilityDecorator must be used within a ColumnTag.");
		}

		// the rules are a little more complicated than what can be expressed in
		// the tld...
		if (resourceIsSet) {
			if (resourceIdIsSet || resourceTypeIdIsSet) {
				throw new JspTagException(
						"An AvailabilityDecorator must either specify a 'resource' "
								+ " attribute or both 'resourceId' and 'resourceTypeId' attributes");
			}
		} else if (!valueIsSet) {
			if (!resourceIdIsSet && !resourceTypeIdIsSet) {
				throw new JspTagException(
						"An AvailabilityDecorator must either specify a 'resource' "
								+ " attribute or both 'resourceId' and 'resourceTypeId' attributes");
			}
		}

		ancestorTag.setDecorator(this);

		return SKIP_BODY;
	}

	public int doEndTag() {
		return EVAL_PAGE;
	}

	public Tag getParent() {
		return parent;
	}

	public void setParent(Tag t) {
		this.parent = t;
	}

	public void setPageContext(PageContext pc) {
		this.context = pc;
	}

	public void release() {
		parent = null;
		context = null;
		resource = null;
		resourceId = null;
		resourceTypeId = null;
		resourceIsSet = false;
		resourceIdIsSet = false;
		resourceTypeIdIsSet = false;
	}
}
