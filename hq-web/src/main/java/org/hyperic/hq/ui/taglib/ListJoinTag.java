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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * A JSP tag that will take a java.util.List and format it using a given
 * delimiter string.
 * 
 */
public class ListJoinTag extends TagSupport {
	private static final long serialVersionUID = 1L;

	private List list;
	private String delimiter;
	private String property;

	/**
	 * Set the name of the variable in the context that holds the
	 * <code>java.util.List</code> to be formatted.
	 * 
	 * @param list
	 *            the el expression for the list variable
	 */
	public void setList(List list) {
		this.list = list;
	}

	public List getList() {
		return list;
	}

	/**
	 * Set the value of the list item delimiter (what will be printed between
	 * list items).
	 * 
	 * @param delimiter
	 *            the text to be printed between list items
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	/**
	 * Set the property to be used for displaying the joined list. This is
	 * useful if the list contains objects that are not primitive types, but are
	 * instead java beans.
	 * 
	 * @param property
	 *            the bean property to display
	 */
	public void setProperty(String property) {
		this.property = property;
	}

	public String getProperty() {
		return property;
	}

	/**
	 * Process the tag, generating and formatting the list.
	 * 
	 * @exception JspException
	 *                if the scripting variable can not be found or if there is
	 *                an error processing the tag
	 */
	public final int doStartTag() throws JspException {
		try {
			JspWriter out = pageContext.getOut();
			List list = getList();
			String property = getProperty();
			
			for (Iterator it = list.iterator(); it.hasNext();) {
				if (null == property || 0 == property.length()) {
					out.write(String.valueOf(it.next()));
				} else {
					try {
						Object bean = it.next();
						PropertyDescriptor pd = new PropertyDescriptor(
								property, bean.getClass());
						Method m = pd.getReadMethod();
						Object value = m.invoke(bean, (Object[]) null);
						
						out.write(String.valueOf(value));
					} catch (IntrospectionException e) {
						out.write("???" + property + "???");
					} catch (IllegalAccessException e) {
						out.write("???" + property + "???");
					} catch (InvocationTargetException e) {
						out.write("???" + property + "???");
					}
				}
				if (it.hasNext()) {
					out.write(delimiter);
				}
			}

			return SKIP_BODY;
		} catch(NullPointerException npe) {
			throw new JspTagException(npe);
		} catch (IOException ioe) {
			throw new JspTagException(ioe);
		}
	}

	public int doEndTag() throws JspException {
		release();
		return EVAL_PAGE;
	}

	public void release() {
		list = null;
		delimiter = null;
		super.release();
	}
}