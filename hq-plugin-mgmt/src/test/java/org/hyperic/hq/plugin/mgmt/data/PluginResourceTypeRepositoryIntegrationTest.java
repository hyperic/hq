package org.hyperic.hq.plugin.mgmt.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import net.sf.ehcache.CacheManager;

import org.hyperic.hq.plugin.mgmt.domain.PluginResourceType;
import org.junit.After;
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
public class PluginResourceTypeRepositoryIntegrationTest {

    @Autowired
    private PluginResourceTypeRepository pluginResourceTypeRepository;

    @After
    public void tearDown() {
        CacheManager.getInstance().clearAll();
    }

    @Test
    public void testFindByResourceType() {
        int type = 123;
        int type2 = 456;
        PluginResourceType pluginResourceType = new PluginResourceType("tomcat", type);
        pluginResourceTypeRepository.save(pluginResourceType);
        PluginResourceType pluginResourceType2 = new PluginResourceType("jboss", type2);
        pluginResourceTypeRepository.save(pluginResourceType2);
        PluginResourceType pluginResourceType3 = new PluginResourceType("tomcat", 789);
        pluginResourceTypeRepository.save(pluginResourceType3);
        assertEquals("tomcat", pluginResourceTypeRepository.findNameByResourceType(type));
        verifyQueryCaching("Plugin.findByResourceType");
    }

    @Test
    public void testFindByResourceTypeNone() {
        assertNull(pluginResourceTypeRepository.findNameByResourceType(123));
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }
}
