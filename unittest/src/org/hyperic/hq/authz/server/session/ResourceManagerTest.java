package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.authz.shared.ResourceManagerTestLocal;
import org.hyperic.util.unittest.server.BaseServerTestCase;
import org.hyperic.util.unittest.server.LocalInterfaceRegistry;

public class ResourceManagerTest extends BaseServerTestCase {

    private static final String FILENAME = "resourceManagerTests.xml.gz";
    private LocalInterfaceRegistry _registry;

    public ResourceManagerTest(String name) {
        super(name, true);
    }

    public void setUp() throws Exception {
        super.setUp();
        super.insertSchemaData(FILENAME);
        _registry = deployHQ();
    }

    public void testFindViewableSvcResources() throws Exception {
        ResourceManagerTestLocal resMan = (ResourceManagerTestLocal)
            _registry.getLocalInterface(
                ResourceManagerTestEJBImpl.class,
                ResourceManagerTestLocal.class);
        resMan.testFindViewableSvcResources();
    }

}
