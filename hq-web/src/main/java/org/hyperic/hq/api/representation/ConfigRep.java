package org.hyperic.hq.api.representation;

import java.util.Map;

import org.hyperic.hq.inventory.domain.Config;

public class ConfigRep {
	
	private Map<String, Object> values;
	
	public ConfigRep() {}
	
	public ConfigRep(Config config) {
		values = config.getValues();
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public void setValues(Map<String, Object> values) {
		this.values = values;
	}
}

