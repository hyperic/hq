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

import org.hyperic.hq.product.pluginxml.PluginData;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import org.hyperic.util.PluginLoader;
import org.hyperic.util.PluginLoaderException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.ConfigResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class GenericPlugin {
    private static final String PROP_NETSTAT = "netservices.netstat";

    /**
     * Key used to store value of XML resource name attribute.
     */
    public static final String PROP_NAME = "NAME";

    static final String[] TYPE_LABELS = {
        null, "platform.", "server.", "service."
    };
       
    PluginData data = null;
    
    private static final boolean isWin32 =
        PlatformDetector.IS_WIN32;

    private String name = null;
    private String version = null;
    private TypeInfo type = null;
    private PluginManager manager = null;
    private ProductPlugin productPlugin = null;
    protected ConfigResponse config;
    private static String hostname = null;
    
    public void setData(PluginData data) {
        this.data = data;
    }
          
    public PluginData getPluginData() {
        return this.data;
    }

    static String[] createTypeLabels(String prop) {
        int len = TYPE_LABELS.length;
        String[] labels = new String[len];
        for (int i=0; i<len; i++) {
            labels[i] = TYPE_LABELS[i] + prop;
        }
        return labels;
    }

    public static final String FILE_DELIM = ",";
    public static final String FILE_DELIM_ESC = "%2C";

    /**
     * Expand a String to List of file names.
     * @param value Comma delimited list of files
     * @return List of String file names, which are trimmed and
     * have unescaped any embedded commas ("%2C" -> ",")
     */
    protected List toFileList(String value) {
        List values = new ArrayList();
        if (value == null) {
            return values;
        }

        StringTokenizer tok =
            new StringTokenizer(value, FILE_DELIM);

        while (tok.hasMoreTokens()) {
            String val = ((String)tok.nextToken()).trim(); 
            values.add(StringUtil.replace(val,
                                          FILE_DELIM_ESC,
                                          FILE_DELIM));
        }

        return values;
    }
    
    /**
     * Expand a String to array of absolute file names.
     * @param value Expanded using toFileList()
     * @param dir Parent directory used to resolve relative file names.
     * @return Array of absolute file names.
     */
    protected String[] getAbsoluteFiles(String value, String dir) {
        if (dir == null) {
            dir = File.separator; 
        }

        List files = toFileList(value);
        int size = files.size();
        String[] absoluteFiles = new String[size];

        for (int i=0; i<size; i++) {
            File file = new File((String)files.get(i));
            if (!file.isAbsolute()) {
                file = new File(dir, file.toString());
            }
            absoluteFiles[i] = file.toString();
        }

        return absoluteFiles;
    }

    public boolean isNetStatEnabled() {
        return !"false".equals(getManagerProperty(PROP_NETSTAT));
    }

    /**
     * @return true if the current platform is in the Windows family.
     */
    public static boolean isWin32() {
        return isWin32;
    }

    static void setPlatformName(String name) {
        hostname = name;
    }

    /**
     * Method to assist with naming of resources.
     * 
     * @return The hostname of the current platform.
     */
    public static String getPlatformName(){
        if (hostname == null) {
            try {
                hostname =
                    InetAddress.getLocalHost().getHostName();
            } catch(Exception exc){
                Sigar sigar = new Sigar();
                try {
                    hostname =
                        sigar.getNetInfo().getHostName();
                } catch (SigarException e) {
                    hostname = "localhost.unknown"; // Unlikely to occur
                } finally {
                    sigar.close();
                }
            }
        }
        
        return hostname;
    }

    /**
     * Unique name used by PluginManager.getPlugin
     * @return Name of the plugin instance.
     */
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPluginVersion() {
		return version;
	}

	public void setPluginVersion(String version) {
		this.version = version;
	}

	/**
     * @return The TypeInfo of this plugin from ProductPlugin.getTypes
     */
    public TypeInfo getTypeInfo() {
        return this.type;
    }
  
    public void setTypeInfo(TypeInfo type) {
        this.type = type;
    }

    /**
     * The ConfigSchema used to render config options for this resource
     * in the UI and client shell.
     * @param info The TypeInfo of this plugin from ProductPlugin.getTypes
     * @param config ConfigReponse of the parent resource (if any).
     * @return ConfigSchema for this resource.
     */
    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        return new ConfigSchema();
    }

    public ConfigSchema getCustomPropertiesSchema(String name) {
        ConfigSchema schema = null;
        if (this.data != null) {
            schema = this.data.getCustomPropertiesSchema(name);
        }
        if (schema == null) {
            schema = new ConfigSchema();
        }
        return schema;
    }

    public ConfigSchema getCustomPropertiesSchema(TypeInfo info) {
        return getCustomPropertiesSchema(info.getName());
    }

    public ConfigSchema getCustomPropertiesSchema() {
        return getCustomPropertiesSchema(getTypeInfo());
    }

    public String getManagerProperty(String key) {
        return this.manager.getProperty(key);
    }

    /**
     * Get a value defined by a &lt;property&gt; tag in the plugin's hq-plugin.xml 
     */
    public String getPluginProperty(String name) {
        if (this.data == null) {
            return null;
        }
        return this.data.getProperty(name);
    }

    /**
     * Get all properties defined using &lt;property&gt; at the top-level
     * in the plugin's hq-plugin.xml
     */
    public Properties getProperties() {
        return this.data.getProperties();
    }

    /**
     * Get all properties defined using &lt;property&gt; within the resource 
     * tag (platform|server|service) for this type.
     */
    public Properties getTypeProperties() {
        //XXX ugly, should just store resource properties
        //in another Map
        Properties props = new Properties();
        Map global = PluginData.getGlobalProperties();

        String prefix = getTypeInfo().getName() + ".";

        for (Iterator it=global.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();

            if (!key.startsWith(prefix)) {
                continue;
            }
            props.put(key.substring(prefix.length()),
                                    entry.getValue());
        }

        return props;
    }

    /**
     * Get a value defined by a &lt;property&gt; tag in the plugin's hq-plugin.xml,
     * within a &lt;server&gt; or &lt;service&gt; tag for this plugin's type.
     * <p>
     * Same as:
     * <code>
     * getProperty(type + "." + name);
     * </code> 
     */
    public String getTypeProperty(String type, String name) {
        return getPluginProperty(type + "." + name);
    }

    /**
     * If name property is not found for the given type,
     * try using the type's parent.  If not found using parent's
     * type, try using top-level properties from hq-plugin.xml.
     */
    public String getTypeProperty(TypeInfo type, String name) {
        String value = getTypeProperty(type.getName(), name);
        if (value != null) {
            return value;
        }
        if (type.getType() == TypeInfo.TYPE_SERVICE) {
            ServiceTypeInfo service = (ServiceTypeInfo)type;
            return getTypeProperty(service.getServerName(), name);
        }
        return getPluginProperty(name);
    }

    /**
     * Shortcut, same as:
     * <code>
     * getTypeProperty(getTypeInfo().getName(), name);
     * </code> 
     */
    public String getTypeProperty(String name) {
        return getTypeProperty(getTypeInfo().getName(), name);
    }

    /**
     * Shortcut, same as:
     * <code>
     * getTypeProperty(type, PROP_NAME);
     * </code> 
     */
    public String getTypeNameProperty(String type) {
        return getTypeProperty(type, PROP_NAME);
    }
    
    /**
     * Shortcut, same as:
     * <code>
     * getTypeNameProperty(getTypeInfo().getName());
     * </code> 
     */
    public String getTypeNameProperty() {
        return getTypeNameProperty(getTypeInfo().getName());
    }

    /**
     * Called when the plugin is loaded on the server and on the agent side.
     * @param manager The plugin manager for this plugin type.
     */
    public void init(PluginManager manager)
        throws PluginException {
        this.manager = manager;
    }

    /**
     * Called when the server or agent is shutdown.
     * Use this method to cleanup any resources that were created
     * during the init() method.
     * @throws PluginException
     */
    public void shutdown()
        throws PluginException {
    }

    public ConfigResponse getConfig() {
        return this.config;
    }

    /**
     * Shortcut, same as getConfig().getValue(key)
     */
    public String getConfig(String key) {
        ConfigResponse config = getConfig();
        if (config == null) {
            return null;
        }
        return config.getValue(key);
    }

    public void configure(ConfigResponse config)
        throws PluginException {

        this.config = config;
    }

    public String getDefaultInstallPath() {
        return
            ProductPlugin.DEFAULT_INSTALLPATH +
            File.separator +
            getTypeInfo().getFormattedName();
    }

    /**
     * @return ".bat" if isWin32() else ".sh"
     */
    public static String getScriptExtension() {
        return getScriptExtension(isWin32());
    }

    /**
     * @return ".bat" if info.isWin32Platform() else ".sh"
     */
    public String getScriptExtension(TypeInfo info) {
        return getScriptExtension(info.isWin32Platform());
    }
    
    public static String getScriptExtension(boolean isWin32) {
        return "." + (isWin32 ? "bat" : "sh");        
    }
    
    /**
     * Default name for getLog() method.
     */
    protected String getLogName() {
        //XXX '.' in versions effs things, logger things is a package delim
        //return getTypeInfo().getName();
        return this.getClass().getName();
    }

    /**
     * Wrapper for LogFactory.getLog which uses the name
     * returned by getLogName().
     */
    protected Log getLog() {
        return LogFactory.getLog(getLogName());
    }

    ProductPlugin getProductPlugin(String name) {
        ProductPluginManager ppm =
            (ProductPluginManager)this.manager.getParent();
        PluginInfo info = this.manager.getPluginInfo(name);
        if (info == null) {
            //e.g. lookup for service name during autodiscovery
            //no autodiscovery plugin registered for the service name
            info =
                ppm.getMeasurementPluginManager().getPluginInfo(name);
        }
        if (info == null) {
            String msg = "Can't find PluginInfo for: " + name;
            throw new IllegalArgumentException(msg);
        }
        return ppm.getProductPlugin(info.product);
    }

    /**
     * Get the ProductPlugin that defined the TypeInfo for this
     * plugin instance.
     */
    public ProductPlugin getProductPlugin() {
        if (this instanceof ProductPlugin) {
            return (ProductPlugin)this;
        }
        if (this.productPlugin == null) {
            this.productPlugin = getProductPlugin(getName());
        }
        //XXX seems this is the correct name to use and the above should be removed?
        if ((this.productPlugin == null) && (getTypeInfo() != null)) {
            this.productPlugin = getProductPlugin(getTypeInfo().getName());
        }
        return this.productPlugin;
    }

    /**
     * Any jars that exist relative to the given installpath
     * will be added to the plugin's classpath.
     */
    protected void adjustClassPath(String installpath) {

        ClassLoader cl = this.data.getClassLoader();
        if (!(cl instanceof PluginLoader)) {
            return;
        }
        PluginLoader loader = (PluginLoader)cl;

        List classpath = this.data.getClassPath();

        if (classpath == null) {
            return;
        }

        boolean isDebug = getLog().isDebugEnabled();
        URL[] orig = null;
        if (isDebug) {
            orig = loader.getURLs();
        }

        for (int i=0; i<classpath.size(); i++) {
            String path = (String)classpath.get(i);
            File file = new File(path);

            if (file.isAbsolute() || file.exists()) {
                continue;
            }

            file = new File(installpath, path);
            if ((path.indexOf('*') != -1) || file.exists()) {
                try {
                    loader.addURL(file.toString());
                } catch (PluginLoaderException e) {
                    continue;
                }
            }
        }

        if (isDebug) {
            URL[] curr = loader.getURLs();
            if (curr.length > orig.length) {
                for (int i=orig.length; i<curr.length; i++) {
                    getLog().debug("classpath += " +
                                   curr[i].getFile());
                }
            }
            else {
                getLog().debug("classpath unchanged using: " +
                               installpath);
            }
        }
    }

    /**
     * Wrapper around ClassLoader.getResource/getResourceAsStream
     * to open a resource from this plugin's .jar file.
     */
    public InputStream openResource(String name)
        throws IOException {
        
        ClassLoader loader = this.getClass().getClassLoader();
        InputStream is = PluginData.openPluginResource(loader, name);
        if (is == null) {
            throw new FileNotFoundException("Cannot find: " + name);
        }
        return is;
    }

    public String getPluginClassName(String pluginType, String resourceType) {
        return this.data.getPlugin(pluginType, resourceType);
    }
}

