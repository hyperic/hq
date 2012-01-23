/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
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

package org.hyperic.hq.escalation.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.ehcache.CacheManager;

import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.context.IntegrationTestSpringJUnit4ClassRunner;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationManagerImpl;
import org.hyperic.hq.escalation.server.session.EscalationRuntime;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.escalation.server.session.EscalationStateDAO;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.ClassicEscalatableCreator;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test of the {@link EscalationManagerImpl}
 * @author trader
 * 
 */
@RunWith(IntegrationTestSpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = IntegrationTestContextLoader.class, locations = { "classpath*:META-INF/spring/*-context.xml" })
@Transactional
@DirtiesContext

public class EscalationManagerTest  {

	@Autowired
	private EscalationManager eManager;
	
	@Autowired
	private AuthzSubjectManager authzSubjectManager;
	
	@Autowired
	private AlertManager aManager;
	
	@Autowired
	private AlertDefinitionManager aDefManager;
	
	@Autowired
	private EscalationStateDAO stateDAO;
	
	@Autowired
	private EscalationRuntime runtime;
	
	@Autowired
	private PlatformManager platformManager;
	
	@Autowired
	private ServerManager serverManager;
	
	@Autowired
	private ServiceManager serviceManager;
	
	@Autowired
	private AgentManager agentManager;

	@Autowired
	private ResourceGroupManager resourceGroupManager;

	@Autowired
	private RoleManager roleManager;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private Messenger messenger;

	private ResourceGroup resGrp;

	private AlertDefinition testPlatformAlertDef;

	private AlertDefinition testServerAlertDef;

	private AlertDefinition testServiceAlertDef;

	private final List<Role> testRoles = new ArrayList<Role>();

	private final List<AuthzSubject> testUsers = new ArrayList<AuthzSubject>();

