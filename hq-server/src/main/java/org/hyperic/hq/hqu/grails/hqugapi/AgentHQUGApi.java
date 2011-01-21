package org.hyperic.hq.hqu.grails.hqugapi;

import java.util.List;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.context.Bootstrap;

/**
 * Middleware api to handle info for agents.
 */
public class AgentHQUGApi {

	private AgentManager aMan = Bootstrap.getBean(AgentManager.class);

	public AgentHQUGApi(){}
	
	/**
	 * Returns all Agents.
	 * 
	 * @return List of all agents.
	 */
	public List<Agent> getAllAgents() {
		return aMan.getAgents();
	}

}
