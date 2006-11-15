package org.hyperic.hq.control.agent.commands;

import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class ControlPluginRemove_result extends AgentRemoteValue {

    public void setValue(String key, String val)
    {
        throw new AgentAssertionException("This should never be called");
    }

    public ControlPluginRemove_result()
    {
        super();
    }

    public ControlPluginRemove_result(AgentRemoteValue val)
        throws AgentRemoteException
    {
    }
}
