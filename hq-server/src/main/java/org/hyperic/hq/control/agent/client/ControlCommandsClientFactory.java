package org.hyperic.hq.control.agent.client;

import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;

public interface ControlCommandsClientFactory {
    ControlCommandsClient getClient(AppdefEntityID aid) throws AgentNotFoundException;

    ControlCommandsClient getClient(String agentToken) throws AgentNotFoundException;
}
