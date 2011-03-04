package org.hyperic.hq.hqu.data;

import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/hqu/data/jpa-integration-test-context.xml" })
public class UIPluginRepositoryIntegrationTest {

    @Autowired
    private UIPluginRepository uiPluginRepository;

    @Test
    public void testFindByName() {
        UIPlugin plugin = new UIPlugin("mass", "2.0");
        uiPluginRepository.save(plugin);
        assertEquals(plugin, uiPluginRepository.findByName("mass"));
    }

    @Test
    public void testFindByNameNotFound() {
        assertNull(uiPluginRepository.findByName("mass"));
    }
}
