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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Define a bundle of properties of a spider object corresponding to
 * the columns of each of the "from" and "to" tables in the Add To
 * List widget.
 *
 * After this tag and its nested body are evaluated, its ancestor
 * <code>AddToListValueTag</code> will have added a <code>
 * <code>Map</code> object (representing the spider object) with
 * properties as defined by nested
 * <code>&lt;spider:addToListValueProperty&gt;</code> tags.
 */
public class AddToListValueTag extends TagSupport {
    private static final long serialVersionUID = 1L;
	
    private String id;
    private Map<String, String> properties;

    public AddToListValueTag() {
        super();
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
		return id;
	}

	public void addProperty(String name, String value) {
        properties.put(name, value);
    }

    public int doStartTag() throws JspException {
        properties = new HashMap<String, String>();
        
        return EVAL_BODY_INCLUDE;
    }

    public int doEndTag() throws JspException {
        AddToListValuesTag ancestor = (AddToListValuesTag) findAncestorWithClass(this, AddToListValuesTag.class);
        
        if (ancestor == null) {
            throw new JspException("no ancestor of class " + AddToListValuesTag.class.getName());
        }

        try {
        	String id = getId();
        
        	properties.put("id", id);
        	ancestor.addValue(properties);
        } catch(NullPointerException npe) {
        	throw new JspException("Id attribute value is null", npe);
        }
        
        return EVAL_PAGE;
    }

    /**
     * Release tag state.
     *
     */
    public void release() {
        id = null;
        properties = null;
        
        super.release();
    }
}
