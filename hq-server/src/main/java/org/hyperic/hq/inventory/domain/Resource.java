package org.hyperic.hq.inventory.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.reference.RelationshipDirection;

public class Resource implements IdentityAware, PersistenceAware<Resource>, RelationshipAware<Resource> {
	private Integer id;
    private String name;
    private ResourceType type;
    private Integer version;
    private Set<ResourceGroup> resourceGroups;
    private Map<String, Object> properties;
    private AuthzSubject owner;

    public Resource() {

    }

    // Identity Aware code 
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    // Persistence Aware code
    public Resource merge() {
    	return this;
    }

    public void persist() {
    	
    }

    public void remove() {
    	
    }

    // Custom code
    public void flush() {
    	
    }
    
    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public Integer getVersion() {
        return this.version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AuthzSubject getOwner() {
        return null; //owner;
    }

    public void setOwner(AuthzSubject owner) {
    	
    }

    public Config getMeasurementConfig() {
    	return new Config();
    }

    public void setMeasurementConfig(Config config) {
    	
    }

    public Config getControlConfig() {
    	return new Config();
    }

    public void setControlConfig(Config config) {

    }

    public Config getProductConfig() {
    	return new Config();
    }

    public void setProductConfig(Config config) {
    	
    }

    public Config getAutoInventoryConfig() {
    	return new Config();
    }

    public void setAutoInventoryConfig(Config config) {
    	
    }

    public Config getResponseTimeConfig() {
    	return new Config();
    }

    public void setResponseTimeConfig(Config config) {
    	
    }

	public Set<ResourceGroup> getResourceGroups() {
        return resourceGroups;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
    	this.properties = properties;
    }
    
    public Object getProperty(String key) {
    	return properties.get(key);
    }

    public Object setProperty(String key, Object value) {
    	return new Object();
    }

    public void removeProperties() {
    	properties.clear();
    }

    // Relationship Aware code
    public Set<Relationship<Resource>> getRelationships(Resource entity, String name, RelationshipDirection direction) {
		// TODO Auto-generated method stub
    	System.out.println("Getting relationship with resource[" + entity + "] with name[" + name + "] with direction[" + direction.toString() + "]");
    	
		return new HashSet<Relationship<Resource>>();
	}

	public boolean isRelated(Resource entity, String name, RelationshipDirection direction) {
		// TODO Auto-generated method stub
		
		return false;
	}

	public Relationship<Resource> relate(Resource entity, String name, RelationshipDirection direction) {
		// TODO Auto-generated method stub
		System.out.println("Create relationship with resource[" + entity + "] with name[" + name + "] with direction[" + direction.toString() + "]");
		
		return new Relationship<Resource>();
	}

	public void removeRelationships(Resource entity, String name, RelationshipDirection direction) {
		// TODO Auto-generated method stub
		System.out.println("Remove relationships with resource[" + entity + "] with name[" + name + "] with direction[" + direction.toString() + "]");
	}

    public Set<Relationship<Resource>> getRelationships() {
    	return getRelationships(null, null, RelationshipDirection.ALL);
    }

    public Set<Relationship<Resource>> getRelationshipsFrom(String relationName) {
    	return getRelationships(null, relationName, RelationshipDirection.OUTGOING);
    }

    public Set<Relationship<Resource>> getRelationshipsTo(String relationName) {
    	return getRelationships(null, relationName, RelationshipDirection.INCOMING);
    }
    
    public Set<Resource> getResourcesFrom(String relationName) {
    	return new HashSet<Resource>();
    }

    public Set<Resource> getResourcesTo(String relationName) {
    	return new HashSet<Resource>();
    }

    public Relationship<Resource> getRelationshipTo(Resource resource, String relationName) {
    	Set<Relationship<Resource>> relations = getRelationships(resource, relationName, RelationshipDirection.OUTGOING);
    	
    	return (relations.iterator().hasNext()) ? relations.iterator().next() : null;
    }

    public boolean isRelatedTo(Resource resource, String relationName) {
        return isRelated(resource, relationName, RelationshipDirection.OUTGOING);
    }

    public Relationship<Resource> relateTo(Resource resource, String relationName) {
    	return relate(resource, relationName, RelationshipDirection.OUTGOING);
    }

    public void removeRelationship(Resource resource, String relationName) {
    	removeRelationships(resource, relationName, RelationshipDirection.ALL);
    }

    public void removeRelationships() {
    	removeRelationships(null, null, RelationshipDirection.ALL);
    }

    public void removeRelationships(String relationName) {
    	removeRelationships(null, relationName, RelationshipDirection.ALL);
    }

    public static int count() {
    	return 0;
    }
    
    public static List<Resource> findAllResources() {
    	return new ArrayList<Resource>();
    }

    public static Resource findById(Integer id) {
    	return new Resource();
    }

    public static Resource findResourceByName(String name) {
    	return new Resource();
    }

    public static List<Resource> find(Integer firstResult, Integer maxResults) {
    	return new ArrayList<Resource>();
    }

    public static Resource findResourcePrototypeByName(String name) {
        return null;
    }

    public static Resource findRootResource() {
        return null;
    }

    public boolean isOwner(Integer subjectId) {
        // TODO some overlord checking, then check owner's ID
        return true;
    }

    public static List<Resource> findResourcesOfPrototype(Resource proto, PageInfo pInfo) {
        // TODO get rid of this
        return null;
    }

    public static List<Resource> findAppdefPrototypes() {
        // TODO get rid of this
        return null;
    }

    public static List<Resource> findAllAppdefPrototypes() {
        // TODO get rid of this
        return null;
    }

    public boolean isInAsyncDeleteState() {
        // TODO get rid of this
        return false;
    }

    public static Collection<Resource> findByOwner(AuthzSubject owner) {
        // TODO best way to implement cutting across to AuthzSubject
        return null;
    }

    public Resource getPrototype() {
        // TODO remove
        return null;
    }

    public void setPrototype(Resource resource) {
        // TODO remove
    }

    public Integer getInstanceId() {
        // TODO remove this
        return id;
    }

    public void setInstanceId(Integer instanceId) {
        // TODO remove this
    }

    public static Resource findByInstanceId(Integer typeId, Integer instanceId) {
        // TODO remove this
        return Resource.findById(instanceId);
    }

    public String getSortName() {
        // TODO remove
        return null;
    }

    public void setSortName(String sortName) {
        // TODO remove
    }

    public static Collection findSvcRes_orderName(Boolean fSystem) {
        // TODO remove
        return null;
    }

    public void setConfigValidationError(String error) {
        // TODO from ConfigResponseDB. remove?
    }

    public void setConfigUserManaged(boolean userManaged) {
        // TODO from ConfigResponseDB. remove?
    }

    public boolean isConfigUserManaged() {
        // TODO from ConfigResponseDB. remove?
        return true;
    }
}