package org.hyperic.hq.galert.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GalertEscalatable;
import org.hyperic.hq.galerts.server.session.GalertLog;
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
        int group = 8888;
        int group2 = 98352;
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
        int group = 8888;
        int group2 = 98352;
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
        int group = 8888;
        int group2 = 98352;
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
        PageRequest pageRequest = new PageRequest(0, 20, new Sort("timestamp"));
        assertEquals(new PageImpl<GalertLog>(expected, pageRequest, 2l),
            galertLogRepository.findByCreateTimeAndPriority(timestamp - 5000, timestamp,
                AlertSeverity.HIGH, false, true, null, null, pageRequest));
    }

    @Test
    public void testFindByCreateTimeAndPriorityAndNotFixedSort() {
        int group = 8888;
        int group2 = 98352;
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
            timestamp, AlertSeverity.HIGH, false, true, null, null, new Sort("timestamp")));
    }

    @Test
    public void testFindByCreateTimeAndPriorityInEscalation() {
        // TODO impl
    }

    @Test
    public void testFindByCreateTimeAndPriorityReqParams() {
        int group = 8888;
        int group2 = 98352;
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
        PageRequest request = new PageRequest(0, 20, new Sort("timestamp"));
        assertEquals(new PageImpl<GalertLog>(expected, request, 3l),
            galertLogRepository.findByCreateTimeAndPriority(timestamp - 5000, timestamp,
                AlertSeverity.HIGH, false, false, null, null, request));
    }

    @Test
    public void testFindByCreateTimeAndPrioritySpecifyDefId() {
        int group = 8888;
        int group2 = 98352;
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
        PageRequest request = new PageRequest(0, 20, new Sort("timestamp"));
        assertEquals(new PageImpl<GalertLog>(expected, request, 1l),
            galertLogRepository.findByCreateTimeAndPriority(timestamp - 5000, timestamp,
                AlertSeverity.HIGH, false, false, null, def2.getId(), request));
    }

    @Test
    public void testFindByCreateTimeAndPrioritySpecifyGroupId() {
        int group = 8888;
        int group2 = 98352;
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
        PageRequest request = new PageRequest(0, 20, new Sort("timestamp"));
        assertEquals(new PageImpl<GalertLog>(expected, request, 1l),
            galertLogRepository.findByCreateTimeAndPriority(timestamp - 5000, timestamp,
                AlertSeverity.HIGH, false, false, group2, null, request));
    }

    @Test
    public void testFindByGroup() {
        int group = 8888;
        int group2 = 98352;
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
        int group = 8888;
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
        int group = 8888;
        long timestamp = System.currentTimeMillis();
        PageRequest request = new PageRequest(0, 3, new Sort("timestamp"));
        assertEquals(new PageImpl<GalertLog>(new ArrayList<GalertLog>(0), request, 0),
            galertLogRepository.findByGroupAndTimestampBetween(group, timestamp - 5000, timestamp,
                request));
    }

    @Test
    public void testFindLastByDefinition() {
        int group = 8888;
        int group2 = 98352;
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
        int group = 8888;
        int group2 = 98352;
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
        int group = 8888;
        int group2 = 98352;
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
        int group = 8888;
        int group2 = 98352;
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
        assertEquals(Long.valueOf(3), galertLogRepository.countByGroup(group));
    }

    @Test
    public void testHasEscalationState() {
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        int group2 = 98352;
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        def2.setEscalation(escalation2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        EscalationState state = new EscalationState(new GalertEscalatable(log));
        entityManager.persist(state);
        assertTrue(galertLogRepository.hasEscalationState(log));
    }

    @Test
    public void testHasNoEscalationState() {
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        int group2 = 98352;
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        def2.setEscalation(escalation2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        assertFalse(galertLogRepository.hasEscalationState(log));
    }

    @Test
    public void testIsAcknowledgable() {
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        int group2 = 98352;
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        def2.setEscalation(escalation2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        EscalationState state = new EscalationState(new GalertEscalatable(log));
        entityManager.persist(state);
        assertTrue(galertLogRepository.isAcknowledgeable(log));
    }

    @Test
    public void testIsAcknowledgableNot() {
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        entityManager.persist(bob);
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        int group2 = 98352;
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        def2.setEscalation(escalation2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        EscalationState state = new EscalationState(new GalertEscalatable(log));
        state.setAcknowledgedBy(bob);
        entityManager.persist(state);
        assertFalse(galertLogRepository.isAcknowledgeable(log));
    }

    @Test
    public void testIsAcknowledged() {
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        entityManager.persist(bob);
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        int group2 = 98352;
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        def2.setEscalation(escalation2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        EscalationState state = new EscalationState(new GalertEscalatable(log));
        state.setAcknowledgedBy(bob);
        entityManager.persist(state);
        assertTrue(galertLogRepository.isAcknowledged(log));
    }

    @Test
    public void testIsAcknowledgedNot() {
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        int group2 = 98352;
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.MEDIUM, true, group2);
        def2.setEscalation(escalation2);
        entityManager.persist(def2);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        galertLogRepository.save(log);
        EscalationState state = new EscalationState(new GalertEscalatable(log));
        entityManager.persist(state);
        assertFalse(galertLogRepository.isAcknowledged(log));
    }
}
