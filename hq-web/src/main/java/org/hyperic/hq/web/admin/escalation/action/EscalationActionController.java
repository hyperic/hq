package org.hyperic.hq.web.admin.escalation.action;

import java.util.List;

import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.shared.ActionManager;
import org.hyperic.hq.web.admin.escalation.EscalationController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin/escalation/{escalationId}")
public class EscalationActionController extends EscalationController {
	private ActionManager actionManager;
	
	@Autowired
	public EscalationActionController(ActionManager actionManager, AuthzBoss authzBoss, EscalationManager escalationManager) {
		super(authzBoss, escalationManager);
		
		this.actionManager = actionManager;
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/actions")
	public ModelAndView getActions(@PathVariable Integer escalationId) {
		Escalation escalation = getEscalationManager().findById(escalationId);
		List<Action> actions = escalation.getActions();
		
		return new ModelAndView();
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/action")
	public ModelAndView createAction(@PathVariable Integer escalationId) {
		// getEscalationManager().addAction(arg0, arg1, arg2);
		return new ModelAndView();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/action/{actionId}")
	public ModelAndView getAction(@PathVariable Integer escalationId, @PathVariable Integer actionId) {
		// actionManager.findById();
		
		return new ModelAndView();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/action/{actionId}")
	public ModelAndView updateAction(@PathVariable Integer escalationId, @PathVariable Integer actionId) {
		// actionManager.updateAction(arg0);
		
		return new ModelAndView();
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/action/{actionId}")
	public ModelAndView deleteAction(@PathVariable Integer escalationId, @PathVariable Integer actionId) {
		// getEscalationManager().removeAction(arg0, arg1);
		return new ModelAndView();
	}
}