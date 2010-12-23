package org.hyperic.hq.api.representation;

import java.util.Map;

import org.hyperic.hq.inventory.domain.Config;

public class ConfigRep {
	private Integer id;
	private Map<String, Object> values;
	
	public ConfigRep() {}
	
	public ConfigRep(Config config) {
		id = config.getId();
		values = config.getValues();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
	}
}

