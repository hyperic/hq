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

package org.hyperic.hq.web.resource.association;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthzBoss;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.web.BaseControllerTest;
import org.hyperic.hq.web.resource.association.GroupAssociationController;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;

public class GroupAssociationControllerTest extends BaseControllerTest {
	private GroupAssociationController controller;
	private AppdefBoss mockAppdefBoss;
	private AuthzBoss mockAuthzBoss;
	private HttpSession mockSession;

	@Before
	public void setUp() {
		super.setUp();

		mockAppdefBoss = getMockAppdefBoss();
		mockAuthzBoss = getMockAuthzBoss();
		mockSession = getMockSession();
		controller = new GroupAssociationController(mockAppdefBoss,
				mockAuthzBoss);
	}

	@Test
	public void testGetAvailableAssociations() throws PermissionException,
			SessionException {
		// ...setup inputs and outputs...
		Map<String, Object> inputOutput = constructInputStringArrayAndOutputAppdefEntityIdArrayMap(10);
		
		// ...setup our great expectations...
		expect(
				mockAppdefBoss.findAllGroupsMemberExclusive(eq(SESSION_ID
						.intValue()), isA(PageControl.class),
						isA(AppdefEntityID[].class))).andReturn((PageList<AppdefGroupValue>) inputOutput.get("output"));

		// ...replay the expectations, so we can run the test...
		replay(mockAppdefBoss);

		// ...now test it...
		Map<String, List<Map<String, Object>>> result = controller
				.getAvailableAssociations(
						(String[]) inputOutput.get("input"), mockSession);
		
		// ...verify our expectations...
		verify(mockAppdefBoss);

		// ...inspect the result, and make any assertions...
		assertTrue("Result should not be empty", !result.isEmpty());
		assertTrue("Result should contain a key called 'groups'", result
				.containsKey("groups"));
	}

	@Test
	public void testCreateAssociationMoreThanOneGroupId() throws SessionException, PermissionException, VetoException {
		// ...setup inputs and outputs...
		String[] resourceAppdefEntityIds = constructStringArrayOfAppdefEntityIds(10);
		Integer[] groupIds = constructIntegerArrayOfResourceIds(5);
		
		// Test the case where there's more than one group id specified...
		String result = testCreateAssociation(resourceAppdefEntityIds, groupIds);
		
		// ...inspect the result, and make any assertions...
		assertTrue("Result should not be empty", !result.isEmpty());
		assertEquals("redirect:/app/resource/associations", result);
	}


	@Test
	public void testCreateAssociationOnlyOneGroupId() throws SessionException, PermissionException, VetoException {
		// ...setup inputs and outputs...
		String[] resourceAppdefEntityIds = constructStringArrayOfAppdefEntityIds(10);
		Integer[] groupIds = constructIntegerArrayOfResourceIds(1);
		
		// Test the case where there's only one group id specified...
		String result = testCreateAssociation(resourceAppdefEntityIds, groupIds);
		
		// ...inspect the result, and make any assertions...
		assertTrue("Result should not be empty", !result.isEmpty());
		assertEquals("redirect:/app/resource/association/" + groupIds[0], result);
	}

	@Test
	public void testGetAssociation() {
		// TODO For now this is a stub method, once we have a proper
		// implementation, this test case will need to be updated...
		Map<String, String> result = controller.getAssociation();

		assertTrue("Result should be empty", result.isEmpty());
	}

	private String testCreateAssociation(String[] resourceAppdefEntityIds, Integer[] groupIds) throws SessionException, PermissionException, VetoException {
		// ...setup our great expectations...
		mockAppdefBoss.batchGroupAdd(eq(SESSION_ID.intValue()),
						isA(AppdefEntityID.class), isA(Integer[].class));
		expectLastCall().times(resourceAppdefEntityIds.length);
		
		// ...replay the expectations, so we can run the test...
		replay(mockAppdefBoss);

		// ...now test it...
		String result = controller.createAssociation(groupIds,
				resourceAppdefEntityIds, mockSession);

		// ...verify our expectations...
		verify(mockAppdefBoss);
		
		return result;
	}
	
	// ...Helper function that sets up the input and output...
	private Map<String, Object> constructInputStringArrayAndOutputAppdefEntityIdArrayMap(int size) {
		String[] input = constructStringArrayOfAppdefEntityIds(size);
		PageList<AppdefGroupValue> output = new PageList<AppdefGroupValue>();
		
		for (String value : input) {
			AppdefEntityID id = new AppdefEntityID(value);
			AppdefGroupValue group = new AppdefGroupValue();
			
			group.setId(id.getId());
			group.setName("Name_" + value);
			group.setDescription("Description_" + value);
			output.add(group);
		}
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		result.put("input", input);
		result.put("output", output);
		
		return result;
	}
}