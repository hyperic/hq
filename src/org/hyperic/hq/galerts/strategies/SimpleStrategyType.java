package org.hyperic.hq.galerts.strategies;

import org.hyperic.hq.galerts.server.session.ExecutionStrategy;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyType;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;


public class SimpleStrategyType 
    implements ExecutionStrategyType
{
    public ExecutionStrategy createStrategy(ConfigResponse config) {
        return new SimpleStrategy();
    }

    public ConfigSchema getConfig() {
        return new ConfigSchema();
    }

    public String getName() {
        return "Simple Strategy";
    }
}
