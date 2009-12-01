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

import org.hyperic.util.StringUtil;

public class ControlPluginCommand_args extends AgentRemoteValue {

    private static final String PARAM_NAME    = "name";    // Plugin to act on
    private static final String PARAM_TYPE    = "type";    // Plugin type
    private static final String PARAM_ID      = "id";      // Job id
    private static final String PARAM_COMMAND = "command"; // Command to issue
    private static final String PARAM_ARGS    = "args";    // Command args

    public ControlPluginCommand_args()
    {
        super();
    }

    public ControlPluginCommand_args(AgentRemoteValue args)
        throws AgentRemoteException
    {
        // Do verification
        String name = args.getValue(ControlPluginCommand_args.PARAM_NAME);
        String type = args.getValue(ControlPluginCommand_args.PARAM_TYPE);
        String id = args.getValue(ControlPluginCommand_args.PARAM_ID);
        String cmd = args.getValue(ControlPluginCommand_args.PARAM_COMMAND);
        String arguments = args.getValue(ControlPluginCommand_args.PARAM_ARGS);
        
        if (name == null) {
            throw new AgentRemoteException("Plugin name not given");
        } else if (type == null) {
            throw new AgentRemoteException("Plugin type not given");
        } else if (cmd == null) {
            throw new AgentRemoteException("Plugin command not given");
        }

        // Test parse of arguments
        try {
            StringUtil.explodeQuoted(arguments);
        } catch (IllegalArgumentException e) {
            throw new AgentRemoteException("Invalid arguments '" + arguments +
                                           "': " + e, e);
        }
     
        // Job id is optional
        if (id == null)
            this.setCommand(name, type, cmd, arguments);
        else
            this.setCommand(name, type, new Integer(Integer.parseInt(id)),
                            cmd, arguments);
    }

    public void setCommand(String plugin, String type, String action,
                           String args)
    {
        super.setValue(ControlPluginCommand_args.PARAM_NAME, plugin);
        super.setValue(ControlPluginCommand_args.PARAM_TYPE, type);
        super.setValue(ControlPluginCommand_args.PARAM_COMMAND, action);
        super.setValue(ControlPluginCommand_args.PARAM_ARGS, args);
    }

    public void setCommand(String plugin, String type, Integer id,
                           String action, String args)
    {
        setCommand(plugin, type, action, args);
        super.setValue(ControlPluginCommand_args.PARAM_ID, id.toString());
    }

    public String getPluginName()
    {
        return this.getValue(ControlPluginCommand_args.PARAM_NAME);
    }

    public String getPluginType()
    {
        return this.getValue(ControlPluginCommand_args.PARAM_TYPE);
    }

    public String getId()
    {
        return this.getValue(ControlPluginCommand_args.PARAM_ID);
    }

    public String getPluginAction()
    {
        return this.getValue(ControlPluginCommand_args.PARAM_COMMAND);
    }

    public String[] getArgs() {
        String args = this.getValue(ControlPluginCommand_args.PARAM_ARGS);
        return StringUtil.explodeQuoted(args);
    }
}
