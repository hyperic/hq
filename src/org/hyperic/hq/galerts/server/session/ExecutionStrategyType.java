package org.hyperic.hq.galerts.server.session;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public interface ExecutionStrategyType {
    String getName();
    ConfigSchema getConfig();
    ExecutionStrategy createStrategy(ConfigResponse config);
}
