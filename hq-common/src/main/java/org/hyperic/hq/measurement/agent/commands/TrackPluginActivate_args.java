package org.hyperic.hq.measurement.agent.commands;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.encoding.Base64;


public class TrackPluginActivate_args
    extends AgentRemoteValue {

    private static final String PARAM_CONFIG  = "config";  // Base64 encoded
    //private static final String PARAM_TYPE    = "entity.type";    // Plugin type
    private static final String PROP_TYPE_NAME = "entity.typeName";
//  private static final String PROP_ID = "entity.id";
    private static final String INSTALL_PATH = "config.installpath";
    
    public TrackPluginActivate_args() {
       super();
    }

    public TrackPluginActivate_args(AgentRemoteValue args) throws AgentRemoteException{
        //String configStr    = args.getValue(PARAM_CONFIG);
        String type         = args.getValue(PROP_TYPE_NAME);
        String installPath  = args.getValue(INSTALL_PATH);

/*        ConfigResponse config;
        try {
            config = configStr == null ? null : ConfigResponse.decode(Base64.decode(configStr));
        } catch (Exception e) {
            config = Convert(args);
        }
*/
        ConfigResponse config = Convert(args);
        
        if (type == null) {
            throw new AgentRemoteException("Plugin type to activate not specified");
        } else if (installPath == null) {
            throw new AgentRemoteException("Install path not specified");
        } else {
            setConfig(type, installPath, config);
        }     
    }
    
    private ConfigResponse Convert(AgentRemoteValue args) {
        final Map<String, String> map = new HashMap<String, String>();
        for (final Object key:args.getKeys()){
            final String strKey = key.toString();
            map.put(key.toString(), args.getValue(strKey));
        }
        
        final ConfigResponse config = new ConfigResponse(map);
        return config;
        
    }

    public void setConfig(String pluginName, String pluginType, ConfigResponse config)
        throws AgentRemoteException
    {
        String configStr = null;

        try {
            if (config != null)
                configStr = Base64.encode(config.encode());

        } catch (EncodingException e) {
            throw new AgentRemoteException("Unable to encode plugin " +
                                           "configuration: " + 
                                           e.getMessage());
        }
        if (configStr != null)
            super.setValue(PARAM_CONFIG, configStr);
        super.setValue(PROP_TYPE_NAME, pluginName);
        super.setValue(INSTALL_PATH, pluginType);
    }

    public String getType(){
        return this.getValue(PROP_TYPE_NAME);
    }

    public String getInstallPath(){
        return this.getValue(INSTALL_PATH);
    }
    
    public ConfigResponse getConfigResponse()
    throws AgentRemoteException
    {
        String configStr = this.getValue(PARAM_CONFIG);
        ConfigResponse config;
    
        // This shouldn't fail, since we have already decoded once
        try {
            config = ConfigResponse.decode(Base64.decode(configStr));
        } catch (EncodingException e) {
            throw new AgentRemoteException("Unable to decode plugin " +
                                           "configuration: " +
                                           e.getMessage());
        }
        
        return config;
    }
}
