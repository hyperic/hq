package org.hyperic.hq.control.agent.commands;

import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class ControlPluginCommand_result extends AgentRemoteValue {

    public void setValue(String key, String val)
    {
        throw new AgentAssertionException("This should never be called");
    }

    public ControlPluginCommand_result()
    {
        super();
    }

    public ControlPluginCommand_result(AgentRemoteValue val)
        throws AgentRemoteException
    {
    }
}
