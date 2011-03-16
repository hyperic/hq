package org.hyperic.hq.galert.data;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.GalertDef;
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
public class GalertDefRepositoryIntegrationTest {

    @Autowired
    private GalertDefRepository galertDefRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void testFindByGroup() {
        ResourceGroup group = new ResourceGroup();
        group.setName("Group1");
        entityManager.persist(group);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group);
        galertDefRepository.save(def1);
        GalertDef def2 = new GalertDef("CPU High", "desc", AlertSeverity.HIGH, true, group2);
        galertDefRepository.save(def2);
        GalertDef def3 = new GalertDef("Free Mem Low", "desc", AlertSeverity.HIGH, true, group);
        galertDefRepository.save(def3);
        List<GalertDef> expected = new ArrayList<GalertDef>();
        expected.add(def1);
        expected.add(def3);
        assertEquals(expected,galertDefRepository.findByGroup(group));
    }
}
