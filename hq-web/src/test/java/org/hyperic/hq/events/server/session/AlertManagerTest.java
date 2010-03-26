package org.hyperic.hq.events.server.session;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
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
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.context.TestContextLoader;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
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
    private ResourceGroupManager resourceGroupManager;

    @Autowired
    private ResourceManager resourceManager;

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

    private Platform testPlatform;

    private Server testServer;

    private Service testService;

    // private Alert testPlatformAlert;

    // private Alert testServerAlert;

    // private Alert testServiceAlert;

    private List<AuthzSubject> testUsers = new ArrayList<AuthzSubject>();

    private List<Role> testRoles = new ArrayList<Role>();

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
        this.testPlatform = createPlatform(agentToken, platformType, "leela.local");
        // Create ServerType
        ServerType testServerType = createServerType("Tomcat", "6.0", new String[] { "Linux" },
            "Test Server Plugin");
        // Create test server
        this.testServer = createServer(testPlatform, testServerType);
        // Create ServiceType
        ServiceType serviceType = createServiceType("Spring JDBC Template", "Test Server Plugin",
            testServerType);
        // Create test service
        this.testService = serviceManager.createService(authzSubjectManager.getOverlordPojo(),
            testServer.getId(), serviceType.getId(), "leela.local jdbcTemplate",
            "Spring JDBC Template", "my computer");
        return testPlatform;
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
            Collections.EMPTY_LIST, resources);
        return resGrp;
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

    /*
     * private void createResourceAlerts() { this.testPlatformAlert =
     * alertManager.createAlert(this.testPlatformAlertDef, System
     * .currentTimeMillis()); this.testServerAlert =
     * alertManager.createAlert(this.testServerAlertDef, System
     * .currentTimeMillis()); this.testServiceAlert =
     * alertManager.createAlert(this.testServiceAlertDef, System
     * .currentTimeMillis()); }
     */

    private AuthzSubject getAuthUser(String nameToken) {
        // Return this if there are no matching
        AuthzSubject authUser = authzSubjectManager.getOverlordPojo();
        for (AuthzSubject user : this.testUsers) {
            if (user.getName().equalsIgnoreCase(nameToken)) {
                authUser = user;
            }
        }
        return authUser;
    }

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        // Setup Agent
        String agentToken = "agentToken123";
        agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        Platform testPlatform = createResources(agentToken);
        ResourceGroup resGrp = createResourceGroup(testPlatform);
        createRoles(new Integer[] { resGrp.getId() });
        createResourceAlertDefs(testPlatform);
        // createResourceAlerts();
        // Manual flush is required in any method in which you are updating the
        // Hibernate session in
        // order to avoid false positive in test
        sessionFactory.getCurrentSession().flush();
    }

    @Test
    public void testCreatePlatformAlert() {
        long ctime = System.currentTimeMillis();
        Alert alert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        assertNotNull(alert);
        assertEquals("Wrong Alert Definition Id", this.testPlatformAlertDef.getId(), alert
            .getAlertDefinition().getId());
        assertEquals("Creation time is incorrectly updated", Long.valueOf(ctime), Long
            .valueOf(alert.getCtime()));
    }

    /*
     * Usecase: Verify that the alertDefinition state's lastFired time is
     * correctly updated with the alert's ctime. This flow is a part
     * createAlert() service method.
     */
    @Test
    public void testAlertDefStateLastFiredUpdate() {
        long now = System.currentTimeMillis();
        long alertLastFiredTime = now - 100l;
        Alert alert1 = alertManager.createAlert(this.testPlatformAlertDef, alertLastFiredTime);
        assertNotNull(alert1);
        assertEquals("Last fired time changed:" + this.testPlatformAlertDef.getLastFired() +
                     "  alert: " + alertLastFiredTime + "now: " + now, this.testPlatformAlertDef
            .getLastFired(), alertLastFiredTime);
        Alert alert2 = alertManager.createAlert(this.testPlatformAlertDef, now);
        assertNotNull(alert2);
        assertEquals("Last fired time updated", this.testPlatformAlertDef.getLastFired(), now);
    }

    @Test
    public void testCreateServerAlert() {
        long ctime = System.currentTimeMillis();
        Alert alert = alertManager.createAlert(this.testServerAlertDef, ctime);
        assertNotNull(alert);
        assertEquals("Wrong Alert Definition Id", this.testServerAlertDef.getId(), alert
            .getAlertDefinition().getId());
        assertEquals("Creation time is incorrectly updated", Long.valueOf(ctime), Long
            .valueOf(alert.getCtime()));
    }

    @Test
    public void testCreateServiceAlert() {
        long ctime = System.currentTimeMillis();
        Alert alert = alertManager.createAlert(this.testServiceAlertDef, ctime);
        assertNotNull(alert);
        assertEquals("Wrong Alert Definition Id", this.testServiceAlertDef.getId(), alert
            .getAlertDefinition().getId());
        assertEquals("Creation time is incorrectly updated", Long.valueOf(ctime), Long
            .valueOf(alert.getCtime()));
    }

    @Test
    public void testFindAlertById() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        Alert alert1 = alertManager.findAlertById(testPlatformAlert.getId());
        assertNotNull("Alert should be available here", alert1);
        assertFalse("Alert should not be acknowledgeable", alert1.isAckable());
    }

    @Test
    public void testSetAlertFixed() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        alertManager.setAlertFixed(testPlatformAlert);
        // flush the update
        // sessionFactory.getCurrentSession().flush();
        // retrieve the alert and check if it was fixed
        assertTrue("Alert is not fixed", alertManager.findAlertById(testPlatformAlert.getId())
            .isFixed());
    }

    @Test
    public void testLogActionDetail() {
        Action alertAction = Action.newNoOpAction();
        // Save transient instance of Action before flushing
        sessionFactory.getCurrentSession().save(alertAction);
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        testPlatformAlert.createActionLog("Notified users:", alertAction, authzSubjectManager
            .getOverlordPojo());
        // Flush the changes
        sessionFactory.getCurrentSession().flush();
        Collection<AlertActionLog> actionLogs = testPlatformAlert.getActionLog();
        assertEquals("Incorrect Detail", actionLogs.iterator().next().getDetail(),
            "Notified users:");
        assertEquals("Incorrect Action class", actionLogs.iterator().next().getAction()
            .getClassName(), "org.hyperic.hq.events.NoOpAction");
        assertEquals("Incorrect Subject", actionLogs.iterator().next().getSubject(),
            authzSubjectManager.getOverlordPojo());
    }

    @Test
    public void testDeleteAlertsByIds() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        Alert testServerAlert = alertManager.createAlert(this.testServerAlertDef, ctime);
        Alert testServiceAlert = alertManager.createAlert(this.testServiceAlertDef, ctime);
        alertManager.deleteAlerts(new Integer[] { testPlatformAlert.getId(),
                                                 testServerAlert.getId(),
                                                 testServiceAlert.getId() });
        // The underlying DAO uses HQL to do bulk delete. This will NOT update
        // the session cache, so a subsequent query will make it seem as though
        // alert is still there. We have to explicitly remove it from cache.
        sessionFactory.getCurrentSession().clear();
        // verify alert cannot be loaded from DB
        assertNull("Alert is not deleted", alertManager.getById(testPlatformAlert.getId()));
        assertNull("Alert is not deleted", alertManager.getById(testServerAlert.getId()));
        assertNull("Alert is not deleted", alertManager.getById(testServiceAlert.getId()));
    }

    @Test
    public void testDeleteAlertsByAlertDefs() throws PermissionException {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        Alert testServerAlert = alertManager.createAlert(this.testServerAlertDef, ctime);
        Alert testServiceAlert = alertManager.createAlert(this.testServiceAlertDef, ctime);

        alertManager.deleteAlerts(authzSubjectManager.getOverlordPojo(), testPlatformAlertDef);
        alertManager.deleteAlerts(authzSubjectManager.getOverlordPojo(), testServerAlertDef);
        alertManager.deleteAlerts(authzSubjectManager.getOverlordPojo(), testServiceAlertDef);
        sessionFactory.getCurrentSession().clear();
        // Verify Alerts not present
        assertNull("Alert is not deleted", alertManager.getById(testPlatformAlert.getId()));
        assertNull("Alert is not deleted", alertManager.getById(testServerAlert.getId()));
        assertNull("Alert is not deleted", alertManager.getById(testServiceAlert.getId()));
    }

    @Test
    public void testDeleteAlertsByRange() {
        // Create Alerts with the specific time range
        long time1 = System.currentTimeMillis();
        long time2 = System.currentTimeMillis() + 99l;
        long time3 = System.currentTimeMillis() + 200l;
        long time4 = System.currentTimeMillis() + 500l;
        long time5 = System.currentTimeMillis() + 700l;
        Alert alert1 = alertManager.createAlert(this.testPlatformAlertDef, time1);
        Alert alert2 = alertManager.createAlert(this.testPlatformAlertDef, time2);
        Alert alert3 = alertManager.createAlert(this.testPlatformAlertDef, time3);
        Alert alert4 = alertManager.createAlert(this.testPlatformAlertDef, time4);
        Alert alert5 = alertManager.createAlert(this.testPlatformAlertDef, time5);

        // Now, delete alerts for a specific range
        alertManager.deleteAlerts(time3, time4);
        sessionFactory.getCurrentSession().clear();
        // Verify the alerts are deleted only within the given range
        assertNotNull("Alert1 shouldn't be deleted", alertManager.getById(alert1.getId()));
        assertNotNull("Alert2 shouldn't be deleted", alertManager.getById(alert2.getId()));
        assertNull("Alert3 should have been deleted", alertManager.getById(alert3.getId()));
        assertNull("Alert4 should have been deleted", alertManager.getById(alert4.getId()));
        assertNotNull("Alert5 shouldn't be deleted", alertManager.getById(alert5.getId()));
    }

    /*
     * @Test(expected = PermissionException.class) public void
     * testDeletePlatformAlertDefAlertsByReadOnly() throws PermissionException {
     * AuthzSubject readOnlyUser = getAuthUser("readOnly");
     * alertManager.deleteAlerts(readOnlyUser, testPlatformAlertDef); //
     * alertManager.deleteAlerts(authzSubjectManager.getOverlordPojo(), //
     * testServerAlertDef); //
     * alertManager.deleteAlerts(authzSubjectManager.getOverlordPojo(), //
     * testServiceAlertDef); }
     */

    @Test
    public void testGetAlertValueById() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertValue alertVal = alertManager.getById(testPlatformAlert.getId());
        // Verify org.hyperic.hq.events.server.session.PagerProcessor_events
        // updates on AlertValue & ack flag

        // Verify AlertValue: AlertDefinition Id
        assertTrue(alertVal.alertDefIdHasBeenSet());
        assertEquals("Alert Value: AlertDefId is incorrect", this.testPlatformAlertDef.getId(),
            alertVal.getAlertDefId());
        // Verify AlertValue: isAck
        assertFalse("Alert should not be acknowledgeable", alertVal.isAcknowledgeable());

    }

    @Test
    public void testFindLastUnfixedByDefinition() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert1 = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        Alert testPlatformAlert2 = alertManager
            .createAlert(this.testPlatformAlertDef, ctime + 999l);
        Alert testPlatformAlert3 = alertManager.createAlert(this.testPlatformAlertDef,
            ctime + 2999l);
        Alert lastUnfixed = alertManager.findLastUnfixedByDefinition(authzSubjectManager
            .getOverlordPojo(), testPlatformAlertDef.getId());
        assertEquals("Incorrect last unfixed alert", testPlatformAlert3.getId(), lastUnfixed
            .getId());
    }

    @Test
    public void testFindLastUnfixedByDefWhenFixed() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert1 = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        Alert testPlatformAlert2 = alertManager
            .createAlert(this.testPlatformAlertDef, ctime + 999l);
        Alert testPlatformAlert3 = alertManager.createAlert(this.testPlatformAlertDef,
            ctime + 2999l);
        alertManager.setAlertFixed(testPlatformAlert1);
        alertManager.setAlertFixed(testPlatformAlert2);
        alertManager.setAlertFixed(testPlatformAlert3);
        // flush the update
        sessionFactory.getCurrentSession().flush();
        Alert lastUnfixed = alertManager.findLastUnfixedByDefinition(authzSubjectManager
            .getOverlordPojo(), testPlatformAlertDef.getId());
        assertNull("There should be no last unfixed alerts", lastUnfixed);
    }

    @Test
    public void testFindLastFixedByDefinition() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert1 = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        Alert testPlatformAlert2 = alertManager
            .createAlert(this.testPlatformAlertDef, ctime + 999l);
        Alert testPlatformAlert3 = alertManager.createAlert(this.testPlatformAlertDef,
            ctime + 2999l);
        alertManager.setAlertFixed(testPlatformAlert1);
        alertManager.setAlertFixed(testPlatformAlert2);
        alertManager.setAlertFixed(testPlatformAlert3);
        // flush the update
        sessionFactory.getCurrentSession().flush();
        Alert lastFixed = alertManager.findLastFixedByDefinition(testPlatformAlertDef);
        assertEquals("Incorrect Last fixed Alert", testPlatformAlert3.getId(), lastFixed.getId());
    }

    @Test
    public void testFindLastFixedNoneFixed() {
        long ctime = System.currentTimeMillis();
        alertManager.createAlert(this.testPlatformAlertDef, ctime);
        alertManager.createAlert(this.testPlatformAlertDef, ctime + 999l);
        alertManager.createAlert(this.testPlatformAlertDef, ctime + 2999l);
        assertNull("There should be no last fixed alerts", alertManager
            .findLastFixedByDefinition(testPlatformAlertDef));
    }

    @Test
    public void testAlertCount() {
        long ctime = System.currentTimeMillis();
        alertManager.createAlert(this.testPlatformAlertDef, ctime);
        alertManager.createAlert(this.testPlatformAlertDef, ctime + 999l);
        alertManager.createAlert(this.testPlatformAlertDef, ctime + 2999l);
        assertEquals("Wrong alert count", 3, alertManager.getAlertCount());
    }

    @Test
    public void testAlertCountByByAppDefIds() {
        long ctime = System.currentTimeMillis();
        alertManager.createAlert(this.testPlatformAlertDef, ctime);
        alertManager.createAlert(this.testServerAlertDef, ctime);
        alertManager.createAlert(this.testServerAlertDef, ctime + 99l);
        alertManager.createAlert(this.testServiceAlertDef, ctime);
        int[] alertCounts = { 1, 2, 1 };
        assertArrayEquals("Incorrect Alerts count", alertCounts, alertManager
            .getAlertCount(new AppdefEntityID[] { this.testPlatformAlertDef.getAppdefEntityId(),
                                                 this.testServerAlertDef.getAppdefEntityId(),
                                                 this.testServiceAlertDef.getAppdefEntityId() }));
    }

    @Test
    public void testfindPageListAlertsByAppDef() throws PermissionException {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert1 = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        Alert testPlatformAlert2 = alertManager
            .createAlert(this.testPlatformAlertDef, ctime + 999l);
        Alert testPlatformAlert3 = alertManager.createAlert(this.testPlatformAlertDef,
            ctime + 2999l);
        // TODO: Need to use proper parameterization
        // Currently the findAlerts() returns AlertValue(the
        // PagerProcessor_events) instead of Alert.
        PageList allAlerts = new PageList<Alert>();
        allAlerts.add(testPlatformAlert1.getAlertValue());
        allAlerts.add(testPlatformAlert2.getAlertValue());
        allAlerts.add(testPlatformAlert3.getAlertValue());

        // Fetch PageList<Alert> for a given AppDef
        PageControl pc = new PageControl();
        PageList<Alert> pageAlerts = alertManager.findAlerts(overlord, testPlatformAlertDef
            .getAppdefEntityId(), pc);

        // Verify both the pageList matches
        assertTrue("PageList doesn't have all alerts", pageAlerts.containsAll(allAlerts));
        assertTrue("Alerts don't exist in page list", allAlerts.containsAll(pageAlerts));
    }

    @Test
    public void testShortReasonForThresholdType() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("Availability");
        cond.setType(EventConstants.TYPE_THRESHOLD);
        testPlatformAlert.createConditionLog("threshold exceeded", cond);
        // Short reason text is:
        // "<AlertDefName> <AppDefEntityName> <conditionName> (<conditionValue>) "
        assertEquals("Incorrect Short Reason",
            "Platform Down leela.local Availability (threshold exceeded) ", alertManager
                .getShortReason(testPlatformAlert));
    }

    @Test
    public void testShortReasonForBaselineType() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("Baseline");
        cond.setType(EventConstants.TYPE_BASELINE);
        testPlatformAlert.createConditionLog("baseline threshold", cond);
        // Short reason text is:
        // "<AlertDefName> <AppDefEntityName> <conditionName> (<conditionValue>) "
        assertEquals("Incorrect Short Reason",
            "Platform Down leela.local Baseline (baseline threshold) ", alertManager
                .getShortReason(testPlatformAlert));
    }
    
    @Test
    public void testShortReasonForControlType() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("Control");
        cond.setType(EventConstants.TYPE_CONTROL);
        testPlatformAlert.createConditionLog("controlValue", cond);
        // Short reason text is:
        // "<AlertDefName> <AppDefEntityName> <conditionName>"
        assertEquals("Incorrect Short Reason",
            "Platform Down leela.local Control", alertManager
                .getShortReason(testPlatformAlert));
    }
    
    @Test
    public void testShortReasonForChangeType() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("Change");
        cond.setType(EventConstants.TYPE_CHANGE);
        testPlatformAlert.createConditionLog("changeValue", cond);
        // Short reason text is:
        // "<AlertDefName> <AppDefEntityName> <conditionName> (<conditionValue>) "
        assertEquals("Incorrect Short Reason",
            "Platform Down leela.local Change (changeValue) ", alertManager
                .getShortReason(testPlatformAlert));
    }
    
    @Test
    public void testShortReasonForCustPropertyType() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("CustomProperty");
        cond.setType(EventConstants.TYPE_CUST_PROP);
        testPlatformAlert.createConditionLog("customPropertyValue", cond);
        // Short reason text is:
        // "<AlertDefName> <AppDefEntityName> <conditionName> (<conditionValue>) "
        assertEquals("Incorrect Short Reason",
            "Platform Down leela.local CustomProperty (customPropertyValue) ", alertManager
                .getShortReason(testPlatformAlert));
    }
    
    @Test
    public void testShortReasonForLogType() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("JustName");
        cond.setType(EventConstants.TYPE_LOG);
        testPlatformAlert.createConditionLog("logValue", cond);
        // Short reason text is:
        // "<AlertDefName> <AppDefEntityName> Log (<conditionValue>) "
        assertEquals("Incorrect Short Reason",
            "Platform Down leela.local Log (logValue) ", alertManager
                .getShortReason(testPlatformAlert));
    }
    
    @Test
    public void testShortReasonForConfigChangeType() {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("ChangeOfConfig");
        cond.setType(EventConstants.TYPE_CFG_CHG);
        testPlatformAlert.createConditionLog("configValue", cond);
        // Short reason text is:
        // "<AlertDefName> <AppDefEntityName> Config changed (<conditionValue>) "
        assertEquals("Incorrect Short Reason",
            "Platform Down leela.local Config changed (configValue) ", alertManager
                .getShortReason(testPlatformAlert));
    }
}
