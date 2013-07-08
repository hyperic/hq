/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.product;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.util.PluginLoader;
import org.hyperic.util.PluginLoaderException;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.security.MD5;

/**
 * This class is a manager for ProductPlugin implementations and is also a
 * manager of plugin managers.
 */
public class ProductPluginManager
    extends PluginManager {
    public static final String PROP_PDK_DIR = AgentConfig.PDK_DIR_KEY;

    private static final String PROP_PDK_PLUGINS_DIR = AgentConfig.PDK_PLUGIN_DIR_KEY;

    private static final String PROP_PDK_WORK_DIR = AgentConfig.PDK_WORK_DIR_KEY;

    // this is really verbose and not very helpful
    static final boolean DEBUG_LIFECYCLE = false;

    private static final String PLUGIN_STUB_NAME = "org.hyperic.hq.product.ProductPluginXML";

    private static final String PLUGIN_STUB = "org/hyperic/hq/product/ProductPluginXML.stub";

    // absolute must-have
    private static final String SYSTEM_PLUGIN = "system";

    private static final String[] BASE_PLUGINS = { "netservices", // many
                                                                  // plugins
                                                                  // depend on
                                                                  // this
                                                  "sqlquery", // for sql:
                                                              // metrics
    };

    // support plugins loaded before product plugins
    private static final String[] PLUGIN_SUPPORT_DIRS = { "scripting" };

    private boolean registerTypes = false;
    private boolean client;
    private HashMap<String, PluginManager> managers = new HashMap<String, PluginManager>();
    private Map<String, Map<String, TypeInfo>> types = new HashMap<String, Map<String, TypeInfo>>();
    private HashMap includePlugins = null;
    private HashMap excludePlugins = null;
    private Log log = LogFactory.getLog(this.getClass().getName());
    private byte[] pluginStub = null;
    private int pluginStubLength = 0;
   

    private static final File HQ_DIR = new File(System.getProperty("user.home"), ".hq");

    public static final File PLUGIN_PROPERTIES_FILE = new File(HQ_DIR, "plugin.properties");

    public static final String PROPERTY_PREFIX = "hq.plugins.";

    private static final Map<String, String> JAVA_VERSIONS = new HashMap<String, String>();

    // java.class.version.major -> java.version
    static {
        JAVA_VERSIONS.put("48", "1.4");
        JAVA_VERSIONS.put("49", "1.5");
        JAVA_VERSIONS.put("50", "1.6");
        JAVA_VERSIONS.put("51", "1.7");
    }

    private MeasurementPluginManager mpm;
    private ControlPluginManager cpm;
    private AutoinventoryPluginManager apm;
    private RtPluginManager rpm;
    private LogTrackPluginManager ltpm;
    private ConfigTrackPluginManager ctpm;
    private LiveDataPluginManager ldpm;

    public ProductPluginManager() {
        this(System.getProperties());
    }

    /**
     * If true creates a mapping of the ProductPlugin TypeInfos, only needed on
     * the server side.
     */
    public void setRegisterTypes(boolean registerTypes) {
        this.registerTypes = registerTypes;
    }

    public boolean getRegisterTypes() {
        return this.registerTypes;
    }

    public static String getPropertyKey(String plugin, String key) {
        return PROPERTY_PREFIX + plugin + "." + key;
    }

    // ignore failures. if you want to check for errors, do it yourself.
    private static Properties getFileProperties(File file) {
        Properties props = new Properties();
        FileInputStream is = null;

        try {
            is = new FileInputStream(file);
            props.load(is);
        } catch (IOException e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ie) {
                }
            }
        }

        return props;
    }

    public ProductPluginManager(File file) {
        this(getFileProperties(file));
    }

    public ProductPluginManager(Properties props) {
        super(props);
    }

    public String getName() {
        return ProductPlugin.TYPE_PRODUCT;
    }

    /**
     * Derive plugin name from file name-plugin.ext
     */
    public static String getNameFromFile(String file) {
        String name = new File(file).getName();
        int ix = name.indexOf("-plugin.");
        if (ix != -1) {
            return name.substring(0, ix);
        } else {
            return null;
        }
    }

    // assumes type names are unique across plugins,
    // which they should be. if needed we could index on
    // product name too.
    /**
     * Find TypeInfo for the given platform and type name.
     * @param platform The platform name, e.g. "Linux"
     * @param name The type name, e.g. "Apache 2.0"
     */
    public TypeInfo getTypeInfo(String platform, String name) {
        Map<String, TypeInfo> platforms = this.types.get(platform);

        if (platforms == null) {
            return null;
        }

        return platforms.get(name);
    }

    /**
     * Find TypeInfo's accross all platforms for the given type name.
     * @param name The type name, e.g. "Apache 2.0"
     * @return  map<platformName, typeInfo> the type per platform
     */    
    public Map<String, TypeInfo> getTypeInfo(String typeName)  {
        Map<String, TypeInfo> typeInfos = Collections.emptyMap();
        
        if((null != typeName) && (typeName.length() > 0)) {
            typeInfos = new HashMap<String, TypeInfo>(10);
            final Map<String, Map<String, TypeInfo>> types = getTypes();
            for(final Entry<String, Map<String, TypeInfo>> platform:types.entrySet()) {
                final TypeInfo typeInfo = platform.getValue().get(typeName);
                if (null != typeInfo)
                    typeInfos.put(platform.getKey(), typeInfo);
            }
        }
        return typeInfos;
    }        
    
    public Map<String, Map<String, TypeInfo>> getTypes() {
        return Collections.unmodifiableMap(types);
    }
    
    protected void setTypeInfo(String platform, String name, TypeInfo info) {

        Map<String, TypeInfo> platforms = this.types.get(platform);

        if (platforms == null) {
            platforms = new HashMap<String, TypeInfo>();
            this.types.put(platform, platforms);
        }

        platforms.put(name, info);
    }

    // XXX we could just cache lookups in getTypeInfo
    // instead of mapping everything.
    /**
     * Create a mapping of product plugin TypeInfos.
     * @see #getTypeInfo
     * @see #registerPluginJar(String jarName)
     */
    private void registerTypeInfo(TypeInfo[] types) {
        if (types == null) {
            return;
        }

        for (int i = 0; i < types.length; i++) {
            TypeInfo type = types[i];
            String[] platforms = type.getPlatformTypes();

            for (int j = 0; j < platforms.length; j++) {
                setTypeInfo(platforms[j], type.getName(), type);
            }
        }
    }

    private String[] getPluginNames(String plugins) {
        if (plugins == null) {
            return null;
        }

        List<String> names = StringUtil.explode(plugins, ",");
        return names.toArray(new String[0]);
    }

 
    private void initPluginFilters() {
        HashMap<String, Integer> basePlugins = new HashMap<String, Integer>();
        basePlugins.put(SYSTEM_PLUGIN, // must-have
            new Integer(TypeInfo.TYPE_PLATFORM));
        String[] defaultPlugins;
        String base = getProperty("plugins.base");
        if (base == null) {
            defaultPlugins = BASE_PLUGINS;
        } else {
            defaultPlugins = getPluginNames(base);
        }

        for (int i = 0; i < defaultPlugins.length; i++) {
            basePlugins.put(defaultPlugins[i], new Integer(TypeInfo.TYPE_SERVER));
        }

       

        String include = getProperty("plugins.include");
        String exclude = getProperty("plugins.exclude");
        if ((include != null) && (exclude != null)) {
            log.warn("plugins.{include,exclude} are both defined" + ", use one or the other.");
        }

        if (include != null) {
            this.includePlugins = new HashMap();
            String[] plugins = getPluginNames(include);
            for (int i = 0; i < plugins.length; i++) {
                this.includePlugins.put(plugins[i], Boolean.TRUE);
            }
            // must-haves
            this.includePlugins.putAll(basePlugins);
        }

        if (exclude != null) {
            this.excludePlugins = new HashMap();
            String[] plugins = getPluginNames(exclude);
            for (int i = 0; i < plugins.length; i++) {
                String name = plugins[i];
                if (basePlugins.get(name) == null) {
                    this.excludePlugins.put(name, Boolean.TRUE);
                } else {
                    this.log.warn("Cannot exclude " + name + " plugin, ignoring.");
                }
            }
        }
    }

    private void setSystemProperties() {
        String pdk = getProperty(PROP_PDK_DIR);
        String workDir = getProperty(PROP_PDK_WORK_DIR, pdk + "/" + AgentConfig.WORK_DIR);
        String pluginsDir = getProperty(PROP_PDK_PLUGINS_DIR, pdk + "/plugins");

        if (pdk != null) {
            setPdkDir(pdk);
            setPdkWorkDir(workDir);
            setPdkPluginsDir(pluginsDir);
            log.info(PROP_PDK_DIR + "=" + getPdkDir());
        } else {
            String tmp = System.getProperty("java.io.tmpdir");
            File work = new File(tmp + "/pdk" + workDir);
            setPdkWorkDir(work.getPath());
            if (!work.exists()) {
                work.mkdirs();
            }
        }

        log.info(PROP_PDK_PLUGINS_DIR + "=" + getPdkPluginsDir());
        log.info(PROP_PDK_WORK_DIR + "=" + getPdkWorkDir());
    }

    public static String getPdkDir() {
        return System.getProperty(PROP_PDK_DIR);
    }

    public static void setPdkDir(String dir) {
        System.setProperty(PROP_PDK_DIR, dir);
    }

    public static String getPdkPluginsDir() {
        return System.getProperty(PROP_PDK_PLUGINS_DIR);
    }

    public static void setPdkPluginsDir(String dir) {
        System.setProperty(PROP_PDK_PLUGINS_DIR, dir);
    }

    public static String getPdkWorkDir() {
        return System.getProperty(PROP_PDK_WORK_DIR);
    }

    public static void setPdkWorkDir(String dir) {
        System.setProperty(PROP_PDK_WORK_DIR, dir);
    }

    public void init() throws PluginException {

        loadProductPluginStub();

        super.init(null); // null == we dont have a parent manager

        Properties props = getProperties();
        props.putAll(ProductProperties.getProperties());
        setSystemProperties();
        // not the same as platform.fqdn
        String name = props.getProperty(ProductPlugin.PROP_PLATFORM_NAME);
        if (name != null) {
            GenericPlugin.setPlatformName(name);
        }

        initPluginFilters();

        String pdk = getPdkDir();
        if (pdk != null) {
            this.client = new File(pdk, "lib").exists();
        }
        if (this.client) {
            log.debug("Initializing in client mode " + "(pdk=" + pdk + ")");
        } else {
            log.debug("Initializing in server mode");
        }
        initPluginManagers(props);
    }

    protected void initPluginManagers(Properties props) throws PluginException {
        this.mpm = new MeasurementPluginManager(props);
        this.cpm = new ControlPluginManager(props);
        this.apm = new AutoinventoryPluginManager(props);
        this.rpm = new RtPluginManager(props);
        this.ltpm = new LogTrackPluginManager(props);
        this.ctpm = new ConfigTrackPluginManager(props);
        this.ldpm = new LiveDataPluginManager(props);

        PluginManager[] mgrs = { this.mpm, this.cpm, this.apm, this.rpm, this.ltpm, this.ctpm, this.ldpm, this // note
                                                                                                               // to
                                                                                                               // self
        };

        for (int i = 0; i < mgrs.length; i++) {
            PluginManager mgr = mgrs[i];
            mgr.init(this);
            this.managers.put(mgr.getName(), mgr);
            if (!this.client || DEBUG_LIFECYCLE) {
                log.debug(mgr.getName() + " plugins enabled=" + isPluginTypeEnabled(mgr.getName()));
            }
        }

        // XXX by-passing server hot-deploy
        String plugins = getPdkPluginsDir();
        for (int i = 0; i < PLUGIN_SUPPORT_DIRS.length; i++) {
            File dir = new File(plugins, PLUGIN_SUPPORT_DIRS[i]);
            if (!dir.exists()) {
                continue;
            }
            registerPlugins(dir.getPath(), null);
        }
    }

    public Map<String, String> getPluginVersions(String pluginDir) {
        Map<String, String> rtn = new HashMap<String, String>();
        for (int i = 0; i < PLUGIN_SUPPORT_DIRS.length; i++) {
            File dir = new File(pluginDir, PLUGIN_SUPPORT_DIRS[i]);
            if (!dir.exists()) {
                continue;
            }
            File[] pluginFiles = listPlugins(dir);
            for (final File file : pluginFiles) {
                rtn.put(file.toString(), MD5.getMD5Checksum(file));
            }
        }
        return rtn;
    }

    public void shutdown() throws PluginException {

        synchronized (managers) {
            managers.remove(getName());
            for (Map.Entry<String, PluginManager> entry : managers.entrySet()) {
                PluginManager manager = entry.getValue();
                try {
                    manager.shutdown();
                } catch (PluginException e) {
                    log.error(manager.getName() + ".shutdown() failed", e);
                }
            }
            this.types.clear();
            this.managers.clear();
        }

        // shutdown() all registered ProductPlugins
        super.shutdown();
    }

    public MeasurementPluginManager getMeasurementPluginManager() {
        return this.mpm;
    }

    public ControlPluginManager getControlPluginManager() {
        return this.cpm;
    }

    public AutoinventoryPluginManager getAutoinventoryPluginManager() {
        return this.apm;
    }

    public RtPluginManager getRtPluginManager() {
        return this.rpm;
    }

    public LogTrackPluginManager getLogTrackPluginManager() {
        return this.ltpm;
    }

    public ConfigTrackPluginManager getConfigTrackPluginManager() {
        return this.ctpm;
    }

    public LiveDataPluginManager getLiveDataPluginManager() {
        return this.ldpm;
    }

    public MeasurementPlugin getMeasurementPlugin(String name) {
        try {
            return (MeasurementPlugin) this.mpm.getPlugin(name);
        } catch (PluginNotFoundException e) {
            log.debug("No MeasurementPlugin found for: " + name);
            return null;
        }
    }

    public ControlPlugin getControlPlugin(String name) {
        try {
            return (ControlPlugin) this.cpm.getPlugin(name);
        } catch (PluginNotFoundException e) {
            log.debug("No ControlPlugin found for: " + name);
            return null;
        }
    }

    public ServerDetector getAutoinventoryPlugin(String name) {
        try {
            return (ServerDetector) this.apm.getPlugin(name);
        } catch (PluginNotFoundException e) {
            log.debug("No AutoinventoryPlugin found for: " + name);
            return null;
        }
    }

    public RtPlugin getRtPlugin(String name) {
        try {
            return (RtPlugin) this.rpm.getPlugin(name);
        } catch (PluginNotFoundException e) {
            log.debug("No RtPlugin found for: " + name);
            return null;
        }
    }

    public ProductPlugin getProductPlugin(String name) {
        try {
            return (ProductPlugin) getPlugin(name);
        } catch (PluginNotFoundException e) {
            log.debug("No ProductPlugin found for: " + name);
            return null;
        }
    }

    /**
     * Register a plugin with the given GenericPluginManger, one instance
     * per-platform of server/service types.
     * @param pm The plugin manager
     * @param info Plugin info from the product plugin where this plugin is
     *        implemented.
     * @param plugin The plugin instance to register.
     * @param type The resource type info for this plugin.
     * @param registerTypes If true registers a plugin instance for all platform
     *        types (server-side), otherwise the current platform only
     *        (agent-side).
     * @throws PluginExistsException If an instance already exists with the same
     *         name in the given plugin manager.
     */
    void registerTypePlugin(PluginManager pm, PluginInfo info, GenericPlugin plugin, TypeInfo type)
        throws PluginExistsException {

        boolean register = this.registerTypes;
        boolean hasServer = false;
        ServiceTypeInfo service;
        ServerTypeInfo server = null;
        String[] platforms = null;
        String thisPlatform = HypericOperatingSystem.getInstance().getName();
        String name = plugin.getName();
        String skipMsg = null;

        if (DEBUG_LIFECYCLE) {
            skipMsg = "Skipping registration of '" + name + "' " + pm.getName() + " plugin on this platform";
        }

        switch (type.getType()) {
            case TypeInfo.TYPE_SERVER:
                server = (ServerTypeInfo) type;
                hasServer = true;
                break;
            case TypeInfo.TYPE_SERVICE:
                service = (ServiceTypeInfo) type;
                server = service.getServerTypeInfo();
                hasServer = true;
                break;
            case TypeInfo.TYPE_PLATFORM:
                if (((PlatformTypeInfo) type).isDevice()) {
                    // always register devices, so they can be serviced by
                    // an agent on another platform
                    register = true;
                }
                if (!register && !thisPlatform.equals(name)) {
                    if (DEBUG_LIFECYCLE) {
                        log.trace(skipMsg);
                    }
                    return;
                }
            default:
                break;
        }

        if (hasServer) {
            if (server.isPlatformDevice()) {
                // always register server types on platform devices
                register = true;
            }

            String[] validPlatforms = server.getValidPlatformTypes();

            if (register) {
                platforms = validPlatforms;
            } else {
                if (Arrays.asList(validPlatforms).contains(thisPlatform)) {
                    platforms = new String[] { thisPlatform };
                } else {
                    if (DEBUG_LIFECYCLE) {
                        log.trace(skipMsg + ", validPlatforms=" + Arrays.asList(validPlatforms));
                    }
                    return;
                }
            }
        }

        try {
            // XXX in the case of server/service type plugins
            // if there are to TypeInfos defined with the same
            // name but different platforms, first one wins
            // here. this should just be for temporary compat,
            // until subsystems include platform name when
            // looking up a server/service type plugin.

            PluginInfo gInfo = new PluginInfo(name, info);
            pm.setPluginInfo(name, gInfo);

            pm.registerPlugin(plugin, null);
        } catch (PluginExistsException e) {
            if (!hasServer) {
                throw e;
            }
        } catch (PluginException e) {
            this.log.error("registerPlugin=" + plugin.getName(), e);
        }

        if (!hasServer) {
            return;
        }

        // for server/service types we register an instance of
        // the plugin per-platform
        for (int i = 0; i < platforms.length; i++) {
            String pName = TypeBuilder.composePlatformTypeName(name, platforms[i]);

            try {
                pm.createPlugin(pName, plugin, null);
            } catch (PluginException e) {
                this.log.error("createPlugin=" + plugin.getName(), e);
            }
        }
    }

    private boolean isPluginTypeEnabled(String type) {
        String typeProp = getPropertyKey(type, "disable");
        return !"true".equals(getProperty(typeProp));
    }

    public boolean isLoadablePluginName(String name) {
        if (!(name.endsWith("-plugin.jar") || name.endsWith("-plugin.xml"))) {
            if (DEBUG_LIFECYCLE) {
                log.debug(name + " not a loadable plugin");
            }
            return false;
        }

        name = name.substring(0, name.length() - 11);
        if (this.includePlugins != null) {
            if (this.includePlugins.get(name) == null) {
                if (DEBUG_LIFECYCLE) {
                    log.debug("Skipping " + name + " (not in plugins.include)");
                }
                return false;
            }
        }
        if (isExcluded(name)) {
            return false;
        }

        return true;
    }

    private boolean isExcluded(String name) {
        if (name.endsWith("-plugin.jar") || name.endsWith("-plugin.xml")) {
            name = name.substring(0, name.length() - 11);
        }
        if (this.excludePlugins != null) {
            if (this.excludePlugins.get(name) != null) {
                if (DEBUG_LIFECYCLE) {
                    log.debug("Skipping " + name + " (in plugins.exclude)");
                }
                return true;
            }
        }
        return false;
    }

    /**
     * registerPluginJar() without mapping types.
     * @param jarName The name of the jar file on disk.
     * @see #registerPluginJar(String jarName,ClassLoader resourceLoader)
     */
    public PluginInfo registerPluginJar(String jarName) throws PluginException, PluginExistsException {
        return registerPluginJar(jarName, null);
    }

    private void loadProductPluginStub() throws PluginException {
        this.pluginStub = new byte[1024];
        ClassLoader loader = this.getClass().getClassLoader();

        InputStream is = null;
        try {
            is = loader.getResourceAsStream(PLUGIN_STUB);
            if (is == null) {
                throw new PluginException("Unable to find: " + PLUGIN_STUB);
            }
            this.pluginStubLength = is.read(this.pluginStub);
        } catch (IOException e) {
            throw new PluginException("Unable to read: " + PLUGIN_STUB + ": " + e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private File[] listPlugins(File dir) {
        File[] plugins = dir.listFiles();
        return plugins;
    }

    private String unsupportedClassVersionMessage(String msg) {
        final String ex = "Unsupported major.minor version ";
        int ix;
        if ((ix = msg.indexOf(ex)) > -1) {
            ix += ex.length();
            String major = msg.substring(ix, ix + 2);
            String jre = (String) JAVA_VERSIONS.get(major);
            if (jre == null) {
                return msg;
            }
            return "requires JRE " + jre + " or higher";
        } else {
            return msg;
        }
    }
    
    public Collection<PluginInfo> getAllPluginInfoDirectFromFileSystem(String path) {
        final Collection<PluginInfo> rtn = new ArrayList<PluginInfo>();
        final List<String> dirs = StringUtil.explode(path, File.pathSeparator);
        for (final String d : dirs) {
            final File dir = new File(d);
            if (!dir.exists() || !dir.isDirectory()) {
                continue;
            }
            File[] plugins = dir.listFiles();
            for (File plugin : plugins) {
                String name = plugin.getName();
                if (name.endsWith("-plugin.jar") || name.endsWith("-plugin.xml")) {
                    rtn.add(new PluginInfo(plugin, "NONE"));
                }
            }
        }
        return rtn;
    }

    public Collection<PluginInfo> registerPlugins(String path, Collection<PluginInfo> excludes) {
        Collection<PluginInfo> rtn = new ArrayList<PluginInfo>();
        List<String> dirs = StringUtil.explode(path, File.pathSeparator);
        for (int i = 0; i < dirs.size(); i++) {
            File dir = new File(dirs.get(i));
            if (!dir.exists()) {
                log.warn("register plugins: " + dir + " does not exist");
                continue;
            }
            if (!dir.isDirectory()) {
                log.warn("register plugins: " + dir + " not a directory");
                continue;
            }
            File[] plugins = listPlugins(dir);
            Collection<PluginInfo> pluginInfo = register(Arrays.asList(plugins), excludes);
            rtn.addAll(pluginInfo);
        }
        return rtn;
    }

    private Collection<PluginInfo> register(Collection<File> plugins, Collection<PluginInfo> excludes) {
        Collection<PluginInfo> rtn = new ArrayList<PluginInfo>();
        for (File plugin : plugins) {
            String name = plugin.getName();
            if (!isLoadablePluginName(name)) {
                if (isExcluded(name) && excludes != null && plugin.exists() && !plugin.isDirectory()) {
                    PluginInfo info = new PluginInfo(plugin, "EXCLUDED");
                    excludes.add(info);
                }
                continue;
            }
            log.info("Loading plugin: " + name + " (" + plugin.getParent() + ")");
            try {
                PluginInfo info = null;
                if ((info = registerPluginJar(plugin.getAbsolutePath())) != null) {
                	rtn.add(info);
                }
            } catch (UnsupportedClassVersionError e) {
                log.info("Cannot load " + name + ": " + unsupportedClassVersionMessage(e.getMessage()));
            } catch (PluginExistsException e) {
                log.debug("Plugin " + name + " already exists.");
            } catch (PluginException e) {
            	// ...we're unable to register this particular plugin, log it and press on...
            	log.error("A problem occured while registering plugin [" + name + "]", e);
            }
        }
        return rtn;
    }

    private void addClassPath(PluginLoader loader, String path) throws PluginException {

        try {
            loader.addURL(path);
        } catch (PluginLoaderException e) {
            throw new PluginException(e.getMessage());
        }
    }

    private void addClassPath(PluginLoader loader, String name, String[] classpath) throws PluginException {

        if (classpath.length == 0) {
            return;
        }

        String pdkDir = getPdkDir();
        String pdkPluginsDir = getPdkPluginsDir();
        for (int i = 0; i < classpath.length; i++) {
            String path = classpath[i];
            if(path.startsWith("pdk/plugins/") && pdkPluginsDir !=null) {
                path = pdkPluginsDir + "/" + path.substring(12);
            }else if (path.startsWith("pdk/") && (pdkDir != null)) {
                path = pdkDir + "/" + path.substring(3);
            }
            addClassPath(loader, path);
        }
    }
    
    private void logPluginManifest(String jarName) {
        if (log.isDebugEnabled()) {
            URL url;
            try {
                url = new URL("jar", "", "file:" + jarName + "!/");
                JarURLConnection jarConn = (JarURLConnection) url.openConnection();
                Map attributeMap = jarConn.getManifest().getMainAttributes();
                if (!attributeMap.isEmpty()) {
                    StringBuilder manifestLog = new StringBuilder("\n--- Manifest entries for: " + url.toString() +
                                                                  " ---\n");
                    Iterator iter = attributeMap.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry entry = (Map.Entry) iter.next();
                        manifestLog.append(entry.getKey() + " - " + entry.getValue() + "\n");
                    }
                    log.debug(manifestLog.toString());
                }
            } catch (Exception e) {
                log.debug("Manifest retrieval had an exception (continuing): " + e.getMessage());
            }
        }
    }


    /**
     * Load a product plugin jar. Registers the product plugin, as defined by
     * the Main-Class jar manifest attribute which must be a class which
     * implements the ProductPlugin interface. Registers plugins supported for
     * each plugin type (control, measurement, responsetime) as returned by the
     * ProductPlugin.getPlugin method.
     * @param jarName The name of the jar file on disk.
     * @param resourceLoader ClassLoader used to find jar resources.
     * @return The name of the product plugin as returned by
     *         ProductPlugin.getName.
     * @see org.hyperic.hq.product.ProductPlugin
     */
    public PluginInfo registerPluginJar(String jarName, ClassLoader resourceLoader) throws PluginException,
        PluginExistsException {

        ProductPlugin plugin = null;
        Class<?> pluginClass = null;
        PluginData data;
        String defaultPluginName = getNameFromFile(jarName);

        try {
            PluginLoader loader = PluginLoader.create(jarName, this.getClass().getClassLoader());

            logPluginManifest(jarName);
            PluginLoader.setClassLoader(loader);
            ClassLoader dataLoader;
            if (resourceLoader != null) {
                dataLoader = resourceLoader;
            } else {
                dataLoader = loader;
            }

            data = PluginData.getInstance(this, dataLoader, jarName);

            String[] classpath = ProductPlugin.getDataClassPath(data);
            addClassPath(loader, jarName, classpath);

            if (jarName.endsWith(".jar")) {
                String pdk = getPdkWorkDir();

                ClientPluginDeployer deployer = new ClientPluginDeployer(pdk, defaultPluginName);
                List jars = deployer.unpackJar(jarName);
                //if (this.isClient) {
                    loader.addURLs(jars);
                //}
            }

            String implName = data.getPlugin(ProductPlugin.TYPE_PRODUCT, ProductPlugin.TYPE_PRODUCT);

            if (implName == null) {
                pluginClass = loader.loadPlugin(PLUGIN_STUB_NAME, this.pluginStub, this.pluginStubLength);
            } else {
                pluginClass = ProductPlugin.getPluginClass(PluginLoader.getClassLoader(), data, implName, jarName);
            }

            if (pluginClass == null) {
                throw new PluginException("Class [" + implName + "] not found " + "via classloader=[" +
                                          PluginLoader.getClassLoader());
            }

            plugin = (ProductPlugin) pluginClass.newInstance();
            plugin.data = data;

            // there are 3 ways to set the product name:
            // - legacy ProductPlugin.setName()
            // - <product name="foo"> in hq-plugin.xml
            // - default to name of the plugin file minus "-plugin.{xml,jar}"
            // we try all three and make sure plugin.name and data.name
            // are both set with the same value.
            String pluginName = plugin.getName(); // legacy
            if (pluginName == null) {
                pluginName = data.getName(); // hq-plugin.xml
            }
            if (pluginName == null) {
                pluginName = defaultPluginName;
                if (pluginName == null) {
                    throw new PluginException("Malformed name for: " + jarName);
                }
            }
            if (data.getName() == null) {
                data.setName(pluginName);
            }
            if (plugin.getName() == null) {
                plugin.setName(pluginName);
            }
            if (plugin.getPluginVersion() == null) {
            	plugin.setPluginVersion(data.getVersion());
            }
            
            if (this.client && (implName != null)) {
                // already added the classpath, but the impl may override/adjust
                String[] pluginClasspath = plugin.getClassPath(this);
                addClassPath(loader, plugin.getName(), pluginClasspath);
            }

            PluginInfo info = new PluginInfo(plugin, jarName);
            // e.g. for finding hq-plugin.xml
            // when deployed on server
            // resourceLoader != plugin.getClass().getClassLoader()
            if (resourceLoader == null) {
                resourceLoader = plugin.getClass().getClassLoader();
            }
            info.resourceLoader = resourceLoader;

            setPluginInfo(pluginName, info);

            registerPlugin(plugin, null);
            TypeInfo[] types = plugin.getTypes();

            if (types == null) {
                this.log.error(pluginName + ".getTypes returned null");
                return null;
            }
            addPluginTypes(types, plugin);
            return info;
        } catch (PluginException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        } finally {
            if (plugin != null) {
                PluginLoader.resetClassLoader(plugin);
            }
        }
    }

    public void addPluginTypes(TypeInfo[] types, ProductPlugin plugin) throws PluginExistsException {
        PluginInfo info = getPluginInfo(plugin.getName());
        if (this.registerTypes) {
             registerTypeInfo(types);
        }
        for (int i = 0; i < ProductPlugin.TYPES.length; i++) {
            String type = ProductPlugin.TYPES[i];

            if (type.equals(ProductPlugin.TYPE_PRODUCT))
                continue;

            if (!isPluginTypeEnabled(type)) {
                continue;
            }
            PluginManager pm = (PluginManager) managers.get(type);

            for (int j = 0; j < types.length; j++) {
                GenericPlugin gPlugin;
                String typeName = types[j].getName();

                gPlugin = plugin.getPlugin(type, types[j]);
                if (gPlugin == null) {
                    if (DEBUG_LIFECYCLE) {
                        log.debug(plugin.getName() + " does not implement " + type + " for type=" + typeName);
                    }
                    continue;
                }

                gPlugin.data = plugin.data;
                gPlugin.setName(typeName);
                gPlugin.setTypeInfo(types[j]);

                if (DEBUG_LIFECYCLE) {
                    log.debug(plugin.getName() + " implements " + type + " for type=" + typeName);
                }

                registerTypePlugin(pm, info, gPlugin, types[j]);
            }
        }

    }

    public void removePluginTypes(List<TypeInfo> typeInfos) {
        for (int i = 0; i < ProductPlugin.TYPES.length; i++) {
            String type = ProductPlugin.TYPES[i];

            if (type.equals(ProductPlugin.TYPE_PRODUCT))
                continue;

            if (!isPluginTypeEnabled(type)) {
                continue;
            }
            PluginManager pm = (PluginManager) managers.get(type);
            Map<String, GenericPlugin> plugins = pm.getPlugins();
            Set<String> pluginsToDelete = new HashSet<String>();
            for (GenericPlugin gPlugin : plugins.values()) {

                if (typeInfos.contains(gPlugin.getTypeInfo())) {
                    pluginsToDelete.add(gPlugin.getName());
                }
            }
            for (String pluginToDelete : pluginsToDelete) {
                try {
                    pm.removePlugin(pluginToDelete);
                } catch (PluginNotFoundException e) {
                    log.warn("Error attempting to remove a plugin that does not exist");
                } catch (PluginException e) {
                    log
                        .warn("Error removing a plugin while updating type metadata.  " +
                              "This could cause a PluginExistsException later when an attempt is made to re-register the plugin.  Cause: " +
                              e.getMessage());
                }
            }
        }
    }

    private void removeManagerPlugins(PluginManager mgr, String jarName) throws PluginException {

        Map<String, GenericPlugin> mPlugins = mgr.getPlugins();

        // cannot use keySet().iterator() else
        // ConcurrentModificationException is thrown during removePlugin()
        String[] keys = mPlugins.keySet().toArray(new String[0]);

        for (int i = 0; i < keys.length; i++) {
            String name = keys[i];
            PluginInfo info = mgr.getPluginInfo(name);

            if (info == null) {
                String msg = "no plugin info found for " + mgr.getName() + " plugin " + name;
                throw new PluginException(msg);
            }

            // XXX: should prolly check more than jar basename
            // but then again, they live in the same directory
            // so jar basename should be unique.
            if (info.jar.equals(jarName)) {
                try {
                    mgr.removePlugin(name);
                } catch (PluginNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void removePluginJar(String jarName) throws PluginException {

        String fileName = new File(jarName).getName();

        for (Map.Entry<String, PluginManager> entry : this.managers.entrySet()) {

            PluginManager manager = entry.getValue();

            removeManagerPlugins(manager, fileName);
        }
    }

    public void updatePluginJar(String jarName) throws PluginException {

        removePluginJar(jarName);

        try {
            registerPluginJar(jarName);
        } catch (PluginExistsException e) {
            // should not happen if removePluginJar was a success
            throw new PluginException(e);
        }
    }

    public PluginManager getPluginManager(String type) throws PluginException {

        PluginManager mgr = this.managers.get(type);

        if (mgr == null) {
            // XXX PluginManagerNotFoundException
            throw new PluginException();
        }

        return mgr;
    }

    public void setProperty(String key, String value) {
        for (Map.Entry<String, PluginManager> entry : this.managers.entrySet()) {
            PluginManager manager = entry.getValue();
            manager.getProperties().setProperty(key, value);
        }
    }
    
    public boolean isClient() {
        return this.client;
    }
    
    /**
     * As opposed to other plugins, product plugins
     * are fetched by plugin name 
     */
    @Override
    public ConfigSchema getConfigSchema(String pluginName, 
            String platformName, 
            String typeName, 
            TypeInfo info,
            ConfigResponse config) 
                    throws PluginNotFoundException {
        
        return getConfigSchema(pluginName, info, config);
    }       
}
