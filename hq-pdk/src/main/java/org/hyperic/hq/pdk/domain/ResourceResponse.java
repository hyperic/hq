package org.hyperic.hq.pdk.domain;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResourceResponse {
	private String message;
	private Resource data;
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public Resource getData() {
		return data;
	}

	public void setData(Resource data) {
		this.data = data;
	}
}

