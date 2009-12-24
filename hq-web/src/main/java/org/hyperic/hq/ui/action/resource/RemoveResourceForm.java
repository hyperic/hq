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

package org.hyperic.hq.ui.action.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;

/**
 * removes a list of resources
 * 
 * 
 */
public class RemoveResourceForm
    extends ResourceForm {

    // -------------------------------------instance variables

    private Integer[] resources;
    protected Integer resourceType;
    protected List resourceTypes;

    public RemoveResourceForm() {
        super();
    }

    // -------------------------------------public methods

    public Integer getF() {
        return getResourceType();
    }

    public void setF(Integer f) {
        setResourceType(f);
    }

    public Integer[] getR() {
        return getResources();
    }

    public void setR(Integer[] r) {
        setResources(r);
    }

    /**
     * Getter for property users.
     * @return Value of property users.
     * 
     */
    public Integer[] getResources() {
        return resources;
    }

    /**
     * Setter for property users.
     * @param users New value of property users.
     * 
     */
    public void setResources(Integer[] resources) {
        this.resources = resources;
    }

    /**
     * Returns the resourceType.
     * @return Integer
     */
    public Integer getResourceType() {
        return resourceType;
    }

    /**
     * Sets the resourceType.
     * @param resourceType The resourceType to set
     */
    public void setResourceType(Integer resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * Returns the resourceTypes.
     * @return List
     */
    public List getResourceTypes() {
        return resourceTypes;
    }

    /**
     * Sets the resourceTypes.
     * @param resourceTypes The resourceTypes to set
     */
    public void setResourceTypes(List resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public Integer getPss() {
        return getPs();
    }

    public void setPss(Integer pageSize) {
        setPs(pageSize);
    }

    /**
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        resources = new Integer[0];
        resourceType = null;
        resourceTypes = null;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" rid=");
        s.append(" type=");
        s.append(" resources=");
        s.append(resources);
        s.append(" resourceType=");
        s.append(resourceType);
        s.append(" resourceTypes=");
        s.append(resourceTypes);

        return s.toString();
    }
}
