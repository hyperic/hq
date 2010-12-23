package org.hyperic.hq.plugin.vsphere.domain;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ListOfResourcesResponse {
	private String message;
	private ListResponse data;
	
	public String getMessage() {
		return message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}

	public ListResponse getData() {
		return data;
	}

	public void setData(ListResponse data) {
		this.data = data;
	}
}