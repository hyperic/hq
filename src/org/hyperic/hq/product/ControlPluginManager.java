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

package org.hyperic.hq.product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.hyperic.util.PluginLoader;

import org.hyperic.util.config.ConfigResponse;

public class ControlPluginManager extends PluginManager {
    //allow command-line disablement for testing w/o adding actions to plugin.xml
    private static final boolean _validateAction =
        !"false".equals(System.getProperty("control.action.validate"));

    private HashMap pluginQueue  = new HashMap();

    public static final String[] BUILTIN_CMDS = {
    };

    public ControlPluginManager() {
        super();
    }

    public ControlPluginManager(Properties props) {
        super(props);
    }

    public String getName() {
        return ProductPlugin.TYPE_CONTROL;
    }

    public void createControlPlugin(String name, String type, 
                                    ConfigResponse config)
        throws PluginNotFoundException, PluginExistsException,
               PluginException
    { 
        createPlugin(name, type, config);
    }

    public void updateControlPlugin(String name, ConfigResponse config)
        throws PluginNotFoundException,
               PluginException
    {
        ControlPlugin plugin = (ControlPlugin)getPlugin(name);
        
        updatePlugin(plugin, config);
    }

    public List getActions(String name) 
        throws PluginNotFoundException
    { 
        ControlPlugin plugin = (ControlPlugin)getPlugin(name);
        
        //clone the plugin action list before adding to it
        List actions = new ArrayList(plugin.getActions());

        // Add any builtin plugin control commands
        for (int i = 0; i < BUILTIN_CMDS.length; i++) {
            actions.add(BUILTIN_CMDS[i]);
        }
        
        return actions;
    }

    public void addJob(String name, String jobId)
    {
        synchronized(this.pluginQueue) {
            LinkedList list = (LinkedList)this.pluginQueue.get(name);
            if (list == null) {
                list = new LinkedList();
                this.pluginQueue.put(name, list);
            }

            list.addLast(jobId);
        }
    }

    public String getNextJob(String name)
        throws NoSuchElementException
    {
        LinkedList list = (LinkedList)this.pluginQueue.get(name);
        if (list == null)
            throw new NoSuchElementException();

        return (String)list.getFirst();
    }

    public void removeNextJob(String name)
        throws NoSuchElementException
    {
        synchronized(this.pluginQueue) {
            LinkedList list = (LinkedList)this.pluginQueue.get(name);
            if (list == null)
                throw new NoSuchElementException();

            list.removeFirst();
        }
    }

    public void doAction(String name, String action, String[] args)
        throws PluginNotFoundException,
               PluginException
    {
        List methods = getActions(name);
        if (_validateAction && !methods.contains(action)) {
            throw new PluginException("Action '" + action + "' not supported");
        }

        ControlPlugin plugin = (ControlPlugin)getPlugin(name);

        // Reset previous result code and error message
        plugin.setResult(ControlPlugin.RESULT_FAILURE);
        plugin.setMessage(null);

        PluginLoader.setClassLoader(plugin);

        try {
            // Do the action
            if (args.length > 0) {
                plugin.doAction(action, args);
            }
            else {
                //XXX deprecate this too?
                plugin.doAction(action);
            }
        } finally {
            PluginLoader.resetClassLoader(plugin);
        }
    }

    public int getResult(String name)
        throws PluginNotFoundException
    {
        ControlPlugin plugin = (ControlPlugin)getPlugin(name);
        
        return plugin.getResult();
    }

    public String getMessage(String name)
        throws PluginNotFoundException
    {
        ControlPlugin plugin = (ControlPlugin)getPlugin(name);
        
        return plugin.getMessage();
    }
    
    public void removeControlPlugin(String name)
        throws PluginException, PluginNotFoundException
    {
        removePlugin(name);
    }
}
