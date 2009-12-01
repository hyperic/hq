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

import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.config.ConfigResponse;

public class JRunServerControlPlugin 
    extends ServletEngineControlPlugin
{
    public JRunServerControlPlugin() {
        super();
        type = ServletProductPlugin.JRUN_SERVER_NAME;
    }
    
    protected void updateOptions(TypeInfo info) {
        StringConfigOption startupScript =
                new StringConfigOption(ServletEngineControlPlugin.
                                       PROP_STARTUP_SCRIPT,
                                       "Startup script. If relative, " +
                                       "the basedir will be prepended. " +
                                       "It will be called with -start/-stop " +
                                       "parameters appended, followed by " +
                                       "the server name",
                                       "bin/jrun");
        configOptions.add(startupScript);
        StringConfigOption serverName =
            new StringConfigOption(ServletEngineControlPlugin.
                                   PROP_SERVER_NAME,
                                   "Server name",
                                   "default");
        configOptions.add(serverName);
    }        
   
    protected void initScript(ConfigResponse config ) 
        throws PluginException
    {
        isBackground = true;
        String defStartupScript = config.getValue(ServletEngineControlPlugin.
                                                  PROP_STARTUP_SCRIPT);
        setControlProgram(defStartupScript);   
        String serverName = config.getValue(ServletEngineControlPlugin.
                                            PROP_SERVER_NAME);
        
        startScript = defStartupScript + " -start " + serverName;
        stopScript = defStartupScript + " -stop " + serverName;
        
        getLog().info( "Init scripts " + startScript + " " + stopScript);
    }
}
