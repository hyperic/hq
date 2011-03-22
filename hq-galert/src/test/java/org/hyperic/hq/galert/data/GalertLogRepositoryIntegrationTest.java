package org.hyperic.hq.galert.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/galert/data/jpa-integration-test-context.xml" })
public class GalertLogRepositoryIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private GalertLogRepository galertLogRepository;

    @Test
    public void testDeleteByDef() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        log4.setFixed(true);
        galertLogRepository.save(log4);
        galertLogRepository.deleteByDef(def1);
        assertEquals(Long.valueOf(1), galertLogRepository.count());
    }

    @Test
    public void testDeleteByGroup() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        log4.setFixed(true);
        galertLogRepository.save(log4);
        galertLogRepository.deleteByGroup(group);
        assertEquals(Long.valueOf(1), galertLogRepository.count());
    }

    @Test
    public void testFindByCreateTimeAndPriorityAndNotFixed() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        log4.setFixed(true);
        galertLogRepository.save(log4);
        List<GalertLog> expected = new ArrayList<GalertLog>();
        expected.add(log3);
        expected.add(log2);
        assertEquals(expected, galertLogRepository.findByCreateTimeAndPriority(timestamp - 5000,
            timestamp, AlertSeverity.HIGH, false, true, null, null, new PageRequest(0, 20,
                new Sort("timestamp"))));
    }

    @Test
    public void testFindByCreateTimeAndPriorityInEscalation() {
        // TODO impl
    }

    @Test
    public void testFindByCreateTimeAndPriorityReqParams() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        log4.setFixed(true);
        galertLogRepository.save(log4);
        List<GalertLog> expected = new ArrayList<GalertLog>();
        expected.add(log3);
        expected.add(log2);
        expected.add(log4);
        assertEquals(expected, galertLogRepository.findByCreateTimeAndPriority(timestamp - 5000,
            timestamp, AlertSeverity.HIGH, false, false, null, null, new PageRequest(0, 20,
                new Sort("timestamp"))));
    }

    @Test
    public void testFindByCreateTimeAndPrioritySpecifyDefId() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        log4.setFixed(true);
        galertLogRepository.save(log4);
        List<GalertLog> expected = new ArrayList<GalertLog>();
        expected.add(log);
        assertEquals(expected, galertLogRepository.findByCreateTimeAndPriority(timestamp - 5000,
            timestamp, AlertSeverity.HIGH, false, false, null, def2.getId(), new PageRequest(0, 20,
                new Sort("timestamp"))));
    }

    @Test
    public void testFindByCreateTimeAndPrioritySpecifyGroupId() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        log4.setFixed(true);
        galertLogRepository.save(log4);
        List<GalertLog> expected = new ArrayList<GalertLog>();
        expected.add(log);
        assertEquals(expected, galertLogRepository.findByCreateTimeAndPriority(timestamp - 5000,
            timestamp, AlertSeverity.HIGH, false, false, group2.getId(), null, new PageRequest(0,
                20, new Sort("timestamp"))));
    }

    @Test
    public void testFindByGroup() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def2, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        List<GalertLog> expected = new ArrayList<GalertLog>();
        expected.add(log2);
        expected.add(log);
        assertEquals(expected, galertLogRepository.findByDefGroupOrderByTimestampAsc(group));
    }

    @Test
    public void testFindByGroupAndTimestampBetween() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        galertLogRepository.save(log4);
        List<GalertLog> expected = new ArrayList<GalertLog>();
        expected.add(log3);
        expected.add(log2);
        expected.add(log4);
        PageRequest request = new PageRequest(0, 3, new Sort("timestamp"));
        assertEquals(new PageImpl<GalertLog>(expected, request, 4),
            galertLogRepository.findByGroupAndTimestampBetween(group, timestamp - 5000, timestamp,
                request));
    }

    @Test
    public void testFindByGroupAndTimestampBetweenNoResults() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        long timestamp = System.currentTimeMillis();
        PageRequest request = new PageRequest(0, 3, new Sort("timestamp"));
        assertEquals(new PageImpl<GalertLog>(new ArrayList<GalertLog>(0), request, 0),
            galertLogRepository.findByGroupAndTimestampBetween(group, timestamp - 5000, timestamp,
                request));
    }

    @Test
    public void testFindLastByDefinition() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        log2.setFixed(true);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def2, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        log4.setFixed(true);
        galertLogRepository.save(log4);
        assertEquals(log4, galertLogRepository.findLastByDefinition(def1, true));
    }

    @Test
    public void testFindLastByDefinitionNone() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def2, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        galertLogRepository.save(log4);
        assertNull(galertLogRepository.findLastByDefinition(def1, true));
    }

    @Test
    public void testFindUnfixedByGroupAndTimestampBetween() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        log4.setFixed(true);
        galertLogRepository.save(log4);
        Set<GalertLog> expected = new HashSet<GalertLog>();
        expected.add(log2);
        expected.add(log3);
        assertEquals(
            expected,
            new HashSet<GalertLog>(galertLogRepository.findUnfixedByGroupAndTimestampBetween(group,
                timestamp - 5000, timestamp)));
    }

    @Test
    public void testGetCountByGroup() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        GalertLog log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        galertLogRepository.save(log2);
        GalertLog log3 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 5000);
        galertLogRepository.save(log3);
        GalertLog log4 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 2000);
        log4.setFixed(true);
        galertLogRepository.save(log4);
        assertEquals(Long.valueOf(3), galertLogRepository.getCountByGroup(group));
    }
}
