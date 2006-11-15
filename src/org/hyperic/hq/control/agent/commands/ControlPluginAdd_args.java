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

package org.hyperic.hq.control.agent.commands;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import org.hyperic.util.encoding.Base64;

public class ControlPluginAdd_args extends AgentRemoteValue {

    // The plugin config response.  Stored base64 encoded.
    private static final String PARAM_CONFIG  = "config";  // Base64 encoded
    private static final String PARAM_TYPE    = "type";    // Plugin type
    private static final String PARAM_NAME    = "name";    // Plugin name

    public ControlPluginAdd_args()
    {
        super();
    }

    public ControlPluginAdd_args(AgentRemoteValue args)
        throws AgentRemoteException
    {
        String configStr = args.getValue(ControlPluginAdd_args.PARAM_CONFIG);
        String type = args.getValue(ControlPluginAdd_args.PARAM_TYPE);
        String name = args.getValue(ControlPluginAdd_args.PARAM_NAME);
        ConfigResponse config;

        // Parse early to detect errors
        try {
            config = ConfigResponse.decode(Base64.decode(configStr));
        } catch (EncodingException e) {
            throw new AgentRemoteException("Unable to decode plugin " +
                                           "configuration: " +
                                           e.getMessage());
        }

        if (type == null) {
            throw new AgentRemoteException("Plugin type not given");
        } else if (name == null) {
            throw new AgentRemoteException("Plugin name not given");
        } else {
            this.setConfig(name, type, config);
        }
    }

    public void setConfig(String name, String type, ConfigResponse config)
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

        super.setValue(ControlPluginAdd_args.PARAM_CONFIG, configStr);
        super.setValue(ControlPluginAdd_args.PARAM_TYPE, type);
        super.setValue(ControlPluginAdd_args.PARAM_NAME, name);
    }

    public ConfigResponse getConfigResponse()
        throws AgentRemoteException
    {
        String configStr = this.getValue(ControlPluginAdd_args.PARAM_CONFIG);
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

    public String getType()
    {
        return this.getValue(ControlPluginAdd_args.PARAM_TYPE);
    }

    public String getName()
    {
        return this.getValue(ControlPluginAdd_args.PARAM_NAME);
    }
}
