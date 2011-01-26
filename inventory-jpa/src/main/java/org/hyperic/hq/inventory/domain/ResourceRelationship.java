package org.hyperic.hq.inventory.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceContext;
import javax.persistence.PrimaryKeyJoinColumn;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.beans.factory.annotation.Configurable;

@Configurable
@Entity
@IdClass(ResourceRelationshipId.class)
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE)
public class ResourceRelationship  {
    
    @OneToMany(cascade=CascadeType.REMOVE)
    private Set<ResourceRelationshipProperty> properties= new HashSet<ResourceRelationshipProperty>();
	
    @ManyToOne
    @PrimaryKeyJoinColumn(name="FROM", referencedColumnName="id")
	private Resource from;
	
    @ManyToOne
    @PrimaryKeyJoinColumn(name="TO", referencedColumnName="id")
	private Resource to;
	
    @Id
    private String name;
    
    @Id
    private int toId;
    
    @Id
    private int fromId;
    
    @PersistenceContext
    transient EntityManager entityManager;
   
    
	public ResourceRelationship() {
	}
	
	public String getName() {
		return this.name;
	}

	public Resource getFrom() {
		return from;
	}

	public Resource getTo() {
		return to;
	}
	
	//TODO really need to provide and specify to/from ids separately?
    public int getToId() {
        return toId;
    }

    public void setToId(int toId) {
        this.toId = toId;
    }

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public void setFrom(Resource from) {
        this.from = from;
    }

    public void setTo(Resource to) {
        this.to = to;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String,Object> getProperties() {
        Map<String,Object> props = new HashMap<String,Object>();
        
        for(ResourceRelationshipProperty property: properties) {
            props.put(property.getName(), property.getValue());
        }
        
        return props;
    }
     
    public Object getProperty(String key) {
        //TODO model default values?  See above
        //TODO neo4j impl thows RuntimeException.  Make consistent
        for(ResourceRelationshipProperty property: properties) {
            if(property.getName().equals(key)) {
                return property.getValue().toString();
            }
        }
        return null;
    }
    
    public void setProperty(String key, Object value) {
        for(ResourceRelationshipProperty property: properties) {
            if(property.getName().equals(key)) {
                property.setValue((String)value);
                property.merge();
                return;
            }
        }
        ResourceRelationshipProperty property =  new ResourceRelationshipProperty();
        property.setName(key);
        property.setValue(value.toString());
        entityManager.persist(property);
        properties.add(property);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("From: ").append(getFrom()).append(", ");
        sb.append("To: ").append(getTo()).append(", ");
        sb.append("Name: ").append(getName());
        return sb.toString();
    }
   
}