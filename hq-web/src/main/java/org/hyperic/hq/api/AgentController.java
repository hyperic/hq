package org.hyperic.hq.api;

import java.util.List;

import org.hyperic.hq.api.representation.AgentRep;
import org.hyperic.hq.api.representation.ListRep;
import org.hyperic.hq.api.representation.SuccessResponse;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/agents")
public class AgentController extends BaseController {
	private AgentDAO agentDao;
	
	@Autowired
	public AgentController(AgentDAO agentDao) {
		this.agentDao = agentDao;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public @ResponseBody SuccessResponse listAgents() {
		List<Agent> agents = agentDao.findAll();
		
		return new SuccessResponse(ListRep.createListRepFromAgents(agents));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{address}:{port}")
	public @ResponseBody SuccessResponse getAgentByIpAndPort(@PathVariable String address, @PathVariable Integer port) {
		Agent agent = agentDao.findByIpAndPort(address, port);
		
		return new SuccessResponse(new AgentRep(agent));
	}
}

