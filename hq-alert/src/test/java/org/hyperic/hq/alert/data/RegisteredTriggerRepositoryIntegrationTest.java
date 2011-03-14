package org.hyperic.hq.alert.data;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.events.server.session.AlertCondition;
import org.hyperic.hq.events.server.session.AlertDefinitionState;
import org.hyperic.hq.events.server.session.RegisteredTrigger;
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
public class RegisteredTriggerRepositoryIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RegisteredTriggerRepository triggerRepository;

    @Test
    public void testFindAllEnabledTriggers() {
        Resource resource2 = new Resource();
        resource2.setName("Resource 2");
        entityManager.persist(resource2);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource2);
        alertdef.setActiveStatus(true);
        alertdef.setAlertDefinitionState(new AlertDefinitionState(alertdef));
        entityManager.persist(alertdef);
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource2);
        alertdef2.setAlertDefinitionState(new AlertDefinitionState(alertdef2));
        entityManager.persist(alertdef2);
        RegisteredTrigger trigger = new RegisteredTrigger("MyClass", new byte[0], 1234l);
        trigger.setAlertDefinition(alertdef);
        triggerRepository.save(trigger);
        AlertCondition condition = new AlertCondition();
        condition.setTrigger(trigger);
        entityManager.persist(condition);
        alertdef.addCondition(condition);
        RegisteredTrigger trigger2 = new RegisteredTrigger("MyOtherClass", new byte[0], 1234l);
        trigger2.setAlertDefinition(alertdef2);
        triggerRepository.save(trigger2);
        AlertCondition condition2 = new AlertCondition();
        condition2.setTrigger(trigger2);
        entityManager.persist(condition2);
        alertdef2.addCondition(condition2);
        Set<RegisteredTrigger> expected = new HashSet<RegisteredTrigger>();
        expected.add(trigger);
        assertEquals(expected, triggerRepository.findAllEnabledTriggers());
    }

    @Test
    public void testFindByAlertDefinition() {
        Resource resource2 = new Resource();
        resource2.setName("Resource 2");
        entityManager.persist(resource2);
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource2);
        entityManager.persist(alertdef2);
        RegisteredTrigger trigger = new RegisteredTrigger("MyClass", new byte[0], 1234l);
        trigger.setAlertDefinition(alertdef2);
        triggerRepository.save(trigger);
        assertEquals(Collections.singletonList(trigger),
            triggerRepository.findByAlertDefinition(alertdef2.getId()));
    }

    @Test
    public void testFindTriggerIdsByAlertDefinitionIds() {
        Resource resource2 = new Resource();
        resource2.setName("Resource 2");
        entityManager.persist(resource2);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource2);
        alertdef.setActiveStatus(true);
        alertdef.setAlertDefinitionState(new AlertDefinitionState(alertdef));
        entityManager.persist(alertdef);
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource2);
        alertdef2.setAlertDefinitionState(new AlertDefinitionState(alertdef2));
        entityManager.persist(alertdef2);
        RegisteredTrigger trigger = new RegisteredTrigger("MyClass", new byte[0], 1234l);
        trigger.setAlertDefinition(alertdef);
        triggerRepository.save(trigger);
        AlertCondition condition = new AlertCondition();
        condition.setTrigger(trigger);
        entityManager.persist(condition);
        alertdef.addCondition(condition);
        RegisteredTrigger trigger2 = new RegisteredTrigger("MyOtherClass", new byte[0], 1234l);
        trigger2.setAlertDefinition(alertdef2);
        triggerRepository.save(trigger2);
        AlertCondition condition2 = new AlertCondition();
        condition2.setTrigger(trigger2);
        entityManager.persist(condition2);
        alertdef2.addCondition(condition2);
        entityManager.flush();
        Map<Integer, List<Integer>> expected = new HashMap<Integer, List<Integer>>();
        expected.put(alertdef.getId(), Collections.singletonList(trigger.getId()));
        assertEquals(expected, triggerRepository.findTriggerIdsByAlertDefinitionIds(Collections
            .singletonList(alertdef.getId())));
    }
}
