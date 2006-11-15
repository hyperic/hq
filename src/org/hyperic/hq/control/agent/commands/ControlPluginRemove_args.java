package org.hyperic.hq.control.agent.commands;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class ControlPluginRemove_args extends AgentRemoteValue {

    private static final String PARAM_NAME = "plugin";

    public ControlPluginRemove_args()
    {
        super();
    }

    public ControlPluginRemove_args(AgentRemoteValue args)
        throws AgentRemoteException
    {
        String pluginName = 
            args.getValue(ControlPluginRemove_args.PARAM_NAME);
        
        if (pluginName == null) {
            throw new AgentRemoteException("Plugin to remove not specified");
        } else {
            this.setPluginName(pluginName);
        }
    }

    public void setPluginName(String pluginName)
        throws AgentRemoteException
    {
        super.setValue(ControlPluginRemove_args.PARAM_NAME, pluginName);
    }

    public String getPluginName()
    {
        return this.getValue(ControlPluginRemove_args.PARAM_NAME);
    }
}
    

        
