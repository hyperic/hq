package org.hyperic.hq.pdk.domain;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceTypeResponse {
	private String message;
	private ResourceType data;
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public ResourceType getData() {
		return data;
	}

	public void setData(ResourceType data) {
		this.data = data;
	}
}