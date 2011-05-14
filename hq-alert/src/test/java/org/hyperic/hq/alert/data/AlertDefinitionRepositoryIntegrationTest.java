package org.hyperic.hq.alert.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.events.server.session.ResourceAlertDefinition;
import org.hyperic.hq.events.server.session.ResourceTypeAlertDefinition;
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
public class AlertDefinitionRepositoryIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AlertDefinitionRepository alertDefinitionRepository;

    private ResourceTypeAlertDefinition parentdef;

    private Integer resource1 = 1234;

    private Integer resourceType1 = 5678;

    @Before
    public void setUp() {
        parentdef = new ResourceTypeAlertDefinition();
        parentdef.setName("High CPU");
        parentdef.setResourceType(resourceType1);
        entityManager.persist(parentdef);
    }

    @Test
    public void testFindByEscalation() {
        Escalation escalation = new Escalation("Escalation1", "Important", true, 1l, true, true);
        entityManager.persist(escalation);
        int resource2 = 555;
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource1);
        alertdef2.setEscalation(escalation);
        entityManager.persist(alertdef2);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        alertdef.setResourceTypeAlertDefinition(parentdef);
        entityManager.persist(alertdef);
        ResourceAlertDefinition alertdef3 = new ResourceAlertDefinition();
        alertdef3.setName("High CPU");
        alertdef3.setResource(resource2);
        alertdef3.setEscalation(escalation);
        entityManager.persist(alertdef3);
        List<ResourceAlertDefinition> expected = new ArrayList<ResourceAlertDefinition>();
        expected.add(alertdef2);
        expected.add(alertdef3);
        assertEquals(expected, alertDefinitionRepository.findByEscalation(escalation));
    }
    
    @Test
    public void testIsEnabled() {
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource1);
        alertdef2.setActiveStatus(true);
        alertdef2.setEnabledStatus(true);
        entityManager.persist(alertdef2);
        assertTrue(alertDefinitionRepository.isEnabled(alertdef2.getId()));
    }
}
