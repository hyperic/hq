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

package org.hyperic.hq.ui.action.resource.group.control;

import java.util.List;

import javax.servlet.http.HttpServletRequest;


import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.ui.action.resource.common.control.ControlForm;

/**
 * A subclass of <code>ControlForm</code> representing the
 * <em>EditUserProperties</em> form.
 *
 * @see org.hyperic.hq.ui.resource.common.control.ControlForm
 */
public final class GroupControlForm extends ControlForm  {

    public static final Boolean IN_PARALLEL = Boolean.TRUE;
    public static final Boolean IN_ORDER = Boolean.FALSE;
    
    //-------------------------------------instance variables
    /** Holds value of property inParallel. */
    private Boolean inParallel;
    
    /** Holds value of property order. */
    private Integer[] resourceOrdering;
    
    /** Holds value of property resourceOrderingOptions. */
    private List resourceOrderingOptions;
    
    //-------------------------------------constructors

    public GroupControlForm() {
    }

    //-------------------------------------public methods

    //-------- form methods-------------------------

    // for validation, please see web/WEB-INF/validation/validation.xml
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        inParallel = Boolean.TRUE;
        resourceOrdering = new Integer[0];
    }
    
    // Checks super class's custom validation rules. This
    // This is primarily for ResourceForm's custom validation.
    public ActionErrors validate(ActionMapping mapping, 
                                 HttpServletRequest request) {
        ActionErrors errs = super.validate(mapping, request);
        if (null == errs) {
                errs = new ActionErrors();
        }
        
        // custom validation
        
        if (errs.isEmpty()) {
            return null;
        }
        return errs;
    }
    
    public String toString() {
        StringBuffer s = new StringBuffer();
        s.append(super.toString());
        s.append("inParrallel=").append(inParallel);
        s.append("resourceOrdering=").append(resourceOrdering);
        return s.toString();
    }
    
    /** Getter for property parallel.
     * @return Value of property parallel.
     *
     */
    public Boolean getInParallel() {
        return this.inParallel;
    }
    
    /** Setter for property parallel.
     * @param parallel New value of property parallel.
     *
     */
    public void setInParallel(Boolean inParallel) {
        this.inParallel = inParallel;
    }
    
    /** Getter for property orde.
     * @return Value of property orde.
     *
     */
    public Integer[] getResourceOrdering() {
        return this.resourceOrdering;
    }
    
    /** Setter for property orde.
     * @param orde New value of property orde.
     *
     */
    public void setResourceOrdering(Integer[] order) {
        this.resourceOrdering = order;
    }
    
    /** Getter for property resourceOrderingOptions.
     * @return Value of property resourceOrderingOptions.
     *
     */
    public List getResourceOrderingOptions() {
        return this.resourceOrderingOptions;
    }
    
    /** Setter for property resourceOrderingOptions.
     * @param resourceOrderingOptions New value of property resourceOrderingOptions.
     *
     */
    public void setResourceOrderingOptions(List resourceOrderingOptions) {
        this.resourceOrderingOptions = resourceOrderingOptions;
    }
    
}
