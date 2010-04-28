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

package org.hyperic.hq.plugin.servlet;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;

/**
 * Server control plugin for Resin.  Currently unix only
 */
public class ResinServerControlPlugin
    extends ServerControlPlugin
{
    // Constants
    private static final String CONTROL_PROGRAM = "bin/httpd.sh";
    private static final String PIDFILE         = "httpd.pid";

    // Config options
    public static final String PROP_ID = "id";

    private static final String actions[]  = { "start", "stop", "restart" };
    private static final List commands     = Arrays.asList(actions);

    public String getControlProgram() {
        return CONTROL_PROGRAM;
    }

    public ConfigSchema getConfigSchema(TypeInfo info,
                                        ConfigResponse config)
    {
        ConfigSchema schema = super.getConfigSchema(info, config);
        
        // Optional server id
        StringConfigOption opt =
            new StringConfigOption(PROP_ID,
                                   "Server id");
        opt.setOptional(true);
        schema.addOption(opt);

        return schema;
    }

    // httpd.sh needs -pid and -server if multiple servers are configured.
    protected String[] getCommandArgs() {
        String[] args;
        String id = config.getValue(PROP_ID);
        if (id != null && id.length() != 0) {
            args = new String[] { "-server", id, "-pid", id+".pid" };
        } else {
            // Always use a pid file
            args = new String[] { "-pid", PIDFILE };
        }

        return args;
    }

    /**
     * We require our mx4j to be loaded first.
     */
    protected String[] getCommandEnv() {
        if (getTypeInfo().getVersion().
            equals(ServletProductPlugin.RESIN_VERSION_2)) {
            String cp = "CLASSPATH=" + getInstallPrefix() + 
                File.separator + "lib" +
                File.separator + "mx4j-jmx.jar";
            
            getLog().info("Setting: " + cp);
            return new String[] {
                cp,
            };
        }

        return null;
    }

    public List getActions()
    {   
        return this.commands;
    }

    public boolean useSigar() {
        return true;
    }

    protected boolean isRunning() {
        //Check for pid file
        String pidFile;
        String id = config.getValue(PROP_ID);
        if (id != null && id.length() != 0) {
            pidFile = getInstallPrefix() +
                File.separator + id + ".pid";
        } else {
            // Single instance
            pidFile = getInstallPrefix() +
                File.separator + PIDFILE;
        }

        return isProcessRunning(pidFile);
    }

    /**
     * Begin control actions.
     */
    public void start() {
        if (isRunning()) {
            if (getMessage() == null)
                setMessage("Server already running");

            setResult(RESULT_FAILURE);
            return;
        }   

        String program = config.getValue(PROP_PROGRAM);
        doCommand(program, "start");
        
        handleResult(STATE_STARTED);
    }

    public void stop() {
        if (!isRunning()) {
            if (getMessage() == null)
                setMessage("Server already stopped");

            setResult(RESULT_FAILURE);
            return;
        }   

        String program = config.getValue(PROP_PROGRAM);
        doCommand(program, "stop");

        handleResult(STATE_STOPPED);
    }

    public void restart() {
        //XXX: Check current server state?
        String program = config.getValue(PROP_PROGRAM);
        doCommand(program, "restart");

        handleResult(STATE_STARTED);
    }
}
