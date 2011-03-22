package org.hyperic.hq.galert.data;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.SimpleAlertAuxLog;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.Assert.assertEquals;

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
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
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
}
