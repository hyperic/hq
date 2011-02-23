package org.hyperic.hq.product.server.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.hyperic.hq.inventory.domain.ConfigOptionType;
import org.hyperic.hq.inventory.domain.ConfigType;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class ProductManagerTest extends BaseInfrastructureTest {

    @Test
    public void testConfigUpdateOnPluginDeployment() {
        //check if config was properly parsed on deployment of test plugin
        ResourceType platType = resourceManager.findResourceTypeByName("PluginTestPlatform");
        ConfigType measType = platType.getConfigType(ProductPlugin.TYPE_MEASUREMENT);
        assertEquals(2,measType.getConfigOptionTypes().size());
        ConfigOptionType url = measType.getConfigOptionType("url");
        //hidden config options don't have descriptions
        assertEquals("",url.getDescription());
        assertTrue(url.isHidden());
        ConfigOptionType pwd = measType.getConfigOptionType("password");
        assertEquals("User Password",pwd.getDescription());
        assertTrue(pwd.isSecret());
        ConfigType productType = platType.getConfigType(ProductPlugin.TYPE_PRODUCT);
        assertEquals(1,productType.getConfigOptionTypes().size());
        ConfigOptionType displayName = productType.getConfigOptionType("DisplayName");
        assertEquals("Display Name",displayName.getDescription());
        assertFalse(displayName.isHidden());
        assertFalse(displayName.isSecret());
        
        ResourceType servType = resourceManager.findResourceTypeByName("PluginTestServer 1.0");
        ConfigType controlType = servType.getConfigType(ProductPlugin.TYPE_CONTROL);
        assertEquals(1,controlType.getConfigOptionTypes().size());
        ConfigOptionType svcName = controlType.getConfigOptionType("service_name");
        assertEquals("Fake Service Name",svcName.getDescription());
        assertEquals("FakeService",svcName.getDefaultValue());
    }
    
    @Test
    public void testPropertyUpdateOnPluginDeployment() {
        //check if properties were properly parsed on deployment of test plugin
        ResourceType servType = resourceManager.findResourceTypeByName("PluginTestServer 1.0");
        PropertyType vendor = servType.getPropertyType("Vendor");
        assertEquals("The Vendor",vendor.getDescription());
        PropertyType internal = servType.getPropertyType("Internal");
        assertTrue(internal.isHidden());
        PropertyType adminPwd = servType.getPropertyType("Admin Password");
        assertTrue(adminPwd.isSecret());
    }
    
    @Test
    public void testOperationUpdateOnPluginDeployment() {
        ResourceType svcType = resourceManager.findResourceTypeByName("PluginTestServer 1.0 Web Module Stats");
        assertEquals(3,svcType.getOperationTypes().size());
        assertNotNull(svcType.getOperationType("stop"));
        assertNotNull(svcType.getOperationType("start"));
        assertNotNull(svcType.getOperationType("reload"));
    }
}
