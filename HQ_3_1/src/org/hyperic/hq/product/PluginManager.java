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

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.util.PluginLoader;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.ConfigResponse;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * This class implements common functionality of the GenericPluginManager
 * interface:
 * - maintain a registry of plugins
 * - shutdown() propagated to all plugins
 */
public abstract class PluginManager {
    private final String OS;
    private final String OS_SUFFIX;

    protected HashMap plugins = new HashMap();
    private HashMap pluginInfo = new HashMap();
    private Properties props = null;
    protected Log log = null;
    private PluginManager parent = null;

    public PluginManager() {
        this(new Properties());
    }

    public PluginManager(Properties props) {
        log = LogFactory.getLog(this.getClass().getName());
        this.props = props;
        OS = OperatingSystem.getInstance().getName();
        OS_SUFFIX = " " + OS;        
    }

    public abstract String getName();

    public void init(PluginManager parent)
        throws PluginException {
        if (ProductPluginManager.DEBUG_LIFECYCLE) {
            log.debug("init " + getName());
        }
        this.parent = parent;
    }

    public void shutdown()
        throws PluginException {

        Iterator it = this.plugins.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            GenericPlugin plugin = (GenericPlugin)entry.getValue();

            try {
                plugin.shutdown();
            } catch (PluginException e) {
                log.error(plugin.getName() + ".shutdown() failed", e);
            }
        }

        this.plugins.clear(); 

