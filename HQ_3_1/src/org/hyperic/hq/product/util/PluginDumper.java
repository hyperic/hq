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

package org.hyperic.hq.product.util;

import gnu.getopt.Getopt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.product.AutoinventoryPluginManager;
import org.hyperic.hq.product.ConfigTrackPluginManager;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.ControlPluginManager;
import org.hyperic.hq.product.LogTrackPluginManager;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.MeasurementPluginManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformTypeInfo;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginExistsException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.hq.product.TrackEventPluginManager;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.sigar.FileWatcherThread;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.timer.StopWatch;
import org.hyperic.util.units.FormattedNumber;

/*
 * class to dump plugin info to files.
 */
public class PluginDumper {
    static final String PRODUCT_JAR = "hq-product.jar";

    PluginDumperConfig config;

    protected ProductPluginManager ppm;
    protected MeasurementPluginManager mpm;
    protected ControlPluginManager cpm;
    protected LogTrackPluginManager ltpm;
    protected ConfigTrackPluginManager ctpm;
    AutoinventoryPluginManager apm;

    HashMap productTypes = new HashMap();

    static final String OS = OperatingSystem.getInstance().getName();

    protected ProductPlugin[] pPlugins;

    private static final String XML_VERSION = "<?xml version=\"1.0\"?>";

    private static final String[] TYPES = {
        "", "platform", "server", "service",
    };

    //like -tree, but not as cool.
    private static final String[] TYPE_INDENT = {
        "", "   ", "   ", "      ",
    };

    static final String PROP_METHOD = "dumper.method";
    static final String PROP_PLUGIN = "dumper.plugin";
    static final String PROP_TYPE   = "dumper.type";

    static final String METHOD_METRIC   = "metric";
    static final String METHOD_CONTROL  = "control";
    static final String METHOD_DISCOVER = "discover";
    static final String METHOD_GENERATE = "generate";
    static final String METHOD_TRACK    = "track";

    public PluginDumper() {
        //for ExecutableThread
        setInterval("exec.interval", 1);
        //for log/config track
        setInterval(TrackEventPluginManager.PROP_INTERVAL, 1);

        //for FileTail based Collectors (in millis)
        FileWatcherThread.getInstance().setInterval(1000);

        this.config = new PluginDumperConfig();
    }

    public PluginDumper(String pdkDir, String pluginDir) {
        this();
        this.config.pdkDir = pdkDir;
        this.config.pluginDir = pluginDir;
    }

    public PluginDumper(String[] args) {
        this();
        getopt(args);
    }

    public static void help(String msg) {
        PrintStream os = System.out;
        if (msg != null) {
            os.println(msg);
        }
        os.println("[-p plugin] [-t type] [-m method] [-a action] " +
                   "[-D key=value]");
    }

    private void setInterval(String prop, int seconds) {
        if (System.getProperty(prop) == null) {
            System.setProperty(prop, String.valueOf(seconds));
        }
    }
    
    class PluginDumperConfig {
        boolean registerTypes = true;
        boolean help = false;
        boolean hasSwitches = false;
        String plugin;
        String type;
        String method;
        String action;

        String outputDir;
        String pdkDir;
        String pluginDir;

        Properties props = new Properties();
        Properties defines = new Properties();

        String[] args;

        PluginDumperConfig() {
        }

        void load(String file) {
            FileInputStream is = null;

            try {
                is = new FileInputStream(file);
                this.props.load(is);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { is.close(); } catch (Exception ie) { }
            }
        }

        //allow props to override
        void init() {
            this.plugin =
                this.props.getProperty(PROP_PLUGIN, this.plugin);

            this.type =
                this.props.getProperty(PROP_TYPE, this.type);

            this.method =
                this.props.getProperty(PROP_METHOD, this.method);

            this.action =
                this.props.getProperty("action", this.action);

            this.outputDir =
                this.props.getProperty("output.dir", this.outputDir);

            this.pdkDir =
                this.props.getProperty(ProductPluginManager.PROP_PDK_DIR,
                                       this.pdkDir);

            this.pluginDir =
                this.props.getProperty("plugin.dir", this.pluginDir);

            if ("all".equals(this.plugin)) {
                this.plugin = null;
            }

            if (this.method == null) {
                this.method = METHOD_METRIC;
            }
            if (this.method.equals(METHOD_DISCOVER) &&
                !METHOD_METRIC.equals(this.action))
            {
                this.registerTypes = false;
            }

            if (this.action == null) {
                if (this.method.equals(METHOD_METRIC)) {
                    this.action = "getvalue";
                }
            }

            if (this.outputDir == null) {
                this.outputDir = ".";
            }

            if (this.pluginDir == null) {
                this.pluginDir =
                    this.pdkDir + File.separator + "plugins";
            }

            String pluginProperties =
                props.getProperty("plugin.properties");
            if (pluginProperties != null) {
                load(pluginProperties);
            }

            if (this.type != null) {
                this.type = TypeInfo.formatName(this.type);
            }
        }
    }

