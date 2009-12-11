package org.hyperic.hq.escalation.server.session;

import org.hyperic.hq.escalation.shared.EscalationManagerTestLocal;
import org.hyperic.util.unittest.server.BaseServerTestCase;
import org.hyperic.util.unittest.server.LocalInterfaceRegistry;

public class EscalationManagerTest extends BaseServerTestCase {
    
    private static final String FILENAME = "escalationManagerTests.xml.gz";
    private LocalInterfaceRegistry _registry;

    public EscalationManagerTest(String name) {
        super(name, true);
    }
    
    public void setUp() throws Exception {
        super.setUp();
        super.insertSchemaData(FILENAME);
        _registry = deployHQ();
    }
    
    public void testExecuteStateWithInvalidAlertId() throws Exception {
        EscalationManagerTestLocal eMan = (EscalationManagerTestLocal)
            _registry.getLocalInterface(
                EscalationManagerTestEJBImpl.class,
                EscalationManagerTestLocal.class);
        eMan.testExecuteStateWithInvalidAlertId();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        super.deleteSchemaData(FILENAME);
        super.tearDown();
        undeployHQ();
    }

}
