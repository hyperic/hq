package org.hyperic.hq.web.admin.escalation;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.web.BaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * This class is the controller for Escalation Scheme management.
 * 
 * @author Yen-Ju "Annie" Chen
 * @author David Crutchfield
 * 
 */
@Controller
@RequestMapping("/admin")
public class EscalationController extends BaseController {
	private final static Log log = LogFactory.getLog(EscalationController.class
			.getName());
	private final static String BASE_ESCALATION_URL = "/app/admin/escalation";

	private EscalationManager escalationManager;

	@Autowired
	public EscalationController(AuthzBoss authzBoss,
			EscalationManager escalationManager) {
		super(null, authzBoss);

		this.escalationManager = escalationManager;
	}

	/**
	 * List all the escalation, if there's no escalation: will have empty
	 * "escalations" in model if exception happens when trying to get
	 * escalations, the escalations won't be in model Maps to
	 * admin/escalations.jsp
	 */
	@RequestMapping(value = "/escalations", method = RequestMethod.GET)
	public ModelAndView listEscalations() {
		ModelAndView result = new ModelAndView();
		List<EscalationListUIBean> escalations;

		try {
			// TODO The subject param that escalationManager.findAll(null) take
			// is for security concern.
			// However, there is no authorization check for "findAll." In
			// addition, we want to get
			// rid of this authorization, use Spring Security instead.
			List<Escalation> allEscalations = (List<Escalation>) escalationManager
					.findAll(null);

			escalations = new ArrayList<EscalationListUIBean>(allEscalations
					.size());

			for (Escalation escalation : allEscalations) {
				escalations.add(new EscalationListUIBean(escalation));
			}
		} catch (PermissionException e) {
			escalations = new ArrayList<EscalationListUIBean>();
		}

		result.addObject("escalations", escalations);
		result.setViewName("admin/escalations");

		return result;
	}

	/**
	 * Populates any default values for creating a new escalation.
	 * 
	 * @param model
	 */
	@RequestMapping(value = "/escalation", method = RequestMethod.GET)
	public ModelAndView getNewEscalationForm() {
		ModelAndView result = new ModelAndView();

		result.addObject("escalationForm", new EscalationForm());
		result.setViewName("admin/escalation/new");

		return result;
	}

	/**
	 * Security: should have authorization checking
	 * 
	 * @param escalationForm
	 * @return
	 */
	@RequestMapping(value = "/escalation", method = RequestMethod.POST)
	public ModelAndView createEscalation(EscalationForm escalationForm) {
		ModelAndView result = new ModelAndView();

		try {
			// TODO: shouldn't save MaxPauseTime if "isPauseAllowed" is false
			Escalation esc = escalationManager.createEscalation(escalationForm
					.getEscalationName(), escalationForm.getDescription(),
					escalationForm.isPauseAllowed(), escalationForm
							.getMaxPauseTime(), escalationForm.isNotifyAll(),
					escalationForm.isRepeat());
			RedirectView redirectView = new RedirectView(BASE_ESCALATION_URL
					+ "/" + esc.getId());

			result.setView(redirectView);

			return result;
		} catch (DuplicateObjectException e) {
			log.debug("An escalation of the name, '"
					+ escalationForm.getEscalationName() + "', already exists",
					e);
		}

		result = getNewEscalationForm();

		result
				.addObject(
						"errorMsg",
						"Already has an escalation has the same name. Please change the escalation name.");

		return result;
	}

	@RequestMapping(value = "/escalation/{escalationId}", method = RequestMethod.GET)
	public ModelAndView getEscalation(@PathVariable int escalationId) {
		ModelAndView result = new ModelAndView();

		Escalation escalation = escalationManager.findById(escalationId);

		if (escalation != null) {
			EscalationForm escalationForm = new EscalationForm(escalation);

			result.addObject("escalationForm", escalationForm);
			result.setViewName("admin/escalation");

			return result;
		}

		return listEscalations();
	}

	@RequestMapping(value = "/escalation/{escalationId}", method = RequestMethod.PUT)
	public ModelAndView updateEscalation(@PathVariable int escalationId,
			EscalationForm escalationForm, HttpSession session) {
		ModelAndView result = new ModelAndView();

		try {
			AuthzSubject subject = getAuthzSubject(session);
			Escalation escalation = escalationManager.findById(escalationId);

			if (escalation != null) {
				escalationManager.updateEscalation(subject, escalation,
						escalationForm.getEscalationName(), escalationForm
								.getDescription(), escalationForm
								.isPauseAllowed(), escalationForm
								.getMaxPauseTime(), escalationForm
								.isNotifyAll(), escalationForm.isRepeat());
			}

			RedirectView redirectView = new RedirectView(BASE_ESCALATION_URL
					+ "/" + escalationId);

			result.setView(redirectView);

			return result;
		} catch (SessionNotFoundException e) {
			log.debug("User's session not found", e);
		} catch (SessionTimeoutException e) {
			log.debug("Users's session has timed out", e);
		} catch (PermissionException e) {
			log.debug(
					"User does not have permission to perform this operation.",
					e);
		} catch (DuplicateObjectException e) {
			log.debug("An escalation of the name, '"
					+ escalationForm.getEscalationName() + "', already exists",
					e);
		}

		result = getEscalation(escalationId);

		result.addObject("errorMsg", "Cannot update escalation");

		return result;
	}

	/**
	 * Delete the escalation
	 * 
	 * @throws ServletException
	 *             from getSessionId
	 * 
	 *             Security: authorization checked in escalationManager, using
	 *             "alertPermissionManager.canRemoveEscalation(subject.getId());"
	 */
	@RequestMapping(value = "/escalation/{escalationId}", method = RequestMethod.DELETE)
	public ModelAndView deleteEscalation(@PathVariable int escalationId,
			HttpSession session) {
		ModelAndView result = new ModelAndView();

		try {
			AuthzSubject subject = getAuthzSubject(session);
			Escalation escalation = escalationManager.findById(escalationId);

			escalationManager.deleteEscalation(subject, escalation);

			RedirectView redirectView = new RedirectView(BASE_ESCALATION_URL
					+ "s");

			result.setView(redirectView);

			return result;
		} catch (ApplicationException e) {
			log.debug("An application error occurred", e);
		}

		result = getEscalation(escalationId);

		result.addObject("errorMsg",
				"admin.config.error.escalation.CannotDelete");

		return result;
	}

	protected EscalationManager getEscalationManager() {
		return escalationManager;
	}
}