    private void getopt(String[] args) {
        int opt;
        Getopt parser =
            new Getopt("plugindumper", args, "hp:t:m:a:o:D:");
        parser.setOpterr(false);
        PluginDumperConfig config = this.config;
        
        while ((opt = parser.getopt()) != -1) {
            config.hasSwitches = true;
            switch (opt) {
              case 'p':
                config.plugin = parser.getOptarg();
                break;
              case 't':
                config.type = parser.getOptarg();
                break;
              case 'm':
                config.method = parser.getOptarg();
                break;
              case 'a':
                config.action = parser.getOptarg();
                break;
              case 'o':
                config.outputDir = parser.getOptarg();
                break;
              case 'D':
                String arg = parser.getOptarg();
                String key = arg;
                String val = "true";
                int ix = arg.indexOf("=");
                if (ix != -1) {
                    key = arg.substring(0, ix);
                    val = arg.substring(ix+1, arg.length());
                }
                config.props.setProperty(key, val);
                config.defines.setProperty(key, val);
                break;
              case 'h':
              default:
                config.help = true;
                return;
            }
        }

        int i = parser.getOptind();
        if (i < args.length) {
            ArrayList extra = new ArrayList();
            for (; i<args.length; i++) {
                if (args[i].endsWith(".properties")) {
                    config.hasSwitches = true;
                    config.load(args[i]);
                }
                else {
                    extra.add(args[i]);
                }
            }
            config.args = (String[])extra.toArray(new String[0]);
        }
        else {
            config.args = new String[0];
        }
    }

    public void init() throws PluginException {
        this.config.props.putAll(System.getProperties());
                           
        this.config.init();

        if (!new File(this.config.pdkDir).exists()) {
            System.out.println(this.config.pdkDir + " does not exist");
            System.exit(1);
        }

        this.ppm = new ProductPluginManager(this.config.props);
        this.ppm.setRegisterTypes(this.config.registerTypes);
        this.ppm.init();

        this.mpm = this.ppm.getMeasurementPluginManager();

        this.cpm = this.ppm.getControlPluginManager();

        this.apm = this.ppm.getAutoinventoryPluginManager();

        this.ctpm = this.ppm.getConfigTrackPluginManager();

        this.ltpm = this.ppm.getLogTrackPluginManager();
        
        loadPlugins();

        if (this.config.plugin == null) {
            String include =
                this.config.props.getProperty("plugins.include");
            if (include != null && include.indexOf(",") == -1) {
                //e.g. if -Dplugins.include=apache, set -p apache
                this.config.plugin = include;
            }
        }

        if (this.config.plugin != null) {
            this.pPlugins = new ProductPlugin[] {
                (ProductPlugin)this.ppm.getPlugin(this.config.plugin)
            };
        }
        else {
            this.pPlugins = (ProductPlugin[])this.ppm.getPlugins().
                values().toArray(new ProductPlugin[0]);
        }
    }

    public void shutdown() {
        try {
            this.ppm.shutdown();
        } catch (PluginException e) {
            e.printStackTrace();
        }
    }

