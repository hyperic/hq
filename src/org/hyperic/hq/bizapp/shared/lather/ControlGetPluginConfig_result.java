package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;

public class ControlGetPluginConfig_result
    extends LatherValue
{
    private static final String PROP_CONFIG = "config";

    public ControlGetPluginConfig_result(){
        super();
    }

    public void setConfig(byte[] config){
        this.setByteAValue(PROP_CONFIG, config);
    }

    public byte[] getConfig(){
        return this.getByteAValue(PROP_CONFIG);
    }

    public void validate()
        throws LatherRemoteException
    {
        try {
            this.getConfig();
        } catch(LatherKeyNotFoundException exc){
            throw new LatherRemoteException("All values not set");
        }
    }
}