        if (ProductPluginManager.DEBUG_LIFECYCLE) {
            log.debug("shutdown");
        }
    }

    public Properties getProperties() {
        return this.props;
    }

    public String getProperty(String key, String defVal) {
        return getProperties().getProperty(key, defVal);
    }

    public String getProperty(String key) {
        return getProperty(key, System.getProperty(key));
    }

    public boolean isPropertyEnabled(String key, boolean defVal) {
        String val = getProperty(key);
        if (val == null) {
            return defVal;
        }
        return "true".equals(val);
    }
    
    public boolean isPropertyEnabled(String key) {
        return isPropertyEnabled(key, false);
    }

    public PluginManager getParent() {
        return this.parent;
    }

    protected void mergeConfigSchema(PluginManager pm,
                                     ConfigSchema schema,
                                     TypeInfo info,
                                     ConfigResponse config) {
        try {
            GenericPlugin plugin = pm.getPlugin(info.getName());
            List options =
                plugin.getConfigSchema(info, config).getOptions();
            schema.addOptions(options);
        } catch (PluginNotFoundException e) {
            //ok
        }
    }

    //XXX PluginLoader.setClassLoader here?
    public ConfigSchema getConfigSchema(String plugin,
                                        TypeInfo info,
                                        ConfigResponse config)
        throws PluginNotFoundException {

        return getPlugin(plugin).getConfigSchema(info, config);
    }

    private String getServicePluginImpl(GenericPlugin plugin) {
        return plugin.getTypeProperty("SERVICE_" +
                                      getName().toUpperCase() +
                                      "_PLUGIN");
    }
    
    private GenericPlugin getPluginExtension(String name) {
        String platformName = name;
        boolean isPlatformPlugin = false;
        if (name.endsWith(OS_SUFFIX)) {
            //XXX bleh.
            isPlatformPlugin = true;
            name = name.substring(0, name.length()-OS_SUFFIX.length());
        }
        PluginData.ServiceExtension ext =
            PluginData.getServiceExtension(name);
        if (ext == null) {
            return null;
        }

        ServiceTypeInfo service = ext.service;
        String serverName = service.getServerName();
        GenericPlugin serverPlugin;
        String msg =
            " plugin for service '" +
            name + "' from server '" + serverName + "'";
        
        try {
            serverPlugin = getPlugin(serverName);
            log.debug("Created" + msg);
        } catch (PluginNotFoundException e) {
            log.debug("PluginNotFound creating" + msg);
            return null;
        }

        String implClass =
            ext.data.getPlugin(getName(), service.getName());

        if (implClass == null) {
            String defaultImpl =
                getServicePluginImpl(serverPlugin);

            //did not configure <plugin type="$type" class"..."/>
            if (getName().equals(ProductPlugin.TYPE_MEASUREMENT)) {
                //only default measurement plugin to the server's
                //very unlikely the server control plugin will work
                //for a service.
                if (defaultImpl == null) {
                    implClass = serverPlugin.getClass().getName();
                }
                else {
                    implClass = defaultImpl;
                }
            }
            else if (getName().equals(ProductPlugin.TYPE_CONTROL)) {
                //allow server types to define the default service impl
                //<property name="SERVICE.CONTROL"
                //          value="${package}.JBossServiceControlPlugin"/>
                if (ext.data.getControlActions(service.getName()) != null) {
                    implClass = defaultImpl;
                }
            }
            else {
                return null;
            }
        }

        if (implClass == null) {
            return null;
        }

        log.debug("Using " + implClass + " " + getName() +
                  " plugin from " + serverPlugin.getName() +
                  " for " + service.getName());

        String[] mergeProps = {
            MeasurementPlugin.PROP_TEMPLATE_CONFIG,
        };

        for (int i=0; i<mergeProps.length; i++) {
            String key = mergeProps[i];
            String value = ext.data.getProperty(key);
            if (value == null) {
                value = serverPlugin.data.getProperty(key);
                if (value != null) {
                    ext.data.setProperty(key, value);
                }
            }
        }

        //create instance of the plugin using the server type's ClassLoader
        GenericPlugin plugin =
            ProductPlugin.getPlugin(serverPlugin, implClass, getName(), service);

        if (plugin == null) {
            log.error("Failed to create " + msg);
            return null;
        }

        ProductPluginManager ppm = (ProductPluginManager)getParent();

        plugin.data = ext.data;
        plugin.setName(service.getName());
        plugin.setTypeInfo(service);
        //setPluginInfo from the ProductPlugin
        String productName = plugin.data.getName();
        PluginInfo info = ppm.getPluginInfo(productName);
        setPluginInfo(plugin.getName(), info);

        try {
            ppm.registerTypePlugin(this, info, plugin, service);
        } catch (PluginExistsException e) {
            log.debug("PluginExists creating" + msg);
            return null;
        }

        if (isPlatformPlugin) {
            return (GenericPlugin)this.plugins.get(platformName);
        }
        return plugin;
    }
    
    public GenericPlugin getPlugin(String name)
        throws PluginNotFoundException {

        GenericPlugin plugin = (GenericPlugin)this.plugins.get(name);
        if (plugin == null) {
            if ((plugin = getPluginExtension(name)) != null) {
                return plugin;
            }
            String msg =
                getName() + " plugin name=" + name + " not found";
            throw new PluginNotFoundException(msg);
        }

        return plugin;
    }

    public GenericPlugin getPlatformPlugin(String name)
        throws PluginNotFoundException {
    
        return getPlugin(name + OS_SUFFIX);
    }

    public GenericPlugin getPlatformPlugin(String os, String name)
        throws PluginNotFoundException {
    
        return getPlugin(name + " " + os);
    }

    public void removePlugin(String name)
        throws PluginException, PluginNotFoundException {

        if (ProductPluginManager.DEBUG_LIFECYCLE) {
            log.debug("removePlugin=" + name);
        }

        GenericPlugin plugin = (GenericPlugin)getPlugin(name);
        this.plugins.remove(name);

        try {
            plugin.shutdown();
        } catch (PluginException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public boolean isRegistered(String name) {
        return this.plugins.get(name) != null;
    }

    public void registerPlugin(GenericPlugin plugin)
        throws PluginException, PluginExistsException {

        registerPlugin(plugin, null);
    }

    public void registerPlugin(String name, GenericPlugin plugin)
        throws PluginException, PluginExistsException {

        plugin.setName(name);
        registerPlugin(plugin);
    }

    public void registerPlugin(GenericPlugin plugin, ConfigResponse response)
        throws PluginException, PluginExistsException {

        String pluginName = plugin.getName();

        if ((pluginName == null) || (pluginName.length() < 1)) {
            String msg =
                "malformed name '" + pluginName + "' for plugin instance: " +
                plugin.getClass().getName();
            throw new PluginException(msg);
        }

        if (this.plugins.get(pluginName) != null) {
            throw new PluginExistsException("Plugin name=" + pluginName +
                                            " already exists");
        }

        //put before init() since init() may register a proxy
        //who's init() method checks if the proxy is alredy registered 
        this.plugins.put(pluginName, plugin);

        boolean setClassLoader = 
            PluginLoader.setClassLoader(plugin);

        try {
            plugin.init(this);

            if (response != null) {
                plugin.configure(response);
            }
        } catch (PluginException e) {
            this.plugins.remove(pluginName);
            throw e;
        } finally {
            if (setClassLoader) {
                PluginLoader.resetClassLoader(plugin);
            }
        }

        if (ProductPluginManager.DEBUG_LIFECYCLE) {
            log.debug("registerPlugin=" + pluginName);
        }
    }

    public void updatePlugin(GenericPlugin plugin, ConfigResponse response)
        throws PluginException, PluginNotFoundException {

        if (ProductPluginManager.DEBUG_LIFECYCLE) {
            log.debug("updatePlugin=" + plugin.getName());
        }

        removePlugin(plugin.getName());

        try {
            registerPlugin(plugin, response);
        } catch (PluginExistsException e) {
            //this will never happen.
            throw new PluginException("plugin was not removed?");
        }
    }

    public GenericPlugin createPlugin(String name, String type,
                                      ConfigResponse config)
        throws PluginException,
        PluginExistsException, PluginNotFoundException {

        return createPlugin(name, getPlugin(type), config);
    }

    public GenericPlugin createPlugin(String name,
                                      GenericPlugin pluginType)
        throws PluginException, PluginExistsException {

        return createPlugin(name, pluginType, null);
    }
    
    public GenericPlugin createPlugin(String name,
                                      GenericPlugin pluginType,
                                      ConfigResponse config)
        throws PluginException,
        PluginExistsException {

        GenericPlugin plugin;

        try {
            plugin = (GenericPlugin)pluginType.getClass().newInstance();
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        plugin.data = pluginType.data;
        plugin.setName(name);
        plugin.setTypeInfo(pluginType.getTypeInfo());

        registerPlugin(plugin, config);

        PluginInfo info = getPluginInfo(pluginType.getName());
        if (info != null) {
            setPluginInfo(name, new PluginInfo(name, info));
        }

        return plugin;
    }

    public Map getPlugins() {
        return this.plugins;
    }

    /**
     * @return Map of plugins registered for the given platform.
     */
    public Map getPlatformPlugins(String os) {
        os = " " + os;
        HashMap found = new HashMap();

        for (Iterator it = getPlugins().entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            String name = (String)entry.getKey();

            if (name.endsWith(os)) {
                found.put(name, entry.getValue());
            }
        }

        return found;
    }

    /**
     * @return Map of plugins registered for the current platform.
     */
    public Map getPlatformPlugins() {
        return getPlatformPlugins(OS);
    }

    //match all plugins instances from the same jar
    public List getPlugins(PluginInfo info) {
        ArrayList found = new ArrayList();

        for (Iterator it = this.pluginInfo.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            PluginInfo pi = (PluginInfo)entry.getValue();

            if (pi.matches(info)) {
                found.add(pi.name);
            }
        }

        return found;
    }

    public void setPluginInfo(String name, PluginInfo info) {
        this.pluginInfo.put(name, info);
    }

    public PluginInfo getPluginInfo(String name) {
        return (PluginInfo)this.pluginInfo.get(name);
    }

    protected String classNotFoundMessage(NoClassDefFoundError e) {
        return
            "Plugin class not found: " +
            e.getMessage() +
            " (invalid classpath or corrupt plugin jar)";
    }
}
