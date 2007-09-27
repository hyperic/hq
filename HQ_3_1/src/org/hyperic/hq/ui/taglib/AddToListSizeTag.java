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

import org.hyperic.hq.ui.Constants;

/**
 * Set the scoped variable as the value of
 * <code>Constants.ADDTOLIST_SIZE_DEFAULT</code>.
 */
public class AddToListSizeTag extends VarSetterBaseTag {

    //----------------------------------------------------instance variables

    public AddToListSizeTag() {
        super();
    }

    //----------------------------------------------------public methods

    /**
     * Process the tag, setting the value of the scoped variable as
     * the size of the Add To List widget.
     *
     * @exception JspException if there is an error processing the tag
     */
    public final int doStartTag() throws JspException {
        setScopedVariable(Constants.ADDTOLIST_SIZE_DEFAULT);
        return SKIP_BODY;
    }
}
