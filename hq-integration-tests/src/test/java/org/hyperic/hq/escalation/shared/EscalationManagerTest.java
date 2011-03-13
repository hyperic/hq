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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationManagerImpl;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.ClassicEscalatableCreator;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.events.server.session.ResourceAlertDefinition;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.messaging.Messenger;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Integration test of the {@link EscalationManagerImpl}
 * @author trader
 * 
 */

@DirtiesContext
public class EscalationManagerTest
    extends BaseInfrastructureTest {

    @Autowired
    private EscalationManager eManager;

    @Autowired
    private AlertManager aManager;

    @Autowired
    private AlertDefinitionManager aDefManager;

    @Autowired
    private AgentManager agentManager;

    private Server testServer;

    private Service testService;

    @Autowired
    private Messenger messenger;

    private AlertDefinition testPlatformAlertDef;

    private Platform testPlatform;

    @Before
    public void initializeTestData() throws ApplicationException, NotFoundException {
        // Setup Agent
        String agentToken = "agentToken123";
        agentManager.createLegacyAgent("127.0.0.1", 2144, "authToken", agentToken, "5.0");
        this.testPlatform = createResources(agentToken);
        createResourceGroup();
        createResourceAlertDefs();
        // createResourceAlerts();
        // Manual flush is required in any method in which you are updating the
        // Hibernate session in
        // order to avoid false positive in test
        getCurrentSession().flush();
    }

    @Test
    public void testCRUDOperations() throws ApplicationException {

        String name = "TestEscalation1";
        String description = "Test Escalation1";
        boolean pauseAllowed = true;
        long maxPauseTime = 20000;
        boolean notifyAll = false;
        boolean repeat = true;

        // Create/read...
        Escalation e = eManager.createEscalation(name, description, pauseAllowed, maxPauseTime,
            notifyAll, repeat);
        assertNotNull("Unexpected null result", e);
        assertEquals("Invalid return value from getName()", e.getName(), name);
        assertEquals("Invalid return value from getDescription()", e.getDescription(), description);
        assertEquals("Invalid return value from isPauseAllowed()", e.isPauseAllowed(), pauseAllowed);
        assertEquals("Invalid return value from getMaxWaitTime()", e.getMaxPauseTime(),
            maxPauseTime);
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
        eManager.updateEscalation(subj, e, name, description, pauseAllowed, maxPauseTime,
            notifyAll, repeat);
        assertEquals("Invalid return value from getName()", e.getName(), name);
        assertEquals("Invalid return value from getDescription()", e.getDescription(), description);
        assertEquals("Invalid return value from isPauseAllowed()", e.isPauseAllowed(), pauseAllowed);
        assertEquals("Invalid return value from getMaxWaitTime()", e.getMaxPauseTime(),
            maxPauseTime);
        assertEquals("Invalid return value from isNotifyAll()", e.isNotifyAll(), notifyAll);
        assertEquals("Invalid return value from isRepeat()", e.isRepeat(), repeat);

        eManager.deleteEscalation(subj, e);

    }

    @Test
    public void testFindOperations() throws ApplicationException {

        String name = "TestEscalation2";
        String description = "Test Escalation2";
        boolean pauseAllowed = true;
        long maxPauseTime = 20000;
        boolean notifyAll = false;
        boolean repeat = true;

        // Create/read...
        Escalation origEsc = eManager.createEscalation(name, description, pauseAllowed,
            maxPauseTime, notifyAll, repeat);
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
        assertFalse("Expected non-empty collection from findAll()", all.size() == 0);

        boolean found = false;
        for (Escalation esc : all) {
            if (esc.equals(origEsc)) {
                found = true;
            }
        }

        assertTrue("Escalation not returned from findAll()", found);

    }

    @Test
    public void testStartStopEscalation() throws ApplicationException {

        TriggerFiredEvent[] triggerFiredEvents = new TriggerFiredEvent[1];
        triggerFiredEvents[0] = new TriggerFiredEvent(testPlatform.getId(), new MockEvent(1001,
            testPlatform.getId()));
        AlertConditionsSatisfiedZEvent ev = new AlertConditionsSatisfiedZEvent(
            testPlatformAlertDef.getId(), triggerFiredEvents);
        ClassicEscalatableCreator creator = new ClassicEscalatableCreator(testPlatformAlertDef, ev,
            messenger, aManager);
        Escalation e = eManager.createEscalation("TestEscalation", "Test Escalation 3", true, 1000,
            false, true);
        eManager.setEscalation(ClassicEscalationAlertType.CLASSIC, testPlatformAlertDef.getId(), e);

        assertTrue(eManager.startEscalation(testPlatformAlertDef, creator));
        eManager.endEscalation(testPlatformAlertDef);

    }

    @Test
    public void testAckAndFixAlert() throws ApplicationException {

        ResourceAlertDefinition def = createResourceAlertDefinition(testPlatform.getId(),
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, "Platform Down");
        aDefManager.updateAlertDefinitionActiveStatus(authzSubjectManager.getOverlordPojo(), def,
            true);

        AlertFiredEvent event = new AlertFiredEvent(123, def.getId(), testPlatform.getEntityId().getId(),
            "Platform Down", System.currentTimeMillis(), "Firing Alert-123");
        TriggerFiredEvent triggerFired = new TriggerFiredEvent(15, event);
        AlertConditionsSatisfiedZEvent alertZEvent = new AlertConditionsSatisfiedZEvent(
            def.getId(), new TriggerFiredEvent[] { triggerFired });
        new ClassicEscalatableCreator(def, alertZEvent, messenger, aManager);
        Escalation e = eManager.createEscalation("TestEscalation", "Test Escalation 3", true, 1000,
            false, true);
        eManager.setEscalation(ClassicEscalationAlertType.CLASSIC, def.getId(), e);
        aManager.fireAlert(alertZEvent);

        List<EscalationState> escalationStates = eManager.getActiveEscalations(10);
        assertNotNull("Expected non-null list of EscalationStates", escalationStates);
        assertEquals("Expected exactly one EscalationState object in list", 1,
            escalationStates.size());

        Alert a = aManager.findLastUnfixedByDefinition(authzSubjectManager.getOverlordPojo(),
            def.getId());
        assertNotNull("Alert should not be null", a);
        assertEquals(a.getDefinition().getId(), def.getId());
        assertTrue(eManager.isAlertAcknowledgeable(a.getId(), def));
        eManager.acknowledgeAlert(authzSubjectManager.getOverlordPojo(),
            ClassicEscalationAlertType.CLASSIC, a.getId(), "more info", 10);
        // ACK'ed alert should still be unfixed
        a = aManager
            .findLastUnfixedByDefinition(authzSubjectManager.getOverlordPojo(), def.getId());
        assertNotNull("Acknowledged alert should not be null", a);
        assertTrue("Expected true return value from fixAlert",
            eManager.fixAlert(authzSubjectManager.getOverlordPojo(), def, ""));

    }

    private ResourceAlertDefinition createResourceAlertDefinition(Integer appdefId,
                                                                  Integer appdefType,
                                                                  String alertDefName)
        throws AlertDefinitionCreateException, PermissionException {
        AlertDefinitionValue alertDefValue = new AlertDefinitionValue();
        alertDefValue.setName(alertDefName);
        alertDefValue.setAppdefId(appdefId);
        alertDefValue.setAppdefType(appdefType);

        return aDefManager.createResourceAlertDefinition(authzSubjectManager.getOverlordPojo(),
            alertDefValue);
    }

    private Platform createResources(String agentToken) throws ApplicationException,
        NotFoundException {
        // Create PlatformType
        String platformType = "Linux";
        createPlatformType(platformType);
        // Create test platform
        Platform testPlatform = createPlatform(agentToken, platformType, "leela.local",
            "leela.local", 2);
        // Create ServerType
        ServerType testServerType = createServerType("Tomcat", "6.0", new String[] { "Linux" });
        // Create test server
        this.testServer = createServer(testPlatform, testServerType, "Server1");
        // Create ServiceType
        ServiceType serviceType = createServiceType("Spring JDBC Template", testServerType);
        // Create test service
        this.testService = serviceManager.createService(authzSubjectManager.getOverlordPojo(),
            testServer.getId(), serviceType.getId(), "leela.local jdbcTemplate",
            "Spring JDBC Template", "my computer");
        return testPlatform;
    }

    private ResourceGroup createResourceGroup() throws ApplicationException, PermissionException {
        Resource platformRes = testPlatform.getResource();
        Resource serverRes = testServer.getResource();
        Resource serviceRes = testService.getResource();

        List<Resource> resources = new ArrayList<Resource>();
        resources.add(platformRes);
        resources.add(serverRes);
        resources.add(serviceRes);
        return createMixedGroup("AllResourcesGroup",
            AppdefEntityConstants.APPDEF_TYPE_GROUP_ADHOC_PSS, resources);
    }

    private void createResourceAlertDefs() throws AlertDefinitionCreateException,
        PermissionException {
        // Create Platform Alert Definition
        this.testPlatformAlertDef = createResourceAlertDefinition(testPlatform.getId(),
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, "Platform Down");
        // Create Server Alert Definition
        Integer serverAppDefId = testServer.getId();
        createResourceAlertDefinition(serverAppDefId, AppdefEntityConstants.APPDEF_TYPE_SERVER,
            "Server Down");
        // Create Service Alert Definition
        Integer serviceAppDefId = testService.getId();
        createResourceAlertDefinition(serviceAppDefId, AppdefEntityConstants.APPDEF_TYPE_SERVICE,
            "Service Down");
    }

}
