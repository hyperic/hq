package org.hyperic.hq.events.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

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
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.context.TestContextLoader;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test of the {@link AlertManagerImpl}
 * @author jhickey
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = TestContextLoader.class, locations = { "classpath*:META-INF/spring/*-context.xml" })
@Transactional
@DirtiesContext
public class AlertManagerTest {

    @Autowired
    private AlertManager alertManager;

    @Autowired
    private AlertDefinitionManager alertDefinitionManager;

    @Autowired
    private PlatformManager platformManager;

    @Autowired
    private AuthzSubjectManager authzSubjectManager;

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private AgentManager agentManager;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ServerManager serverManager;

    @Autowired
    private ServiceManager serviceManager;

    private AlertDefinition testPlatformAlertDef;

    private AlertDefinition testServerAlertDef;

    private AlertDefinition testServiceAlertDef;

    private Alert testPlatformAlert;

    private Alert testServerAlert;

    private Alert testServiceAlert;

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
                                                  String alertDefName) {
        AlertDefinitionValue alertDefValue = new AlertDefinitionValue();
        alertDefValue.setName(alertDefName);
        alertDefValue.setAppdefId(appdefId);
        alertDefValue.setAppdefType(appdefType);
        AlertDefinitionValue createdAlertDef = alertDefinitionManager
            .createAlertDefinition(alertDefValue);
        return alertDefinitionManager.getByIdNoCheck(createdAlertDef.getId());
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
        serviceManager.createService(authzSubjectManager.getOverlordPojo(), testServer.getId(),
            serviceType.getId(), "leela.local jdbcTemplate", "Spring JDBC Template", "my computer");
        return testPlatform;
    }

    // TODO: Create Resource Groups
    private void createResourceGroups() {

    }

    private void createSubjectsAndRoles() throws ApplicationException, PermissionException {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        // Full permission includes create, delete, view & modify
        AuthzSubject platformFull = authzSubjectManager.createSubject(overlord,
            "platformFullPermission", true, "", "", "test@test.com", "first", "last", "", "", true);
        AuthzSubject serverFull = authzSubjectManager.createSubject(overlord,
            "serverFullPermission", true, "", "", "test@test.com", "first", "last", "", "", true);
        AuthzSubject serviceFull = authzSubjectManager.createSubject(overlord,
            "serviceFullPermission", true, "", "", "test@test.com", "first", "last", "", "", true);

        // ReadOnly = view only
        AuthzSubject platformReadOnly = authzSubjectManager.createSubject(overlord,
            "platformReadOnly", true, "", "", "test@test.com", "first", "last", "", "", true);
        AuthzSubject serverReadOnly = authzSubjectManager.createSubject(overlord, "serverReadOnly",
            true, "", "", "test@test.com", "first", "last", "", "", true);
        AuthzSubject serviceReadOnly = authzSubjectManager.createSubject(overlord,
            "serviceReadOnly", true, "", "", "test@test.com", "first", "last", "", "", true);

        // ReadWrite is view+modify
        AuthzSubject platformReadWrite = authzSubjectManager.createSubject(overlord,
            "platformReadWrite", true, "", "", "test@test.com", "first", "last", "", "", true);
        AuthzSubject serverReadWrite = authzSubjectManager.createSubject(overlord,
            "serverReadWrite", true, "", "", "test@test.com", "first", "last", "", "", true);
        AuthzSubject serviceReadWrite = authzSubjectManager.createSubject(overlord,
            "serviceReadWrite", true, "", "", "test@test.com", "first", "last", "", "", true);

        // No permission
        AuthzSubject platformNone = authzSubjectManager.createSubject(overlord,
            "platformNoPermission", true, "", "", "test@test.com", "first", "last", "", "", true);
        AuthzSubject serverNone = authzSubjectManager.createSubject(overlord, "serverNoPermission",
            true, "", "", "test@test.com", "first", "last", "", "", true);
        AuthzSubject serviceNone = authzSubjectManager.createSubject(overlord,
            "serviceNoPermission", true, "", "", "test@test.com", "first", "last", "", "", true);

    }

    private void createResourceAlertDefs(Platform testPlatform) {
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

    private void createResourceAlerts() {
        this.testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, System
            .currentTimeMillis());
        this.testServerAlert = alertManager.createAlert(this.testServerAlertDef, System
            .currentTimeMillis());
        this.testServiceAlert = alertManager.createAlert(this.testServiceAlertDef, System
            .currentTimeMillis());
    }

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        // Setup Agent
        String agentToken = "agentToken123";
        agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        Platform testPlatform = createResources(agentToken);
        createResourceGroups();
        createSubjectsAndRoles();
        createResourceAlertDefs(testPlatform);
        createResourceAlerts();
        // Manual flush is required in any method in which you are updating the
        // Hibernate session in
        // order to avoid false positive in test
        sessionFactory.getCurrentSession().flush();
    }

    @Test
    public void testFindAlertById() {
        Alert alert = alertManager.findAlertById(testPlatformAlert.getId());
        assertNotNull(alert);
        assertFalse(alert.isAckable());
    }

    @Test
    public void testSetAlertFixed() {
        alertManager.setAlertFixed(testPlatformAlert);
        // flush the update
        sessionFactory.getCurrentSession().flush();
        // retrieve the alert and check if it was fixed
        assertTrue(alertManager.findAlertById(testPlatformAlert.getId()).isFixed());
    }

    @Test
    public void testDeleteAlerts() {
        alertManager.deleteAlerts(new Integer[] { testPlatformAlert.getId(),
                                                 testServerAlert.getId(),
                                                 testServiceAlert.getId() });
        // The underlying DAO uses HQL to do bulk delete. This will NOT update
        // the session cache, so a subsequent query will make it seem as though
        // alert is still there. We have to explicitly remove it from cache.
        sessionFactory.getCurrentSession().clear();
        // verify alert cannot be loaded from DB
        assertNull(alertManager.getById(testPlatformAlert.getId()));
        assertNull(alertManager.getById(testServerAlert.getId()));
        assertNull(alertManager.getById(testServiceAlert.getId()));
    }

   
    @Test
    public void testDeleteAlertDefAlerts() throws PermissionException {
        alertManager.deleteAlerts(authzSubjectManager.getOverlordPojo(), testPlatformAlertDef);
        alertManager.deleteAlerts(authzSubjectManager.getOverlordPojo(), testServerAlertDef);
        alertManager.deleteAlerts(authzSubjectManager.getOverlordPojo(), testServiceAlertDef);
        sessionFactory.getCurrentSession().clear();
        // Verify Alerts not present
        assertNull(alertManager.getById(testPlatformAlert.getId()));
        assertNull(alertManager.getById(testServerAlert.getId()));
        assertNull(alertManager.getById(testServiceAlert.getId()));
    }

    @Test
    public void testFindLastFixedByDefinition() {
        alertManager.setAlertFixed(testPlatformAlert);
        // flush the update
        sessionFactory.getCurrentSession().flush();
        Alert lastFixed = alertManager.findLastFixedByDefinition(testPlatformAlertDef);
        assertEquals(testPlatformAlert.getId(), lastFixed.getId());
    }

    @Test
    public void testFindLastFixedNoneFixed() {
        assertNull(alertManager.findLastFixedByDefinition(testPlatformAlertDef));
    }

    @Test
    public void testFindLastUnfixedByDefinition() {
        Alert lastUnfixed = alertManager.findLastUnfixedByDefinition(authzSubjectManager
            .getOverlordPojo(), testPlatformAlertDef.getId());
        assertEquals(testPlatformAlert.getId(), lastUnfixed.getId());
    }

    @Test
    public void testFindLastUnfixedWhenFixed() {
        alertManager.setAlertFixed(testPlatformAlert);
        // flush the update
        sessionFactory.getCurrentSession().flush();
        Alert lastUnfixed = alertManager.findLastUnfixedByDefinition(authzSubjectManager
            .getOverlordPojo(), testPlatformAlertDef.getId());
        assertNull(lastUnfixed);
    }

    @Test
    public void testLogActionDetail() {
        Action alertAction = Action.newNoOpAction();
        // Save transient instance of Action before flushing
        sessionFactory.getCurrentSession().save(alertAction);
        testPlatformAlert.createActionLog("Notified users:", alertAction, authzSubjectManager
            .getOverlordPojo());
        // Flush the changes
        sessionFactory.getCurrentSession().flush();
        Collection<AlertActionLog> actionLogs = testPlatformAlert.getActionLog();
        assertEquals(actionLogs.iterator().next().getDetail(), "Notified users:");
        assertEquals(actionLogs.iterator().next().getAction().getClassName(),
            "org.hyperic.hq.events.NoOpAction");
        assertEquals(actionLogs.iterator().next().getSubject(), authzSubjectManager
            .getOverlordPojo());
    }

    @Test
    public void testAddConditionLog() {
        AlertCondition alertCond = new AlertCondition();
        // Save transient instance of Action before flushing
        sessionFactory.getCurrentSession().save(alertCond);
        testPlatformAlert.createConditionLog("Condition changed", alertCond);
        sessionFactory.getCurrentSession().flush();
        Collection<AlertConditionLog> alertCondLogs = testPlatformAlert.getConditionLog();
        assertEquals(alertCondLogs.iterator().next().getValue(), "Condition changed");
    }

}
