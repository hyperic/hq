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

package org.hyperic.hq.measurement.agent.commands;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

public class TrackPluginRemove_args extends AgentRemoteValue {

    private static final String PARAM_NAME = "plugin";
    private static final String PARAM_TYPE = "type";

    public TrackPluginRemove_args()
    {
        super();
    }

    public TrackPluginRemove_args(AgentRemoteValue args)
        throws AgentRemoteException
    {
        String name  = 
            args.getValue(TrackPluginRemove_args.PARAM_NAME);
        String type =
            args.getValue(TrackPluginRemove_args.PARAM_TYPE);
        
        if (name == null) {
            throw new AgentRemoteException("Plugin to remove not specified");
        } else if (type == null) {
            throw new AgentRemoteException("Plugin type not specified");
        } else {
            setConfig(name, type);
        }
    }

    public void setConfig(String pluginName, String pluginType)
        throws AgentRemoteException
    {
        super.setValue(TrackPluginRemove_args.PARAM_NAME, pluginName);
        super.setValue(TrackPluginRemove_args.PARAM_TYPE, pluginType);
    }

    public String getName()
    {
        return this.getValue(TrackPluginRemove_args.PARAM_NAME);
    }

    public String getType()
    {
        return this.getValue(TrackPluginRemove_args.PARAM_TYPE);
    }
}
