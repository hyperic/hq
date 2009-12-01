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

package org.hyperic.hq.ui.action.portlet.addcontent;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

// XXX: remove when ImageBeanButton works
import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.action.portlet.DashboardBaseForm;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience
 * methods for dealing with image-based form buttons.
 */
public class PropertiesForm extends DashboardBaseForm {
    
    /** Holds value of property wide. */
    private boolean wide;
    
    /** Holds value of property portlet. */
    private String portlet;
    
    //-------------------------------------instance variables

    //-------------------------------------constructors

    public PropertiesForm() {
        super();
    }

    //-------------------------------------public methods


    public String toString() {
        StringBuffer s = new StringBuffer();
        return s.toString();
    }
    
    /** Getter for property wide.
     * @return Value of property wide.
     *
     */
    public boolean isWide() {
        return this.wide;
    }
    
    /** Setter for property wide.
     * @param wide New value of property wide.
     *
     */
    public void setWide(boolean wide) {
        this.wide = wide;
    }
    
    /** Getter for property portlet.
     * @return Value of property portlet.
     *
     */
    public String getPortlet() {
        return this.portlet;
    }
    
    /** Setter for property portlet.
     * @param portlet New value of property portlet.
     *
     */
    public void setPortlet(String portlet) {
        this.portlet = portlet;
    }
    
}
