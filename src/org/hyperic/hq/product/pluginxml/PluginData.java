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

package org.hyperic.hq.product.pluginxml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TypeBuilder;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EnumerationConfigOption;

public class PluginData {
    public static final String PLUGIN_XML = "etc/hq-plugin.xml";
    public static final String PLUGIN_PROPERTIES = "etc/plugin.properties";
    
    private static final String PLUGINS_PREFIX = "pdk/plugins/";
    private static final Log log = LogFactory.getLog("PluginData");
    private static final TypeInfo[] NO_TYPES = new TypeInfo[0];
    private static HashMap cache = new HashMap();

    Map scratch;
    PluginParser parser;

    private TypeInfo[] types = NO_TYPES;
    private Map metricStash = new HashMap();
    private Map pluginImpls = new HashMap();

    String name = null;
    String file = null;
    ClassLoader loader;
    Map fileScanIncludes = new HashMap();
    Map rgyScanIncludes = new HashMap();
    Map rgyScanKeys = new HashMap();
    Map help = new HashMap();
    Map config = new HashMap();
    Map cprops = new HashMap();
    static Map sharedConfig = new HashMap();
    Map actions = new HashMap();
    private List classpath = null;
    private Properties properties = new Properties();
    private static Properties globalProperties = new Properties();
    private static Map serviceExtensions = null;
    private static Map serviceInventoryPlugins = new HashMap();
    List includes = new ArrayList();

    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    String getPluginName() {
        if (this.name != null) {
            return this.name;
        }
        if (this.file != null) {
            return ProductPluginManager.getNameFromFile(this.file);
        }
        return null;
    }

    public String getFile() {
        return this.file;
    }
    
    public void setFile(String file) {
        this.file = file;
    }

    public ClassLoader getClassLoader() {
        return this.loader;
    }

    public List getIncludes() {
        return this.includes;
    }

    public static void deployed(ClassLoader loader) {
        PluginData data = (PluginData)cache.get(loader);
        if (data != null) {
            data.deployed();
        }
    }

    static class PluginResolver implements EntityResolver {
        private PluginData data;
        private ClassLoader loader;

        PluginResolver(PluginData data) {
            this(data, null);
        }

        PluginResolver(PluginData data, ClassLoader loader) {
            this.data = data;
            this.loader = loader;
        }

        private String resolveParentFile(String name) {
            if (this.data.getFile() != null) {
                //look for the file using the plugin's location
                File dir = new File(this.data.getFile()).getParentFile();
                while (dir != null) {
                    File resolved = new File(dir, name);

                    if (resolved.exists()) {
                        return resolved.toString();
                    }

                    dir = dir.getParentFile();
                }
            }

            return name;            
        }

        private boolean isPluginFile(String name) {
            return name.startsWith(PLUGINS_PREFIX);
        }

        private String resolvePluginFile(String name) {
            String dir;

            String pdkDir = ProductPluginManager.getPdkDir();
            if (pdkDir != null) {
                dir = pdkDir + "/plugins";
            }
            else {
                dir = ProductPluginManager.getPdkPluginsDir();
                if (dir == null) {
                    return resolveParentFile(name);
                }
            }

            return dir + "/" + name.substring(PLUGINS_PREFIX.length());            
        }

        //resolve external references to files in 
        //the plugin directories
        private String resolveFile(String name) {
            
            if (isPluginFile(name)) {
                return resolvePluginFile(name);
            }
            else {
                return resolveParentFile(name);
            }
        }

