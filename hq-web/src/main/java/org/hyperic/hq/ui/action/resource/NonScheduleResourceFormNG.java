/**
 * Resource form without a schedule attached to it.
 * 
 * 
 */

package org.hyperic.hq.ui.action.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.ui.action.BaseValidatorFormNG;

public class NonScheduleResourceFormNG extends BaseValidatorFormNG {

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
