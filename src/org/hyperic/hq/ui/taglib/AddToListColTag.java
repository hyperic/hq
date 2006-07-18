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
 * Define a column for the "from" and "to" tables in the Add
 * To List widget.
 */
public class AddToListColTag extends TagSupport {

    //----------------------------------------------------instance variables

    private String key;
    private String name;

    //----------------------------------------------------constructors

    public AddToListColTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     *
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     *
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     */
    public int doEndTag() throws JspException {
        Class clazz = AddToListColsTag.class;
        AddToListColsTag ancestor =
            (AddToListColsTag) findAncestorWithClass(this, clazz);
        if (ancestor == null) {
            throw new JspException("no ancestor of class " + clazz);
        }

        ancestor.addCol(name, key);
        return EVAL_PAGE;
    }

    /**
     * Release tag state.
     *
     */
    public void release() {
        key = null;
        name = null;
        super.release();
    }
}
