package org.hyperic.hq.inventory.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.reference.RelationshipDirection;

public class ResourceType implements IdentityAware, PersistenceAware<ResourceType>, RelationshipAware<ResourceType> {
	private Integer id;
    private String name;
    private Set<Resource> resources;
    private Set<OperationType> operationTypes = new HashSet<OperationType>();
    private Set<PropertyType> propertyTypes = new HashSet<PropertyType>();
    private Set<ConfigType> configTypes = new HashSet<ConfigType>();
    private Integer version;

    public ResourceType() {
    }
    
    public void flush() {
    }

    public Integer getId() {
        return this.id;
    }

    public String getName() {
        return name;
    }

    public PropertyType getPropertyType(String name) {
        for (PropertyType propertyType : propertyTypes) {
            if (name.equals(propertyType.getName())) {
                return propertyType;
            }
        }
        return null;
    }

    public OperationType getOperationType(String name) {
        for (OperationType operationType : operationTypes) {
            if (name.equals(operationType.getName())) {
                return operationType;
            }
        }
        return null;
    }

    public ConfigType getConfigType(String name) {
    	for (ConfigType configType : configTypes) {
    		if (name.equals(configType.getName())) {
    			return configType;
    		}
    	}
    	return null;
    }

	public Set<PropertyType> getPropertyTypes() {
        return propertyTypes;
    }

    public Set<OperationType> getOperationTypes() {
        return operationTypes;
    }
    
    public Set<ConfigType> getConfigTypes() {
		return configTypes;
	}
    
	public Set<Relationship<ResourceType>> getRelationships(
			ResourceType entity, String name, RelationshipDirection direction) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isRelated(ResourceType entity, String name,
			RelationshipDirection direction) {
		// TODO Auto-generated method stub
		return false;
	}

	public Relationship<ResourceType> relate(ResourceType entity, String name,
			RelationshipDirection direction) {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeRelationships(ResourceType entity, String name,
			RelationshipDirection direction) {
		// TODO Auto-generated method stub
		
	}

    public Set<Relationship<ResourceType>> getRelationships() {
    	return new HashSet<Relationship<ResourceType>>();
    }

    public Relationship<ResourceType> getRelationshipTo(ResourceType resourceType, String relationName) {
    	return new Relationship<ResourceType>();
    }
    
    public Set<Relationship<ResourceType>> getRelationshipsFrom(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public Set<Relationship<ResourceType>> getRelationshipsTo(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	public void removeRelationships() {
		// TODO Auto-generated method stub
		
	}

	public void removeRelationships(String name) {
		// TODO Auto-generated method stub
		
	}

    public Integer getVersion() {
        return this.version;
    }

    public boolean isRelatedTo(ResourceType resourceType, String relationName) {
        return false;
    }

    public ResourceType merge() {
    	return this;
    }

    public void persist() {
    }

    public Relationship<ResourceType> relateTo(ResourceType resourceType, String relationName) {
    	return null;
    }

    public void remove() {
    }

    public void removeRelationship(ResourceType resourceType, String relationName) {
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Set<Resource> getResources() {
        return resources;
    }
    
    public boolean hasResources() {
        return resources.size() > 0;
    }

    public static int count() {
    	return 0;
    }
    
    public static List<ResourceType> findAllResourceTypes() {
    	return new ArrayList<ResourceType>();
    }

    public static ResourceType findById(Integer id) {
    	return new ResourceType();
    }

    public static ResourceType findResourceTypeByName(String name) {
    	return new ResourceType();
    }

    public static List<ResourceType> find(Integer firstResult, Integer maxResults) {
    	return new ArrayList<ResourceType>();
    }

    public Set<ConfigType> getMeasurementConfigTypes() {
    	return new HashSet<ConfigType>();
    }

    public static ResourceType findTypeResourceType() {
        // TODO get rid of this
        return null;
    }

    public String getLocalizedName() {
        // TODO get rid of this
        return null;
    }

    public int getAppdefType() {
        // TODO get rid of this
        return 0;
    }
}