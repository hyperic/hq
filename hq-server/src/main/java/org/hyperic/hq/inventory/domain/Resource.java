package org.hyperic.hq.inventory.domain;

import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.authz.server.session.AuthzSubject;

public interface Resource {
    Config getControlConfig();

    void setControlConfig(Config config);

    Config getMeasurementConfig();

    void setMeasurementConfig(Config config);

    Config getProductConfig();

    void setProductConfig(Config config);

    Config getAutoInventoryConfig();

    boolean isConfigUserManaged();

    void setConfigUserManaged(boolean userManaged);

    void setConfigValidationError(String error);

    Integer getId();

    String getName();

    Agent getAgent();

    void setAgent(Agent agent);

    boolean isInAsyncDeleteState();

    ResourceType getType();

    String getDescription();

    void setDescription(String description);

    AuthzSubject getOwner();

    boolean isOwner(Integer subjectId);

    void setOwner(AuthzSubject owner);

    String getLocation();

    void setLocation(String location);

    String getModifiedBy();

    void setModifiedBy(String modifiedBy);

    Object getProperty(String key);

    Object setProperty(String key, Object value);

    Map<String, Object> getProperties();

    void removeProperties();

    Set<Resource> getResourcesFrom(String relationName);

    ResourceRelationship relateTo(Resource resource, String relationName);

    void removeRelationship(Resource resource, String relationName);

    Resource getResourceTo(String relationName);

    Set<Integer> getChildrenIds(boolean recursive);

    Set<Resource> getChildren(boolean recursive);

    boolean hasChild(Resource resource, boolean recursive);

    Resource merge();

    void remove();

}
