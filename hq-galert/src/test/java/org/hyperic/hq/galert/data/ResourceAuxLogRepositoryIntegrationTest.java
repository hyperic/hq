package org.hyperic.hq.galert.data;

import static org.junit.Assert.assertEquals;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.appdef.galerts.ResourceAuxLog;
import org.hyperic.hq.appdef.server.session.ResourceAuxLogPojo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.SimpleAlertAuxLog;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceGroup;
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
public class ResourceAuxLogRepositoryIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ResourceAuxLogRepository resourceAuxLogRepository;

    @Test
    public void testDeleteByDef() {
        long timestamp = System.currentTimeMillis();
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        Resource resource = new Resource();
        resource.setName("Resource1");
        entityManager.persist(resource);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def2);
        GalertLog log = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        entityManager.persist(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        entityManager.persist(log2);
        GalertAuxLog auxLog = new GalertAuxLog(log, new SimpleAlertAuxLog("Descr", timestamp), null);
        entityManager.persist(auxLog);
        GalertAuxLog auxLog2 = new GalertAuxLog(log2, new SimpleAlertAuxLog("Descr", timestamp),
            null);
        entityManager.persist(auxLog2);
        ResourceAuxLogPojo resourceAuxLog = new ResourceAuxLogPojo(auxLog, new ResourceAuxLog(
            "Desc", timestamp, new AppdefEntityID(2, resource.getId())), def1);
        resourceAuxLogRepository.save(resourceAuxLog);
        ResourceAuxLogPojo resourceAuxLog2 = new ResourceAuxLogPojo(auxLog2, new ResourceAuxLog(
            "Desc", timestamp, new AppdefEntityID(2, resource.getId())), def2);
        resourceAuxLogRepository.save(resourceAuxLog2);
        entityManager.flush();
        entityManager.clear();
        resourceAuxLogRepository.deleteByDef(def1);
        assertEquals(Long.valueOf(1), resourceAuxLogRepository.count());
    }

    @Test
    public void testFindByAuxLog() {
        long timestamp = System.currentTimeMillis();
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        Resource resource = new Resource();
        resource.setName("Resource1");
        entityManager.persist(resource);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def1);
        GalertLog log = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        entityManager.persist(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        entityManager.persist(log2);
        GalertAuxLog auxLog = new GalertAuxLog(log, new SimpleAlertAuxLog("Descr", timestamp), null);
        entityManager.persist(auxLog);
        GalertAuxLog auxLog2 = new GalertAuxLog(log2, new SimpleAlertAuxLog("Descr", timestamp),
            null);
        entityManager.persist(auxLog2);
        ResourceAuxLogPojo resourceAuxLog = new ResourceAuxLogPojo(auxLog, new ResourceAuxLog(
            "Desc", timestamp, new AppdefEntityID(2, resource.getId())), def1);
        resourceAuxLogRepository.save(resourceAuxLog);
        ResourceAuxLogPojo resourceAuxLog2 = new ResourceAuxLogPojo(auxLog2, new ResourceAuxLog(
            "Desc", timestamp, new AppdefEntityID(2, resource.getId())), def1);
        resourceAuxLogRepository.save(resourceAuxLog2);
        assertEquals(resourceAuxLog, resourceAuxLogRepository.findByAuxLog(auxLog));
    }
}
