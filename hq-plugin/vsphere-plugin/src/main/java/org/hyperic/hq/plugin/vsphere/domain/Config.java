package org.hyperic.hq.plugin.vsphere.domain;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Config {
	Integer id;
	Map<String, Object> values;
	
	public Config() {}
	
	public Config(Integer id, Map<String, Object> values) {
		this.id = id;
		this.values = values;
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