        public InputSource resolveEntity(String publicId, String systemId) {
            log.debug("resolveEntity: public=" + publicId + ", systemId=" + systemId);

            try {
                String name = null;
                //WTF.  certain xerces impls will pass the relative uri as-is
                //others prepend the file:// protocol.
                if (systemId.startsWith("/")) {
                    name = systemId;
                }
                else if (systemId.startsWith("file:/")) {
                    name = new URL(systemId).getFile();
                }

                if (name != null) {
                    String resolvedName;
                    if (name.startsWith("/")) {
                        name = name.substring(1);
                    }
                    InputStream is = null;
                    if (this.loader != null) {
                        is = openPluginResource(this.loader, name);
                    }
                    if (is == null) {
                        resolvedName = resolveFile(name);
                        log.debug("resolveEntity: " +
                                  name + "->" + resolvedName);
                        is = new FileInputStream(resolvedName);
                        this.data.includes.add(resolvedName);
                    }
                    return new InputSource(is);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public synchronized static PluginData getInstance(ProductPluginManager manager,
                                                      ClassLoader loader,
                                                      String file) 
        throws PluginException {

        PluginData data = (PluginData)cache.get(loader);
        if (data != null) {
            return data;
        }

        PluginParser parser = new PluginParser();
        boolean isServer = manager.getRegisterTypes();
        if (!isServer) {
            parser.collectHelp(false);
            parser.collectMetrics(false);
        }

        boolean isJar = file.endsWith(".jar");
        InputStream is = null;
        data = new PluginData();
        data.file = file;

        if (isJar) {
            try {
                is = openPluginResource(loader, PLUGIN_PROPERTIES);
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    data.properties.putAll(props);
                }
            } catch (FileNotFoundException e) {
                //optional
            } catch (IOException e) {
                throw new PluginException(e.getMessage(), e);
            } finally {
                if (is != null) {
                    try { is.close(); } catch (IOException e) { }
                    is = null;
                }
            }
        }
        EntityResolver resolver = null;
        try {
            if (isJar) {
                is = openPluginResource(loader, PLUGIN_XML);
                if (is == null) {
                    String name = "etc/" + data.getPluginName() + "-plugin.xml";
                    is = openPluginResource(loader, name);
                    if (is == null) {
                        log.debug(file + "!" + PLUGIN_XML + " does not exist");
                    }
                }
                resolver = new PluginResolver(data, loader);
            }
            else {
                is = new FileInputStream(file);
                resolver = new PluginResolver(data);
            }

            if (is != null) {
                parser.parse(is, data, resolver);
            }
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) { }
            }
            is = null;
        }
        
        data.loader = loader;
        cache.put(loader, data);
        
        return data;
    }

    public static InputStream openPluginResource(ClassLoader loader, String file)
        throws IOException {

        InputStream is = null;

        if (log.isTraceEnabled()) {
            Object url = loader.getResource(file);
            log.trace(file + " => " + url);
        }

        is = loader.getResourceAsStream(file);

        return is;
    }

    List getMetrics(String name, boolean create) {
        ArrayList metrics = (ArrayList)this.metricStash.get(name);

        if ((metrics == null) && create) {
            metrics = new ArrayList();
            this.metricStash.put(name, metrics);
        }

        return metrics;
    }
    
    public List getMetrics(String name) {
        return getMetrics(name, false);
    }
    
    void addMetric(String name, MeasurementInfo metric) {
        getMetrics(name, true).add(metric);
    }
    
    public class ServiceExtension {
        public ServiceTypeInfo service;
        public PluginData data;
        
        ServiceExtension(ServiceTypeInfo service, PluginData data) {
            this.service = service;
            this.data = data;
        }
    }

    public static ServiceExtension getServiceExtension(String name) {
        if (serviceExtensions == null) {
            return null;
        }
        return (ServiceExtension)serviceExtensions.get(name);
    }

    String qualifiedPluginClass(String implClass) {
        if ((implClass != null) && (implClass.indexOf(".") == -1)) {
            //<product package="...">
            String pluginPackage =
                getProperty(ProductTag.ATTR_PACKAGE);

            if (pluginPackage != null) {
                return pluginPackage + "." + implClass;
            }
        }
        return implClass;
    }

    void addPlugin(String type, String typeName, String implClass) {
        Map plugins = (Map)this.pluginImpls.get(type);
        if (plugins == null) {
            plugins = new HashMap();
            this.pluginImpls.put(type, plugins);
        }

        plugins.put(typeName, qualifiedPluginClass(implClass));
    }

    private String getPlatformName(TypeInfo info) {
        String name = info.getName();
        String[] platforms = info.getPlatformTypes();
        if (platforms.length == 1) {
            name += " " + platforms[0];
        }
        return name;
    }
    
    public String getPlugin(String type, TypeInfo info) {
        String plugin = getPlugin(type, getPlatformName(info));
        if (plugin != null) {
            return plugin;
        }
        return getPlugin(type, info.getName());
    }

    public String getPlugin(String type, String typeName) {
        Map plugins = (Map)this.pluginImpls.get(type);
        if (plugins == null) {
            return null;
        }
        return (String)plugins.get(typeName);
    }

    void addControlActions(String typeName, List actions) {
        List controlActions = getControlActions(typeName);
        if (controlActions == null) {
            controlActions = new ArrayList();
            this.actions.put(typeName, controlActions);
        }
        controlActions.addAll(actions);
    }

