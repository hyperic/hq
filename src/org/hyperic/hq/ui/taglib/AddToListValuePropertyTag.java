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
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Define a property of a spider object to be displayed in a column of
 * each of the "from" and "to" tables in the Add To List widget.
 */
public class AddToListValuePropertyTag extends TagSupport {
    private static final long serialVersionUID = 1L;
	
    private String name;
    private String value;

    public AddToListValuePropertyTag() {
        super();
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

	public int doEndTag() throws JspException {
        AddToListValueTag ancestor = (AddToListValueTag) findAncestorWithClass(this, AddToListValueTag.class);
        
        if (ancestor == null) {
            throw new JspException("no ancestor of class " + AddToListValueTag.class.getName());
        }

        try {
        	String name = getName();
        	String value = getValue();
        
        	ancestor.addProperty(name, value);
        } catch(NullPointerException npe) {
        	throw new JspException("Name or value attribute value is null", npe);
        }
        
        return EVAL_PAGE;
    }

    /**
     * Release tag state.
     *
     */
    public void release() {
        name = null;
        value = null;
        
        super.release();
    }
}