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
package org.hyperic.hq.hqu.rendit;

import groovy.lang.Binding;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl;
import org.hyperic.hq.hqu.UIPluginDescriptor;

public class RenditServer {
    private static final RenditServer INSTANCE = new RenditServer();
    private static final Log _log = LogFactory.getLog(RenditServer.class);
    private final Object CFG_LOCK = new Object();
    private Map  _plugins = new HashMap();
    private File _sysDir;
    
    private RenditServer() {}

    private ClassLoader getUseLoader() {
        return RenditServer.class.getClassLoader();
    }
    
    public File getSysDir() {
        synchronized (CFG_LOCK) {
            return _sysDir;
        }
    }
    
    /**
     * Set the system directory which contains groovy support classes.
     * Since this object is a singleton, we rely on someone to call this
     * before they start adding plugins.
     */
    public void setSysDir(File sysDir) {
        synchronized (CFG_LOCK) { 
            Map oldPlugins = new HashMap(_plugins);
                
            _sysDir = sysDir;
            // Re-create all the plugins with the new system directory
            _plugins.clear();
            for (Iterator i=oldPlugins.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry ent = (Map.Entry)i.next();
                String pluginName = (String)ent.getKey();
                PluginWrapper plugin = (PluginWrapper)ent.getValue();
                PluginWrapper newWrapper;
                
                newWrapper = new PluginWrapper(plugin.getPluginDir(), _sysDir,
                                               getUseLoader());
                
                _plugins.put(pluginName, newWrapper);
            }
        }
    }
    
    public void addPluginDir(File path) 
        throws Exception
    {
        _log.info("Loading plugin from [" + path + "]");

        loadPlugin(path);
    }
    
    public void removePluginDir(String pluginName) {
        _log.info("Removing plugin [" + pluginName + "]");
        synchronized (CFG_LOCK) {
            _plugins.remove(pluginName);
        }
    }

    /**
     * Loads a plugin into the rendit system, verifying the version numbers,
     * etc.
     * 
     * @param path Path to the plugin
     */
    public PluginWrapper loadPlugin(File path) 
        throws PluginLoadException
    {
        PluginWrapper plugin = new PluginWrapper(path, getSysDir(), 
                                                 getUseLoader());
        
        Binding b = new Binding();
        b.setVariable("invokeArgs", 
                      InvocationBindings.newLoad(plugin.getPluginDir()));
        UIPluginDescriptor pInfo;

        try {
            pInfo = (UIPluginDescriptor) 
                plugin.run("org/hyperic/hq/hqu/rendit/dispatcher.groovy", b);
        } catch(PluginLoadException e) {
            throw e;
        } catch(Exception e) {
            throw new PluginLoadException("Error loading plugin at [" + 
                                          path.getAbsolutePath() + "]", e);
        }

        _log.info(pInfo.getName() + " [" + pInfo.getDescription() + 
                  "] version " + pInfo.getVersion() + 
                  " loaded at [" + path.getName() + "]");
                  
        UIPluginManagerEJBImpl.getOne().createOrUpdate(pInfo);
        
        synchronized (CFG_LOCK) {
            _plugins.put(path.getName(), plugin);
        }
        return plugin;
    }
    
    /**
     * Handles regular web requests for a UI plugin. 
     */
    public void handleRequest(String pluginName, HttpServletRequest req, 
                              HttpServletResponse resp, ServletContext ctx) 
        throws Exception
    {
        PluginWrapper plugin;
        
        synchronized (CFG_LOCK) {
            plugin = (PluginWrapper)_plugins.get(pluginName);
        }
        
        if (plugin == null) {
            throw new IllegalArgumentException("Unknown plugin [" + 
                                               pluginName + "]");
        }
        
        Binding b = new Binding();
        b.setVariable("invokeArgs", 
                      InvocationBindings.newRequest(plugin.getPluginDir(), 
                                                    req, resp, ctx));
        plugin.run("org/hyperic/hq/hqu/rendit/dispatcher.groovy", b);
    }
    
    public static final RenditServer getInstance() {
        return INSTANCE;
    }
}
