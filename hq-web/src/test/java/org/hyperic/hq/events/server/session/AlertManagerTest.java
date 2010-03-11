package org.hyperic.hq.events.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
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
    private AgentManager agentManager;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private ServerManager serverManager;

    @Autowired
    private ServiceManager serviceManager;

    private Alert testAlert;

    private AlertDefinition testAlertDefinition;
   

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

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        String agentToken = "agentToken123";
        agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        String platformType = "Linux";
        Platform testPlatform = createPlatform(agentToken, platformType, "leela.local");
        // For this test, we really don't need a server and service. Could just
        // alert against the platform. Creating these here to provide an example
        // for future tests.
        ServerType testServerType = createServerType("Tomcat", "6.0", new String[] { "Linux" },
            "Test Server Plugin");
        Server testServer = createServer(testPlatform, testServerType);
        ServiceType serviceType = createServiceType("Spring JDBC Template", "Test Server Plugin",
            testServerType);
        Service testService = serviceManager.createService(authzSubjectManager.getOverlordPojo(),
            testServer.getId(), serviceType.getId(), "leela.local jdbcTemplate",
            "Spring JDBC Template", "my computer");
        this.testAlertDefinition = createAlertDefinition(testService.getId(),
            AppdefEntityConstants.APPDEF_TYPE_SERVICE, "Platform Down");
        this.testAlert = alertManager.createAlert(this.testAlertDefinition, System
            .currentTimeMillis());
        // Manual flush is required in any method in which you are updating the
        // Hibernate session in
        // order to avoid false positive in test
        sessionFactory.getCurrentSession().flush();
    }

    @Test
    public void testFindAlertById() {
        Alert alert = alertManager.findAlertById(testAlert.getId());
        assertNotNull(alert);
        assertFalse(alert.isAckable());
    }

    @Test
    public void testSetAlertFixed() {
        alertManager.setAlertFixed(testAlert);
        // flush the update
        sessionFactory.getCurrentSession().flush();
        // retrieve the alert and check if it was fixed
        assertTrue(alertManager.findAlertById(testAlert.getId()).isFixed());
    }

    @Test
    public void testDeleteAlerts() {
        alertManager.deleteAlerts(new Integer[] { testAlert.getId() });
        // The underlying DAO uses HQL to do bulk delete. This will NOT update
        // the session cache, so a subsequent query will make it seem as though
        // alert is still there. We have to explicitly remove it from cache.
        sessionFactory.getCurrentSession().clear();
        // verify alert cannot be loaded from DB
        assertNull(alertManager.getById(testAlert.getId()));
    }

    @Test
    public void testFindLastFixedByDefinition() {
        alertManager.setAlertFixed(testAlert);
        // flush the update
        sessionFactory.getCurrentSession().flush();
        Alert lastFixed = alertManager.findLastFixedByDefinition(testAlertDefinition);
        assertEquals(testAlert.getId(), lastFixed.getId());
    }

    @Test
    public void testFindLastFixedNoneFixed() {
        assertNull(alertManager.findLastFixedByDefinition(testAlertDefinition));
    }

    @Test
    public void testFindLastUnfixedByDefinition() {
        Alert lastUnfixed = alertManager.findLastUnfixedByDefinition(authzSubjectManager
            .getOverlordPojo(), testAlertDefinition.getId());
        assertEquals(testAlert.getId(), lastUnfixed.getId());
    }

    @Test
    public void testFindLastUnfixedWhenFixed() {
        alertManager.setAlertFixed(testAlert);
        // flush the update
        sessionFactory.getCurrentSession().flush();
        Alert lastUnfixed = alertManager.findLastUnfixedByDefinition(authzSubjectManager
            .getOverlordPojo(), testAlertDefinition.getId());
        assertNull(lastUnfixed);
    }

}
