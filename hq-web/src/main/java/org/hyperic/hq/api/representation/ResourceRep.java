package org.hyperic.hq.api.representation;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hyperic.hq.api.LinkHelper;
import org.hyperic.hq.inventory.domain.Resource;
import org.springframework.util.Assert;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceRep implements SimpleRepresentation, LinkedRepresentation {
	private Integer id;
	private String name;
	private String description;
	private String location;
	private String modifiedBy;
	private Map<String, Object> properties;
	private Map<String, Object> configs;
	private AuthzSubjectRep owner;
	private AgentRep agent;
	private SimpleRep type;
	private Map<String, String> links = new HashMap<String, String>();
	
	public ResourceRep() {}
	
	public ResourceRep(Resource resource) {
		Assert.notNull(resource, "Resource argument can not be null");
		
		// Transfer domain object to representation...
		setId(resource.getId()); // Calling setter since it also updates the self link...
		
		name = resource.getName();
		description = resource.getDescription();
		location = resource.getLocation();
		modifiedBy = resource.getModifiedBy();
		properties = resource.getProperties();

		// Setup links...
		links.put(RELATIONSHIPS_LABEL, LinkHelper.getCollectionUri(RESOURCES_LABEL, id, RELATIONSHIPS_LABEL));
		
		// Wrap connected domain objects and linkage if applicable...
		if (resource.getAgent() != null) {
			agent = new AgentRep(resource.getAgent());
		}
		
		if (resource.getOwner() != null) {
			owner = new AuthzSubjectRep(resource.getOwner());
		}
		
		if (resource.getType() != null) {
			type = new SimpleRep(new ResourceTypeRep(resource.getType()));
		}
		
		configs = new HashMap<String, Object>();
		
		if (resource.getAutoInventoryConfig() != null) {
			configs.put("autoinventory", new ConfigRep(resource.getAutoInventoryConfig()));
		}
		
		if (resource.getControlConfig() != null) {
			configs.put("control", new ConfigRep(resource.getControlConfig()));
		}
		
		if (resource.getMeasurementConfig() != null) {
			configs.put("measurement", new ConfigRep(resource.getMeasurementConfig()));
		}
		
		if (resource.getProductConfig() != null) {
			configs.put("product", new ConfigRep(resource.getProductConfig()));
		}
	}
	
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
		
		// Set the self link as well...
		links.put(SELF_LABEL, LinkHelper.getInstanceByIdUri(RESOURCES_LABEL, id));
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
	}

	public AuthzSubjectRep getOwner() {
		return owner;
	}

	public void setOwner(AuthzSubjectRep owner) {
		this.owner = owner;
	}

	public AgentRep getAgent() {
		return agent;
	}

	public void setAgent(AgentRep agent) {
		this.agent = agent;
	}

	public SimpleRep getType() {
		return type;
	}

	public void setType(SimpleRep type) {
		this.type = type;
	}

	public Map<String, Object> getConfigs() {
		return configs;
	}

	public void setConfigs(Map<String, Object> configs) {
		this.configs = configs;
	}

	public Map<String, String> getLinks() {
		return links;
	}
}

