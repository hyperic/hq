package org.hyperic.hq.galert.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.SimpleAlertAuxLog;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.galerts.MetricAuxLog;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MetricAuxLogPojo;
import org.hyperic.hq.measurement.server.session.MonitorableType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/galert/data/jpa-integration-test-context.xml" })
public class MetricAuxLogRepositoryIntegrationTest {

    private GalertAuxLog auxLog;

    private Category category;

    private GalertDef def1;

    @PersistenceContext
    private EntityManager entityManager;

    private ResourceGroup group;

    private GalertLog log;

    private Measurement measurement;

    @Autowired
    private MetricAuxLogRepository metricAuxLogRepository;

    private Resource resource;

    private MeasurementTemplate template;

    private long timestamp;

    private MonitorableType type;

    @Before
    public void setUp() {
        timestamp = System.currentTimeMillis();
        type = new MonitorableType("Tomcat Server", "tomcat");
        entityManager.persist(type);
        category = new Category("Availability");
        entityManager.persist(category);
        this.resource = new Resource();
        resource.setName("Resource1");
        entityManager.persist(resource);
        this.template = new MeasurementTemplate("Queue Size", "Availability", "messages",
            MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true, "service:queueSize", type,
            category, "tomcat");
        entityManager.persist(template);
        this.measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        entityManager.persist(measurement);
        group = new ResourceGroup();
        group.setName("Group2");
        entityManager.persist(group);
        def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        log = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        entityManager.persist(log);
        auxLog = new GalertAuxLog(log, new SimpleAlertAuxLog("Descr", timestamp), null);
        entityManager.persist(auxLog);
    }

    @Test
    public void testDeleteByDef() {
        GalertAuxLog auxLog2 = new GalertAuxLog(log, new SimpleAlertAuxLog("Descr2",
            timestamp - 3000), null);
        entityManager.persist(auxLog2);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def2);
        MetricAuxLogPojo metricAuxLog = new MetricAuxLogPojo(auxLog, new MetricAuxLog("desc",
            timestamp, measurement), def1);
        metricAuxLogRepository.save(metricAuxLog);
        MetricAuxLogPojo metricAuxLog2 = new MetricAuxLogPojo(auxLog2, new MetricAuxLog("desc",
            timestamp, measurement), def2);
        metricAuxLogRepository.save(metricAuxLog2);
        metricAuxLogRepository.deleteByDef(def1);
        entityManager.flush();
        entityManager.clear();
        assertEquals(Long.valueOf(1), metricAuxLogRepository.count());
    }

    @Test
    public void testDeleteByMetricIds() {
        Resource resource2 = new Resource();
        resource2.setName("Resource2");
        entityManager.persist(resource2);
        MeasurementTemplate template2 = new MeasurementTemplate("Free Mem", "Availability", "MB",
            MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true, "service:queueSize", type,
            category, "tomcat");
        entityManager.persist(template2);
        Measurement measurement2 = new Measurement(resource, template2, 1234);
        measurement2.setDsn("freeMem");
        entityManager.persist(measurement2);
        Measurement measurement3 = new Measurement(resource2, template, 1234);
        measurement3.setDsn("usedMem");
        entityManager.persist(measurement3);
        MetricAuxLogPojo metricAuxLog = new MetricAuxLogPojo(auxLog, new MetricAuxLog("desc",
            timestamp, measurement), def1);
        metricAuxLogRepository.save(metricAuxLog);
        MetricAuxLogPojo metricAuxLog2 = new MetricAuxLogPojo(auxLog, new MetricAuxLog("desc",
            timestamp, measurement2), def1);
        metricAuxLogRepository.save(metricAuxLog2);
        MetricAuxLogPojo metricAuxLog3 = new MetricAuxLogPojo(auxLog, new MetricAuxLog("desc",
            timestamp, measurement3), def1);
        metricAuxLogRepository.save(metricAuxLog3);
        entityManager.flush();
        metricAuxLogRepository
            .deleteByMetricIds(Arrays.asList(new Integer[] { measurement.getId(),
                                                            measurement2.getId() }));
        entityManager.flush();
        entityManager.clear();
        assertEquals(Long.valueOf(1), metricAuxLogRepository.count());
    }

    @Test
    public void testFindByAuxLog() {
        GalertAuxLog auxLog2 = new GalertAuxLog(log, new SimpleAlertAuxLog("Descr2",
            timestamp - 3000), null);
        entityManager.persist(auxLog2);
        MetricAuxLogPojo metricAuxLog = new MetricAuxLogPojo(auxLog, new MetricAuxLog("desc",
            timestamp, measurement), def1);
        metricAuxLogRepository.save(metricAuxLog);
        MetricAuxLogPojo metricAuxLog2 = new MetricAuxLogPojo(auxLog2, new MetricAuxLog("desc",
            timestamp, measurement), def1);
        metricAuxLogRepository.save(metricAuxLog2);
        assertEquals(metricAuxLog2, metricAuxLogRepository.findByAuxLog(auxLog2));
    }

    @Test
    public void testFindByAuxLogNone() {
        MetricAuxLogPojo metricAuxLog = new MetricAuxLogPojo(auxLog, new MetricAuxLog("desc",
            timestamp, measurement), def1);
        metricAuxLogRepository.save(metricAuxLog);
        assertNull(metricAuxLogRepository.findByAuxLog(auxLog));
    }

}