    public List getControlActions(TypeInfo info) {
        List actions = getControlActions(getPlatformName(info));
        if (actions != null) {
            return actions;
        }
        return getControlActions(info.getName());
    }
    
    public List getControlActions(String typeName) {
        return (List)this.actions.get(typeName);
    }

    public TypeInfo[] getTypes() {
        return types;
    }

    void addTypes(TypeInfo[] types) {
        if (this.types == NO_TYPES) {
            this.types = types;
        }
        else {
            this.types =
                (TypeInfo[])ArrayUtil.merge(this.types, types,
                                            new TypeInfo[0]);
        }
    }

    /**
     * Add a service to server type which may be defined in
     * another plugin.
     */
    void addServiceExtension(ServiceTypeInfo service) {
        addTypes(new TypeInfo[] { service});
        if (serviceExtensions == null) {
            serviceExtensions = new HashMap();
        }
        ServiceExtension ext = new ServiceExtension(service, this); 
        serviceExtensions.put(service.getName(), ext);
    }

    public Map getServiceInventoryPlugins(String serverType) {
        return (Map)serviceInventoryPlugins.get(serverType);
    }

    void addServiceInventoryPlugin(String serverType, String serviceType, String name) {
        Map services = getServiceInventoryPlugins(serverType); 
        if (services == null) {
            services = new HashMap();
            serviceInventoryPlugins.put(serverType, services);
        }
        services.put(serviceType, qualifiedPluginClass(name));
    }

    public List getClassPath() {
        return this.classpath;
    }
    
    void setClassPath(List classpath) {
        this.classpath = new ArrayList();
        this.classpath.addAll(classpath);
    }

    public String getHelp(String name) {
        return (String)this.help.get(name);
    }
    
    public List getFileScanIncludes(String name) {
        return (List)this.fileScanIncludes.get(name);
    }
    
    void addFileScanIncludes(String name, List sigs) {
        List serverSigs = getFileScanIncludes(name);

        if (serverSigs == null) {
            serverSigs = new ArrayList();
        }

        serverSigs.addAll(sigs);

        this.fileScanIncludes.put(name, serverSigs);
    }
    
    public List getRegistryScanIncludes(String name) {
        return (List)this.rgyScanIncludes.get(name);
    }
    
    void addRegistryScanIncludes(String name, List sigs) {
        List serverSigs = getRegistryScanIncludes(name);

        if (serverSigs == null) {
            serverSigs = new ArrayList();
        }

        serverSigs.addAll(sigs);

        this.rgyScanIncludes.put(name, serverSigs);
    }
    
    public List getRegistryScanKeys(String name) {
        return (List)this.rgyScanKeys.get(name);
    }
    
    void addRegistryScanKey(String name, String key) {
        List keys = getRegistryScanKeys(name);

        if (keys == null) {
            keys = new ArrayList();
        }

        keys.add(key);

        this.rgyScanKeys.put(name, keys);
    }
    
    public ConfigSchema getConfigSchema(TypeInfo info, int type) {
        ConfigSchema schema = getConfigSchema(getPlatformName(info), type);
        if (schema != null) {
            return schema;
        }
        return getConfigSchema(info.getName(), type);
    }
    
    public ConfigSchema getConfigSchema(String name, int type) {
        ConfigSchema[] schemas = (ConfigSchema[])this.config.get(name);
        if (schemas == null) {
            return null;
        }
        return schemas[type];
    }
    
    void addConfigSchema(String name, int type, ConfigSchema schema) {
        ConfigSchema[] schemas = (ConfigSchema[])this.config.get(name);
        if (schemas == null) {
            schemas = new ConfigSchema[4];
            this.config.put(name, schemas);
        }
        schemas[type] = schema;
    }
    
    static ConfigSchema getSharedConfigSchema(String name) {
        return (ConfigSchema)sharedConfig.get(name);
    }

    public ConfigSchema getCustomPropertiesSchema(TypeInfo info) {
        return getCustomPropertiesSchema(info.getName());
    }
    
    public ConfigSchema getCustomPropertiesSchema(String name) {
        ConfigSchema schema = (ConfigSchema)this.cprops.get(name);

        if (schema == null) {
            ServiceExtension ext =
                PluginData.getServiceExtension(name);
            if (ext != null) {
                return (ConfigSchema)ext.data.cprops.get(name);
            }
        }

        return schema;
    }
    
