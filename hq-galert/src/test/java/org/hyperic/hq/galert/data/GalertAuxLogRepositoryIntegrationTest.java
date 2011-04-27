package org.hyperic.hq.galert.data;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.SimpleAlertAuxLog;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertAuxLogProvider;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.galerts.MetricAuxLog;
import org.hyperic.hq.measurement.server.session.Category;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MetricAuxLogPojo;
import org.hyperic.hq.measurement.server.session.MonitorableType;
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
public class GalertAuxLogRepositoryIntegrationTest {

    @Autowired
    private GalertAuxLogRepository galertAuxLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void testDeleteByDef() {
        int group2 = 98342;
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        entityManager.persist(log);
        GalertLog log2 = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        entityManager.persist(log2);
        GalertAuxLog auxLog = new GalertAuxLog(log, new SimpleAlertAuxLog("Descr", timestamp), null);
        galertAuxLogRepository.save(auxLog);
        GalertAuxLog auxLog2 = new GalertAuxLog(log2, new SimpleAlertAuxLog("Descr", timestamp),
            null);
        galertAuxLogRepository.save(auxLog2);
        galertAuxLogRepository.deleteByDef(def1);
        entityManager.flush();
        entityManager.clear();
        assertEquals(Long.valueOf(1), galertAuxLogRepository.count());
    }

    @Test
    public void testResetAuxType() {
        long timestamp = System.currentTimeMillis();
        int group2 = 98342;
        int resource = 99999;
        MonitorableType type = new MonitorableType("Tomcat Server", "tomcat");
        entityManager.persist(type);
        Category category = new Category("Availability");
        entityManager.persist(category);
        MeasurementTemplate template = new MeasurementTemplate("Queue Size", "Availability",
            "messages", MeasurementConstants.COLL_TYPE_DYNAMIC, true, 1234, true,
            "service:queueSize", type, category, "tomcat");
        entityManager.persist(template);
        Measurement measurement = new Measurement(resource, template, 1234);
        measurement.setDsn("queueSize");
        entityManager.persist(measurement);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def1);
        GalertLog log = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        entityManager.persist(log);
        GalertAuxLog auxLog = new GalertAuxLog(log, new SimpleAlertAuxLog("Descr", timestamp), null);
        galertAuxLogRepository.save(auxLog);
        MetricAuxLogPojo metricAuxLog = new MetricAuxLogPojo(auxLog, new MetricAuxLog("desc",
            timestamp, measurement), def1);
        entityManager.persist(metricAuxLog);
        galertAuxLogRepository.resetAuxType(Arrays.asList(new Integer[] { measurement.getId() }));
        entityManager.flush();
        entityManager.clear();
        assertEquals(GalertAuxLogProvider.INSTANCE.getCode(),
            galertAuxLogRepository.findOne(auxLog.getId()).getAuxType());
    }
}
