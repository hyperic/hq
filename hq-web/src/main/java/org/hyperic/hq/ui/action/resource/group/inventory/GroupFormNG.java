package org.hyperic.hq.ui.action.resource.group.inventory;

import java.util.LinkedHashMap;
import java.util.List;

import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.ui.action.resource.NonScheduleResourceFormNG;
import org.hyperic.hq.ui.util.BizappUtilsNG;
import org.hyperic.util.StringUtil;

public class GroupFormNG extends NonScheduleResourceFormNG {

    /**
     * contains the [appdef_type]:[appdef_resource_type] value
     */
    private String _typeAndResourceTypeId;
    private Integer _groupType;
    private List _platformTypes;
    private List _applicationTypes;
    private List _serverTypes;
    private List _serviceTypes;
    private LinkedHashMap<String, String> _groupTypes;
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
        _platformTypes = BizappUtilsNG.buildAppdefOptionList(platformTypes, true);
    }

    public void setApplicationTypes(List applicationTypes) throws InvalidAppdefTypeException {
        _applicationTypes = BizappUtilsNG.buildAppdefOptionList(applicationTypes, true);
    }

    public void setServerTypes(List serverTypes) throws InvalidAppdefTypeException {
        _serverTypes = BizappUtilsNG.buildAppdefOptionList(serverTypes, true);
    }

    public void setServiceTypes(List serviceTypes) throws InvalidAppdefTypeException {
        _serviceTypes = BizappUtilsNG.buildAppdefOptionList(serviceTypes, true);
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

    public LinkedHashMap<String, String> getGroupTypes() {
        return _groupTypes;
    }

    public void setGroupTypes(LinkedHashMap<String, String> groupTypes) {
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


	
}
