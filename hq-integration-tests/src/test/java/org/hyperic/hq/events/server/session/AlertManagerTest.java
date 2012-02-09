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

package org.hyperic.hq.events.server.session;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.ObjectNotFoundException;
import org.hibernate.SessionFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroup.ResourceGroupCreateInfo;
import org.hyperic.hq.authz.server.session.Role;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.RoleManager;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.hq.util.Reference;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Integration test of the {@link AlertManagerImpl}
 * @author jhickey
 * 
 */
@DirtiesContext
public class AlertManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private AlertManager alertManager;

    @Autowired
    private AlertDefinitionManager alertDefinitionManager;

    @Autowired
    private RoleManager roleManager;

    @Autowired
    private MeasurementManager measurementManager;

    @Autowired
    private SessionFactory sessionFactory;

    private AlertDefinition testPlatformAlertDef;

    private AlertDefinition testServerAlertDef;

    private AlertDefinition testServiceAlertDef;

    private Platform testPlatform;

    private ResourceGroup resGrp;

    private List<AuthzSubject> testUsers = new ArrayList<AuthzSubject>();

    private List<Role> testRoles = new ArrayList<Role>();

    private AlertDefinition createAlertDefinition(Integer appdefId, Integer appdefType,
                                                  String alertDefName) throws AlertDefinitionCreateException {
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
        this.testPlatform = createPlatform(agentToken, platformType, "leela.local", "leela.local");
        // Create ServerType
        ServerType testServerType = createServerType("Tomcat", "6.0", new String[] { "Linux" },
            "Test Server Plugin", false);
        // Create test server
        Server testServer = createServer(testPlatform, testServerType, "My Server");
        // Create ServiceType
        ServiceType serviceType = createServiceType("Spring JDBC Template", "Test Server Plugin",
            testServerType);
        // Create test service
        serviceManager.createService(authzSubjectManager.getOverlordPojo(), testServer.getId(),
            serviceType.getId(), "leela.local jdbcTemplate", "Spring JDBC Template", "my computer");
        return testPlatform;
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
            new ArrayList<Role>(0), resources);
        return resGrp;
    }

    private void createResourceAlertDefs(Platform testPlatform) throws AlertDefinitionCreateException {
        // Create Platform Alert Definition
        this.testPlatformAlertDef = createAlertDefinition(testPlatform.getId(),
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, "Platform Down");
        this.testPlatformAlertDef.setPriority(3);
        // Create Server Alert Definition
        Server testServer = testPlatform.getServers().iterator().next();
        Integer serverAppDefId = testServer.getId();
        this.testServerAlertDef = createAlertDefinition(serverAppDefId,
            AppdefEntityConstants.APPDEF_TYPE_SERVER, "Server Down");
        this.testServerAlertDef.setPriority(2);
        // Create Service Alert Definition
        Integer serviceAppDefId = testServer.getServices().iterator().next().getId();
        this.testServiceAlertDef = createAlertDefinition(serviceAppDefId,
            AppdefEntityConstants.APPDEF_TYPE_SERVICE, "Service Down");
        this.testServiceAlertDef.setPriority(1);
    }

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        // Setup Agent
        String agentToken = "agentToken123";
        agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        Platform testPlatform = createResources(agentToken);
        this.resGrp = createResourceGroup(testPlatform);
        createRoles(new Integer[] { resGrp.getId() });
        createResourceAlertDefs(testPlatform);
        // createResourceAlerts();
        // Manual flush is required in any method in which you are updating the
        // Hibernate session in
        // order to avoid false positive in test
        flushSession();
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
        // flushSession();
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
        flushSession();
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
        clearSession();
        // verify alert cannot be loaded from DB
        try {
            alertManager.findAlertById(testPlatformAlert.getId());
            fail("Alert was not deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }
        try {
            alertManager.findAlertById(testServerAlert.getId());
            fail("Alert was not deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }
        try {
            alertManager.findAlertById(testServiceAlert.getId());
            fail("Alert was not deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }
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
        clearSession();
        // Verify Alerts not present
        try {
            alertManager.findAlertById(testPlatformAlert.getId());
            fail("Alert was not deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }
        try {
            alertManager.findAlertById(testServerAlert.getId());
            fail("Alert was not deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }
        try {
            alertManager.findAlertById(testServiceAlert.getId());
            fail("Alert was not deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }
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
        alertManager.deleteAlerts(time3+1, 10);
        clearSession();
        // Verify the alerts are deleted only within the given range
        testAlertNotExists(alert1);
        testAlertNotExists(alert2);
        testAlertNotExists(alert3);
        assertNotNull("Alert4 shouldn't be deleted", alertManager.findAlertById(alert4.getId()));
        assertNotNull("Alert5 shouldn't be deleted", alertManager.findAlertById(alert5.getId()));
    }
    
    private void testAlertNotExists(Alert alert) {
        try {
            alertManager.findAlertById(alert.getId());
            fail("alertId=" + alert.getId() + " was not deleted");
        } catch (ObjectNotFoundException e) {
            // expected
        }
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
        Alert alertVal = alertManager.findAlertById(testPlatformAlert.getId());
        // Verify org.hyperic.hq.events.server.session.PagerProcessor_events
        // updates on AlertValue & ack flag

        assertEquals("Alert Value: AlertDefId is incorrect", this.testPlatformAlertDef.getId(),
            alertVal.getAlertDefinition().getId());
        // Verify AlertValue: isAck
        assertFalse("Alert should not be acknowledgeable", alertVal.isAckable());

    }

    @Test
    public void testFindLastUnfixedByDefinition() {
        long ctime = System.currentTimeMillis();
        alertManager.createAlert(this.testPlatformAlertDef, ctime);
        alertManager.createAlert(this.testPlatformAlertDef, ctime + 999l);
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
        flushSession();
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
        flushSession();
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

    @SuppressWarnings("unchecked")
    @Test
    public void testFireAlert() {
        testPlatformAlertDef.setEnabled(true);
        AlertFiredEvent event = new AlertFiredEvent(123, testPlatformAlertDef.getId(),
            testPlatformAlertDef.getAppdefEntityId(), "Platform Down", System.currentTimeMillis(),
            "Firing Alert-123");
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(15, event);
        AlertConditionsSatisfiedZEvent alertZEvent = new AlertConditionsSatisfiedZEvent(
            testPlatformAlertDef.getId(), new TriggerFiredEvent[] { triggerFired });
        alertManager.fireAlert(alertZEvent);
        List<TransactionSynchronization> synchs = TransactionSynchronizationManager
            .getSynchronizations();
        for (TransactionSynchronization sync : synchs) {
            String enclosingMethod = sync.getClass().getEnclosingMethod().getName();
            if (enclosingMethod.equalsIgnoreCase("registerAlertFiredEvent")) {
                Class<ClassicEscalatableCreator> c = (Class<ClassicEscalatableCreator>) sync
                    .getClass().getEnclosingClass();
                assertEquals("Enclosing class is not ClassicEscalatableCreator", c
                    .getCanonicalName(),
                    "org.hyperic.hq.events.server.session.ClassicEscalatableCreator");
                return;
            }
        }
        fail("Alert Fired Event didn't take place");
    }

    @Test
    public void testFindAlertsPageList() throws PermissionException {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        long time1 = System.currentTimeMillis();
        long time2 = time1 - 2 * 60000l;
        long time3 = time1 - 4 * 60000l;
        long time4 = time1 - 6 * 60000l;
        long time5 = time1 - 8 * 60000l;
        Alert alert1 = alertManager.createAlert(this.testPlatformAlertDef, time1);
        Alert alert2 = alertManager.createAlert(this.testPlatformAlertDef, time2);
        Alert alert3 = alertManager.createAlert(this.testPlatformAlertDef, time3);
        alertManager.createAlert(this.testPlatformAlertDef, time4);
        alertManager.createAlert(this.testPlatformAlertDef, time5);
        PageList<Alert> actualAlerts = new PageList<Alert>();
        actualAlerts.add(0, alert1);
        actualAlerts.add(1, alert2);
        actualAlerts.add(2, alert3);
        PageList<Alert> alerts = alertManager.findAlerts(overlord, testPlatformAlertDef
            .getAppdefEntityId(), time3 - 60000l, time1 + 2 * 6000l, new PageControl());
        assertEquals("PageList is incorrect", actualAlerts, alerts);
    }

    @Test
    public void testFindAlertsByCriteriaWithoutAlertDef() throws PermissionException {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        PageControl pc = new PageControl();
        PageInfo pi = PageInfo.create(pc, AlertSortField.SEVERITY);
        long time1 = System.currentTimeMillis();
        long time2 = time1 - 2 * 60000l;
        long time3 = time1 - 3 * 60000l;
        long time4 = time1 - 4 * 60000l;
        long time5 = time1 - 5 * 60000l;
        long time6 = time1 - 6 * 60000l;
        Alert alert1 = alertManager.createAlert(this.testPlatformAlertDef, time1);
        alertManager.createAlert(this.testServerAlertDef, time2);
        Alert alert3 = alertManager.createAlert(this.testServiceAlertDef, time3);
        Alert alert4 = alertManager.createAlert(this.testPlatformAlertDef, time4);
        alertManager.createAlert(this.testServerAlertDef, time5);
        alertManager.createAlert(this.testServiceAlertDef, time6);
        alertManager.setAlertFixed(alert3);
        alertManager.setAlertFixed(alert4);
        flushSession();
        // Following query should fetch only the platform alert which has
        // priority 3 & unfixed
        List<Alert> alerts = alertManager.findAlerts(overlord.getId(), 3, 10 * 60000l,
            time1 + 2 * 60000l, false, true, resGrp.getId(), pi);
        assertEquals("Fetched alert Id is wrong", alert1.getId(), alerts.get(0).getId());
        // For the alerts having priority 3 and without considering fixed flag
        List<Alert> alerts1 = alertManager.findAlerts(overlord.getId(), 3, 10 * 60000l,
            time1 + 2 * 60000l, false, false, resGrp.getId(), pi);
        assertEquals("Wrong alerts count", 2, alerts1.size());
        // For the alerts that have priority 2 and above and considering fixed
        // flag
        List<Alert> alerts2 = alertManager.findAlerts(overlord.getId(), 2, 10 * 60000l,
            time1 + 2 * 60000l, false, true, resGrp.getId(), pi);
        assertEquals("Fetched alerts count is wrong", 3, alerts2.size());
    }

    @Test
    public void testFindAlertsByCriteriaWithAlertDef() throws PermissionException {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        PageControl pc = new PageControl();
        PageInfo pi = PageInfo.create(pc, AlertSortField.SEVERITY);
        long time1 = System.currentTimeMillis();
        long time2 = time1 - 2 * 60000l;
        long time3 = time1 - 3 * 60000l;
        long time4 = time1 - 4 * 60000l;
        long time5 = time1 - 5 * 60000l;
        long time6 = time1 - 6 * 60000l;
        Alert alert1 = alertManager.createAlert(this.testPlatformAlertDef, time1);
        Alert alert2 = alertManager.createAlert(this.testServerAlertDef, time2);
        alertManager.createAlert(this.testServiceAlertDef, time3);
        Alert alert4 = alertManager.createAlert(this.testPlatformAlertDef, time4);
        Alert alert5 = alertManager.createAlert(this.testServerAlertDef, time5);
        alertManager.createAlert(this.testServiceAlertDef, time6);
        alertManager.setAlertFixed(alert4);
        alertManager.setAlertFixed(alert5);
        flushSession();
        // Following query should fetch only the platform alerts & unfixed
        List<Alert> alerts = alertManager.findAlerts(overlord.getId(), 3, 10 * 60000l,
            time1 + 2 * 60000l, false, true, resGrp.getId(), this.testPlatformAlertDef.getId(), pi);
        assertEquals("Fetched alert Id is wrong", alert1.getId(), alerts.get(0).getId());
        // Fetch Server alerts & unfixed
        List<Alert> alerts1 = alertManager.findAlerts(overlord.getId(), 2, 10 * 60000l,
            time1 + 2 * 60000l, false, true, resGrp.getId(), this.testServerAlertDef.getId(), pi);
        assertEquals("Wrong Fetched alerts count", alert2.getId(), alerts1.get(0).getId());
        // Fetch Service alerts & unfixed
        List<Alert> alerts2 = alertManager.findAlerts(overlord.getId(), 1, 10 * 60000l,
            time1 + 2 * 60000l, false, false, resGrp.getId(), this.testServerAlertDef.getId(), pi);
        assertEquals("Fetched alerts count is wrong", 2, alerts2.size());
    }

    @Test
    public void testFindAlertsByCriteriaWithAppDefs() throws PermissionException {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        PageControl pc = new PageControl();
        PageInfo.create(pc, AlertSortField.SEVERITY);
        long time1 = System.currentTimeMillis();
        long time2 = time1 - 2 * 60000l;
        long time3 = time1 - 3 * 60000l;
        long time4 = time1 - 4 * 60000l;
        long time5 = time1 - 5 * 60000l;
        long time6 = time1 - 6 * 60000l;
        Alert alert1 = alertManager.createAlert(this.testPlatformAlertDef, time1);
        Alert alert2 = alertManager.createAlert(this.testServerAlertDef, time2);
        Alert alert3 = alertManager.createAlert(this.testServiceAlertDef, time3);
        List<Alert> recent3Alerts = new ArrayList<Alert>();
        recent3Alerts.add(0, alert1);
        recent3Alerts.add(1, alert2);
        recent3Alerts.add(2, alert3);
        alertManager.createAlert(this.testPlatformAlertDef, time4);
        alertManager.createAlert(this.testServerAlertDef, time5);
        alertManager.createAlert(this.testServiceAlertDef, time6);
        List<AppdefEntityID> all = new ArrayList<AppdefEntityID>();
        all.add(this.testPlatformAlertDef.getAppdefEntityId());
        all.add(this.testServerAlertDef.getAppdefEntityId());
        all.add(this.testServiceAlertDef.getAppdefEntityId());
        // Verify Only 3 *recent* alerts are retrieved; Set count to 3
        List<Alert> alerts = alertManager.findAlerts(overlord, 3, 1, 10 * 60000l,
            time1 + 2 * 60000l, all);
        assertEquals("Recent alerts aren't fetched", recent3Alerts, alerts);
        // Verify All alerts are retrieved for the list of appDefs
        List<Alert> alerts1 = alertManager.findAlerts(overlord, 10, 1, 10 * 60000l,
            time1 + 2 * 60000l, all);
        assertEquals("All alerts aren't fetched", 6, alerts1.size());
        all.remove(this.testPlatformAlertDef.getAppdefEntityId());
        List<Alert> alerts2 = alertManager.findAlerts(overlord, 10, 1, 10 * 60000l,
            time1 + 2 * 60000l, all);
        assertEquals("Alerts aren't fetched correctly", 4, alerts2.size());
    }

    @Test
    public void testFindEscalatable() throws PermissionException {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        long time1 = System.currentTimeMillis();
        testPlatformAlertDef.setEnabled(true);
        List<AppdefEntityID> platformAppDef = new ArrayList<AppdefEntityID>();
        platformAppDef.add(this.testPlatformAlertDef.getAppdefEntityId());
        AlertFiredEvent event = new AlertFiredEvent(123, testPlatformAlertDef.getId(),
            testPlatformAlertDef.getAppdefEntityId(), "Platform Down", time1, "Firing Alert-123");
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(15, event);
        AlertConditionsSatisfiedZEvent alertZEvent = new AlertConditionsSatisfiedZEvent(
            testPlatformAlertDef.getId(), new TriggerFiredEvent[] { triggerFired });
        alertManager.fireAlert(alertZEvent);
        List<Escalatable> escList = alertManager.findEscalatables(overlord, 1, 3, 5 * 60000l,
            time1 + 2 * 60000l, platformAppDef);
        assertNotNull(escList);
        assertEquals("Wrong Short Reason", escList.get(0).getShortReason(),
            "Platform Down leela.local ");
    }

    @Test
    public void testGetUnfixedCount() throws PermissionException {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        long time1 = System.currentTimeMillis();
        long time2 = time1 - 2 * 60000l;
        long time3 = time1 - 3 * 60000l;
        long time4 = time1 - 4 * 60000l;
        long time5 = time1 - 5 * 60000l;
        long time6 = time1 - 6 * 60000l;
        alertManager.createAlert(this.testPlatformAlertDef, time1);
        alertManager.createAlert(this.testServerAlertDef, time2);
        Alert alert3 = alertManager.createAlert(this.testServiceAlertDef, time3);
        Alert alert4 = alertManager.createAlert(this.testPlatformAlertDef, time4);
        alertManager.createAlert(this.testServerAlertDef, time5);
        alertManager.createAlert(this.testServiceAlertDef, time6);
        alertManager.setAlertFixed(alert3);
        alertManager.setAlertFixed(alert4);
        flushSession();
        Map<Integer,List<Alert>> alertMap = alertManager.getUnfixedByResource(overlord.getId(), 10 * 60000l,
            time1 + 60000l);

        int unfixedCounts = 0;
        Collection<Resource> resources = resourceGroupManager.getMembers(resGrp);
        for (Resource r : resources) {
            List<Alert> alerts = alertMap.get(r.getId());
            unfixedCounts += alerts.size();
        }

        assertEquals("Unfixed alerts count is incorrect", 4, unfixedCounts);
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
        assertEquals("Incorrect Short Reason", "Platform Down leela.local Control", alertManager
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
        assertEquals("Incorrect Short Reason", "Platform Down leela.local Change (changeValue) ",
            alertManager.getShortReason(testPlatformAlert));
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
        assertEquals("Incorrect Short Reason", "Platform Down leela.local Log (logValue) ",
            alertManager.getShortReason(testPlatformAlert));
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

    @Test
    public void testLongReasonForThresholdType() throws ApplicationException {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("AvailabilityName");
        cond.setType(EventConstants.TYPE_THRESHOLD);
        cond.setComparator("<");
        cond.setThreshold(1);
        int appDefType = testPlatform.getResource().getResourceType().getAppdefType();
        MonitorableType monitor_Type = new MonitorableType("Platform monitor", appDefType, "test");
        Category cate = new Category("Test Category");
        sessionFactory.getCurrentSession().save(monitor_Type);
        sessionFactory.getCurrentSession().save(cate);
        MeasurementTemplate testTempl = new MeasurementTemplate("AvailabilityTemplate", "avail",
            "percentage", 1, true, 60000l, true, "Availability:avail", monitor_Type, cate, "test");
        sessionFactory.getCurrentSession().save(testTempl);
        List<Measurement> meas = measurementManager.createOrUpdateMeasurements(this.testPlatform
            .getEntityId(), new Integer[] { testTempl.getId() }, new long[] { 60000l },
            new ConfigResponse(), new Reference<Boolean>());
        cond.setMeasurementId(meas.get(0).getId());
        testPlatformAlert.createConditionLog("50", cond);
        // Long reason text is: //
        // "    If <AlertConditionName>  <Threshold with unit> (actual value = <actual value>)"
        String longReason = alertManager.getLongReason(testPlatformAlert);
        assertEquals("Incorrect Long Reason",
            "\n    If AvailabilityName < 100.0% (actual value = 50)", longReason);
    }

    @Test
    public void testLongReasonForBaselineType() throws ApplicationException {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("Heap Memory");
        cond.setType(EventConstants.TYPE_BASELINE);
        cond.setComparator("<");
        cond.setThreshold(75);
        int appDefType = testPlatform.getResource().getResourceType().getAppdefType();
        MonitorableType monitor_Type = new MonitorableType("Platform monitor", appDefType, "test");
        Category cate = new Category("Test Category");
        sessionFactory.getCurrentSession().save(monitor_Type);
        sessionFactory.getCurrentSession().save(cate);
        MeasurementTemplate testTempl = new MeasurementTemplate("HeapMemoryTemplate", "avail",
            "percentage", 1, true, 60000l, true, "Availability:avail", monitor_Type, cate, "test");
        sessionFactory.getCurrentSession().save(testTempl);
        List<Measurement> meas = measurementManager.createOrUpdateMeasurements(this.testPlatform
            .getEntityId(), new Integer[] { testTempl.getId() }, new long[] { 60000l },
            new ConfigResponse(), new Reference<Boolean>());
        cond.setMeasurementId(meas.get(0).getId());
        testPlatformAlert.createConditionLog("50", cond);
        // Long reason text is: //
        // "    If <AlertConditionName>  <Threshold with unit> (actual value = <actual value>)"
        String longReason = alertManager.getLongReason(testPlatformAlert);
        assertEquals("Incorrect Long Reason",
            "\n    If Heap Memory < 75.0% of Baseline (actual value = 50)", longReason);
    }

    @Test
    public void testLongReasonForControlType() throws ApplicationException {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("Control Type Reason");
        cond.setType(EventConstants.TYPE_CONTROL);
        int appDefType = testPlatform.getResource().getResourceType().getAppdefType();
        MonitorableType monitor_Type = new MonitorableType("Platform monitor", appDefType, "test");
        Category cate = new Category("Test Category");
        sessionFactory.getCurrentSession().save(monitor_Type);
        sessionFactory.getCurrentSession().save(cate);
        MeasurementTemplate testTempl = new MeasurementTemplate("ControlTemplate", "avail",
            "percentage", 1, true, 60000l, true, "Availability:avail", monitor_Type, cate, "test");
        sessionFactory.getCurrentSession().save(testTempl);
        List<Measurement> meas = measurementManager.createOrUpdateMeasurements(this.testPlatform
            .getEntityId(), new Integer[] { testTempl.getId() }, new long[] { 60000l },
            new ConfigResponse(), new Reference<Boolean>());
        cond.setMeasurementId(meas.get(0).getId());
        testPlatformAlert.createConditionLog("50", cond);
        // Long reason text is: //
        // "    If <AlertConditionName>)"
        String longReason = alertManager.getLongReason(testPlatformAlert);
        assertEquals("Incorrect Long Reason", "\n    If Control Type Reason", longReason);
    }

    @Test
    public void testLongReasonForChangeType() throws ApplicationException {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("Change Config");
        cond.setType(EventConstants.TYPE_CHANGE);
        int appDefType = testPlatform.getResource().getResourceType().getAppdefType();
        MonitorableType monitor_Type = new MonitorableType("Platform monitor", appDefType, "test");
        Category cate = new Category("Test Category");
        sessionFactory.getCurrentSession().save(monitor_Type);
        sessionFactory.getCurrentSession().save(cate);
        MeasurementTemplate testTempl = new MeasurementTemplate("ChangeConfigemplate", "avail",
            "percentage", 1, true, 60000l, true, "Availability:avail", monitor_Type, cate, "test");
        sessionFactory.getCurrentSession().save(testTempl);
        List<Measurement> meas = measurementManager.createOrUpdateMeasurements(this.testPlatform
            .getEntityId(), new Integer[] { testTempl.getId() }, new long[] { 60000l },
            new ConfigResponse(), new Reference<Boolean>());
        cond.setMeasurementId(meas.get(0).getId());
        testPlatformAlert.createConditionLog("50", cond);
        // Long reason text is: //
        // "    If <AlertConditionName>  value changed (New value: <actual value>)"
        String longReason = alertManager.getLongReason(testPlatformAlert);
        assertEquals("Incorrect Long Reason",
            "\n    If Change Config value changed (New value: 50)", longReason);
    }

    @Test
    public void testLongReasonForCustPropertyType() throws ApplicationException {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("Custom Property");
        cond.setType(EventConstants.TYPE_CUST_PROP);
        int appDefType = testPlatform.getResource().getResourceType().getAppdefType();
        MonitorableType monitor_Type = new MonitorableType("Platform monitor", appDefType, "test");
        Category cate = new Category("Test Category");
        sessionFactory.getCurrentSession().save(monitor_Type);
        sessionFactory.getCurrentSession().save(cate);
        MeasurementTemplate testTempl = new MeasurementTemplate("CustomPropertyTemplate", "avail",
            "percentage", 1, true, 60000l, true, "Availability:avail", monitor_Type, cate, "test");
        sessionFactory.getCurrentSession().save(testTempl);
        List<Measurement> meas = measurementManager.createOrUpdateMeasurements(this.testPlatform
            .getEntityId(), new Integer[] { testTempl.getId() }, new long[] { 60000l },
            new ConfigResponse(), new Reference<Boolean>());
        cond.setMeasurementId(meas.get(0).getId());
        testPlatformAlert.createConditionLog("50", cond);
        // Long reason text is: //
        // "    If <AlertConditionName> value changed \n    <actual value>"
        String longReason = alertManager.getLongReason(testPlatformAlert);
        assertEquals("Incorrect Long Reason", "\n    If Custom Property value changed\n    50",
            longReason);
    }

    @Test
    public void testLongReasonForLogType() throws ApplicationException {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        // Set INFO level string
        cond.setName("6");
        cond.setType(EventConstants.TYPE_LOG);
        // Set matching substring
        cond.setOptionStatus("server startup");
        int appDefType = testPlatform.getResource().getResourceType().getAppdefType();
        MonitorableType monitor_Type = new MonitorableType("Platform monitor", appDefType, "test");
        Category cate = new Category("Test Category");
        sessionFactory.getCurrentSession().save(monitor_Type);
        sessionFactory.getCurrentSession().save(cate);
        MeasurementTemplate testTempl = new MeasurementTemplate("LogTemplate", "avail",
            "percentage", 1, true, 60000l, true, "Availability:avail", monitor_Type, cate, "test");
        sessionFactory.getCurrentSession().save(testTempl);
        List<Measurement> meas = measurementManager.createOrUpdateMeasurements(this.testPlatform
            .getEntityId(), new Integer[] { testTempl.getId() }, new long[] { 60000l },
            new ConfigResponse(), new Reference<Boolean>());
        cond.setMeasurementId(meas.get(0).getId());
        testPlatformAlert.createConditionLog("log value", cond);
        // Long reason text is: //
        // "    If Event/Log Level(<LogLevel>) and matching substring "<OptionStatus>"\n    Log: <value>"
        String longReason = alertManager.getLongReason(testPlatformAlert);
        assertEquals(
            "Incorrect Long Reason",
            "\n    If Event/Log Level(INF) and matching substring \"server startup\"\n    Log: log value",
            longReason);
    }

    @Test
    public void testLongReasonForConfigChangeType() throws ApplicationException {
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        AlertCondition cond = new AlertCondition();
        cond.setName("Config Change");
        cond.setType(EventConstants.TYPE_CFG_CHG);
        cond.setOptionStatus("platform.properties");
        int appDefType = testPlatform.getResource().getResourceType().getAppdefType();
        MonitorableType monitor_Type = new MonitorableType("Platform monitor", appDefType, "test");
        Category cate = new Category("Test Category");
        sessionFactory.getCurrentSession().save(monitor_Type);
        sessionFactory.getCurrentSession().save(cate);
        MeasurementTemplate testTempl = new MeasurementTemplate("LogTemplate", "avail",
            "percentage", 1, true, 60000l, true, "Availability:avail", monitor_Type, cate, "test");
        sessionFactory.getCurrentSession().save(testTempl);
        List<Measurement> meas = measurementManager.createOrUpdateMeasurements(this.testPlatform
            .getEntityId(), new Integer[] { testTempl.getId() }, new long[] { 60000l },
            new ConfigResponse(), new Reference<Boolean>());
        cond.setMeasurementId(meas.get(0).getId());
        testPlatformAlert.createConditionLog("config change value", cond);
        // Long reason text is: //
        // "    If Config changed: <OptionStatus>\n    Details: <value>"
        String longReason = alertManager.getLongReason(testPlatformAlert);
        assertEquals("Incorrect Long Reason",
            "\n    If Config changed: platform.properties\n    Details: config change value",
            longReason);
    }

    @Test
    public void testHandleSubjectRemoval() {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        Action alertAction = Action.newNoOpAction();
        // Save transient instance of Action before flushing
        sessionFactory.getCurrentSession().save(alertAction);
        long ctime = System.currentTimeMillis();
        Alert testPlatformAlert = alertManager.createAlert(this.testPlatformAlertDef, ctime);
        testPlatformAlert.createActionLog("Notified users:", alertAction, overlord); // Flush
        // the
        // changes
        flushSession();
        Collection<AlertActionLog> alertActionLogs = testPlatformAlert.getActionLog();
        for (AlertActionLog actionLog : alertActionLogs) {
            assertNotNull(actionLog.getSubject());
        }
        alertManager.handleSubjectRemoval(overlord);
        // The underlying DAO uses HQL to do bulk update. This will NOT update
        // the session cache, so a subsequent query will make it seem as though
        // subject is still there. We have to explicitly remove it from cache.
        clearSession();
        // Now retrieve the alert and action log bag again
        Alert updatedAlert = alertManager.findAlertById(testPlatformAlert.getId());
        alertActionLogs = updatedAlert.getActionLog();
        for (AlertActionLog actionLog : alertActionLogs) {
            assertNull(actionLog.getSubject());
        }
    }
}
