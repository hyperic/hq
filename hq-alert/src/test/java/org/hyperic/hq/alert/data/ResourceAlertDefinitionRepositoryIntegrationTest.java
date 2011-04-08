package org.hyperic.hq.alert.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/alert/data/jpa-integration-test-context.xml" })
public class ResourceAlertDefinitionRepositoryIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    private ResourceTypeAlertDefinition parentdef;

    private Integer resource1 = 1234;

    @Autowired
    private ResourceAlertDefinitionRepository resourceAlertDefinitionRepository;

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
        resourceAlertDefinitionRepository.save(alertdef2);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        alertdef.setResourceTypeAlertDefinition(parentdef);
        resourceAlertDefinitionRepository.save(alertdef);
        ResourceAlertDefinition alertdef3 = new ResourceAlertDefinition();
        alertdef3.setName("High CPU");
        alertdef3.setResource(resource2);
        alertdef3.setEscalation(escalation);
        resourceAlertDefinitionRepository.save(alertdef3);
        List<ResourceAlertDefinition> expected = new ArrayList<ResourceAlertDefinition>();
        expected.add(alertdef2);
        expected.add(alertdef3);
        assertEquals(expected, resourceAlertDefinitionRepository.findByEscalation(escalation));
    }

    @Test
    public void testFindByResource() {
        int resource2 = 555;
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource1);
        resourceAlertDefinitionRepository.save(alertdef2);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        alertdef.setResourceTypeAlertDefinition(parentdef);
        resourceAlertDefinitionRepository.save(alertdef);
        ResourceAlertDefinition alertdef3 = new ResourceAlertDefinition();
        alertdef3.setName("High CPU");
        alertdef3.setResource(resource2);
        resourceAlertDefinitionRepository.save(alertdef3);
        List<ResourceAlertDefinition> expected = new ArrayList<ResourceAlertDefinition>();
        expected.add(alertdef);
        expected.add(alertdef2);
        assertEquals(expected,
            resourceAlertDefinitionRepository.findByResource(resource1, new Sort("name")));
    }

    @Test
    public void testFindByResourceAndResourceTypeAlertDefinition() {
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        alertdef.setResourceTypeAlertDefinition(parentdef);
        resourceAlertDefinitionRepository.save(alertdef);
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource1);
        resourceAlertDefinitionRepository.save(alertdef2);
        assertEquals(alertdef,
            resourceAlertDefinitionRepository.findByResourceAndResourceTypeAlertDefinition(
                resource1, parentdef.getId()));
    }

    @Test
    public void testFindByResourceAndResourceTypeAlertDefinitionNotFound() {
        assertNull(resourceAlertDefinitionRepository.findByResourceAndResourceTypeAlertDefinition(
            resource1, parentdef.getId()));
    }

    @Test
    public void testIsEnabled() {
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High Heap");
        alertdef2.setResource(resource1);
        alertdef2.setActiveStatus(true);
        alertdef2.setEnabledStatus(true);
        resourceAlertDefinitionRepository.save(alertdef2);
        assertTrue(resourceAlertDefinitionRepository.isEnabled(alertdef2.getId()));
    }

    @Test
    public void testSetChildrenActive() {
        int resource2 = 555;
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource1);
        alertdef.setResourceTypeAlertDefinition(parentdef);
        resourceAlertDefinitionRepository.save(alertdef);
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High CPU");
        alertdef2.setResource(resource2);
        alertdef2.setResourceTypeAlertDefinition(parentdef);
        resourceAlertDefinitionRepository.save(alertdef2);
        resourceAlertDefinitionRepository.setChildrenActive(parentdef, true);
        entityManager.flush();
        entityManager.clear();
        assertTrue(resourceAlertDefinitionRepository.findOne(alertdef.getId()).isEnabled());
        assertTrue(resourceAlertDefinitionRepository.findOne(alertdef2.getId()).isEnabled());
    }

    @Test
    public void testSetChildrenEscalation() {
        Escalation escalation = new Escalation("Escalation1", "Important", true, 1l, true, true);
        entityManager.persist(escalation);
        parentdef.setEscalation(escalation);
        int resource2 = 555;
        ResourceAlertDefinition alertdef2 = new ResourceAlertDefinition();
        alertdef2.setName("High CPU");
        alertdef2.setResource(resource1);
        alertdef2.setResourceTypeAlertDefinition(parentdef);
        resourceAlertDefinitionRepository.save(alertdef2);
        ResourceAlertDefinition alertdef = new ResourceAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResource(resource2);
        alertdef.setResourceTypeAlertDefinition(parentdef);
        resourceAlertDefinitionRepository.save(alertdef);
        resourceAlertDefinitionRepository.setChildrenEscalation(parentdef, escalation);
        entityManager.flush();
        entityManager.clear();
        assertEquals(escalation, resourceAlertDefinitionRepository.findOne(alertdef.getId())
            .getEscalation());
        assertEquals(escalation, resourceAlertDefinitionRepository.findOne(alertdef2.getId())
            .getEscalation());
    }
}
