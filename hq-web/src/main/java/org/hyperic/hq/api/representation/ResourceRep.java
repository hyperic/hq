package org.hyperic.hq.api.representation;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.hyperic.hq.api.LinkHelper;
import org.hyperic.hq.inventory.domain.Config;
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
		
		
		if (resource.getType() != null) {
			type = new SimpleRep(new ResourceTypeRep(resource.getType()));
		}
		
		configs = new HashMap<String, Object>();
		for(Config config: resource.getConfigs()) {
		    configs.put(config.getType().getName(), new ConfigRep(config));
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

