package org.hyperic.hq.measurement.server.session;

import org.hyperic.util.unittest.server.BaseServerTestCase;
import org.hyperic.util.unittest.server.LocalInterfaceRegistry;
import org.hyperic.hq.measurement.shared.DataManager_testLocal;

public class DataManager_test extends BaseServerTestCase {

    private LocalInterfaceRegistry _registry;

    public DataManager_test(String name) {
        super(name, true);
    }

    public void setUp() throws Exception {
        // Disable DB dump and restore, it does not work on Oracle.
        //super.setUp();
        _registry = deployHQ();
    }

    public void tearDown() throws Exception {
        // Disable DB dump and restore, it does not work on Oracle.
        //super.tearDown();
        undeployHQ();
    }

    public void testDuplicateData() throws Exception {
        DataManager_testLocal dMan =
             (DataManager_testLocal)
                  _registry.getLocalInterface(DataManager_testEJBImpl.class,
                                              DataManager_testLocal.class);
        dMan.testDuplicateData();
    }
}