    void addCustomPropertiesSchema(String name, ConfigSchema schema) {
        ConfigSchema cpropSchema = getCustomPropertiesSchema(name);

        if (cpropSchema == null) {
            cpropSchema = schema;
        }
        else {
            cpropSchema.addOptions(schema.getOptions());
        }
        
        this.cprops.put(name, cpropSchema);
    }
    
    public static void addSharedConfigSchema(String name, ConfigSchema schema) {
        sharedConfig.put(name, schema);
    }

    //parsing helpers
    String applyFilters(String s) {
        return this.parser.applyFilters(s);
    }

    void addFilter(String key, String value) {
        this.parser.addFilter(key, value);
    }

    void addFilters(Map props) {
        this.parser.addFilters(props);
    }

    String getFilter(String key) {
        return this.parser.getFilter(key);
    }
    
    public void setProperty(String key, String value) {
        this.properties.setProperty(key, value);
    }

    /**
     * Get a property by name
     */
    public String getProperty(String key) {
        String val = this.properties.getProperty(key);
        if (val == null) {
            return getGlobalProperty(key); 
        }
        else {
            return val;
        }
    }
    
    /**
     * Get all defined properites
     */
    public Properties getProperties() {
        return this.properties;
    }

    void setGlobalProperty(String key, String value) {
        addFilter(key, value);
        globalProperties.setProperty(key, value);
    }

    public static String getGlobalProperty(String key) {
        return globalProperties.getProperty(key);
    }

    public static Map getGlobalProperties() {
        return globalProperties;
    }

    //e.g. "JBoss 3.2 JGroups Channel.OBJECT_NAME" ->
    //     "JBoss 4.0 JGroups Channel.OBJECT_NAME"
    void includeGlobalProperties(String fromType, String toType) {
        Properties props = new Properties();
        for (Iterator it=globalProperties.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();
            if (key.startsWith(fromType + ".")) {
                String val = (String)entry.getValue();
                key = toType + key.substring(fromType.length());
                props.setProperty(key, val);
            }
        }
        globalProperties.putAll(props);
    }

    //cleanup resources no longer needed after the plugin
    //has been deployed
    public void deployed() {
        this.metricStash.clear();
        this.metricStash = new HashMap();
    }
    
    Map getTypeMap() {
        HashMap map = new HashMap();

        for (int i=0; i<this.types.length; i++) {
            int type = this.types[i].getType(); 
            if ((type == TypeInfo.TYPE_SERVER) ||
                (type == TypeInfo.TYPE_PLATFORM))
            {
                map.put(this.types[i], new ArrayList());
            }
        }

        for (int i=0; i<this.types.length; i++) {
            if (this.types[i].getType() == TypeInfo.TYPE_SERVICE) {
                ServiceTypeInfo type = (ServiceTypeInfo)this.types[i];
                List services = (List)map.get(type.getServerTypeInfo());
                services.add(type);
            }
        }

        return map;
    }

    void dumpConfigXML(PrintStream out, TypeInfo type, String indent) {
        for (int i=0; i<ProductPlugin.CONFIGURABLE_TYPES.length; i++) {
            dumpConfigXML(out, type, i, indent);
        }
    }

    void dumpConfigXML(PrintStream out,
                       TypeInfo type,
                       int typeIndex,
                       String indent) {
        ConfigSchema schema = getConfigSchema(type, typeIndex);
        if (schema == null) {
            return;
        }
        String typeName = ProductPlugin.CONFIGURABLE_TYPES[typeIndex];
        out.println(indent + "<config type=" + typeName + ">");
        List options = schema.getOptions();
        for (int i=0; i<options.size(); i++) {
            ConfigOption option = (ConfigOption)options.get(i);
            out.println(indent + "  <option name=" + option.getName());
            out.println(indent + "          description=" +
                        option.getDescription());
            out.println(indent + "          default=" +
                        option.getDefault() + "/>");
            if (option instanceof EnumerationConfigOption) {
                EnumerationConfigOption eoption =
                    (EnumerationConfigOption)option;
                List values = eoption.getValues();
                for (int j=0; j<values.size(); j++) {
                    out.println(indent +
                                "    <include name=" + values.get(j) + "/>");
                }
            }
        }
        out.println(indent + "</config>");
    }
    
    void dumpPluginXML(PrintStream out, TypeInfo type, String indent) {
        for (int i=0; i<ProductPlugin.TYPES.length; i++) {
            dumpPluginXML(out, type, ProductPlugin.TYPES[i], indent);
        }
    }
    
