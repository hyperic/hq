/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.web.login;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpSession;

import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.web.BaseControllerTest;
import org.hyperic.hq.web.login.LoginController;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.web.servlet.ModelAndView;

public class LoginControllerTest extends BaseControllerTest {
	private AuthzSubjectManager mockAuthzSubjectManager;
	private LoginController loginController;
	
	@Before
	public void setUp() {
		mockAuthzSubjectManager = createMock(AuthzSubjectManager.class);
		loginController = new LoginController(mockAuthzSubjectManager);
	}
	
	@Test
	public void testLoginGuestDisabled() {
		// ...setup input and output...
		final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		final MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		final HttpSession mockSession = mockRequest.getSession(true);
		
		// ...setup our great expectations...
		expect(mockAuthzSubjectManager.getSubjectById(AuthzConstants.guestId)).andReturn(null);
		
		// ...replay those expectations...
		replay(mockAuthzSubjectManager);
						
		// ...make sure our security context holder is empty...
		SecurityContextHolder.getContext().setAuthentication(null);

		// ...test it...
		ModelAndView result = loginController.login(mockRequest, mockResponse, mockSession);
		
		// ...verify our expectations...
		verify(mockAuthzSubjectManager);
		
		// ...check the results...
		assertTrue("Result should not be empty", !result.isEmpty());
		assertTrue("Result should contain a guestUsername", result.getModel().containsKey("guestUsername"));
		assertTrue("Result should contain a guestEnabled", result.getModel().containsKey("guestEnabled"));
		assertEquals(result.getModel().get("guestUsername"), "guest");
		assertFalse("Guest enabled should be false", (Boolean) result.getModel().get("guestEnabled"));
	}
	
	@Test
	public void testLoginGuestEnabled() {
		// ...setup input and output...
		final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		final MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		final HttpSession mockSession = mockRequest.getSession(true);
		
		AuthzSubject subject = constructAuthzSubject();
		
		subject.setName("test_guest");

		// ...setup our great expectations...
		expect(mockAuthzSubjectManager.getSubjectById(AuthzConstants.guestId)).andReturn(subject);
		
		// ...replay those expectations...
		replay(mockAuthzSubjectManager);
				
		// ...make sure our security context holder is empty...
		SecurityContextHolder.getContext().setAuthentication(null);

		// ...test it...
		ModelAndView result = loginController.login(mockRequest, mockResponse, mockSession);
		
		// ...verify our expectations...
		verify(mockAuthzSubjectManager);
		
		// ...check the results...
		assertTrue("Result should not be empty", !result.isEmpty());
		assertTrue("Result should contain a guestUsername", result.getModel().containsKey("guestUsername"));
		assertTrue("Result should contain a guestEnabled", result.getModel().containsKey("guestEnabled"));
		assertEquals(result.getModel().get("guestUsername"), "test_guest");
		assertTrue("Guest enabled should be false", (Boolean) result.getModel().get("guestEnabled"));		
	}
	
	@Test
	public void testLoginWithError() {
		// ...setup input and output...
		final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		final MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		final HttpSession mockSession = mockRequest.getSession(true);
		
		// ...set error flag & exception...
		mockSession.setAttribute(AbstractAuthenticationProcessingFilter.SPRING_SECURITY_LAST_EXCEPTION_KEY, new BadCredentialsException("test.authentication.exception"));
		mockRequest.setParameter("authfailed", "1");
		
		// ...setup our great expectations...
		expect(mockAuthzSubjectManager.getSubjectById(AuthzConstants.guestId)).andReturn(null);
		
		// ...replay those expectations...
		replay(mockAuthzSubjectManager);
		
		// ...make sure our security context holder is empty...
		SecurityContextHolder.getContext().setAuthentication(null);
		
		// ...test it...
		ModelAndView result = loginController.login(mockRequest, mockResponse, mockSession);
		
		// ...verify our expectations...
		verify(mockAuthzSubjectManager);
		
		// ...check the results...
		assertTrue("Result should not be empty", !result.isEmpty());
		assertTrue("Result should contain a errorMessage", result.getModel().containsKey("errorMessage"));
		 // ...controller tries to retrieve from string file, which we don't have, hence the ???...???
		assertEquals(result.getModel().get("errorMessage"), "???test.authentication.exception???");
	}
	
	@Test
	public void testLoginWithAuthenticatedUser() {
		// ...setup input and output...
		final MockHttpServletRequest mockRequest = new MockHttpServletRequest();
		final MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		final HttpSession mockSession = mockRequest.getSession(true);
		
		// ...setup dummy Authentication object and put it into the SecurityContextHolder...
		Collection<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
		
		roles.add(new GrantedAuthorityImpl("ROLE_TEST_USER"));
		
		Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", "password", roles);
		
		SecurityContextHolder.getContext().setAuthentication(authentication);
		
		// ...test it...
		ModelAndView result = loginController.login(mockRequest, mockResponse, mockSession);
		
		// ...check the results...
		assertTrue("Result should be empty", result.isEmpty());
		assertEquals(mockResponse.getRedirectedUrl(), "/Dashboard.do");
	}	
}