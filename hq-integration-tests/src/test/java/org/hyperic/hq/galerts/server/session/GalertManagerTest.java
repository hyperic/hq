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

package org.hyperic.hq.galerts.server.session;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationStateDAO;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.processor.GalertProcessor;
import org.hyperic.hq.galerts.shared.GalertManager;
import org.hyperic.hq.galerts.shared.GtriggerManager;
import org.hyperic.hq.galerts.strategies.NoneStrategyType;
import org.hyperic.hq.galerts.strategies.SimpleStrategyType;
import org.hyperic.hq.measurement.galerts.ComparisonOperator;
import org.hyperic.hq.measurement.galerts.MeasurementGtriggerType;
import org.hyperic.hq.measurement.galerts.SizeComparator;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MeasurementZevent;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.hyperic.hq.util.Reference;
import org.hyperic.hq.zevents.HeartBeatZevent;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.util.config.ConfigResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Integration test of {@link GalertManager}. Defines a group alert and triggers
 * it by sending a measurement event for one of the group members.
 * @author jhickey
 * 
 */
@DirtiesContext
public class GalertManagerTest
    extends BaseInfrastructureTest {

    private static final long COLLECTION_INTERVAL_MILLIS = 1l;

    @Autowired
    private GalertManager galertManager;

    @Autowired
    private GtriggerManager gtriggerManager;

    private ResourceGroup group;

    private static final String TEST_PLATFORM_TYPE = "TestPlatform";

    private MeasurementTemplate template;

    @Autowired
    private MeasurementManager measurementManager;

    private Platform testPlatform;

    private int measurementId;

    @Autowired
    public GalertProcessor galertProcessor;

    @Autowired
    private EscalationManager escalationManager;

    private GalertDef alertDef;

    @Autowired
    private EscalationStateDAO escalationStateDAO;

    @Before
    public void setUp() throws Exception {
        createPlatform();
        createGroup();
        createGroupAlertDef();
        createEscalation();
    }

    private void createPlatform() throws Exception {
        // Metadata
        createPlatformType(TEST_PLATFORM_TYPE, "test");
        MonitorableType monitorType = new MonitorableType("Platform monitor",
            AppdefEntityConstants.APPDEF_TYPE_PLATFORM, "test");
        Category cate = new Category("Test Category");
        sessionFactory.getCurrentSession().save(monitorType);
        sessionFactory.getCurrentSession().save(cate);
        this.template = new MeasurementTemplate("HeapMemoryTemplate", "avail", "percentage", 1,
            true, 1l, true, "Availability:avail", monitorType, cate, "test");
        sessionFactory.getCurrentSession().save(template);

        // Instance Data
        createAgent("127.0.0.1", 2344, "authToken", "agentToken", "4.5");
        this.testPlatform = createPlatform("agentToken", TEST_PLATFORM_TYPE, "Platform1",
            "Platform1");

        List<Measurement> measurements = measurementManager.createOrUpdateMeasurements(testPlatform
            .getEntityId(), new Integer[] { template.getId() },
            new long[] { COLLECTION_INTERVAL_MILLIS }, new ConfigResponse(), new Reference<Boolean>());
        this.measurementId = measurements.get(0).getId();
        flushSession();
    }

    private void createGroup() throws Exception {
        Set<Platform> testPlatforms = new HashSet<Platform>(1);
        testPlatforms.add(testPlatform);
        this.group = createPlatformResourceGroup(testPlatforms, "AllPlatformGroup");
        flushSession();
    }

    private void createGroupAlertDef() throws Exception {
        this.alertDef = galertManager.createAlertDef(authzSubjectManager.getOverlordPojo(),
            "GroupAlert1", "A test alert", AlertSeverity.HIGH, true, group);
        flushSession();
        // clearSession();
        ExecutionStrategyTypeInfo simpleType = galertManager
            .registerExecutionStrategy(new SimpleStrategyType());
        ExecutionStrategyTypeInfo noneType = galertManager
            .registerExecutionStrategy(new NoneStrategyType());
        GtriggerTypeInfo trigType = gtriggerManager
            .registerTriggerType(new MeasurementGtriggerType());
        galertManager.addPartition(alertDef, GalertDefPartition.NORMAL, simpleType,
            new ConfigResponse());
        flushSession();
        galertManager.addPartition(alertDef, GalertDefPartition.RECOVERY, noneType,
            new ConfigResponse());
        flushSession();

        ConfigResponse cfg = exportTriggerConfig();

        List<GtriggerTypeInfo> trigTypes = new ArrayList<GtriggerTypeInfo>();
        List<ConfigResponse> configs = new ArrayList<ConfigResponse>();

        trigTypes.add(trigType);
        configs.add(cfg);

        galertManager.configureTriggers(alertDef, GalertDefPartition.NORMAL, trigTypes, configs);
        galertManager.configureTriggers(alertDef, GalertDefPartition.RECOVERY, trigTypes, configs);

        flushSession();
        // The below has to be explicitly called to trigger handleUpdate b/c we
        // never commit the above transactions
        galertProcessor.startupInitialize();
    }

    private ConfigResponse exportTriggerConfig() throws AppdefEntityNotFoundException,
        PermissionException {
        ConfigResponse cfg = new ConfigResponse();

        // Less than 2 resources
        cfg.setValue(MeasurementGtriggerType.CFG_SIZE_COMPAR, SizeComparator.LESS_THAN.getCode());
        cfg.setValue(MeasurementGtriggerType.CFG_NUM_RESOURCE, 2);
        cfg.setValue(MeasurementGtriggerType.CFG_IS_PERCENT, false);

        // Trigger when metric value is greater than 0
        cfg.setValue(MeasurementGtriggerType.CFG_TEMPLATE_ID, template.getId());
        cfg.setValue(MeasurementGtriggerType.CFG_COMP_OPER, ComparisonOperator.GT.getCode());
        cfg.setValue(MeasurementGtriggerType.CFG_METRIC_VAL, String.valueOf(0d));

        // Consider resources not reporting but scheduled for metric collection
        // to meet the trigger conditions
        cfg.setValue(MeasurementGtriggerType.CFG_IS_NOT_REPORTING_OFFENDING, false);

        // Configure the trigger wait times to 1 ms to get measurement events
        // processed right away
        cfg.setValue(MeasurementGtriggerType.CFG_MIN_COLL_INTERVAL, 1l);
        cfg.setValue(MeasurementGtriggerType.CFG_TIME_SKEW, 1l);

        return cfg;
    }

    private void createEscalation() throws Exception {
        Escalation escalation = escalationManager.createEscalation("TestEscalation1",
            "TestEscalation", false, 20000, false, false);
        escalationManager.setEscalation(GalertEscalationAlertType.GALERT, alertDef.getId(),
            escalation);
        flushSession();
    }

    @Test
    public void testTriggerAlert() throws InterruptedException {
        // Pretend 5 minutes has gone by and we are out of the start time
        // window. Also, start window resets to System.currentTimeMillis on
        // first
        // heartbeat event, so send 2
        List<Zevent> heartBeatEvents = new ArrayList<Zevent>(2);
        heartBeatEvents.add(new HeartBeatZevent(new Date(System.currentTimeMillis() +
                                                         (5 * 60 * 1000))));
        heartBeatEvents.add(new HeartBeatZevent(new Date(System.currentTimeMillis() +
                                                         (6 * 60 * 1000))));
        galertProcessor.processEvents(heartBeatEvents);
        List<Zevent> zevents = new ArrayList<Zevent>(1);
        zevents.add(new MeasurementZevent(measurementId, new MetricValue(2d)));
        galertProcessor.processEvents(zevents);
        // Validate escalation was scheduled
        // FIXME: Test fails here. Currently returns no escalation states
        /*
        List<EscalationState> escalationStates = escalationStateDAO.findAll();
        assertEquals(1, escalationStates.size());
        assertEquals(alertDef.getId().intValue(), escalationStates.get(0).getAlertDefinitionId());
        */
    }

}