    void dumpPluginXML(PrintStream out, TypeInfo type, String pluginType,
                       String indent) {
        String plugin = getPlugin(pluginType, type);
        if (plugin == null) {
            return;
        }
        out.println("");
        out.println(indent + "<plugin type=" + pluginType);
        out.println(indent + "        class=" + plugin + "/>");

        if (!pluginType.equals("control")) {
            return;
        }
        List actions = getControlActions(type);
        if (actions != null) {
            out.println("\n" + indent + "<actions>");
            for (int i=0; i<actions.size(); i++) {
                out.println(indent + "  <include name=" +
                        actions.get(i) + "/>");
            }
            out.println(indent + "</actions>");
        }
    }

    void dumpMetricsXML(PrintStream out, TypeInfo type, String indent) {
        List metrics = getMetrics(type.getName());
        if (metrics == null) {
            return;
        }
        out.println("");
        for (int i=0; i<metrics.size(); i++) {
            MeasurementInfo metric = (MeasurementInfo)metrics.get(i);
            out.println(metric.toXML(indent));
        }
    }
    
    void dumpHelpXML(PrintStream out, TypeInfo type, String indent) {
        String help = getHelp(type.getName());
        if (help == null) {
            return;
        }
        out.println("");
        out.println(indent + "<help>");
        out.println(help);
        out.println(indent + "</help>");
    }
    
    void dumpServerXML(PrintStream out, ServerTypeInfo server, List services) {
        String[] platforms = server.getPlatformTypes();
        out.println("  <server name=" + server.getName());
        String platform = null;
        if (platforms.length == 1) {
            platform = platforms[0];
        }
        else if (platforms == TypeBuilder.UNIX_PLATFORM_NAMES) {
            platform = "Unix";
        }
        if (platform != null) {
            out.println("          platforms=" + platform);
        }
        out.println("          description=" +
                    server.getDescription() + ">");
        dumpConfigXML(out, server, "    ");
        dumpPluginXML(out, server, "    ");
        dumpHelpXML(out, server, "    ");
        dumpMetricsXML(out, server, "    ");

        List fileScan = getFileScanIncludes(server.getName());
        if (fileScan != null) {
            out.println("    <scan type=file>");
            for (int i=0; i<fileScan.size(); i++) {
                out.println("      <include name=" + 
                            fileScan.get(i) + "/>");
            }
            out.println("    </scan>");
        }
        
        List rgyIncludes = getRegistryScanIncludes(server.getName());
        List rgyKeys = getRegistryScanKeys(server.getName());

        if (rgyKeys != null) {
            for (int i=0; i<rgyKeys.size(); i++) {
                out.println("    <scan registry=" + rgyKeys.get(i) + ">");
                for (int j=0; j<rgyIncludes.size(); j++) {
                    out.println("      <include name=" + 
                                rgyIncludes.get(j) + "/>");
                }
                out.println("    </scan>");
            }
        }
        out.println("");
        
        for (int i=0; i<services.size(); i++) {
            ServiceTypeInfo service = (ServiceTypeInfo)services.get(i);
            out.println("    <service name=" + service.getName() + ">");
            dumpConfigXML(out, service, "        ");
            dumpPluginXML(out, service, "        ");
            dumpHelpXML(out, service, "        ");
            dumpMetricsXML(out, service, "        ");
            out.println("    </service>\n");
        }
        
        out.println("  </server>\n");        
    }
    
    public void dumpXML() {
        PrintStream out = System.out;
        out.println("<plugin>");
        List classpath = getClassPath();

        if (classpath != null) {
            out.println("  <classpath>");
            for (int i=0; i<classpath.size(); i++) {
                out.println("    <include name=" +
                            classpath.get(i) + "/>");
            }
            out.println("  </classpath>");
        }

        Map typeMap = getTypeMap();

        for (Iterator it=typeMap.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            Object resource = entry.getKey();
            if (resource instanceof ServerTypeInfo) {
                ServerTypeInfo server = (ServerTypeInfo)resource;
                List services = (List)entry.getValue();
                dumpServerXML(out, server, services);
            }
            else {
                PlatformTypeInfo platform = (PlatformTypeInfo)resource;
                out.println("  <platform name=" + platform.getName() + "\"/>");
            }
        }
        
        out.println("</plugin>");
    }
}
