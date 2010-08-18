package org.hyperic.hq.web;

import static org.easymock.EasyMock.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.ui.WebUser;
import org.hyperic.hq.web.SessionParameterKeys;
import org.springframework.mock.web.MockHttpServletRequest;

public abstract class BaseControllerTest {
	protected final Integer SESSION_ID = 1;
	protected final Integer AUTHZ_SUBJECT_VALUE_ID = 1;

	private AppdefBoss mockAppdefBoss;
	private AuthzBoss mockAuthzBoss;
	private HttpServletRequest mockHttpServletRequest;
	private HttpSession mockSession;
	
	protected void setUp() {
		// Set up a mock authz subect value object...
		AuthzSubjectValue subject = new AuthzSubjectValue();

		subject.setId(AUTHZ_SUBJECT_VALUE_ID);

		// ...so we can create a web user object...
		WebUser webUser = new WebUser(subject);

		webUser.setSessionId(SESSION_ID);

		// ...and put it into the mock http session which we'll use
		// during testing...
		mockHttpServletRequest = new MockHttpServletRequest();
		mockSession = mockHttpServletRequest.getSession(true);
		
		mockSession.setAttribute(SessionParameterKeys.WEB_USER, webUser);

		// ...finally, set up the mock services for this controller...
		mockAppdefBoss = createMock(AppdefBoss.class);
		mockAuthzBoss = createMock(AuthzBoss.class);
	}

	protected String[] constructStringArrayOfAppdefEntityIds(int size) {
		String[] result = new String[size];

		for (int x = 0; x < size; x++) {
			result[x] = "1:" + (10000 + x);
		}

		return result;
	}

	protected Integer[] constructIntegerArrayOfResourceIds(int size) {
		Integer[] result = new Integer[size];

		for (int x = 0; x < size; x++) {
			result[x] = 10000 + x;
		}

		return result;
	}

	protected AuthzSubject constructAuthzSubject() {
		return new AuthzSubject(true, null, null, null, false, null, null,
				null, null, null, true);
	}

	protected AppdefBoss getMockAppdefBoss() {
		return mockAppdefBoss;
	}

	protected AuthzBoss getMockAuthzBoss() {
		return mockAuthzBoss;
	}

	protected HttpServletRequest getMockHttpServletRequest() {
		return mockHttpServletRequest;
	}

	protected HttpSession getMockSession() {
		return mockSession;
	}
}