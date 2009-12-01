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
import java.util.ArrayList;

import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.servlet.client.JMXRemote;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.IntegerConfigOption;

/** 
 * Attempt to a generic control plugin for servlet engines and similar 
 * tools. It currently does a hack to get around param passing - it'll 
 * eventually be replaced with classes that extend it and just override
 * the script name, or if the creation of control plugins is fixed we 
 * can pass the script in params.
 */ 
public class ServletEngineControlPlugin 
    extends ServerControlPlugin
{
    private static final String actions[]  = { "start", "stop", "restart" };
    private static final List commands     = Arrays.asList(actions);

    private static final int SERVLET_ENGINE_TIMEOUT = 600; // 10 minutes

    // For avail checks
    private String jmxUrl;
    private JMXRemote jmxRemote;
    
    /** 
     * Protected field, must be update by updateOptions with the 
     * server-specific options
     */
    protected ArrayList configOptions = new ArrayList();

    protected String type = null;

    // By default the install dir will be used
    protected String workDir = null;
    
    // Base dir for servers that support one install and multiple instances 
    protected String baseDir = null;

    // Scripts for starting and stopping
    protected String startScript;
    protected String stopScript;

    // If the script needs to run in the background
    protected boolean isBackground = false;
    
    // Config properties, to avoid duplication 
    public static final String PROP_STARTUP_SCRIPT = "startupScript";
    public static final String PROP_STOP_SCRIPT    = "stopScript";
    public static final String PROP_SERVER_NAME    = "serverName";

    public ServletEngineControlPlugin() {
        super();
        setTimeout(SERVLET_ENGINE_TIMEOUT);
    }

    public List getActions()
    {
        return this.commands;
    }

    protected boolean isBackgroundCommand() {
        return isBackground;
    }
    
    /** 
     * Return the "home" of the instance - either the install dir 
     * or the the "based" dir ( CATALINA_BASE for example ).
     */ 
    protected File getBaseDir() {
        if(baseDir != null) 
            return new File(baseDir);
        else 
            return new File(getInstallPrefix());        
    }
    
    protected File getWorkingDirectory() {
        if(workDir == null)
            return new File(getInstallPrefix());
        else 
            return new File(workDir);
    }
    
    // Override this options to provide more config options
    protected void updateOptions(TypeInfo info) {
    }        

    // Initialize the scripts to be used to start and stop.  Must be
    // overriden by each server
    protected void initScript(ConfigResponse config ) 
        throws PluginException
    {
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        if( configOptions.size() == 0 ) { 
            updateOptions(info);
        
            IntegerConfigOption timeout = 
                 new IntegerConfigOption(ControlPlugin.PROP_TIMEOUT,
                                         "Timeout of control operations " +
                                         "(in seconds)",
                                         new Integer(getTimeout()));
            configOptions.add(timeout);
        }
        
        ConfigOption options[]=new ConfigOption[ configOptions.size()];
        for( int i=0; i< configOptions.size() ; i++ ) {
            options[i]=(ConfigOption)configOptions.get(i);
        }
        
        return new ConfigSchema(options);
    }
    
    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);

        this.jmxUrl= config.getValue(JMXRemote.PROP_JMX_URL);
        jmxRemote = new JMXRemote();
        jmxRemote.setJmxUrl(jmxUrl);

        try {
            jmxRemote.init();
        } catch (Exception e) {
            throw new PluginException("Invalid jmxUrl " + jmxUrl, e);
        }

        setInstallPrefix(config.getValue(ProductPlugin.PROP_INSTALLPATH));
        
        initScript(config);

        String install = getInstallPrefix();
        String control = getControlProgram();

        getLog().debug("Configured servlet engine control with " + install + " " + 
                       control + " " + startScript + " " + jmxUrl);
        
        File installF = new File(getInstallPrefix());
        if(!installF.exists()) {
            throw new PluginException("Install dir not found " +
                                             installF );            
        }
        
        if( baseDir != null && !getBaseDir().exists()) {
            throw new PluginException("Base dir not found " +
                                             baseDir);            
        }

        // Adjust control program if relative. 
        if(control != null && !control.startsWith( "/")) {
            if( getInstallPrefix().endsWith("/")) 
                control = getInstallPrefix() + control;
            else 
                control = getInstallPrefix() + "/" + control;
            setControlProgram(control);            
        }

        if(startScript !=null && ! startScript.startsWith( "/")) {
            if(getInstallPrefix().endsWith("/")) 
                startScript=getInstallPrefix() + startScript;
            else 
                startScript=getInstallPrefix() + "/" + startScript;
        }

        if(stopScript != null && ! stopScript.startsWith("/")) {
            if(getInstallPrefix().endsWith("/")) 
                stopScript = getInstallPrefix() + stopScript;
            else 
                stopScript = getInstallPrefix() + "/" + stopScript;
        }
    }

    public void doAction(String action)
        throws PluginException
    {
        int res = -1;
        if (action.equals("start")) {
            res = start();
        } else if (action.equals("stop")) {
            res = stop();
        } else if (action.equals("restart")) {
            res = restart();
        }

        getLog().debug("ServletEngineControl " + action + " " + res);
        this.setResult(res);
    }

    // Private helper methods

    public boolean isRunning()
    {
        try {
            String url = jmxRemote.getJmxAttributeServletPath() + "?available";
            return jmxRemote.getAvailability(url);
        } catch (Exception e) {
            // Fallthrough
            if( getLog().isDebugEnabled() )
                getLog().debug("Fail " + jmxUrl + " " + e.toString());
        }

        return false;
    }

    // Define control methods

    public int start()
    {
        if (startScript != null) {
            doCommand(startScript, "");
        } else {
            doCommand("start");
        }
        
        getLog().debug("Done " + startScript + " " + getResult());

        handleResult(STATE_STARTED);

        return getResult();
    }

    public int stop()
    {
        if(stopScript != null ) {
            doCommand(stopScript, "");
        } else {
            doCommand("stop");
        }

        handleResult(STATE_STOPPED);

        return getResult();
    }

    public int restart()
    {
        stop();

        // XXX: check stop failure?
        int res = start();

        return res;
    }
}
