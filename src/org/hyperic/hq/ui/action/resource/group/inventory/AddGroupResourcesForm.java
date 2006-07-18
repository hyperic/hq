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

package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.ui.action.resource.NonScheduleResourceForm;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;


/**
 * A subclass of <code>ResourceForm</code> representing the
 * <em>AddGroupResources</em> form. The purpose of this form is
 * to add AppdefResourceValues to a AppdefGroupValue
 */
public class AddGroupResourcesForm extends NonScheduleResourceForm  {

    //-------------------------------------instance variables

    private String[] availableResources;
    private String[] pendingResources;
    private Integer psa;
    private Integer psp;
    private List availResourceTypes;
    private String filterBy;
    private String nameFilter;
    
    //-------------------------------------constructors

    public AddGroupResourcesForm() {
        super();
    }

    //-------------------------------------public methods

    public String[] getAvailableResource() {
	   return this.availableResources;
    }

    public String[] getAvailableResources() {
        return getAvailableResource();
    }

    public void setAvailableResource(String[] availableResource) {
        this.availableResources = availableResource;
    }

    public void setAvailableResources(String[] availableResources) {
        setAvailableResource(availableResources);
    }

    public String[] getPendingResource() {
	return this.pendingResources;
    }

    public String[] getPendingResources() {
        return getPendingResource();
    }

    public void setPendingResource(String[] pendingResource) {
        this.pendingResources = pendingResource;
    }

    public void setPendingResources(String[] pendingResources) {
        setPendingResource(pendingResources);
    }

    public Integer getPsa() {
        return this.psa;
    }

    public void setPsa(Integer ps) {
        this.psa = ps;
    }

    public Integer getPsp() {
        return this.psp;
    }

    public void setPsp(Integer ps) {
        this.psp = ps;
    }

    public void reset(ActionMapping mapping,
                      HttpServletRequest request) {
        this.availResourceTypes = null;
        this.availableResources = new String[0];
        this.pendingResources = new String[0];
        this.filterBy = null;
        this.psa = null;
        this.psp = null;
        super.reset(mapping, request);
    }

    // Checks super class's custom validation rules.
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
        StringBuffer s = new StringBuffer(super.toString());
        s.append("psa=" + psa + " ");
        s.append("psp=" + psp + " ");
        s.append("nameFilter=").append(nameFilter).append(" ");

        s.append("availableResources={");
        listToString(s, availableResources);
        s.append("} ");

        s.append("pendingResources={");
        listToString(s, pendingResources);
        s.append("}");

        return s.toString();
    }

    private void listToString(StringBuffer s, String[] l) {
        if (l != null) {
            for (int i=0; i<l.length; i++) {
                s.append(l[i]);
                if (i<l.length-1) {
                    s.append(", ");
                }
            }
        }
    }
    
    /**
     * @return List
     */
    public List getAvailResourceTypes() {
        return availResourceTypes;
    }

    /**
     * Sets the availResourceTypes.
     * @param availResourceTypes The availResourceTypes to set
     */
    public void setAvailResourceTypes(List availResourceTypes) {
        this.availResourceTypes = availResourceTypes;
    }

    /**
     * 
     * @return Integer
     */
    public String getFilterBy() {
        return filterBy;
    }

    /**
     * Sets the filter by param.
     * 
     * @param fs The fs to set
     */
    public void setFilterBy(String fs) {
        this.filterBy = fs;
    }
    
    public String getNameFilter() {
        return nameFilter;   
    }
    
    public void setNameFilter(String s) {
        this.nameFilter = s;
    }
}
