package org.hyperic.hq.web;

public class InputRepresentation {
	private String type;
	private Boolean required;
	private String name;
	private String defaultValue;
	
	public InputRepresentation(String type, Boolean required, String name, String defaultValue) {
		this.type = type;
		this.required = required;
		this.name = name;
		this.defaultValue = defaultValue;
	}

	public String getType() {
		return type;
	}

	public Boolean getRequired() {
		return required;
	}

	public String getName() {
		return name;
	}
	
	public String getDefaultValue() {
		return defaultValue;
	}
}