package org.hyperic.hq.web.admin.escalation;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.web.BaseControllerTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ModelAndView;

public class EscalationControllerTest extends BaseControllerTest {
	private EscalationController controller;
	private EscalationManager mockEscalationManager;
	
	@Before
	public void setUp() {
		mockEscalationManager = createMock(EscalationManager.class);
		
		controller = new EscalationController(getMockAuthzBoss(), mockEscalationManager);
	}
	
	@Test
	public void testCreateEscalation() {
		// ...setup input and output...
		final EscalationForm escalationForm = new EscalationForm();
		
		escalationForm.setDescription("test description");
		escalationForm.setEscalationName("test name");
		escalationForm.setId(10000);
		escalationForm.setMaxPauseTime(60000l);
		escalationForm.setNotifyAll(false);
		escalationForm.setPauseAllowed(true);
		escalationForm.setRepeat(false);
		
		controller.createEscalation(escalationForm);
	}
	
	@Test
	public void testDeleteEscalation() {
		final Integer escalationId = 10000;
		
		controller.deleteEscalation(escalationId, getMockSession());
	}
	
	@Test
	public void testGetEscalation() {
		// ...setup input and output...
		final Integer escalationId = 10000;
		final Escalation escalation = constructEscalation(escalationId, "Test_Escalation");
		
		// ...setup our great expectations...
		expect(mockEscalationManager.findById(escalationId)).andReturn(escalation);
		
		// ...replay those expectations...
		replay(mockEscalationManager);
		
		// ...test it...
		ModelAndView result = controller.getEscalation(escalationId);
		
		// ...verify our expectations...
		verify(mockEscalationManager);
		
		// ...check the result...
		assertTrue("Result should not be empty", !result.isEmpty());
		assertTrue("Result should contain an 'escalationForm' property", result.getModel().containsKey("escalationForm"));
		assertEquals(result.getViewName(), "admin/escalation");
	}
	
	@Test
	public void testGetNewEscalationForm() {
		// ...test it...
		ModelAndView result = controller.getNewEscalationForm();

		// ...check the result...
		assertTrue("Result should not be empty", !result.isEmpty());
		assertTrue("Result should contain an 'escalationForm' property", result.getModel().containsKey("escalationForm"));
		assertEquals(result.getViewName(), "admin/escalation/new");
	}
	
	@Test
	public void testListEscalations() throws PermissionException {
		// ...setup input and output...
		final List<Escalation> escalations = constructListOfEscalations(10);
		
		// ...setup our great expectations...
		expect(mockEscalationManager.findAll((AuthzSubject) isNull())).andReturn(escalations);
		
		// ...replay those expectations...
		replay(mockEscalationManager);
		
		// ...test it...
		ModelAndView result = controller.listEscalations();
		
		// ...verify our expectations...
		verify(mockEscalationManager);
		
		// ...check the result...
		assertTrue("Result should not be empty", !result.isEmpty());
		assertTrue("Result should contain an 'escalations' property", result.getModel().containsKey("escalations"));
		assertEquals(result.getViewName(), "admin/escalations");
		
		List<EscalationListUIBean> beans = (List<EscalationListUIBean>) result.getModel().get("escalations");
		
		assertTrue("Result should have 10 items", beans.size() == 10);
	}
	
	@Test
	public void testUpdateEscalation() {
		final Integer escalationId = 10000;
		final EscalationForm escalationForm = new EscalationForm();
		
		escalationForm.setDescription("test description");
		escalationForm.setEscalationName("test name");
		escalationForm.setId(escalationId);
		escalationForm.setMaxPauseTime(60000l);
		escalationForm.setNotifyAll(false);
		escalationForm.setPauseAllowed(true);
		escalationForm.setRepeat(false);
		
		controller.updateEscalation(escalationId, escalationForm, getMockSession());
	}
	
	protected Escalation constructEscalation(Integer id, String name) {
		Escalation escalation = new Escalation();
		
		escalation.setId(id);
		escalation.setName(name);
		
		return escalation;
	}
	
	protected List<Escalation> constructListOfEscalations(int size) {
		List<Escalation> result = new ArrayList<Escalation>(size);
	
		for (int x = 0; x < size; x++) {
			result.add(constructEscalation(10000 + x, "Test_Escalation_" + x));
		}
		
		return result;
	}
}