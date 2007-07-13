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
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.hqu.UIPluginDescriptor;
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl;

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
    
    private PluginWrapper getPlugin(String name) {
        PluginWrapper res;
        
        synchronized (CFG_LOCK) {
            res = (PluginWrapper)_plugins.get(name);
        }
        
        if (res == null) {
            throw new IllegalArgumentException("Unknown plugin [" + name + "]"); 
        }
        return res;
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
        
        InvocationBindings bindings = 
            new LoadInvocationBindings(plugin.getPluginDir());
        
        UIPluginDescriptor pInfo;

        try {
            pInfo = (UIPluginDescriptor)invokeDispatcher(plugin, bindings);
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
    public void handleRequest(String pluginName, RequestInvocationBindings b)
        throws Exception
    {
        PluginWrapper plugin = getPlugin(pluginName);
        b.setPluginDir(plugin.getPluginDir());
        invokeDispatcher(plugin, b);
    }
    
    /**
     * Renders a template (.gsp file) to a Writer
     * This facility relies on a plugin being registered under the name
     * 'tmpl_render' which how to deal with a render invocation from the
     * dispatcher.
     * 
     * @params template The template to render
     * @params params   Local variables to pass to the template
     * @params output   Writer to render to
     */
    public void renderTemplate(File template, Map params, Writer output)
        throws Exception
    {
        PluginWrapper plugin = getPlugin("tmpl_render");
        List args = new ArrayList();
        
        if (plugin == null) {
            throw new SystemException("Required plugin [tmpl_render] not " + 
                                      "found");
        }
        
        args.add(template);
        args.add(params);
        args.add(output);
        InvocationBindings bindings =
            new InvokeMethodInvocationBindings(plugin.getPluginDir(),
                                               "Renderer", "render", args);
        invokeDispatcher(plugin, bindings);
    }

    private Object invokeDispatcher(PluginWrapper plugin, 
                                    InvocationBindings bindings)
        throws Exception
    {
        Binding b = new Binding();

        b.setVariable("invokeArgs", bindings);
        return plugin.run("org/hyperic/hq/hqu/rendit/dispatcher.groovy", b);
    }

    public static final RenditServer getInstance() {
        return INSTANCE;
    }
}
