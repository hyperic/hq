package org.hyperic.hq.inventory.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@DirtiesContext
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:META-INF/spring/neo4j-context.xml",
                                   "classpath:org/hyperic/hq/inventory/InventoryIntegrationTest-context.xml" })
public class ConfigIntegrationTest {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private Config config;

    @Before
    public void setUp() {
        config = new Config();
        ConfigType configType =  new ConfigType("Product");
        entityManager.persist(configType);
        configType.getId();
        configType.addConfigOptionType(new ConfigOptionType("user", "A user"));
        config.setType(configType);
    }
    
    @Test
    public void testGetSetValue() {
        assertNull(config.setValue("user", "bob"));
        assertEquals("bob",config.getValue("user"));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGetValueInvalidKey() {
        config.getValue("password");
    }
    
    @Test
    public void testGetDefaultValue() {
        config.getType().getConfigOptionType("user").setDefaultValue("Joe");
        assertEquals("Joe",config.getValue("user"));  
    }
    
    @Test
    public void testGetValues() {
        config.setValue("user", "bob");
        Map<String,Object> expected = new HashMap<String,Object>();
        expected.put("user","bob");
        assertEquals(expected,config.getValues());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSetNullValue() {
        config.setValue("user", null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSetValueInvalidOptionType() {
        config.setValue("password", "foo");
    }
    
    @Test
    public void testSetUpdateValue() {
        assertNull(config.setValue("user", "bob"));
        assertEquals("bob",config.setValue("user", "Jim"));
    }
    
    @Test
    public void testGetConfigTypeDefaultValues() {
        config.getType().getConfigOptionType("user").setDefaultValue("mark");
        Map<String,Object> expected = new HashMap<String,Object>();
        expected.put("user", "mark");
        assertEquals(expected,config.getType().getDefaultConfigValues());
    }
}
