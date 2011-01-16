package org.hyperic.hq.hqu.grails.helpers;

import java.util.List;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.context.Bootstrap;

public class AgentHelper {

	private AgentManager aMan = Bootstrap.getBean(AgentManager.class);
	
	public List<Agent> getAllAgents() {
		return aMan.getAgents();
	}
	
}
