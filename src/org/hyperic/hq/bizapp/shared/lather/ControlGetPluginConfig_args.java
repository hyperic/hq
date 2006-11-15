package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.hq.bizapp.shared.lather.SecureAgentLatherValue;

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;

public class ControlGetPluginConfig_args 
    extends SecureAgentLatherValue
{
    private static final String PROP_PLUGINNAME = "pluginName";
    private static final String PROP_MERGE      = "merge";

    public ControlGetPluginConfig_args(){
        super();
    }

    public void setPluginName(String pluginName){
        this.setStringValue(PROP_PLUGINNAME, pluginName);
    }

    public String getPluginName(){
        return this.getStringValue(PROP_PLUGINNAME);
    }

    public void setMerge(boolean merge){
        this.setIntValue(PROP_MERGE, merge ? 1 : 0);
    }

    public boolean getMerge(){
        return this.getIntValue(PROP_MERGE) == 1 ? true : false;
    }

    public void validate()
        throws LatherRemoteException
    {
        super.validate();
        try {
            this.getPluginName();
            this.getMerge();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}
