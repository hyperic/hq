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

package org.hyperic.hq.ui.action.template;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.BaseValidatorForm;

/**
 * A subclass of <code>BaseValidatorForm</code> representing the
 * <em>Control</em> form data
 *
 * Customize as you see fit. 
 *
 * @see org.hyperic.hq.ui.action.BaseValidatorForm
 */
public class ExampleForm extends BaseValidatorForm  {

    private String exampleProperty;

    public String getExampleProperty() {
        return this.exampleProperty;
    }

    public void setExampleProperty(String a) {
        this.exampleProperty = a;
    }

    /**
     * Resets all fields to values valid for validation.
     * Calls super.reset() to insure that parent classes'
     * fields are initialialized validly.
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.exampleProperty = null;
        super.reset(mapping, request);
    }

    /**
     * Validates the form's fields in a custom way.
     * 
     * XXX Delete this method if no custom validation outside of
     * validation.xml needs to be done.
     */
    public ActionErrors validate(ActionMapping mapping, 
                                 HttpServletRequest request) {
        
        if (!shouldValidate(mapping, request)) {
            return null;
        }
        
        ActionErrors errs = super.validate(mapping, request);        
        if (errs == null ) {
            errs = new ActionErrors();
        }
        
        // custom validation rules

        if (errs.size() == 0) {
            return null;
        }
        
        return errs;
    }
    
    public String toString() {
        StringBuffer buf = new StringBuffer();
        
        buf.append("exampleProperty= ").append(exampleProperty);
        
        return super.toString() + buf.toString();
    }
    
}
