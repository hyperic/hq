package org.hyperic.hq.authz.server.session;

import org.hyperic.util.unittest.server.BaseServerTestCase;
import org.hyperic.util.unittest.server.LocalInterfaceRegistry;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;

public class ResourceGroupManagerEJBImpl_test extends BaseServerTestCase {

    private LocalInterfaceRegistry _registry;

    public ResourceGroupManagerEJBImpl_test(String name) {
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

    public void testNothing() throws Exception {
        ResourceGroupManagerLocal rmMan =
             (ResourceGroupManagerLocal)
                  _registry.getLocalInterface(ResourceGroupManagerEJBImpl.class,
                                              ResourceGroupManagerLocal.class);
    }
}
