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
public class GroupForm
    extends NonScheduleResourceForm {

    /**
     * contains the [appdef_type]:[appdef_resource_type] value
     */
    private String _typeAndResourceTypeId;
    private Integer _groupType;
    private List _platformTypes;
    private List _applicationTypes;
    private List _serverTypes;
    private List _serviceTypes;
    private List _groupTypes;
    private String[] _entityIds;
    private String _typeName;
    private boolean _privateGroup;

    public Integer getCompatibleCount() {
        if (_platformTypes == null || _serverTypes == null || _serviceTypes == null || _applicationTypes == null)
            return new Integer(0);

        return new Integer(_platformTypes.size() + _serverTypes.size() + _serviceTypes.size() +
                           _applicationTypes.size());
    }

    public Integer getClusterCount() {
        if (_serviceTypes == null) {
            return new Integer(0);
        }

        return new Integer(_serviceTypes.size());
    }

    public List getPlatformTypes() {
        return _platformTypes;
    }

    public Integer getPlatformTypeCount() {
        if (_platformTypes == null) {
            return new Integer(0);
        }

        return new Integer(_platformTypes.size());
    }

    public List getApplicationTypes() {
        return _applicationTypes;
    }

    public List getServerTypes() {
        return _serverTypes;
    }

    public Integer getServerTypeCount() {
        if (_serverTypes == null) {
            return new Integer(0);
        }

        return new Integer(_serverTypes.size());
    }

    public List getServiceTypes() {
        return _serviceTypes;
    }

    public Integer getServiceTypeCount() {
        if (_serviceTypes == null) {
            return new Integer(0);
        }

        return new Integer(_serviceTypes.size());
    }

    public void setPlatformTypes(List platformTypes) throws InvalidAppdefTypeException {
        _platformTypes = BizappUtils.buildAppdefOptionList(platformTypes, true);
    }

    public void setApplicationTypes(List applicationTypes) throws InvalidAppdefTypeException {
        _applicationTypes = BizappUtils.buildAppdefOptionList(applicationTypes, true);
    }

    public void setServerTypes(List serverTypes) throws InvalidAppdefTypeException {
        _serverTypes = BizappUtils.buildAppdefOptionList(serverTypes, true);
    }

    public void setServiceTypes(List serviceTypes) throws InvalidAppdefTypeException {
        _serviceTypes = BizappUtils.buildAppdefOptionList(serviceTypes, true);
    }

    public String getTypeAndResourceTypeId() {
        return _typeAndResourceTypeId;
    }

    /**
     * Returns the entity type id in [entity type id]:[resource type id]
     */
    public Integer getEntityTypeId() {
        if (_typeAndResourceTypeId.equals("-1")) {
            return new Integer("-1");
        }

        List typeList = StringUtil.explode(_typeAndResourceTypeId, ":");
        return new Integer((String) typeList.get(0));
    }

    /**
     * Returns resource type id in [entity type id]:[resource type id]
     */
    public Integer getResourceTypeId() {
        if (_typeAndResourceTypeId.equals("-1"))
            return new Integer("-1");

        List typeList = StringUtil.explode(_typeAndResourceTypeId, ":");
        return new Integer((String) typeList.get(1));
    }

    /**
     * Sets the typeAndResourceTypeId.
     */
    public void setTypeAndResourceTypeId(String typeAndResourceTypeId) {
        _typeAndResourceTypeId = typeAndResourceTypeId;
    }

    public Integer getGroupType() {
        return _groupType;
    }

    public void setGroupType(Integer groupType) {
        _groupType = groupType;
    }

    public List getGroupTypes() {
        return _groupTypes;
    }

    public void setGroupTypes(List groupTypes) {
        _groupTypes = groupTypes;
    }

    public String[] getEntityIds() {
        return _entityIds;
    }

    public void setEntityIds(String[] entityIds) {
        _entityIds = entityIds;
    }

    public String getTypeName() {
        return _typeName;
    }

    public void setTypeName(String typeName) {
        _typeName = typeName;
    }

    public boolean isPrivateGroup() {
        return _privateGroup;
    }

    public void setPrivateGroup(boolean privateGroup) {
        _privateGroup = privateGroup;
    }

    /**
     * Overide the validate method. need to do validation.
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        ActionErrors errors = super.validate(mapping, request);

        if (shouldValidate(mapping, request)) {
            if (errors == null) {
                errors = new ActionErrors();
            }

            if (_groupType.intValue() == Constants.APPDEF_TYPE_GROUP_ADHOC ||
                _groupType.intValue() == Constants.APPDEF_TYPE_GROUP_COMPAT) {
                if ((_typeAndResourceTypeId == null) || (_typeAndResourceTypeId.equals("-1")) )
                    errors.add("typeAndResourceTypeId", new ActionMessage("resource.group.inventory.error."
                                                                          + "ResourceTypeIsRequired"));
            }
        }
        return errors;
    }
}
