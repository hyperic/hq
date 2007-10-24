/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.TransactionListener;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.TransactionManagerEJBImpl;
import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.hyperic.hq.hqu.server.session.UIPluginManagerEJBImpl;
import org.hyperic.hq.hqu.shared.UIPluginManagerLocal;
import org.hyperic.util.Runnee;

public class RenditServer {
    public static final String PROP_PLUGIN_NAME   = "plugin.name";
    public static final String PROP_PLUGIN_VER    = "plugin.version";
    public static final String PROP_PLUGIN_APIMAJ = "plugin.apiMajor";
    public static final String PROP_PLUGIN_APIMIN = "plugin.apiMinor";
    
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
            if (!_plugins.isEmpty()) {
                throw new IllegalStateException("Unable to set sysdir after " + 
                                                "plugins have been loaded");
            }
            _sysDir = sysDir;
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
    public PluginWrapper loadPlugin(final File path) 
        throws PluginLoadException
    {
        final PluginWrapper plugin = new PluginWrapper(path, getSysDir(), 
                                                       getUseLoader());
        
        try {
            plugin.loadDispatcher();
        } catch(Exception e) {
            throw new PluginLoadException("Failed to load plugin", e);
        }
        
        Properties props;

        try {
            props = plugin.loadPlugin();
        } catch(PluginLoadException e) {
            throw e;
        } catch(Exception e) {
            throw new PluginLoadException("Error loading plugin at [" + 
                                          path.getAbsolutePath() + "]", e);
        }

        final String pluginName = props.getProperty(PROP_PLUGIN_NAME);
        final String pluginVer  = props.getProperty(PROP_PLUGIN_VER);
        
        _log.info(pluginName + " version " + pluginVer + 
                  " loaded at [" + path.getName() + "]");
             
        final UIPluginManagerLocal uMan = UIPluginManagerEJBImpl.getOne();
        try {
            TransactionManagerEJBImpl.getOne().runInTransaction(new Runnee() {
                public Object run() throws Exception {
                    UIPlugin p = uMan.createOrUpdate(pluginName, pluginVer);
                    plugin.deploy(p);
                    HQApp.getInstance().addTransactionListener(
                    new TransactionListener() {
                        public void afterCommit(boolean success) {
                            if (success) {
                                synchronized (CFG_LOCK) {
                                    _plugins.put(path.getName(), plugin);
                                }
                            }
                        }

                        public void beforeCommit() {
                        }
                    });
                    return null;
                }
            });
        } catch(Exception e) {
            throw new PluginLoadException("Error loading HQU plugin [" + 
                                          pluginName + "]", e);
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
        plugin.handleRequest(b);
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
        InvokeMethodInvocationBindings b = 
            new InvokeMethodInvocationBindings("Renderer", "render", args);
                                               
        plugin.invokeMethod(b);
    }

    public static final RenditServer getInstance() {
        return INSTANCE;
    }
}
