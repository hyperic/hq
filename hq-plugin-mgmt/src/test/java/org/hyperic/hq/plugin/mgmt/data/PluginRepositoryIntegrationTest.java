package org.hyperic.hq.plugin.mgmt.data;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.plugin.mgmt.domain.Plugin;
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
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/plugin/mgmt/data/jpa-integration-test-context.xml" })
public class PluginRepositoryIntegrationTest {
    
    @Autowired
    private PluginRepository pluginRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Test
    public void testFindByResourceType() {
        ResourceType type = new ResourceType("Tomcat Server");
        entityManager.persist(type);
        ResourceType type2 = new ResourceType("Web Module Stats");
        entityManager.persist(type2);
        Plugin plugin = new Plugin("tomcat","/some/place","hash");
        plugin.addResourceType(type);
        plugin.addResourceType(type2);
        pluginRepository.save(plugin);
        pluginRepository.findByResourceType(type);
        
    }
}
