package org.hyperic.hq.plugin.vsphere.domain;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Agent {
	private Integer id;
	private String address;
	private Integer port;
	private String authToken;
	private String agentToken;
	private String agentVersion;
	private Boolean unidirectional;

	public Agent() {}
	
	public Agent(Integer id, String address, Integer port, String authToken, String agentToken, String agentVersion, Boolean unidirectional) {
		this.id = id;
		this.address = address;
		this.port = port;
		this.authToken = authToken;
		this.agentToken = agentToken;
		this.agentVersion = agentVersion;
		this.unidirectional = unidirectional;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getAuthToken() {
		return authToken;
	}

	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}

	public String getAgentToken() {
		return agentToken;
	}

	public void setAgentToken(String agentToken) {
		this.agentToken = agentToken;
	}

	public String getAgentVersion() {
		return agentVersion;
	}

	public void setAgentVersion(String agentVersion) {
		this.agentVersion = agentVersion;
	}

	public Boolean getUnidirectional() {
		return unidirectional;
	}

	public void setUnidirectional(Boolean unidirectional) {
		this.unidirectional = unidirectional;
	}
}

