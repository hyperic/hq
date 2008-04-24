package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.authz.shared.ResourceGroupManager_testLocal;
import org.hyperic.util.unittest.server.BaseServerTestCase;
import org.hyperic.util.unittest.server.LocalInterfaceRegistry;

public class ResourceGroupManager_test extends BaseServerTestCase {

    private LocalInterfaceRegistry _registry;

    public ResourceGroupManager_test(String name) {
        super(name, true);
    }

    public void setUp() throws Exception {
        super.setUp();
        _registry = deployHQ();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        undeployHQ();
    }

    public void testDuplicateNameCreate() throws Exception {
        ResourceGroupManager_testLocal rmMan =
             (ResourceGroupManager_testLocal)
                  _registry.getLocalInterface(ResourceGroupManager_testEJBImpl.class,
                                              ResourceGroupManager_testLocal.class);
        
        rmMan.testDuplicateNameCreate();
    }

    public void testUpdate() throws Exception {
        ResourceGroupManager_testLocal rmMan =
             (ResourceGroupManager_testLocal)
                  _registry.getLocalInterface(ResourceGroupManager_testEJBImpl.class,
                                              ResourceGroupManager_testLocal.class);

        rmMan.testUpdate();        
    }
    
    public void testResourceGroupSetCriteria() throws Exception {
        ResourceGroupManager_testLocal rmMan =
            (ResourceGroupManager_testLocal)
                 _registry.getLocalInterface(ResourceGroupManager_testEJBImpl.class,
                                             ResourceGroupManager_testLocal.class);

       rmMan.testResourceGroupSetCriteria();
   }
}
