package org.hyperic.hq.alert.data;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertActionLog;
import org.hyperic.hq.events.server.session.ResourceAlertDefinition;
import org.hyperic.hq.inventory.domain.Resource;
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
public class ActionRepositoryIntegrationTest {

    @Autowired
    private ActionRepository actionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    public void testDeleteByAlertDefinition() {
        Resource resource2 = new Resource();
        resource2.setName("Resource 2");
        entityManager.persist(resource2);
        Action action = new Action("myclass", new byte[0], null);
        actionRepository.save(action);
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource2);
        alertdef2.addAction(action);
        entityManager.persist(alertdef2);
        actionRepository.deleteByAlertDefinition(alertdef2);
        entityManager.flush();
        entityManager.clear();
        assertTrue(actionRepository.findOne(action.getId()).isDeleted());
    }
    
    @Test
    public void testFindByAlert() {
        Resource resource2 = new Resource();
        resource2.setName("Resource 2");
        entityManager.persist(resource2);
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource2);
        entityManager.persist(alertdef2);
        Alert alert = new Alert();
        alert.setAlertDefinition(alertdef2);
        entityManager.persist(alert);
        Action action = new Action("myclass", new byte[0], null);
        actionRepository.save(action);
        AuthzSubject bob = new AuthzSubject(true, "bob", "dev", "bob@bob.com", true, "Bob",
            "Bobbins", "Bob", "123123123", "123123123", false);
        entityManager.persist(bob);
        AlertActionLog actionLog = new AlertActionLog(alert, "Something happened", action, bob);
        entityManager.persist(actionLog);
        List<Action> expected = new ArrayList<Action>();
        expected.add(action);
        assertEquals(expected,actionRepository.findByAlert(alert));
    }
}
