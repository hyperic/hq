package org.hyperic.hq.api.representation;

import org.hyperic.hq.appdef.Agent;

public class AgentRep {
	private Integer id;
	private String address;
	private Integer port;
	private String authToken;
	private String agentToken;
	private String agentVersion;
	private Boolean unidirectional;
	
	public AgentRep() {}
	
	public AgentRep(Agent agent) {
		id = agent.getId();
		address = agent.getAddress();
		port = agent.getPort();
		authToken = agent.getAuthToken();
		agentToken = agent.getAgentToken();
		agentVersion = agent.getAgentVersion();
		unidirectional = agent.isUnidirectional();
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