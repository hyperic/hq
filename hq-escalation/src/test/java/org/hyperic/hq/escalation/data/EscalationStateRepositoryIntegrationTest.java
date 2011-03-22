package org.hyperic.hq.escalation.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.sf.ehcache.CacheManager;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GalertEscalatable;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.inventory.domain.ResourceGroup;
import org.junit.After;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/escalation/data/jpa-integration-test-context.xml" })
public class EscalationStateRepositoryIntegrationTest {

    private GalertDef def1;

    @PersistenceContext
    private EntityManager entityManager;

    private Escalation escalation;

    @Autowired
    private EscalationStateRepository escalationStateRepository;

    private GalertLog log2;

    private EscalationState state;

    @Before
    public void setUp() {
        long timestamp = System.currentTimeMillis();
        escalation = new Escalation("Escalation1", "Important", true, 1l, true, true);
        entityManager.persist(escalation);
        Escalation escalation2 = new Escalation("Escalation2", "Important", true, 1l, true, true);
        entityManager.persist(escalation2);
        ResourceGroup group = new ResourceGroup();
        group.setName("Group2");
        entityManager.persist(group);
        def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        def1.setEscalation(escalation);
        entityManager.persist(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group);
        def2.setEscalation(escalation2);
        entityManager.persist(def2);
        log2 = new GalertLog(def1, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        entityManager.persist(log2);
        GalertLog log = new GalertLog(def2, new ExecutionReason("Threshold Exceeded Again",
            "Something bad happened again", null, GalertDefPartition.NORMAL), timestamp - 3000);
        entityManager.persist(log);
        state = new EscalationState(new GalertEscalatable(log2));
        escalationStateRepository.save(state);
        EscalationState state2 = new EscalationState(new GalertEscalatable(log));
        escalationStateRepository.save(state2);
    }

    @After
    public void tearDown() {
        CacheManager.getInstance().clearAll();
    }

    @Test
    public void testDeleteByIds() {
        escalationStateRepository.deleteByIds(Arrays.asList(new Integer[] { state.getId() }));
        entityManager.flush();
        entityManager.clear();
        assertEquals(Long.valueOf(1), escalationStateRepository.count());
    }

    @Test
    public void testFindByAlertDefAndAlertType() {
        assertEquals(state, escalationStateRepository.findByAlertDefAndAlertType(def1.getId(),
            GalertEscalationAlertType.GALERT.getCode()));
        verifyQueryCaching("EscalationState.findByTypeAndDef");
    }

    @Test
    public void testFindByAlertIdAndAlertType() {
        assertEquals(state, escalationStateRepository.findByAlertIdAndAlertType(log2.getId(),
            GalertEscalationAlertType.GALERT.getCode()));
    }

    @Test
    public void testFindByEscalation() {
        List<EscalationState> expected = new ArrayList<EscalationState>();
        expected.add(state);
        assertEquals(expected, escalationStateRepository.findByEscalation(escalation));
    }

    @Test
    public void testRemoveAcknowledgedBy() {
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        entityManager.persist(bob);
        state.setAcknowledgedBy(bob);
        escalationStateRepository.removeAcknowledgedBy(bob);
        entityManager.flush();
        entityManager.clear();
        assertNull(escalationStateRepository.findById(state.getId()).getAcknowledgedBy());
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }
}
