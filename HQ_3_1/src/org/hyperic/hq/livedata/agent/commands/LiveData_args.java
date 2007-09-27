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

import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.encoding.Base64;

public class LiveData_args extends AgentRemoteValue {

    private static final String PARAM_TYPE    = "type";
    private static final String PARAM_COMMAND = "command";
    private static final String PARAM_CONFIG  = "config";

    public LiveData_args() {
        super();
    }

    public LiveData_args(AgentRemoteValue val)
        throws AgentRemoteException
    {
        String type = val.getValue(PARAM_TYPE);
        String command = val.getValue(PARAM_COMMAND);
        String configStr = val.getValue(PARAM_CONFIG);

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
        super.setValue(PARAM_CONFIG, configStr);
    }

    public String getType() {
        return getValue(PARAM_TYPE);
    }

    public String getCommand() {
        return getValue(PARAM_COMMAND);
    }
    
    public ConfigResponse getConfig()
        throws AgentRemoteException
    {
        String configStr = getValue(PARAM_CONFIG);
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
