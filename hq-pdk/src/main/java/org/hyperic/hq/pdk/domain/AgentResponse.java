package org.hyperic.hq.pdk.domain;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentResponse {
	private String message;
	private Agent data;
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public Agent getData() {
		return data;
	}

	public void setData(Agent data) {
		this.data = data;
	}
}