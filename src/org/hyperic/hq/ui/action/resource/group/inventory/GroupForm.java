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

import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.ui.Constants;
import org.hyperic.hq.ui.action.resource.NonScheduleResourceForm;
import org.hyperic.hq.ui.util.BizappUtils;
import org.hyperic.util.StringUtil;

import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 *
 */
public class GroupForm extends NonScheduleResourceForm {

    /**
     * contains the [appdef_type]:[appdef_resource_type] value
     */
    private String typeAndResourceTypeId;
    private Integer groupType;
    private List platformTypes;
    private List applicationTypes;
    private List serverTypes;
    private List serviceTypes;
    private List groupTypes;
    private String[] entityIds;
    private String typeName;

    /**
     * @return the number of items of compatible types
     */
    public Integer getCompatibleCount()
    {
        if (platformTypes == null || serverTypes == null ||
            serviceTypes == null || applicationTypes == null)
            return new Integer(0);
            
        return new Integer(platformTypes.size() + serverTypes.size() + 
               serviceTypes.size() + applicationTypes.size());
    }
    
    /**
     * @return the number of items of service types 
     */
    public Integer getClusterCount()
    {
        if (serviceTypes == null)
            return new Integer(0);
            
        return new Integer(serviceTypes.size());
    }
    
    /**
     * Returns the platformTypes.
     * @return List
     */
    public List getPlatformTypes() {
	return platformTypes;
    }
    
    /**
     * Returns the number of platformTypes.
     * @return List
     */
    public Integer getPlatformTypeCount() {
        if (platformTypes == null)
            return new Integer(0);
            
        return new Integer(platformTypes.size());
    }
    
    /**
     * Returns the platformTypes.
     * @return List
     */
    public List getApplicationTypes() {
        return applicationTypes;
    }
    
    /**
     * Returns the serverTypes.
     * @return List
     */
    public List getServerTypes() {
	return serverTypes;
    }
    
    /**
     * Returns the number of serverTypes.
     * @return Integer
     */
    public Integer getServerTypeCount() {
        if (serverTypes == null)
            return new Integer(0);
            
        return new Integer(serverTypes.size());
    }
    
    /**
     * Returns the serviceTypes.
     * @return List
     */
    public List getServiceTypes() {
	   return serviceTypes;
    }
    
    /**
     * Returns the serviceTypes.
     * @return List
     */
    public Integer getServiceTypeCount() {
        if (serviceTypes == null)
            return new Integer(0);
            
       return new Integer(serviceTypes.size());
    }
    
    /**
     * Sets the platformTypes.
     * @param platformTypes The platformTypes to set
     */
    public void setPlatformTypes(List platformTypes)
        throws InvalidAppdefTypeException 
    {
    	this.platformTypes = BizappUtils.buildAppdefOptionList(platformTypes, true);
    }
    
    /**
     * Sets the platformTypes.
     * @param platformTypes The platformTypes to set
     */
    public void setApplicationTypes(List applicationTypes)
        throws InvalidAppdefTypeException 
    {
        this.applicationTypes = BizappUtils.buildAppdefOptionList(applicationTypes, true);
    }
    
    /**
     * Sets the serverTypes.
     * @param serverTypes The serverTypes to set
     */
    public void setServerTypes(List serverTypes) 
        throws InvalidAppdefTypeException 
    {
	   this.serverTypes = BizappUtils.buildAppdefOptionList(serverTypes, true);
    }
    
    /**
     * Sets the serviceTypes.
     * @param serviceTypes The serviceTypes to set
     */
    public void setServiceTypes(List serviceTypes) 
        throws InvalidAppdefTypeException 
    {
	   this.serviceTypes = BizappUtils.buildAppdefOptionList(serviceTypes, true);
    }

    /**
     * Returns the typeAndResourceTypeId.
     * @return String
     */
    public String getTypeAndResourceTypeId() {
	return typeAndResourceTypeId;
    }

    /**
     * Returns the entity type id in [entity type id]:[resource type id]
     * .
     * @return String
     */
    public Integer getEntityTypeId()
    {
        if (typeAndResourceTypeId.equals("-1"))
            return new Integer("-1");
            
        List typeList = StringUtil.explode(typeAndResourceTypeId, ":");
        return new Integer((String)typeList.get(0));
    }

    /**
     * Returns resource type id in [entity type id]:[resource type id]
     * 
     * @return String
     */
    public Integer getResourceTypeId() {
        if (typeAndResourceTypeId.equals("-1"))
            return new Integer("-1");
            
        List typeList = StringUtil.explode(typeAndResourceTypeId, ":");
        return new Integer((String)typeList.get(1));
    }

    /**
     * Sets the typeAndResourceTypeId.
     * @param typeAndResourceTypeId The typeAndResourceTypeId to set
     */
    public void setTypeAndResourceTypeId(String typeAndResourceTypeId) {
	this.typeAndResourceTypeId = typeAndResourceTypeId;
    }
    
    /**
     * Returns the groupType.
     * @return Integer
     */
    public Integer getGroupType() {
	return groupType;
    }

    /**
     * Sets the groupType.
     * @param groupType The groupType to set
     */
    public void setGroupType(Integer groupType) {
	this.groupType = groupType;
    }

    /**
     * @return List
     */
    public List getGroupTypes() {
        return groupTypes;
    }

    /**
     * Sets the groupTypes.
     * @param groupTypes The groupTypes to set
     */
    public void setGroupTypes(List groupTypes) {
        this.groupTypes = groupTypes;
    }

    public String[] getEntityIds() {
        return entityIds;
    }
    
    public void setEntityIds(String[] entityIds) {
        this.entityIds = entityIds;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
    
    /** 
     * over-ride the validate method.  need to do validation.
     */
    public ActionErrors validate(ActionMapping mapping,
                                 HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);

        if (shouldValidate(mapping, request)) {
            if (errors == null) {
                errors = new ActionErrors();
            }
            
            if (  groupType.intValue() == Constants.APPDEF_TYPE_GROUP_ADHOC ||  
                 groupType.intValue()  == Constants.APPDEF_TYPE_GROUP_COMPAT ) { 
                 if ( typeAndResourceTypeId.equals("-1")   )
                    errors.add("typeAndResourceTypeId",
                        new ActionMessage("resource.group.inventory.error." +
                                          "ResourceTypeIsRequired"));
            }
        }
        return errors;
    }
    
}