	private Platform testPlatform;
	
    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        // Setup Agent
        String agentToken = "agentToken123";
        agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        this.testPlatform = createResources(agentToken);
        this.resGrp = createResourceGroup(testPlatform);
        createRoles(new Integer[] { resGrp.getId() });
        createResourceAlertDefs(testPlatform);
        // createResourceAlerts();
        // Manual flush is required in any method in which you are updating the
        // Hibernate session in
        // order to avoid false positive in test
        sessionFactory.getCurrentSession().flush();
    }
    
    @After
    public void tearDown() {
        sessionFactory.evictQueries();
        //Clear the 2nd level cache including regions with queries
        CacheManager.getInstance().clearAll();
    }

	@Test
	public void testCRUDOperations() {
		
		try {
		
			String name = "TestEscalation1";
			String description = "Test Escalation1";
			boolean pauseAllowed = true;
			long maxPauseTime = 20000;
			boolean notifyAll = false;
			boolean repeat = true;
			
			// Create/read...
			Escalation e = eManager.createEscalation(name, description, pauseAllowed, maxPauseTime, notifyAll, repeat);
			assertNotNull("Unexpected null result", e);
			assertEquals("Invalid return value from getName()", e.getName(), name);
			assertEquals("Invalid return value from getDescription()", e.getDescription(), description);
			assertEquals("Invalid return value from isPauseAllowed()", e.isPauseAllowed(), pauseAllowed);
			assertEquals("Invalid return value from getMaxWaitTime()", e.getMaxPauseTime(), maxPauseTime);
			assertEquals("Invalid return value from isNotifyAll()", e.isNotifyAll(), notifyAll);
			assertEquals("Invalid return value from isRepeat()", e.isRepeat(), repeat);
			
			name = "Modified TestEscalation1";
			description = "Modified Test Escalation1";
			pauseAllowed = !pauseAllowed;
			maxPauseTime += 100;
			notifyAll = !notifyAll;
			repeat = !repeat;
			
			AuthzSubject subj = authzSubjectManager.getOverlordPojo();
			
			// Update/read...
			eManager.updateEscalation(subj, e, name, description, pauseAllowed, maxPauseTime, notifyAll, repeat);
			assertEquals("Invalid return value from getName()", e.getName(), name);
			assertEquals("Invalid return value from getDescription()", e.getDescription(), description);
			assertEquals("Invalid return value from isPauseAllowed()", e.isPauseAllowed(), pauseAllowed);
			assertEquals("Invalid return value from getMaxWaitTime()", e.getMaxPauseTime(), maxPauseTime);
			assertEquals("Invalid return value from isNotifyAll()", e.isNotifyAll(), notifyAll);
			assertEquals("Invalid return value from isRepeat()", e.isRepeat(), repeat);
			
			eManager.deleteEscalation(subj, e);
			
		} catch (Exception e) {

			e.printStackTrace();
			fail("Exception during test");

		}
	}
	
	@Test
	public void testFindOperations() {
		
		try {
			
			String name = "TestEscalation2";
			String description = "Test Escalation2";
			boolean pauseAllowed = true;
			long maxPauseTime = 20000;
			boolean notifyAll = false;
			boolean repeat = true;

			// Create/read...
			Escalation origEsc = eManager.createEscalation(name, description, pauseAllowed, maxPauseTime, notifyAll, repeat);
			Integer escId = origEsc.getId();
			
			Escalation foundEsc1 = eManager.findById(escId);
			assertNotNull("Expected non-null escalation", foundEsc1);
			assertEquals("Espected escalations to be equal", origEsc, foundEsc1);
			
			AuthzSubject subj = authzSubjectManager.getOverlordPojo();			
			Escalation foundEsc2 = eManager.findById(subj, escId);
			assertNotNull("Expected non-null escalation", foundEsc2);
			assertEquals("Espected escalations to be equal", origEsc, foundEsc2);
			assertEquals("Espected escalations to be equal", foundEsc1, foundEsc2);
			
			Escalation foundEsc3 = eManager.findByName(name);
			assertNotNull("Expected non-null escalation", foundEsc3);
			assertEquals("Espected escalations to be equal", origEsc, foundEsc3);
			assertEquals("Espected escalations to be equal", foundEsc1, foundEsc3);
			assertEquals("Espected escalations to be equal", foundEsc2, foundEsc3);
			
			Escalation foundEsc4 = eManager.findByName(subj, name);
			assertNotNull("Expected non-null escalation", foundEsc4);
			assertEquals("Espected escalations to be equal", origEsc, foundEsc4);
			assertEquals("Espected escalations to be equal", foundEsc1, foundEsc4);
			assertEquals("Espected escalations to be equal", foundEsc2, foundEsc4);
			assertEquals("Espected escalations to be equal", foundEsc3, foundEsc4);
			
			Collection<Escalation> all = eManager.findAll(subj);
			assertNotNull("Expected non-null collection from findAll()", all);
			assertFalse("Expected non-empty collection from findAll()", all.size() ==  0);
			
			boolean found = false;
			for (Escalation esc : all) {
				if (esc.equals(origEsc)) {
					found = true;
				}
			}
			
			assertTrue("Escalation not returned from findAll()", found);
			
		} catch (Exception e) {

			e.printStackTrace();
			fail("Exception during test");

		}
	}
	
	@Test
	public void testStartStopEscalation() {

		try {
			TriggerFiredEvent[] triggerFiredEvents = new TriggerFiredEvent[1];
			triggerFiredEvents[0] = new TriggerFiredEvent(testPlatform.getId(),
					new MockEvent(1001, testPlatform.getId()));
			AlertConditionsSatisfiedZEvent ev =
				new AlertConditionsSatisfiedZEvent(testPlatformAlertDef.getId(), triggerFiredEvents);
			ClassicEscalatableCreator creator =
				new ClassicEscalatableCreator(testPlatformAlertDef, ev, messenger, aManager);
			Escalation e = eManager.createEscalation("TestEscalation",
					"Test Escalation 3", true, 1000, false, true);
			eManager.setEscalation(ClassicEscalationAlertType.CLASSIC, testPlatformAlertDef.getId(), e);

			assertTrue(eManager.startEscalation(testPlatformAlertDef, creator));
			eManager.endEscalation(testPlatformAlertDef);
			
		} catch (Exception e) {

			e.printStackTrace();
			fail("Exception during test");

		}
	}
	
	@Test
	public void testAckAndFixAlert() {

		try {
			
			AlertDefinition def = createAlertDefinition(testPlatform.getId(),
		            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, "Platform Down");
			aDefManager.updateAlertDefinitionActiveStatus(authzSubjectManager.getOverlordPojo(), def, true);
			
	        AlertFiredEvent event = new AlertFiredEvent(123, def.getId(),
	            def.getAppdefEntityId(), "Platform Down", System.currentTimeMillis(),
	            "Firing Alert-123");
	        TriggerFiredEvent triggerFired = new TriggerFiredEvent(15, event);
	        AlertConditionsSatisfiedZEvent alertZEvent = new AlertConditionsSatisfiedZEvent(
	            def.getId(), new TriggerFiredEvent[] { triggerFired });
			ClassicEscalatableCreator creator =
				new ClassicEscalatableCreator(def, alertZEvent, messenger, aManager);
			Escalation e = eManager.createEscalation("TestEscalation",
					"Test Escalation 3", true, 1000, false, true);
			eManager.setEscalation(ClassicEscalationAlertType.CLASSIC, def.getId(), e);
	        aManager.fireAlert(alertZEvent);

	        List<EscalationState> escalationStates = eManager.getActiveEscalations(10);
	        assertNotNull("Expected non-null list of EscalationStates", escalationStates);
	        assertEquals("Expected exactly one EscalationState object in list", 1, escalationStates.size());
	        
	        Alert a = aManager.findLastUnfixedByDefinition(authzSubjectManager.getOverlordPojo(), def.getId());
	        assertNotNull("Alert should not be null", a);
	        assertEquals(a.getDefinition().getId(), def.getId());
	        assertTrue(eManager.isAlertAcknowledgeable(a.getId(), def));
	        eManager.acknowledgeAlert(authzSubjectManager.getOverlordPojo(), ClassicEscalationAlertType.CLASSIC,
	        		a.getId(), "more info", 10);
	        // ACK'ed alert should still be unfixed
	        a = aManager.findLastUnfixedByDefinition(authzSubjectManager.getOverlordPojo(), def.getId());
	        assertNotNull("Acknowledged alert should not be null", a);
	        assertTrue("Expected true return value from fixAlert",
	        		eManager.fixAlert(authzSubjectManager.getOverlordPojo(), def, ""));
	        
		} catch (Exception e) {

			e.printStackTrace();
			fail("Exception during test");

		}
	}
	
	private Platform createPlatform(String agentToken, String platformType, String fqdn)
	throws ApplicationException {
		AIPlatformValue aiPlatform = new AIPlatformValue();
		aiPlatform.setCpuCount(2);
		aiPlatform.setPlatformTypeName(platformType);
		aiPlatform.setAgentToken(agentToken);
		aiPlatform.setFqdn(fqdn);
		return platformManager.createPlatform(authzSubjectManager.getOverlordPojo(), aiPlatform);
	}

	private Server createServer(Platform platform, ServerType serverType)
	throws PlatformNotFoundException, AppdefDuplicateNameException, ValidationException,
	PermissionException, NotFoundException {
		ServerValue server = new ServerValue();
		return serverManager.createServer(authzSubjectManager.getOverlordPojo(), platform.getId(),
				serverType.getId(), server);
	}

	private ServerType createServerType(String serverName, String serverVersion,
			String[] validPlatformTypes, String plugin)
	throws NotFoundException {
		ServerTypeInfo serverTypeInfo = new ServerTypeInfo();
		serverTypeInfo.setDescription(serverName);
		serverTypeInfo.setName(serverName);
		serverTypeInfo.setVersion(serverVersion);
		serverTypeInfo.setVirtual(false);
		serverTypeInfo.setValidPlatformTypes(validPlatformTypes);
		return serverManager.createServerType(serverTypeInfo, plugin);
	}

	private ServiceType createServiceType(String serviceTypeName, String plugin,
			ServerType serverType) throws NotFoundException {
		ServiceTypeInfo sinfo = new ServiceTypeInfo();
		sinfo.setDescription(serviceTypeName);
		sinfo.setInternal(false);
		sinfo.setName(serviceTypeName);
		return serviceManager.createServiceType(sinfo, plugin, serverType);
	}

	private AlertDefinition createAlertDefinition(Integer appdefId, Integer appdefType,
			String alertDefName) throws AlertDefinitionCreateException {
		AlertDefinitionValue alertDefValue = new AlertDefinitionValue();
		alertDefValue.setName(alertDefName);
		alertDefValue.setAppdefId(appdefId);
		alertDefValue.setAppdefType(appdefType);
		AlertDefinitionValue createdAlertDef = aDefManager.createAlertDefinition(alertDefValue);
		return aDefManager.getByIdNoCheck(createdAlertDef.getId());
	}

	private Platform createResources(String agentToken) throws ApplicationException,
	NotFoundException {
		// Create PlatformType
		String platformType = "Linux";
		platformManager.createPlatformType(platformType, "Test Plugin");
		// Create test platform
		Platform testPlatform = createPlatform(agentToken, platformType, "leela.local");
		// Create ServerType
		ServerType testServerType = createServerType("Tomcat", "6.0", new String[] { "Linux" },
		"Test Server Plugin");
		// Create test server
		Server testServer = createServer(testPlatform, testServerType);
		// Create ServiceType
		ServiceType serviceType = createServiceType("Spring JDBC Template", "Test Server Plugin",
				testServerType);
		// Create test service
		serviceManager.createService(authzSubjectManager.getOverlordPojo(),
				testServer.getId(), serviceType.getId(), "leela.local jdbcTemplate",
				"Spring JDBC Template", "my computer");
		return testPlatform;
	}
	
    private void createRoles(Integer[] resGrpId) throws ApplicationException, PermissionException {

        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        // ReadOnly = view only
        AuthzSubject readUser = authzSubjectManager.createSubject(overlord, "readOnly", true, "",
            "", "test@test.com", "first", "last", "", "", true);
        this.testUsers.add(readUser);
        AuthzSubject readNWrite = authzSubjectManager.createSubject(overlord, "readNWrite", true,
            "", "", "test@test.com", "first", "last", "", "", true);
        this.testUsers.add(readNWrite);
        AuthzSubject fullUser = authzSubjectManager.createSubject(overlord, "fullUser", true, "",
            "", "test@test.com", "first", "last", "", "", true);
        this.testUsers.add(fullUser);
        AuthzSubject noPermUser = authzSubjectManager.createSubject(overlord, "noPermUser", true,
            "", "", "test@test.com", "first", "last", "", "", true);
        this.testUsers.add(noPermUser);

        Integer readRoleId = createReadOnlyRole(overlord, readUser);
        Integer fullRoleId = createFullAccessRole(overlord, fullUser);
        Integer noPermRoleId = createNoPermissionRole(overlord, noPermUser);
        roleManager.addResourceGroups(overlord, readRoleId, resGrpId);
        roleManager.addResourceGroups(overlord, fullRoleId, resGrpId);
        roleManager.addResourceGroups(overlord, noPermRoleId, resGrpId);
        this.testRoles.add(roleManager.getRoleById(readRoleId));
        this.testRoles.add(roleManager.getRoleById(fullRoleId));
        this.testRoles.add(roleManager.getRoleById(noPermRoleId));
    }

    private ResourceGroup createResourceGroup(Platform testPlatform) throws ApplicationException,
        PermissionException {

        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        Server testServer = testPlatform.getServers().iterator().next();

        Resource platformRes = testPlatform.getResource();
        Resource serverRes = testServer.getResource();
        Resource serviceRes = testServer.getServices().iterator().next().getResource();

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(platformRes);
        resources.add(serverRes);
        resources.add(serviceRes);
        ResourceGroupCreateInfo gCInfo = new ResourceGroupCreateInfo("AllResourcesGroup", "",
            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_GRP, null, "", 0, false, false);
        ResourceGroup resGrp = resourceGroupManager.createResourceGroup(overlord, gCInfo,
           new ArrayList<Role>(0), resources);
        return resGrp;
    }

    private void createResourceAlertDefs(Platform testPlatform) throws AlertDefinitionCreateException {
        // Create Platform Alert Definition
        this.testPlatformAlertDef = createAlertDefinition(testPlatform.getId(),
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, "Platform Down");
        // Create Server Alert Definition
        Server testServer = testPlatform.getServers().iterator().next();
        Integer serverAppDefId = testServer.getId();
        this.testServerAlertDef = createAlertDefinition(serverAppDefId,
            AppdefEntityConstants.APPDEF_TYPE_SERVER, "Server Down");
        // Create Service Alert Definition
        Integer serviceAppDefId = testServer.getServices().iterator().next().getId();
        this.testServiceAlertDef = createAlertDefinition(serviceAppDefId,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE, "Service Down");
    }

    private Integer createReadOnlyRole(AuthzSubject overlord, AuthzSubject viewOnlySubject)
        throws ApplicationException, PermissionException {
        Integer[] viewSubjects = { viewOnlySubject.getId() };
        // Create Roles
        RoleValue rValue = new RoleValue();
        rValue.setName("readOnly");
        String[] viewOperations = { AuthzConstants.platformOpViewPlatform,
                                   AuthzConstants.serverOpViewServer,
                                   AuthzConstants.serviceOpViewService };
        List<Operation> viewOnlyOps = getMappedOperations(viewOperations);
        Operation[] ops = viewOnlyOps.toArray(new Operation[] {});
        return roleManager.createOwnedRole(overlord, rValue, ops, viewSubjects, null);

    }

    private Integer createFullAccessRole(AuthzSubject overlord, AuthzSubject fullAccessSubject)
        throws ApplicationException, PermissionException {
        Integer[] fullAccessSubjects = { fullAccessSubject.getId() };
        // Create Roles
        RoleValue rValue = new RoleValue();
        rValue.setName("fullAccess");
        List<Operation> fullAccessOps = getAllOperations();
        Operation[] ops = fullAccessOps.toArray(new Operation[] {});
        return roleManager.createOwnedRole(overlord, rValue, ops, fullAccessSubjects, null);

    }

    private Integer createNoPermissionRole(AuthzSubject overlord, AuthzSubject noPermSubject)
        throws ApplicationException, PermissionException {
        Integer[] noPermSubjects = { noPermSubject.getId() };
        // Create Roles
        RoleValue rValue = new RoleValue();
        rValue.setName("NoPermission");
        return roleManager.createOwnedRole(overlord, rValue, new Operation[] {}, noPermSubjects,
            null);
    }

    private List<Operation> getMappedOperations(String[] operationNames) {
        List<Operation> operations = Bootstrap.getBean(OperationDAO.class).findAll();
        List<Operation> mappedOps = new ArrayList<Operation>();
        for (Operation op : operations) {
            for (String operName : operationNames) {
                if (operName.equalsIgnoreCase(op.getName())) {
                    mappedOps.add(op);
                }
            }
        }
        return mappedOps;
    }

    private List<Operation> getAllOperations() {
        return Bootstrap.getBean(OperationDAO.class).findAll();

    }
}
