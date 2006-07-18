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

package org.hyperic.hq.ui.action.portlet.resourcehealth;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

import org.hyperic.hq.ui.util.RequestUtils;
import org.hyperic.hq.ui.action.portlet.DashboardBaseForm;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience
 * methods for dealing with image-based form buttons.
 */
public class PropertiesForm extends DashboardBaseForm  {
    
    /** Holds value of property availability. */
    private boolean availability;
    
    /** Holds value of property throughput. */
    private boolean throughput;
    
    /** Holds value of property performance. */
    private boolean performance;
    
    /** Holds value of property utilization. */
    private boolean utilization;
    
    /** Holds value of property ids. */
    private String[] ids;

    /** Holds value of property order. */
    private String order;

    public PropertiesForm() {
        super();
    }

    // Public methods

    public String toString() {
        StringBuffer s = new StringBuffer();
        return s.toString();
    }
    
    /** Getter for property availability.
     * @return Value of property availability.
     *
     */
    public boolean isAvailability() {
        return this.availability;
    }
    
    /** Setter for property availability.
     * @param availability New value of property availability.
     *
     */
    public void setAvailability(boolean availability) {
        this.availability = availability;
    }
    
    /** Getter for property throughput.
     * @return Value of property throughput.
     *
     */
    public boolean isThroughput() {
        return this.throughput;
    }
    
    /** Setter for property throughput.
     * @param throughput New value of property throughput.
     *
     */
    public void setThroughput(boolean throughput) {
        this.throughput = throughput;
    }
    
    /** Getter for property performance.
     * @return Value of property performance.
     *
     */
    public boolean isPerformance() {
        return this.performance;
    }
    
    /** Setter for property performance.
     * @param performance New value of property performance.
     *
     */
    public void setPerformance(boolean performance) {
        this.performance = performance;
    }
    
    /** Getter for property utilization.
     * @return Value of property utilization.
     *
     */
    public boolean isUtilization() {
        return this.utilization;
    }
    
    /** Setter for property utilization.
     * @param utilization New value of property utilization.
     *
     */
    public void setUtilization(boolean utilization) {
        this.utilization = utilization;
    }
    
    /** Getter for property id.
     * @return Value of property id.
     *
     */
    public String[] getIds() {
        return this.ids;
    }
    
    /** Setter for property id.
     * @param id New value of property id.
     *
     */
    public void setIds(String[] ids) {
        this.ids = ids;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }
}
