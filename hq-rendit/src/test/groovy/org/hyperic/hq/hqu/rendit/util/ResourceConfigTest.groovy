package org.hyperic.hq.hqu.rendit.util


import java.util.HashMap;
import java.util.Map;

import org.easymock.classextension.EasyMock;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.server.session.ProductBossImpl.ConfigSchemaAndBaseResponse;
import org.hyperic.hq.bizapp.shared.AllConfigResponses;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.hqu.rendit.BaseRenditTest;
import org.hyperic.hq.hqu.rendit.metaclass.AgentCategory 
import org.hyperic.hq.hqu.rendit.metaclass.AppdefCategory 
import org.hyperic.hq.hqu.rendit.metaclass.AuthzSubjectCategory 
import org.hyperic.hq.hqu.rendit.metaclass.MapCategory 
import org.hyperic.hq.hqu.rendit.metaclass.ResourceCategory 
import org.hyperic.hq.hqu.rendit.metaclass.ResourceGroupCategory 
import org.hyperic.hq.hqu.rendit.metaclass.RoleCategory 
import org.hyperic.hq.hqu.rendit.metaclass.StringCategory 
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.ReflectionEqualsArgumentMatcher;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.StringConfigOption;

/**
 * Unit test of ResourceConfig utility
 * @author jhickey
 *
 */
public class ResourceConfigTest extends BaseRenditTest {
    
    private ResourceConfig resourceConfig;
    private AuthzSubject user;
    private SessionManager sessionManager;
    private Resource resource;
    private AppdefEntityID resourceId = AppdefEntityID.newServiceID(12345);
    
    
    protected void setUp() {
        super.setUp();
        this.user = new AuthzSubject(true, "hqadmin", "dept", "email", true, "Joe", "User", "joe", "123-4567", "sms", false);
        Resource prototype = null;
        ResourceType type = new ResourceType();
        type.setId(AuthzConstants.authzService);
        resource = new Resource(type,prototype,"Test Service",user,12345,false);
        this.sessionManager = EasyMock.createMock(SessionManager.class);
        Bootstrap.setBean(SessionManager.class, sessionManager);
    }
    
    
    public void testSetConfig() throws AppdefEntityNotFoundException, ConfigFetchException, PluginNotFoundException, PermissionException, EncodingException, PluginException {
        Map<String,String> config = new HashMap<String,String>(2);
        config.put("name", "default");
        config.put("Application", "swf-booking-mvc");
        
        ConfigSchema schema = new ConfigSchema();
        schema.addOption(new StringConfigOption("name","JMX name key prop"));
        schema.addOption(new StringConfigOption("Application","JMX application key prop"));
        
        ConfigSchemaAndBaseResponse response = new ConfigSchemaAndBaseResponse(schema, new ConfigResponse());
        EasyMock.expect(productBoss.getConfigSchemaAndBaseResponse(null,resourceId,ProductPlugin.TYPE_PRODUCT,true)).andReturn(response);
        EasyMock.expect(productBoss.getConfigSchemaAndBaseResponse(null,resourceId,ProductPlugin.TYPE_MEASUREMENT,true)).andReturn(response);
        EasyMock.expect(productBoss.getConfigSchemaAndBaseResponse(null,resourceId,ProductPlugin.TYPE_CONTROL,true)).andReturn(response);
        EasyMock.expect(productBoss.getConfigSchemaAndBaseResponse(null,resourceId,ProductPlugin.TYPE_RESPONSE_TIME,true)).andReturn(response);
        EasyMock.expect(sessionManager.put(user)).andReturn(5678);
        AllConfigResponses expectedConfigResponse = new AllConfigResponses();
        expectedConfigResponse.setResource(resourceId);
        ConfigResponse expectedConfig = new ConfigResponse();
        expectedConfig.setValue("name", "default");
        expectedConfig.setValue("Application", "swf-booking-mvc");
        expectedConfigResponse.setConfig(ProductPlugin.CFGTYPE_IDX_PRODUCT, expectedConfig);
        expectedConfigResponse.setConfig(ProductPlugin.CFGTYPE_IDX_MEASUREMENT, expectedConfig);
        expectedConfigResponse.setConfig(ProductPlugin.CFGTYPE_IDX_CONTROL, expectedConfig);
        expectedConfigResponse.setConfig(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME, expectedConfig);
        expectedConfigResponse.setSupports(ProductPlugin.CFGTYPE_IDX_PRODUCT,true);
        expectedConfigResponse.setSupports(ProductPlugin.CFGTYPE_IDX_MEASUREMENT,true);
        expectedConfigResponse.setSupports(ProductPlugin.CFGTYPE_IDX_CONTROL,true);
        expectedConfigResponse.setSupports(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME,true);
        
        AllConfigResponses expectedConfigRollback = new AllConfigResponses();
        expectedConfigRollback.setResource(resourceId);
        expectedConfigRollback.setSupports(ProductPlugin.CFGTYPE_IDX_PRODUCT,true);
        expectedConfigRollback.setSupports(ProductPlugin.CFGTYPE_IDX_MEASUREMENT,true);
        expectedConfigRollback.setSupports(ProductPlugin.CFGTYPE_IDX_CONTROL,true);
        expectedConfigRollback.setSupports(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME,true);
        expectedConfigRollback.setConfig(ProductPlugin.CFGTYPE_IDX_PRODUCT, new ConfigResponse());
        expectedConfigRollback.setConfig(ProductPlugin.CFGTYPE_IDX_MEASUREMENT, new ConfigResponse());
        expectedConfigRollback.setConfig(ProductPlugin.CFGTYPE_IDX_CONTROL, new ConfigResponse());
        expectedConfigRollback.setConfig(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME, new ConfigResponse());
        
        appdefBoss.setAllConfigResponses(EasyMock.eq(5678), ReflectionEqualsArgumentMatcher.eqObject(expectedConfigResponse),  ReflectionEqualsArgumentMatcher.eqObject(expectedConfigRollback))
        replay();
        def CATEGORIES = [AuthzSubjectCategory,AppdefCategory,
                MapCategory,
                ResourceCategory,
                ResourceGroupCategory, RoleCategory,
                StringCategory, AgentCategory]
        
        use (*CATEGORIES) {
            this.resourceConfig = new ResourceConfig(resource);
            resourceConfig.setConfig(config,user);
        }
        
        verify();
    }
    
    private void replay() {
        EasyMock.replay(productBoss, permissionManager, sessionManager, appdefBoss);
    }
    
    private void verify() {
        EasyMock.verify(productBoss, permissionManager, sessionManager, appdefBoss);
    }
}
