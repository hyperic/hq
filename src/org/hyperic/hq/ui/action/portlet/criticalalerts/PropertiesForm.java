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

package org.hyperic.hq.ui.action.portlet.criticalalerts;

import org.hyperic.hq.ui.action.portlet.DashboardBaseForm;

/**
 * A subclass of <code>ValidatorForm</code> that adds convenience
 * methods for dealing with image-based form buttons.
 */
public class PropertiesForm extends DashboardBaseForm  {
    public final static String ALERT_NUMBER =
        ".dashContent.criticalalerts.numberOfAlerts";
    public final static String PAST = ".dashContent.criticalalerts.past";
    public final static String PRIORITY =
        ".dashContent.criticalalerts.priority";
    public final static String SELECTED_OR_ALL =
        ".dashContent.criticalalerts.selectedOrAll";
    
    /** Holds value of property numberOfAlerts. */
    private Integer numberOfAlerts;
    
    /** Holds value of property priority. */
    private String priority;
    
    /** Holds value of property past. */
    private long past;
    
    /** Holds value of property selectedOrAll. */
    private String selectedOrAll;
    
    /** Holds value of property key. */
    private String key;
    
    /** Holds value of property ids. */
    private String[] ids;
    
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
    
    /** Getter for property numberOfAlerts.
     * @return Value of property numberOfAlerts.
     *
     */
    public Integer getNumberOfAlerts() {
        return this.numberOfAlerts;
    }
    
    /** Setter for property numberOfAlerts.
     * @param numberOfAlerts New value of property numberOfAlerts.
     *
     */
    public void setNumberOfAlerts(Integer numberOfAlerts) {
        this.numberOfAlerts = numberOfAlerts;
    }
    
    /** Getter for property priority.
     * @return Value of property priority.
     *
     */
    public String getPriority() {
        return this.priority;
    }
    
    /** Setter for property priority.
     * @param priority New value of property priority.
     *
     */
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    /** Getter for property past.
     * @return Value of property past.
     *
     */
    public long getPast() {
        return this.past;
    }
    
    /** Setter for property past.
     * @param past New value of property past.
     *
     */
    public void setPast(long past) {
        this.past = past;
    }
    
    /** Getter for property selectedOrAll.
     * @return Value of property selectedOrAll.
     *
     */
    public String getSelectedOrAll() {
        return this.selectedOrAll;
    }
    
    /** Setter for property selectedOrAll.
     * @param selectedOrAll New value of property selectedOrAll.
     *
     */
    public void setSelectedOrAll(String selectedOrAll) {
        this.selectedOrAll = selectedOrAll;
    }
    
    /** Getter for property key.
     * @return Value of property key.
     *
     */
    public String getKey() {
        return this.key;
    }
    
    /** Setter for property key.
     * @param key New value of property key.
     *
     */
    public void setKey(String key) {
        this.key = key;
    }
    
    /** Getter for property ID.
     * @return Value of property ID.
     *
     */
    public String[] getIds() {
        return this.ids;
    }
    
    /** Setter for property ID.
     * @param ID New value of property ID.
     *
     */
    public void setIds(String[] ids) {
        this.ids = ids;
    }
    
}
