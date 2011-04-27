package org.hyperic.hq.galert.data;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.GalertDef;
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
public class GalertDefRepositoryIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private GalertDefRepository galertDefRepository;

    @Test
    public void testFindAllExcludeDeleted() {
        int group = 8888;
        int group2 = 98352;
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        galertDefRepository.save(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        galertDefRepository.save(def2);
        GalertDef def3 = new GalertDef("Free Mem Low", "desc", AlertSeverity.HIGH, true, group);
        def3.setDeleted(true);
        galertDefRepository.save(def3);
        List<GalertDef> expected = new ArrayList<GalertDef>();
        expected.add(def2);
        expected.add(def1);
        assertEquals(expected, galertDefRepository.findAllExcludeDeletedOrderByName());
    }

    @Test
    public void testFindByEscalation() {
        Escalation escalation = new Escalation("Escalation1", "Important", true, 1l, true, true);
        entityManager.persist(escalation);
        int group = 8888;
        int group2 = 98352;
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        def1.setEscalation(escalation);
        galertDefRepository.save(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        galertDefRepository.save(def2);
        GalertDef def3 = new GalertDef("Free Mem Low", "desc", AlertSeverity.HIGH, true, group);
        def3.setEscalation(escalation);
        galertDefRepository.save(def3);
        List<GalertDef> expected = new ArrayList<GalertDef>();
        expected.add(def1);
        expected.add(def3);
        assertEquals(expected, galertDefRepository.findByEscalation(escalation));
    }

    @Test
    public void testFindByGroup() {
        int group = 8888;
        int group2 = 98352;
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        galertDefRepository.save(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        galertDefRepository.save(def2);
        GalertDef def3 = new GalertDef("Free Mem Low", "desc", AlertSeverity.HIGH, true, group);
        galertDefRepository.save(def3);
        List<GalertDef> expected = new ArrayList<GalertDef>();
        expected.add(def1);
        expected.add(def3);
        assertEquals(expected, galertDefRepository.findByGroup(group));
    }

    @Test
    public void testFindByGroupExcludeDeleted() {
        int group = 8888;
        int group2 = 98352;
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        galertDefRepository.save(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        galertDefRepository.save(def2);
        GalertDef def3 = new GalertDef("Free Mem Low", "desc", AlertSeverity.HIGH, true, group);
        def3.setDeleted(true);
        galertDefRepository.save(def3);
        GalertDef def4 = new GalertDef("Low Disk Space", "desc", AlertSeverity.HIGH, true, group);
        galertDefRepository.save(def4);
        List<GalertDef> expected = new ArrayList<GalertDef>();
        expected.add(def4);
        expected.add(def1);
        assertEquals(expected, galertDefRepository.findByGroupExcludeDeletedOrderByName(group));
    }
}
