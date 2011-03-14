package org.hyperic.hq.config.data;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.CacheManager;

import org.hyperic.hq.common.ConfigProperty;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.Assert.assertEquals;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:org/hyperic/hq/config/data/jpa-integration-test-context.xml" })
public class ConfigPropertyRepositoryIntegrationTest {

    @Autowired
    private ConfigPropertyRepository configPropertyRepository;

    private ConfigProperty prop1;

    private ConfigProperty prop2;

    private ConfigProperty prop3;

    @Before
    public void setUp() {
        prop1 = new ConfigProperty();
        prop1.setPrefix("pre:");
        prop1.setKey("cacheSize");
        prop1.setValue("10");
        prop1.setDefaultValue("5");
        configPropertyRepository.save(prop1);

        prop2 = new ConfigProperty();
        prop2.setPrefix("pre:");
        prop2.setKey("maxUsers");
        prop2.setValue("10");
        prop2.setDefaultValue("5");
        configPropertyRepository.save(prop2);

        prop3 = new ConfigProperty();
        prop3.setPrefix("xx");
        prop3.setKey("maxPasswordLength");
        prop3.setValue("10");
        prop3.setDefaultValue("5");
        configPropertyRepository.save(prop3);
    }

    @After
    public void tearDown() {
        CacheManager.getInstance().clearAll();
    }

    @Test
    public void testFindAllAndCache() {
        List<ConfigProperty> expected = new ArrayList<ConfigProperty>();
        expected.add(prop1);
        expected.add(prop2);
        expected.add(prop3);
        assertEquals(expected, configPropertyRepository.findAllAndCache());
        verifyQueryCaching("org.hyperic.hq.common.ConfigProperty.findAll");
    }

    @Test
    public void testFindByPrefix() {
        List<ConfigProperty> expected = new ArrayList<ConfigProperty>();
        expected.add(prop1);
        expected.add(prop2);
        assertEquals(expected, configPropertyRepository.findByPrefix("pre:"));
    }

    private void verifyQueryCaching(String cacheName) {
        assertEquals(1, CacheManager.getInstance().getCache(cacheName).getSize());
    }
}
