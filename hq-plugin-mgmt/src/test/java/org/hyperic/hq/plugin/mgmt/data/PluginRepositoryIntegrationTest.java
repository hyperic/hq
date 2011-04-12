package org.hyperic.hq.plugin.mgmt.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    @Test
    public void testFindByName() {
        Plugin plugin = new Plugin("tomcat", "/some/place", "hash");
        pluginRepository.save(plugin);
        Plugin plugin2 = new Plugin("weblogic", "/some/place", "hash");
        pluginRepository.save(plugin2);
        assertEquals(plugin, pluginRepository.findByName("tomcat"));
    }

    @Test
    public void testFindByNameNonexistent() {
        assertNull(pluginRepository.findByName("tomcat"));
    }

}
