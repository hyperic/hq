package org.hyperic.hq.alert.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertActionLog;
import org.hyperic.hq.events.server.session.ResourceAlertDefinition;
import org.hyperic.hq.inventory.domain.Resource;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/alert/data/jpa-integration-test-context.xml" })
public class AlertActionLogRepositoryIntegrationTest {

    @Autowired
    private AlertActionLogRepository alertActionLogRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Alert alert;

    private AuthzSubject bob;

    private AlertActionLog actionLog;

    private AlertActionLog actionLog2;

    @Before
    public void setUp() {
        Resource resource2 = new Resource();
        resource2.setName("Resource 2");
        entityManager.persist(resource2);
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource2);
        entityManager.persist(alertdef2);
        alert = new Alert();
        alert.setAlertDefinition(alertdef2);
        entityManager.persist(alert);
        Action action = new Action("myclass", new byte[0], null);
        entityManager.persist(action);
        bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob", "Bobbins", "Bob",
            "123123123", "123123123", false);
        entityManager.persist(bob);
        actionLog = new AlertActionLog(alert, "Something happened", action, bob);
        alertActionLogRepository.save(actionLog);
        actionLog2 = new AlertActionLog(alert, "Something Else happened", action, bob);
        alertActionLogRepository.save(actionLog2);
    }

    @Test
    public void testDeleteAlertActions() {
        alertActionLogRepository.deleteAlertActions(Collections.singletonList(alert));
        assertEquals(Long.valueOf(0), alertActionLogRepository.count());
    }

    @Test
    public void testRemoveSubject() {
        alertActionLogRepository.removeSubject(bob);
        assertNull(alertActionLogRepository.findById(actionLog.getId()).getSubject());
        assertNull(alertActionLogRepository.findById(actionLog2.getId()).getSubject());
    }
}
