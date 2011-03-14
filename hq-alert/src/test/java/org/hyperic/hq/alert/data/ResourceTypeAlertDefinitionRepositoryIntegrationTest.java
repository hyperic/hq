package org.hyperic.hq.alert.data;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import static org.junit.Assert.assertEquals;
import org.hyperic.hq.events.server.session.ResourceTypeAlertDefinition;
import org.hyperic.hq.inventory.domain.ResourceType;
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
public class ResourceTypeAlertDefinitionRepositoryIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    private ResourceType resourceType1;

    private ResourceType resourceType2;

    @Autowired
    private ResourceTypeAlertDefinitionRepository resourceTypeAlertDefinitionRepository;

    @Before
    public void setUp() {
        resourceType1 = new ResourceType("Tomcat");
        entityManager.persist(resourceType1);
        resourceType2 = new ResourceType("JBoss");
        entityManager.persist(resourceType2);
    }

    @Test
    public void testFindByEnabled() {
        ResourceTypeAlertDefinition alertdef = new ResourceTypeAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResourceType(resourceType1);
        alertdef.setActiveStatus(true);
        alertdef.setEnabledStatus(true);
        resourceTypeAlertDefinitionRepository.save(alertdef);

        ResourceTypeAlertDefinition alertdef2 = new ResourceTypeAlertDefinition();
        alertdef2.setName("High CPU");
        alertdef2.setResourceType(resourceType2);
        alertdef2.setEnabledStatus(false);
        resourceTypeAlertDefinitionRepository.save(alertdef2);

        ResourceTypeAlertDefinition alertdef3 = new ResourceTypeAlertDefinition();
        alertdef3.setName("Down");
        alertdef3.setResourceType(resourceType2);
        alertdef3.setActiveStatus(true);
        alertdef3.setEnabledStatus(true);
        resourceTypeAlertDefinitionRepository.save(alertdef3);

        List<ResourceTypeAlertDefinition> expected = new ArrayList<ResourceTypeAlertDefinition>();
        expected.add(alertdef3);
        expected.add(alertdef);
        assertEquals(expected,
            resourceTypeAlertDefinitionRepository.findByEnabled(true, new Sort("name")));
    }

    @Test
    public void testFindByResourceType() {
        ResourceTypeAlertDefinition alertdef = new ResourceTypeAlertDefinition();
        alertdef.setName("High CPU");
        alertdef.setResourceType(resourceType1);
        resourceTypeAlertDefinitionRepository.save(alertdef);

        ResourceTypeAlertDefinition alertdef2 = new ResourceTypeAlertDefinition();
        alertdef2.setName("High CPU");
        alertdef2.setResourceType(resourceType2);
        resourceTypeAlertDefinitionRepository.save(alertdef2);

        List<ResourceTypeAlertDefinition> expected = new ArrayList<ResourceTypeAlertDefinition>();
        expected.add(alertdef);
        assertEquals(expected,
            resourceTypeAlertDefinitionRepository.findByResourceType(resourceType1));
    }
}