    public void invoke() throws PluginException, IOException {
        String method = this.config.method;

        if (this.config.help) {
            help(null);
            return;
        }

        if (method.equals(METHOD_METRIC)) {
            boolean translateOnly =
                "translate".equals(this.config.action);
            
            //useful for testing template back-compat
            //-m metric -a raw \
            //"-Dtemplate=Apache 2.0:url.availability:port=80,...:availability"
            if ("raw".equals(this.config.action)) {
                String template =
                    getProperty("template");
                try {
                    System.out.println(getValue(template));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (this.config.plugin != null) {
                fetchMetrics(this.config.plugin,
                             translateOnly);
            }
            else if (translateOnly) {
                for (int i=0; i<this.pPlugins.length; i++) {
                    fetchMetrics(this.pPlugins[i].getName(),
                                 translateOnly);
                }
            }
            else {
                help("No plugin specified");
                return;
            }
        }
        else if (method.equals(METHOD_CONTROL)) {
            if (this.config.plugin != null) {
                testControl(this.config.plugin);
            }
            else {
                help("No plugin specified");
                return;
            }
        }
        else if (method.equals(METHOD_TRACK)) {
            if (this.config.plugin != null) {
                testTrack();
            }
            else {
                help("No plugin specified");
                return;
            }
        }
        else if (method.equals(METHOD_DISCOVER)) {
            testDiscovery();
        }
        else if (method.equals(METHOD_GENERATE)) {
            String outputDir = this.config.outputDir;
            String action = this.config.action;
            if (action == null) {
                help("No action specified");
                return;
            }
            if (action.equals("help")) {
                dumpHelp(new File(outputDir, "plugin-help"));
            }
            else if (action.equals("metrics-xml")) {
                dumpMetrics(System.out, true);
            }
            else if (action.equals("metrics-txt")) {
                dumpMetrics(System.out, false);
            }
            else if (action.equals("metrics-wiki")) {
                dumpWikiDocs(System.out);
            }
            else {
                help("Unknown action: " + action);
                return;
            }
        }
        else if (method.equals("lifecycle")) {
            //just do init/shutdown
        }
        else {
            help("Unknown method: " + method);
            return;
        }
    }

    public Properties getProperties() {
        return this.config.props;
    }

    public String getProperty(String key) {
        return getProperty(key, null);
    }

    public String getProperty(String key, String defVal) {
        return System.getProperty(key,
                                  this.config.props.getProperty(key, defVal));
    }

    private void loadPlugins() throws PluginException {

        int nplugins =
            ppm.registerPlugins(this.config.pluginDir);

        nplugins += ppm.registerCustomPlugins(".");
        
        if (nplugins == 0) {
            System.out.println("no plugins loaded from directory: " +
                               this.config.pluginDir);
            return;
        }

        for (Iterator it=ppm.getPlugins().keySet().iterator();
             it.hasNext();)
        {
            String name = (String)it.next();

            if (config.plugin != null) {
                if (!name.startsWith(config.plugin)) {
                    continue;
                }
            }

            ProductPlugin plugin = this.ppm.getProductPlugin(name);
            if (plugin == null) {
                continue;
            }

            TypeInfo[] types = plugin.getTypes();

            for (int j=0; j<types.length; j++) {
                this.productTypes.put(types[j].getName(), name);
            }
        }
    }

    private void getConfig(ConfigResponse config,
                           ConfigSchema schema) {

        List options = schema.getOptions();

        for (int i=0; i<options.size(); i++) {
            ConfigOption opt = (ConfigOption)options.get(i);
            String key = opt.getName();
            config.setValue(key, getProperty(key, opt.getDefault()));
        }
    }

    private void getConfig(ConfigResponse config, TypeInfo type)
        throws PluginException {

        String[] types = {
            ProductPlugin.TYPE_CONTROL,
            ProductPlugin.TYPE_MEASUREMENT
        };

        for (int i=0; i<types.length; i++) {
            try {
                PluginManager pm =
                    this.ppm.getPluginManager(types[i]);
                ConfigSchema schema =
                    pm.getConfigSchema(type.getName(), type, config);
                getConfig(config, schema);
            } catch (PluginNotFoundException e) {
                continue;
            }
        }
    }

    ConfigResponse getPluginConfig(ProductPlugin pPlugin,
                                   TypeInfo type)
        throws PluginException {

        ConfigResponse config = new ConfigResponse();

        getConfig(config, pPlugin.getConfigSchema(type, config));

        getConfig(config, type);

        if (type.getType() == TypeInfo.TYPE_SERVICE) {
            ServiceTypeInfo service = (ServiceTypeInfo)type;
            TypeInfo serverType = service.getServerTypeInfo();
            getConfig(config, serverType);
            getConfig(config,
                      pPlugin.getConfigSchema(serverType, config));
        }

        String platform = type.getPlatformTypes()[0];
        getConfig(config, new PlatformTypeInfo(platform));

        //add all -Dkey=value args to the ConfigResponse
        for (Iterator it=this.config.defines.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            config.setValue((String)entry.getKey(),
                            (String)entry.getValue());
        }

        String[] paths = {
            ProductPlugin.PROP_INSTALLPATH,
            this.config.plugin + "." + ProductPlugin.PROP_INSTALLPATH
        };

        String installpath = null;
        for (int j=0; j<paths.length; j++) {
            installpath = this.config.props.getProperty(paths[j]);
            if (installpath != null) {
                break;
            }
        }
            
        if (installpath != null) {
            config.setValue(ProductPlugin.PROP_INSTALLPATH, installpath);
        }

        return config;
    }

    private MetricValue getValue(String template)
        throws Exception {

        int sleep = 1000;
        String execSleep = getProperty("exec.sleep");
        if (execSleep != null) {
            sleep *= Integer.parseInt(execSleep);
        }

        MetricValue value =
            this.mpm.getValue(template);
        while (value.isFuture()) {
            //exec: type metric, keep trying until execution has completed.
            Thread.sleep(sleep);
            value = this.mpm.getValue(template);
        }
        return value;
    }

    private boolean isRunnablePlatform(TypeInfo type) {
        String[] platforms = type.getPlatformTypes();
        if (!PlatformDetector.isSupportedPlatform(platforms[0])) {
            return true; //device stuff can be run from any platform
        }
        return Arrays.asList(platforms).contains(OS);
    }

    private void promptContinue() {
        System.err.println("hit enter to continue");
        try {
            System.in.read();
        } catch (Exception ioe) { }
    }

    public boolean fetchMetrics(TypeInfo type,
                                boolean translateOnly,
                                ConfigResponse config)
        throws PluginException {

        boolean templateOnly =
            translateOnly && (this.config.plugin == null);

        MeasurementPlugin mPlugin =
            this.ppm.getMeasurementPlugin(type.getName());

        if (mPlugin == null) {
            return false;
        }

        MeasurementInfo[] metrics = mPlugin.getMeasurements(type);

        if (metrics == null) {
            return false;
        }

        //-Dmetric-pause=true
        boolean isPause =
            "true".equals(getProperty("metric-pause"));

        //-Dmetric-collect=default
        boolean isDefault =
            "default".equals(getProperty("metric-collect"));

        //-Dmetric-indicator=true
        boolean isIndicator =
            "true".equals(getProperty("metric-indicator"));

        //-Dmetric-cat=availability
        String category = getProperty("metric-cat", "all");
        
        //e.g. -Dmetric-iter=1000
        String iter = getProperty("metric-iter");
        int iterations = 1;
        if (iter != null) {
            iterations = Integer.parseInt(iter);            
        }

        for (int j=0; j<metrics.length; j++) {
            if (isDefault && !metrics[j].isDefaultOn()) {
                continue;
            }
            if (isIndicator && !metrics[j].isIndicator()) {
                continue;
            }
            if (!category.equals("all") &&
                !category.equalsIgnoreCase(metrics[j].getCategory())) {
                continue;
            }

            String template = metrics[j].getTemplate();

            try {
                template = this.mpm.translate(template, config);
            } catch (PluginNotFoundException e) {
                e.printStackTrace();
                continue; //notgonna happen
            }

            if (templateOnly) {
                if (template.indexOf(MeasurementInfo.RATE_KEY) != -1) {
                    continue;
                }
                //just the template (minus plugin name) for Metric.main
                System.out.println(template.substring(template.indexOf(":")));
            }
            else {
                System.out.println(type.getName() + " " +
                                   metrics[j].getName() + ":");

                System.out.println("   " + template);
            }

            if (translateOnly) {
                continue;
            }

            StopWatch timer = new StopWatch();

            try {
                for (int x=0; x<iterations; x++) {
                    MetricValue value = getValue(template);
                    if (value.isNone()) {
                        continue;
                    }
                    FormattedNumber number =
                        UnitsConvert.convert(value.getValue(),
                                             metrics[j].getUnits());
                    if ((iter == null) || isPause) {
                        System.out.println("   =>" + number + "<=");
                    }
                    if (isPause) {
                        promptContinue();
                    }
                }
                if ((iter != null) && !isPause) {
                    System.out.println("   [" + timer + "]");
                }
            } catch (Exception e) {
                String exClass = e.getClass().getName();
                int ix = exClass.lastIndexOf(".");
                exClass = exClass.substring(ix+1);

                System.err.println("getValue failed for metric: " +
                                   template);
                System.err.println(exClass + ": " + e.getMessage());

                if ("debug".equals(getProperty("log"))) {
                    e.printStackTrace();
                    System.out.println("classloader=" +
                                       mPlugin.getClass().getClassLoader());
                    System.out.println("config=" + config);
                }
                promptContinue();
            }
        }

        return true;
    }

    public void fetchMetrics(String pluginName, boolean translateOnly)
        throws PluginException {

        String metricPlugin = this.config.type;
        ProductPlugin pPlugin;

        if ((pPlugin = this.ppm.getProductPlugin(pluginName)) == null) {
            //System.out.println(pluginName + " not found");
            return;
        }

        TypeInfo[] types = pPlugin.getTypes();

        for (int i=0; i<types.length; i++) {
            TypeInfo type = types[i];

            boolean isPlatform =
                type.getType() == TypeInfo.TYPE_PLATFORM;

            if (isPlatform) {
                //special case for system plugin
                if (metricPlugin == null) {
                    metricPlugin = OS.toLowerCase();
                }
            }
            else {
                //for plugins that do unix/win32 split,
                //we don't want to fetch the metrics twice
                if (!isRunnablePlatform(type)) {
                    continue;
                }
            }

            //e.g. -Dtype=iplanet-4.1
            if ((metricPlugin != null) &&
                !metricPlugin.equals(type.getFormattedName()))
            {
                continue;
            }

            ConfigResponse config = getPluginConfig(pPlugin, type);

            fetchMetrics(type, translateOnly, config);
        }
    }

    public void testControl(String pluginName)
        throws PluginException {

        ProductPlugin pPlugin =
            this.ppm.getProductPlugin(pluginName);

        if (pPlugin == null) {
            //System.out.println(pluginName + " not found");
            return;
        }

        String runPlugin = this.config.type;
        String runAction = this.config.action;

        TypeInfo[] types = pPlugin.getTypes();

        for (int i=0; i<types.length; i++) {
            TypeInfo type = types[i];
            String typeName = type.getName();
            ControlPlugin cPlugin = 
                this.ppm.getControlPlugin(typeName);

            if (cPlugin == null) {
                //System.out.println("No control plugin for " + typeName);
                continue;
            }

            List actions;
            try {
                actions = this.cpm.getActions(typeName);
            } catch (PluginNotFoundException e) {
                continue;  //aint gonna happen
            }

            String resourceName = type.getFormattedName();

            boolean wantedPlugin = resourceName.equals(runPlugin);

            if ((runPlugin != null) && !wantedPlugin) {
                //if type not specified, will just try create
                continue;
            }

            ConfigResponse config = getPluginConfig(pPlugin, type);

            System.out.println(typeName + " control plugin");

            System.out.println("   actions=" + actions);

            try {
                this.cpm.createControlPlugin(resourceName,
                                             typeName,
                                             config);
            } catch (PluginNotFoundException pne) {
                //wont happen
                System.out.println(pne.getMessage());
                continue;
            } catch (PluginExistsException pe) {
                //wont happen
                System.out.println(pe.getMessage());
                continue;
            } catch (PluginException gpe) {
                System.out.println("ERROR: " + gpe.getMessage());
                //gpe.printStackTrace();
                continue;
            }

            System.out.println("created '" + resourceName +
                               "' control plugin");

            if (!wantedPlugin) {
                continue;
            }

            if (runAction == null) {
                System.out.println("no control-action configured for " +
                                   runPlugin);
                continue;
            }

            if (runAction.equals("state")) {
                continue;
            }

            //"-Dcontrol.args=one two three"
            String[] args =
                StringUtil.explodeQuoted(getProperty("control.args"));
            
            try {
                System.out.println("   " + resourceName +
                                   " action: " + runAction +
                                   " " + Arrays.asList(args));
                this.cpm.doAction(resourceName, runAction, args);

                String msg = this.cpm.getMessage(resourceName);
                int result = this.cpm.getResult(resourceName);
                if (msg == null) {
                    msg = "No Message";
                }

                if (result == 0) {
                    System.out.println("   result: success - " + msg);
                }
                else {
                    System.out.println("   result: error - " + msg +
                                       " (" + result + ")");
                }
            } catch (PluginNotFoundException pne) {
                continue;
            } catch (PluginException e) {
                System.out.println("ERROR: " + e.getMessage());
                continue;
            }
        }
    }

    private void flushEvents(LinkedList events, String name)
    {
        if (events.isEmpty()) {
            return;
        }

        System.out.println(name + " events...");
        for (Iterator i = events.iterator(); i.hasNext();) {
            TrackEvent event = (TrackEvent)i.next();
            System.out.println("[" + new Date(event.getTime()) + "] " +
                               "(" + event.getSource() + ") " +
                               event.getMessage());
        }
    }

    private void flushLogTrackEvents()
    {
        LinkedList events = this.ltpm.getEvents();
        flushEvents(events, "log track");
    }

    private void flushConfigTrackEvents()
    {
        LinkedList events = this.ctpm.getEvents();
        flushEvents(events, "config track");
    }

    private void testTrack() throws PluginException, IOException {
        BufferedReader in =
            new BufferedReader(new InputStreamReader(System.in));
        String line;
        testTrack(this.config.plugin);

        Thread thread = new Thread() {
            public void run() {
                while (true) {
                    flushLogTrackEvents();
                    flushConfigTrackEvents();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.out.println("Done");
                        break;
                    }
                }
            }
        };
        thread.start();
        System.out.println("hit 'q' to quit");
        while ((line = in.readLine()) != null) {
            if (line.equals("q")) {
                break;
            }
        }
        thread.interrupt();
    }

    private void testTrack(String pluginName)
        throws PluginException {
        ProductPlugin pPlugin =
            this.ppm.getProductPlugin(pluginName);

        if (pPlugin == null) {
            //System.out.println(pluginName + " not found");
            return;
        }

        TypeInfo[] types = pPlugin.getTypes();
        String runPlugin = this.config.type;

        for (int i=0; i<types.length; i++) {
            TypeInfo type = types[i];
            String typeName = type.getName();
            boolean hasLogTrack=false, hasConfigTrack=false;
            boolean wantedPlugin =
                type.getFormattedName().equals(runPlugin);

            if ((runPlugin != null) && !wantedPlugin) {
                continue; //filter for -t arg
            }

            if ((type.getType() == TypeInfo.TYPE_PLATFORM) &&
                !type.getName().equals(OS))
            {
                continue;
            }

            String action = this.config.action;

            if ((action == null) || (action.equals("log"))) {
                try {
                    this.ltpm.getPlugin(typeName);
                    hasLogTrack = true;
                } catch (PluginNotFoundException e) {
                }
            }

            if ((action == null) || (action.equals("config"))) {
                try {
                    this.ctpm.getPlugin(typeName);
                    hasConfigTrack = true;
                } catch (PluginNotFoundException e) {
                }
            }

            String resourceName =
                type.getType() + ":" + type.getName().hashCode();

            ConfigResponse config = getPluginConfig(pPlugin, type);

            try {
                if (hasLogTrack) {
                    this.ltpm.createPlugin(resourceName, typeName, config);
                    System.out.println("Created log track plugin for " + typeName);
                }
                if (hasConfigTrack) {
                    this.ctpm.createPlugin(resourceName, typeName, config);
                    System.out.println("Created config track plugin for " + typeName);
                }
            } catch (PluginNotFoundException e) {
                //wont happen
                System.out.println(e.getMessage());
                continue;
            } catch (PluginExistsException e) {
                //wont happen
                System.out.println(e.getMessage());
                continue;
            } catch (PluginException e) {
                System.out.println("ERROR: " + e.getMessage());
                //gpe.printStackTrace();
                continue;
            }
        }
    }

    public void testDiscovery() {
        PluginDiscoverer discoverer =
            new PluginDiscoverer(this);

        if (this.config.plugin != null) {
            addDiscovery(discoverer, this.config.plugin);
        }
        else {
            for (Iterator it = this.ppm.getPlugins().keySet().iterator();
                 it.hasNext();)
            {
                String name = (String)it.next();
                addDiscovery(discoverer, name);
            }
        }
        
        discoverer.start();
    }

    private void addDiscovery(PluginDiscoverer discoverer, String pluginName) {
        ProductPlugin pPlugin =
            this.ppm.getProductPlugin(pluginName);

        if (pPlugin == null) {
            //System.out.println(pluginName + " not found");
            return;
        }
        
        TypeInfo[] types = pPlugin.getTypes();

        for (int i=0; i<types.length; i++) {
            if (types[i].getType() != TypeInfo.TYPE_SERVER) {
                continue;
            }

            String name = types[i].getName();
            if (this.config.type != null) {
                if (types[i].getFormattedName().equals(this.config.type)) {
                    discoverer.add(name);        
                }
            }
            else {
                discoverer.add(name);
            }
        }
    }

    protected MeasurementPlugin getMeasurementPlugin(TypeInfo info) {
        return this.ppm.getMeasurementPlugin(info.getName());
    }

    private void dumpWikiDocs(PrintStream os)
        throws IOException
    {
        HashMap typeMap = new HashMap();

        File wikiDir = new File("wiki-docs");
        if (!wikiDir.exists()) {
            wikiDir.mkdir();
        }

        for (int n=0; n<this.pPlugins.length; n++) {
            ProductPlugin pp = this.pPlugins[n];
            TypeInfo[] types = pp.getTypes();
            MeasurementPlugin mp;

            if ((types == null) || (types.length == 0)) {
                continue;
            }
            
            for (int i=0; i<types.length; i++) {
                TypeInfo type = types[i];
                String name = type.getName();

                // fold multiple types with the same name.
                if (typeMap.put(name, type) != null) {
                    continue;
                }

                String typeName = TYPES[type.getType()];

                MeasurementInfo[] metrics = null;

                mp = this.ppm.getMeasurementPlugin(name);
                if (mp != null) {
                    metrics = mp.getMeasurements(types[i]);
                }

                if ((metrics == null) || (metrics.length == 0)) {
                    // Skip virtual resources
                    continue;
                }

                String fileName = name + " " + typeName;
                PrintStream ps = openFile(wikiDir, fileName);

                ps.println("h3. " + name + " " + typeName);

                // Print log track info
                ps.print("*Log Track Supported:* ");
                try {
                    this.ltpm.getPlugin(name);
                    ps.println("Yes");
                } catch (PluginNotFoundException e) {
                    ps.println("No");
                }

                // Print config track info
                ps.print("*Config Track Supported:* ");
                try {
                    this.ctpm.getPlugin(name);
                    ps.println("Yes");
                } catch (PluginNotFoundException e) {
                    ps.println("No");
                }

                // Print custom properties
                ps.print("*Custom Properties Supported:* ");
                ConfigSchema c = pp.getCustomPropertiesSchema(name);
                List options = c.getOptions();

                if (options.size() == 0) {
                    ps.println("None");
                } else {
                    ps.println("");
                    ps.println("||Name||Description");
                    for (int j = 0; j < options.size(); j++) {
                        ConfigOption opt = (ConfigOption)options.get(j);
                        ps.println("|" + opt.getName() + "|" + 
                                   opt.getDescription());
                    }
                    ps.println("||");
                }

                // Print control actions
                ps.print("*Supported Control Actions:* ");
                List actions;
                try {
                    actions = this.cpm.getActions(name);
                    for (int j=0; j<actions.size(); j++) {
                        String action = (String)actions.get(j);
                        if (j < actions.size() - 1) {
                            ps.print(action + ",");
                        } else {
                            ps.println(action);
                        }
                    }
                } catch (PluginNotFoundException e) {
                    ps.println("None");
                }

                ps.println("*Supported Metrics:* ");
                ps.println("||Name||Alias||Units||Category||Default On||Default Interval");
                for (int j=0; j<metrics.length; j++) {
                    String colorStart;
                    String colorEnd = "{color}";
                    if (metrics[j].isDefaultOn()) {
                        colorStart = "{color:navy}";
                    } else {
                        colorStart = "{color:gray}";
                    }

                    ps.println("|" +
                               colorStart + metrics[j].getName() + 
                               colorEnd + 
                               "|" +
                               colorStart + metrics[j].getAlias() + 
                               colorEnd + 
                               "|" +
                               colorStart + metrics[j].getUnits() + 
                               colorEnd +
                               "|" +
                               colorStart + metrics[j].getCategory() + 
                               colorEnd +
                               "|" +
                               colorStart + 
                               new Boolean(metrics[j].isDefaultOn()) + 
                               colorEnd +
                               "|" +
                               colorStart +
                               new Long(metrics[j].getInterval()/60000) + " min" +
                               colorEnd);
                }
                ps.println("||||||");
                ps.println("");
                ps.println("*Configuration help:* ");

                Object help;
                try {
                    help = mpm.getHelp(type, getProperties());
                } catch (PluginNotFoundException e) {
                    ps.println("None");
                    continue;
                }

                if (help != null) {
                    ps.println("{html}");
                    ps.print(help.toString());
                    ps.println("{html}");
                }

                ps.close();
            }
        }
    }

    private void dumpMetrics(PrintStream os, boolean asXML) {
        final String pluginIndent  = "   ";
        final String metricsIndent = pluginIndent + "   ";
        final String metricIndent  = metricsIndent + "   ";
        final String pluginEnd = pluginIndent + "</plugin>";
        final String metricsEnd = metricsIndent + "</metrics>";
        HashMap typeMap = new HashMap();

        if (asXML) {
            os.println(XML_VERSION);
            os.println("<hq>");
        }

        for (int n=0; n<this.pPlugins.length; n++) {
            ProductPlugin pp = this.pPlugins[n];
            String productName = pp.getName();

            TypeInfo[] types = pp.getTypes();
            
            MeasurementPlugin mp;

            if (asXML) {
                os.println(pluginIndent + "<plugin name =\"" +
                           productName + "\">");
            }
            else {
                os.println("\n" + productName + " plugin:");
            }

            if ((types == null) || (types.length == 0)) {
                if (asXML) {
                    os.println(pluginEnd);
                }
                else {
                    os.println("   [No types defined]");
                }
                continue;
            }

            for (int i=0; i<types.length; i++) {
                TypeInfo type = types[i];
                String name = type.getName();
                ServerTypeInfo server = null;

                String typeName = TYPES[type.getType()];
                String indent   = TYPE_INDENT[type.getType()];

                switch (type.getType()) {
                  case TypeInfo.TYPE_SERVER:
                    server = (ServerTypeInfo)type;
                    break;
                  case TypeInfo.TYPE_SERVICE:
                    server = ((ServiceTypeInfo)type).getServerTypeInfo();
                    break;
                }

                MeasurementInfo[] metrics = null;

                mp = this.ppm.getMeasurementPlugin(name);
                if (mp != null) {
                    metrics = mp.getMeasurements(types[i]);
                }

                if (asXML) {
                    //fold multiple types with the same name
                    //XXX could/should include platforms in <metrics>
                    if (typeMap.put(name, type) != null) {
                        continue;
                    }

                    os.println("\n" + metricsIndent +
                               "<metrics type=\"" + typeName + "\" " +
                               "name=\"" + name + "\">");
                }
                else {
                    os.print("\n" + indent + "'" + name +
                             "' " + typeName);
                    if (server != null) {
                        String pTypes = 
                            ArrayUtil.toString(server.getValidPlatformTypes());
                        os.print(" ");
                        os.print(pTypes);
                        
                        if (metrics != null) {
                            int numOn = getNumDefaultOn(metrics);
                            os.print(" [" + metrics.length + " metrics" +
                                     ", " + numOn + " default on]");
                        } 
                    }
                    os.println("");
                }

                if ((metrics == null) || (metrics.length == 0)) {
                    if (asXML) {
                        os.println(metricsEnd);
                    }
                    else {
                        os.println(indent + "[No metrics defined]");
                    }
                    continue;
                }

                for (int j=0; j<metrics.length; j++) {
                    if (asXML) {
                        os.println(metrics[j].toXML(metricIndent));
                    }
                    else {
                        os.println(indent + metricString(metrics[j]));
                    }
                }

                if (asXML) {
                    os.println(metricsEnd);
                }
            }
            if (asXML) {
                os.println(pluginEnd);
            }
        }

        if (asXML) {
            os.println("</hq>");
        }
    }

    private void dumpHelp(File dir) throws IOException {
        for (int n=0; n<this.pPlugins.length; n++) {
            ProductPlugin pp = this.pPlugins[n];
            String productName = pp.getName();

            TypeInfo[] types = pp.getTypes();
            
            if ((types == null) || (types.length == 0)) {
                continue;
            }

            File pDir = null;

            for (int i=0; i<types.length; i++) {
                TypeInfo info = types[i];
                int type = info.getType();
                ServerTypeInfo server = null;

                switch (type) {
                  case TypeInfo.TYPE_SERVER:
                    server = (ServerTypeInfo)info;
                    break;
                  case TypeInfo.TYPE_SERVICE:
                    server = ((ServiceTypeInfo)info).getServerTypeInfo();
                    break;
                }

                Object help;
                try {
                    help = mpm.getHelp(info, getProperties());
                } catch (PluginNotFoundException e) {
                    continue;
                }

                if (help == null) {
                    continue;
                }

                if (pDir == null) {
                    pDir = new File(dir, productName);
                    pDir.mkdirs();
                }

                String fileName = info.getFormattedName();
                if (server != null) {
                    String[] platforms = info.getPlatformTypes();
                    if (platforms.length == 1) {
                        fileName += "-" + platforms[0].toLowerCase();
                    }
                }

                PrintStream ps = openFile(pDir, fileName + ".html");
                ps.println(help.toString());
                ps.close();
            }
        }
    }

    private String metricString(MeasurementInfo metric) {
        String desc = metric.getName();
        String alias = metric.getAlias();
        String defaultOn = metric.isDefaultOn() ? "yes" : "no";
        return "   - " + desc + " (" + alias + ")" + " [default=" +
            defaultOn + "]";
    }

    private int getNumDefaultOn(MeasurementInfo metrics[]) {
        int numOn = 0;
        
        for (int i = 0; i < metrics.length; i++) {
            if (metrics[i].isDefaultOn()) {
                numOn++;
            }
        }
    
        return numOn;
    }

    static PrintStream openFile(File dir, String name)
        throws IOException {

        File file = new File(dir, name);

        dir.mkdirs();

        FileOutputStream os = 
            new FileOutputStream(file);

        System.out.println("generating [" + file + "]");

        return new PrintStream(os);
    }

    static PrintStream openFile(String dir, String name)
        throws IOException {

        return openFile(new File(dir), name);
    }
}
