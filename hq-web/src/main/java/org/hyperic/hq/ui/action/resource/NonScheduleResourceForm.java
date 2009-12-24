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

/*
 * Created on May 15, 2003
 *
 */
package org.hyperic.hq.ui.action.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.ui.action.BaseValidatorForm;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;

/**
 * Resource form without a schedule attached to it.
 * 
 * 
 */
public class NonScheduleResourceForm
    extends BaseValidatorForm {

    // -------------------------------------instance variables
    private String name;
    private String description;
    private String location;
    private Integer rid;
    private Integer type;

    private Integer resourceType;
    private List resourceTypes;

    // -------------------------------------constructors

    // -------------------------------------public methods

    /**
     * Returns the name.
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * sets the name.
     * @return String
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the description.
     * @return String
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     * @param description The description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the location.
     * @return Integer
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location.
     * @param location The location to set
     */
    public void setLocation(String location) {
        this.location = location;
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
     * Returns the rid.
     * @return String
     */
    public Integer getRid() {
        return rid;
    }

    /**
     * Sets the rid.
     * @param rid The rid to set
     */
    public void setRid(Integer rid) {
        this.rid = rid;
    }

    /**
     * Returns the type.
     * @return String
     */
    public Integer getType() {
        return type;
    }

    /**
     * Sets the type.
     * @param type The type to set
     */
    public void setType(Integer type) {
        this.type = type;
    }

    /**
     * loads the server value
     * 
     * @param sValue
     */
    public void loadResourceValue(AppdefResourceValue sValue) {
        this.name = sValue.getName();
        this.description = sValue.getDescription();
        this.location = sValue.getLocation();
        this.rid = sValue.getId();
        this.type = new Integer(sValue.getEntityId().getType());
    }

    /**
     * loads the server value
     * 
     * @param sValue
     */
    public void updateResourceValue(AppdefResourceValue rValue) {
        if (name != null)
            rValue.setName(name);
        if (description != null)
            rValue.setDescription(description);
        if (location != null)
            rValue.setLocation(location);
    }

    /**
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        name = null;
        description = null;
        location = null;
        rid = null;
        resourceType = null;
        resourceTypes = null;
    }

    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);

        if (errors == null) {
            errors = new ActionErrors();
        }

        if (errors.isEmpty()) {
            return null;
        }
        return errors;
    }

    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());

        s.append(" ");
        s.append("rid=" + rid + " ");
        s.append("type=" + type + " ");
        s.append("name=" + name + " ");
        s.append("location=" + location + " ");
        s.append("description=" + description + " ");

        return s.toString();
    }
}
