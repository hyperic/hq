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

import groovy.lang.GroovyClassLoader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public abstract class ProductPlugin extends GenericPlugin {

    public static final String TYPE_AUTOINVENTORY = "autoinventory";
    public static final String TYPE_CONTROL       = "control";
    public static final String TYPE_MEASUREMENT   = "measurement";
    public static final String TYPE_PRODUCT       = "product";
    public static final String TYPE_RESPONSE_TIME = "responsetime";
    public static final String TYPE_LOG_TRACK     = "log_track";
    public static final String TYPE_CONFIG_TRACK  = "config_track";
    public static final String TYPE_LIVE_DATA     = "livedata";

    //server import attribute propagated by ConfigManager
    public static final String PROP_INSTALLPATH   = "installpath";

    public static final String PROP_RESOURCE_NAME = "resource.name";

    //platform attributes propogated by ConfigManager
    public static final String PROP_PLATFORM_NAME = "platform.name";
    public static final String PROP_PLATFORM_TYPE = "platform.type";
    public static final String PROP_PLATFORM_FQDN = "platform.fqdn";
    public static final String PROP_PLATFORM_IP   = "platform.ip";
    public static final String PROP_PLATFORM_ID   = "platform.id";
    
    //XXX could be something else for windows or in general..
    //but if everything uses this constant, we can keep things consistent
    public static final String DEFAULT_INSTALLPATH = 
        "/usr/local/apps";

    public static final String[] TYPES = {
        TYPE_AUTOINVENTORY,
        TYPE_CONTROL,
        TYPE_MEASUREMENT,
        TYPE_PRODUCT,
        TYPE_RESPONSE_TIME,
        TYPE_LOG_TRACK,
        TYPE_CONFIG_TRACK,
        TYPE_LIVE_DATA
    };

    public static final String[] CONFIGURABLE_TYPES = {
        TYPE_PRODUCT,
        TYPE_MEASUREMENT,
        TYPE_CONTROL,
        TYPE_RESPONSE_TIME,
    };
    public static final int CFGTYPE_IDX_PRODUCT       = 0;
    public static final int CFGTYPE_IDX_MEASUREMENT   = 1;
    public static final int CFGTYPE_IDX_CONTROL       = 2;
    public static final int CFGTYPE_IDX_RESPONSE_TIME = 3;

    protected ProductPluginManager manager;

    private static final Log _log =
        LogFactory.getLog(ProductPlugin.class.getName());

    public String getInstallPath() {
        //e.g. geronimo.installpath=/usr/local/geronimo-1.0
        String prop = getName() + "." + PROP_INSTALLPATH;
        return getManager().getProperty(prop);
    }

    public void init(PluginManager manager)
        throws PluginException
    {
        this.manager = (ProductPluginManager)manager;
        String installpath = getInstallPath();

        if (installpath != null) {
            adjustClassPath(installpath);
        }
    }

    protected ProductPluginManager getManager() {
        return this.manager;
    }

    static String[] getDataClassPath(PluginData data) {
        if (data == null) {
            return new String[0];
        }
        List cp = data.getClassPath();
        if (cp == null) {
            return new String[0];
        }
        return (String[])cp.toArray(new String[0]);        
    }

    public String[] getClassPath(ProductPluginManager manager) {
        return getDataClassPath(this.data);
    }

    private static boolean isGroovyScript(String name) {
        return name.endsWith(".groovy");
    }

    private static Class loadGroovyClass(ClassLoader loader,
                                         PluginData data,
                                         String name,
                                         String pluginName) {

        GroovyClassLoader cl = new GroovyClassLoader(loader);

        String script = data.getProperties().getProperty(name, name);
        File file = new File(script);
        if (file.exists()) {
            _log.debug(name + "->" + file);
            try {
                return cl.parseClass(file);
            } catch (Exception e) {
                _log.error(pluginName + " - Failed to load: " + name, e);
                return null;
            }
        }
        else {
            InputStream is;
            is = loader.getResourceAsStream(name); //embedded in plugin.jar
            if (is == null) {
                //in memory
                String code = data.getProperty(name);
                if (code == null) {
                    _log.error(pluginName + " - No code found for: " + name);
                    return null;                
                }
                is = new ByteArrayInputStream(code.getBytes());
            }

            try {
                return cl.parseClass(is);
            } catch (Exception e) {
                _log.error(pluginName + " - Failed to parse: " + name, e);
                return null;
            } finally {
                try { is.close(); } catch (Exception e) {}
            }
        }
    }

    private static Class loadClass(ClassLoader loader, String name)
        throws ClassNotFoundException {

        try {
            return loader.loadClass(name);
        } catch (ClassNotFoundException nfe) {
            //provide backward compat for custom plugins
            //still using net.hyperic package
            if (name.startsWith("net.hyperic")) {
                String orgName = "org" + name.substring(3);
                try {
                    return loader.loadClass(orgName);
                } catch (ClassNotFoundException e) {
                    //fallthru
                }
            }
            throw nfe;
        }        
    }

    private static Class loadJavaClass(ClassLoader loader,
                                       PluginData data,
                                       String name,
                                       String pluginName) {

        try {
            return loadClass(loader, name);
        } catch (ClassNotFoundException e) {
            //we get here if the server's implementation is a class loaded
            //from hq-product.jar rather than the plugin's ClassLoader
            try {
                _log.debug("Trying data ClassLoader to load: " +
                           name + " for plugin " + pluginName);
                return loadClass(data.getClassLoader(), name);
            } catch (ClassNotFoundException e2) {
                String msg =
                    "Unable to load " + name +
                    " for plugin " + pluginName;
                if (PluginData.getServiceExtension(pluginName) == null) {
                    _log.error(pluginName + " - " + msg);
                }
                else {
                    //plugin class is likely in another plugin
                    //see PluginManager.getPlugin where we try later.
                    _log.debug(pluginName + " - " + msg + ": " + e);
                }
                return null;
            }
        }
    }

    static Class getPluginClass(ClassLoader loader,
                                PluginData data,
                                String name,
                                String pluginName) {

        if (isGroovyScript(name)) {
            return loadGroovyClass(loader, data, name, pluginName);
        }
        else {
            return loadJavaClass(loader, data, name, pluginName);
        }
    }

    static GenericPlugin getPlugin(GenericPlugin plugin,
                                   String name,
                                   String type, TypeInfo info) {

        String pluginName = info.getName();
        Class pluginClass =
            getPluginClass(plugin.getClass().getClassLoader(),
                           plugin.data, name, pluginName);

        if (pluginClass == null) {
            return null;
        }

        try {
            return (GenericPlugin)pluginClass.newInstance();
        } catch (Exception e) {
           _log.error(pluginName +
                      " - Error creating " + pluginClass.getName() +
                      ": " + e, e);
        }

        return null;        
    }

    public GenericPlugin getPlugin(String type, TypeInfo info)
    {
        if (this.data == null) {
            return null;
        }
        String name = this.data.getPlugin(type, info);
        if (name == null) {
            return null;
        }

        return getPlugin(this, name, type, info);
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        if (this.data != null) {
            ConfigSchema schema =
                this.data.getConfigSchema(info, CFGTYPE_IDX_PRODUCT);
            if (schema != null) {
                return schema;
            }
        }
        return super.getConfigSchema(info, config);
    }

    public TypeInfo[] getTypes() {
        if (this.data != null) {
            return this.data.getTypes();
        }
        return new TypeInfo[0];
    }

    protected File getWorkDir(String type) {
        String pdk = ProductPluginManager.getPdkDir();

        if (pdk == null) {
            return null;
        }

        return ClientPluginDeployer.getSubDirectory(pdk,
                                                    type,
                                                    getName());
    }
}
