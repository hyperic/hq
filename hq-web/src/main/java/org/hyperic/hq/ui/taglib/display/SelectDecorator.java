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

/*
 * Created on Apr 16, 2003
 *
 */
package org.hyperic.hq.ui.taglib.display;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspTagException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This decorator writes whatever is in the value attribute
 * 
 * 
 */
public class SelectDecorator extends BaseDecorator {
	private static final String VALUE_KEY = "value";
	private static final String LABEL_KEY = "label";

	private static Log log = LogFactory.getLog(SelectDecorator.class.getName());

	private String onchange_el;
	private List<Map<String, String>> optionList_el;
	private Integer selectedId_el;

	/**
	 * don't skip the body
	 */
	public int doStartTag() throws JspTagException {
		Object parent = getParent();

		if (parent == null || !(parent instanceof ColumnTag)) {
			throw new JspTagException("A BaseDecorator must be used within a ColumnTag.");
		}

		((ColumnTag) parent).setDecorator(this);

		return SKIP_BODY;
	}

	/**
	 * tag building is done in the buildTag method.
	 * 
	 * This method is not implemented because the table body must be evaluated
	 * first
	 * 
	 * @see org.hyperic.hq.ui.taglib.display.ColumnDecorator#decorate(java.lang.Object)
	 */
	public String decorate(Object obj) {
		return generateOutput();
	}

	private String generateOutput() {
		List<Map<String, String>> list = getOptionItems();

		// do nothing for a null list or list size is zero
		if (list == null || list.size() == 0) {
			return "";
		}
		
		// for list with one item, just return the string of the label
		if (list.size() == 1) {
			Iterator<Map<String, String>> i = list.iterator();
			Map<String, String> items = i.next();
			
			return items.get(LABEL_KEY);
		}

		StringBuffer sb = new StringBuffer("<select ");
		
		sb.append("onchange=\"").append(getOnchange()).append("\">");

		for (Iterator<Map<String, String>> i = list.iterator(); i.hasNext();) {
			Map<String, String> items = i.next();
			String val = items.get(VALUE_KEY);
			String label = items.get(LABEL_KEY);
			Integer intVal = new Integer(val);

			sb.append("<option ");
			
			if (intVal.intValue() == getSelectedId().intValue()) {
				sb.append("\" selected=\"selected\" ");
			}
			
			sb.append(" value=\"");
			sb.append(val).append("\" >");
			sb.append(label).append("</option>");
		}

		sb.append("</select>");
		
		return sb.toString();
	}

	/**
	 * @return
	 */
	public String getOnchange() {
		return onchange_el;
	}

	/**
	 * @param string
	 */
	public void setOnchange(String value) {
		onchange_el = value;
	}

	/**
	 * @return
	 */
	public List<Map<String, String>> getOptionItems() {
		return optionList_el;
	}

	/**
	 * @param string
	 */
	public void setOptionItems(List<Map<String, String>> value) {
		optionList_el = value;
	}

	/**
	 * @return
	 */
	public Integer getSelectedId() {
		return selectedId_el;
	}

	/**
	 * @param string
	 */
	public void setSelectId(Integer value) {
		selectedId_el = value;
	}
}
