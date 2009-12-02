/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.livedata.agent.commands;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.encoding.Base64;



public class LiveData_args extends AgentRemoteValue {

    private static final String PARAM_TYPE    = "type";
    private static final String PARAM_COMMAND = "command";
    private static final String PARAM_CONFIG  = "config";
    
    private static final int MAX_VALUE_SIZE = 65500;

    public LiveData_args() {
        super();
    }
    
    private Map chunkConfig(String key, String value){
    	Map chunkMap = new LinkedHashMap();
    	int valueLength = value.length();
    	int count = 1;
	    while (valueLength > 0){
	    	String newValue;
	    	int offset = (count-1) * MAX_VALUE_SIZE;
	    	if (valueLength > MAX_VALUE_SIZE){
	    		newValue = value.substring(offset, MAX_VALUE_SIZE*count);
	    	} else {
	    		newValue = value.substring(offset, value.length());
	    	}
	    	valueLength = valueLength - MAX_VALUE_SIZE;
	    	chunkMap.put(key+(count),  newValue);
	    	count++;
	    }
	    return chunkMap;
    }

    public LiveData_args(AgentRemoteValue val)
        throws AgentRemoteException
    {
        String type = val.getValue(PARAM_TYPE);
        String command = val.getValue(PARAM_COMMAND);
        String configStr = getConfigValue(val,PARAM_CONFIG);

        ConfigResponse config;
        try {
            config = ConfigResponse.decode(Base64.decode(configStr));
        } catch (EncodingException e) {
            throw new AgentRemoteException("Unable to decode plugin " +
                                           "configuration: " +
                                           e.getMessage());
        }

        setConfig(type, command, config);
    }
    
	public void setConfigValue(String key, String val) {
		if (val.length() > MAX_VALUE_SIZE){
			Iterator iterator = chunkConfig(key, val).entrySet().iterator();
			while (iterator.hasNext()){
				Map.Entry chunkEntry = (Map.Entry) iterator.next();
				super.setValue((String)chunkEntry.getKey(), (String)chunkEntry.getValue());
			}
		}else {
			super.setValue(key, val);
		}
	}

    public void setConfig(String type, String command, ConfigResponse config)
        throws AgentRemoteException
    {
        String configStr;

        try {
            configStr = Base64.encode(config.encode());

        } catch (EncodingException e) {
            throw new AgentRemoteException("Unable to encode plugin " +
                                           "configuration: " +
                                           e.getMessage());
        }

        super.setValue(PARAM_TYPE, type);
        super.setValue(PARAM_COMMAND, command);
        setConfigValue(PARAM_CONFIG, configStr);
    }

    public String getType() {
        return getValue(PARAM_TYPE);
    }

    public String getCommand() {
        return getValue(PARAM_COMMAND);
    }
    
    private String getConfigValue(AgentRemoteValue agentRemoteValue, String key){
    	int count = 1;
    	String configValue = agentRemoteValue.getValue(key);
    	if (configValue == null){
    		configValue = "";
    		String chunkValue;
	    	while ((chunkValue = agentRemoteValue.getValue(key+count)) !=null){
	    		configValue = configValue + chunkValue;
	    		count++;
	    	}
    	}
    	return configValue;
    }
    
    public ConfigResponse getConfig()
        throws AgentRemoteException
    {
        String configStr = getConfigValue(this, PARAM_CONFIG);
        ConfigResponse config;

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
