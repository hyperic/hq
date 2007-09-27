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

import java.util.ArrayList;
import java.util.Map;

import javax.servlet.jsp.JspException;

/**
 * Define the set of object values displayed in the "from" and "to"
 * tables in the Add To List widget. Each object is specified by a
 * nested <code>&lt;spider:addToListValue&gt;</code> tag.
 *
 * After this tag and its nested body are evaluated, a scoped
 * attribute will contain a <code>List</code> of <code>Map</code>
 * objects (each representing a spider object) with the properties
 * specified by <code>&lt;spider:addToListValueProperty&gt;</code>
 * tags nested inside the <code>&lt;spider:addToListValue&gt;</code>
 * tag.
 */
public class AddToListValuesTag extends VarSetterBaseTag {

    //----------------------------------------------------instance variables

    private ArrayList values;

    //----------------------------------------------------constructors

    public AddToListValuesTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     *
     */
    public void addValue(Map map) {
        values.add(map);
    }

    /**
     *
     */
    public int doStartTag() throws JspException {
        values = new ArrayList();
        return EVAL_BODY_INCLUDE;
    }

    /**
     *
     */
    public int doEndTag() throws JspException {
        setScopedVariable(values);
        return EVAL_PAGE;
    }

    /**
     * Release tag state.
     *
     */
    public void release() {
        values = null;
        super.release();
    }
}
