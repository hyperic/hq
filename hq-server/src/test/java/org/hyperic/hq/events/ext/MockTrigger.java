package org.hyperic.hq.events.ext;

import org.hyperic.hq.events.InvalidTriggerDataException;
import org.hyperic.hq.events.MockEvent;
import org.hyperic.hq.events.server.session.AlertConditionEvaluator;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.config.ConfigSchema;

/**
 * Mock trigger for testing RegisteredTriggers. Uses a static variable to
 * confirm that it has been initialized and/or enabled (since instantiation
 * happens via reflection), so can't be used more than once in a VM
 * @author jhickey
 *
 */
public class MockTrigger implements RegisterableTriggerInterface {
    /** Indicates that init has been called **/
    public static boolean initialized = false;
    /** Mock return value for isEnabled or indicator that setEnabled was called **/
    public static boolean enabled = true;

    public ConfigSchema getConfigSchema() {
        return null;
    }

    public Class<?>[] getInterestedEventTypes() {
        return new Class[] { MockEvent.class };
    }

    public Integer[] getInterestedInstanceIDs(Class<?> c) {
        return new Integer[] { 123, 456 };
    }

    public Integer getId() {
        return 3;
    }

    public void init(RegisteredTriggerValue trigger, AlertConditionEvaluator alertConditionEvaluator) throws InvalidTriggerDataException
    {
        MockTrigger.initialized = true;
    }

    public boolean isEnabled() {
        return MockTrigger.enabled;
    }

    public void setEnabled(boolean enabled) {
        MockTrigger.enabled = enabled;
    }

}
