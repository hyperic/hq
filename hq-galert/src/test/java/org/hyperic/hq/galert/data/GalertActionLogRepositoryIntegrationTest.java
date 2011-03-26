package org.hyperic.hq.galert.data;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.galerts.server.session.ExecutionReason;
import org.hyperic.hq.galerts.server.session.GalertActionLog;
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
import static org.junit.Assert.assertNull;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/galert/data/jpa-integration-test-context.xml" })
public class GalertActionLogRepositoryIntegrationTest {
    
    @Autowired
    private GalertActionLogRepository galertActionLogRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Test
    public void testRemoveSubject() {
        Action action = new Action("MyActionClass", new byte[0], null);
        entityManager.persist(action);
        ResourceGroup group2 = new ResourceGroup();
        group2.setName("Group2");
        entityManager.persist(group2);
        GalertDef def1 = new GalertDef("Platforms Down", "desc", AlertSeverity.HIGH, true, group2);
        entityManager.persist(def1);
        long timestamp = System.currentTimeMillis();
        GalertLog log = new GalertLog(def1, new ExecutionReason("Threshold Exceeded",
            "Something bad happened", null, GalertDefPartition.NORMAL), timestamp);
        entityManager.persist(log);
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        entityManager.persist(bob);
        GalertActionLog actionLog= new GalertActionLog(log, "Some details", action, bob);
        galertActionLogRepository.save(actionLog);
        galertActionLogRepository.removeSubject(bob);
        galertActionLogRepository.flush();
        entityManager.clear();
        assertNull(galertActionLogRepository.findOne(actionLog.getId()).getSubject());
    }
}
