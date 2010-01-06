/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.product.server.session;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.annotation.PostConstruct;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.Util;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.hqu.RenditServer;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;
import org.hyperic.hq.notready.NotReadyManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginInfo;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.jdbc.DBUtil;

import org.jboss.system.server.ServerConfig;
import org.jboss.system.server.ServerConfigLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

/**
 * ProductPlugin deployer. We accept $PLUGIN_DIR/*.{jar,xml}
 * 
 * 
 */
@ManagedResource("hyperic.jmx:type=Service,name=ProductPluginDeployer")
@Service
public class ProductPluginDeployer implements Comparator<String> {

    private final Log log = LogFactory.getLog(ProductPluginDeployer.class);

    private static final String PLUGIN_DIR = "hq-plugins";
    private static final String HQU = "hqu";

    private static final String TAB_DATA = MeasurementConstants.TAB_DATA, MEAS_VIEW = MeasTabManagerUtil.MEAS_VIEW;

    private HQApp hqApp;
    private DBUtil dbUtil;
    private RenditServer renditServer;
    private ProductManager productManager;
    private NotReadyManager notReadyManager;

    private Log _log = LogFactory.getLog(ProductPluginDeployer.class);

    private ProductPluginManager _ppm;
    private List<String> _plugins = new ArrayList<String>();

    private String _pluginDir;
    private String _hquDir;

    @Autowired
    public ProductPluginDeployer(HQApp hqApp, DBUtil dbUtil, RenditServer renditServer,
                                 ProductManager productManager,
                                 NotReadyManager notReadyManager) {
        this.hqApp = hqApp;
        this.dbUtil = dbUtil;
        this.renditServer = renditServer;
        this.productManager = productManager;
        this.notReadyManager = notReadyManager;
        // XXX un-hardcode these paths.
        String war = System.getProperty("jboss.server.home.dir") + "/deploy/hq.war";

        // native libraries are deployed into another directory
        // which is not next to sigar.jar, so we drop this hint
        // to find it.
        System.setProperty("org.hyperic.sigar.path", war + "/sigar_bin/lib");

        _hquDir = war + "/" + HQU;
        
        _pluginDir = war + "/" + PLUGIN_DIR;

        // Initialize database
        initDatabase();

        File propFile = ProductPluginManager.PLUGIN_PROPERTIES_FILE;
        _ppm = new ProductPluginManager(propFile);
        _ppm.setRegisterTypes(true);

        if (propFile.canRead()) {
            _log.info("Loaded custom properties from: " + propFile);
        }
    }

    private void initDatabase() {
        Connection conn = null;

        try {

            conn = dbUtil.getConnection();

            DatabaseRoutines[] dbrs = getDBRoutines(conn);

            for (int i = 0; i < dbrs.length; i++) {
                dbrs[i].runRoutines(conn);
            }
        } catch (SQLException e) {
            log.error("SQLException creating connection to " + HQConstants.DATASOURCE, e);
        } catch (NamingException e) {
            log.error("NamingException creating connection to " + HQConstants.DATASOURCE, e);
        } finally {
            DBUtil.closeConnection(ProductPluginDeployer.class, conn);
        }
    }

    interface DatabaseRoutines {
        public void runRoutines(Connection conn) throws SQLException;
    }

    private DatabaseRoutines[] getDBRoutines(Connection conn) throws SQLException {
        ArrayList<CommonRoutines> routines = new ArrayList<CommonRoutines>(2);

        routines.add(new CommonRoutines());

        return (DatabaseRoutines[]) routines.toArray(new DatabaseRoutines[0]);
    }

    class CommonRoutines implements DatabaseRoutines {
        public void runRoutines(Connection conn) throws SQLException {
            final String UNION_BODY = "SELECT * FROM HQ_METRIC_DATA_0D_0S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_0D_1S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_1D_0S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_1D_1S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_2D_0S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_2D_1S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_3D_0S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_3D_1S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_4D_0S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_4D_1S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_5D_0S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_5D_1S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_6D_0S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_6D_1S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_7D_0S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_7D_1S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_8D_0S UNION ALL "
                                      + "SELECT * FROM HQ_METRIC_DATA_8D_1S";

            final String HQ_METRIC_DATA_VIEW = "CREATE VIEW " + MEAS_VIEW + " AS " + UNION_BODY;

            final String EAM_METRIC_DATA_VIEW = "CREATE VIEW " + TAB_DATA + " AS " + UNION_BODY +
                                                " UNION ALL SELECT * FROM HQ_METRIC_DATA_COMPAT";

            Statement stmt = null;
            try {
                HQDialect dialect = Util.getHQDialect();
                stmt = conn.createStatement();
                if (!dialect.viewExists(stmt, TAB_DATA))
                    stmt.execute(EAM_METRIC_DATA_VIEW);
                if (!dialect.viewExists(stmt, MEAS_VIEW))
                    stmt.execute(HQ_METRIC_DATA_VIEW);
            } catch (SQLException e) {
                log.debug("Error Creating Metric Data Views", e);
            } finally {
                DBUtil.closeStatement(ProductPluginDeployer.class, stmt);
            }
        }
    }

    /**
     * This is called when the full server startup has occurred, and you get the
     * "Started in 30s:935ms" message.
     * 
     * We load all startup classes, then initialize the plugins. Currently this
     * is necesssary, since startup classes need to initialize the application
     * (creating callbacks, etc.), and plugins can't hit the app until that's
     * been done. Unfortunately, it also means that any startup listeners that
     * depend on plugins loaded through the deployer won't work. So far that
     * doesn't seem to be a problem, but if it ends up being one, we can split
     * the plugin loading into more stages so that everyone has access to
     * everyone.
     * 
     * 
     */
    private void serverStarted() {
        loadConfig();
        try {
            loadPlugins();
        } catch (Exception e) {
            log.error("Error loading product plugins",e);
        }

        Collections.sort(_plugins, this);

        for (String pluginName : _plugins) {
            deployPlugin(pluginName);
        }
        
        _plugins.clear();
        startConcurrentStatsCollector();

        setReady(true);

    }

    private void startConcurrentStatsCollector() {

        try {
            ConcurrentStatsCollector c = ConcurrentStatsCollector.getInstance();
            c.register(ConcurrentStatsCollector.RUNTIME_PLATFORM_AND_SERVER_MERGER);
            c.register(ConcurrentStatsCollector.AVAIL_MANAGER_METRICS_INSERTED);
            c.register(ConcurrentStatsCollector.DATA_MANAGER_INSERT_TIME);
            c.register(ConcurrentStatsCollector.JMS_TOPIC_PUBLISH_TIME);
            c.register(ConcurrentStatsCollector.JMS_QUEUE_PUBLISH_TIME);
            c.register(ConcurrentStatsCollector.METRIC_DATA_COMPRESS_TIME);
            c.register(ConcurrentStatsCollector.DB_ANALYZE_TIME);
            c.register(ConcurrentStatsCollector.PURGE_EVENT_LOGS_TIME);
            c.register(ConcurrentStatsCollector.PURGE_MEASUREMENTS_TIME);
            c.register(ConcurrentStatsCollector.MEASUREMENT_SCHEDULE_TIME);
            c.register(ConcurrentStatsCollector.EMAIL_ACTIONS);
            c.register(ConcurrentStatsCollector.ZEVENT_QUEUE_SIZE);
            c.register(ConcurrentStatsCollector.FIRE_ALERT_TIME);
            c.register(ConcurrentStatsCollector.EVENT_PROCESSING_TIME);
            c.register(ConcurrentStatsCollector.TRIGGER_INIT_TIME);
            c.startCollector();
        } catch (Exception e) {
            _log.error("Could not start Concurrent Stats Collector", e);
        }
    }

    /**
     * 
     */
    public ProductPluginManager getProductPluginManager() {
        return _ppm;
    }

    /**
     * 
     */
    @ManagedAttribute
    public void setPluginDir(String name) {
        _pluginDir = name;
    }

    /**
     * 
     */
    @ManagedAttribute
    public String getPluginDir() {
        return _pluginDir;
    }

    private Set<String> getPluginNames(String type) throws PluginException {
        return _ppm.getPluginManager(type).getPlugins().keySet();
    }

    /**
     * 
     * List registered plugin names of given type. Intended for use via
     * /jmx-console
     */
    @ManagedAttribute
    public ArrayList<String> getRegisteredPluginNames(String type) throws PluginException {
        return new ArrayList<String>(getPluginNames(type));
    }

    /**
     * 
     * List registered product plugin names. Intended for use via /jmx-console
     */
    @ManagedAttribute
    public ArrayList<String> getRegisteredPluginNames() throws PluginException {
        return new ArrayList<String>(_ppm.getPlugins().keySet());
    }

    /**
     * 
     */
    @ManagedMetric
    public int getProductPluginCount() throws PluginException {
        return _ppm.getPlugins().keySet().size();
    }

    /**
     * 
     */
    @ManagedMetric
    public int getMeasurementPluginCount() throws PluginException {
        return getPluginNames(ProductPlugin.TYPE_MEASUREMENT).size();
    }

    /**
     * 
     */
    @ManagedMetric
    public int getControlPluginCount() throws PluginException {
        return getPluginNames(ProductPlugin.TYPE_CONTROL).size();
    }

    /**
     * 
     */
    @ManagedMetric
    public int getAutoInventoryPluginCount() throws PluginException {
        return getPluginNames(ProductPlugin.TYPE_AUTOINVENTORY).size();
    }

    /**
     * 
     */
    @ManagedMetric
    public int getLogTrackPluginCount() throws PluginException {
        return getPluginNames(ProductPlugin.TYPE_LOG_TRACK).size();
    }

    /**
     * 
     */
    @ManagedMetric
    public int getConfigTrackPluginCount() throws PluginException {
        return getPluginNames(ProductPlugin.TYPE_CONFIG_TRACK).size();
    }

    /**
     * 
     */
    @ManagedOperation
    public void setProperty(String name, String value) {
        _ppm.setProperty(name, value);
        _log.info("setProperty(" + name + ", " + value + ")");
    }

    /**
     * 
     */
    @ManagedOperation
    public String getProperty(String name) {
        return _ppm.getProperty(name);
    }

    /**
     * 
     */
    @ManagedOperation
    public PluginInfo getPluginInfo(String name) throws PluginException {
        PluginInfo info = _ppm.getPluginInfo(name);

        if (info == null) {
            throw new PluginException("No PluginInfo found for: " + name);
        }

        return info;
    }

   

    public int compare(String s1, String s2) {
        int order1 = _ppm.getPluginInfo(s1).deploymentOrder;
        int order2 = _ppm.getPluginInfo(s2).deploymentOrder;

        return order1 - order2;
    }

    private void setReady(boolean ready) {
        try {
            notReadyManager.setReady(ready);
        } catch (Exception e) {
            _log.error("Unable to declare application ready", e);
        }
    }

    /**
     *
     */
    @ManagedAttribute
    public boolean isReady() {
        Boolean isReady;
        try {
            isReady = notReadyManager.isReady();
        } catch (Exception e) {
            _log.error("Unable to get Application's ready state", e);
            return false;
        }

        return isReady.booleanValue();
    }

    private void loadConfig() {
        ServerConfig sc = ServerConfigLocator.locate();
        hqApp.setRestartStorageDir(sc.getHomeDir());
        File deployDir = new File(sc.getServerHomeDir(), "deploy");
        File warDir = new File(deployDir, "hq.war");
        hqApp.setWebAccessibleDir(warDir);
    }

    private String registerPluginJar(String pluginJar) {
        if (!_ppm.isLoadablePluginName(pluginJar)) {
            return null;
        }
        try {
            String plugin = _ppm.registerPluginJar(pluginJar, null);
            return plugin;
        } catch (Exception e) {
            _log.error("Unable to deploy plugin '" + pluginJar + "'", e);
            return null;
        }
    }

    private void deployPlugin(String plugin)  {
        try {
            productManager.deploymentNotify(plugin);
        } catch (Exception e) {
            _log.error("Unable to deploy plugin '" + plugin + "'", e);
        }
    }

    /**
     * MBean Service start method. This method is called when JBoss is deploying
     * the MBean, unfortunately, the dependencies that this has with
     * HighAvailService and with other components is such that the only thing
     * this method does is queue up the plugins that are ready for deployment.
     * The actual deployment occurs when the startDeployer() method is called.
     */
    @PostConstruct
    public void start() throws Exception {
        _ppm.init();

        try {
            // hq.war contains sigar_bin/lib with the
            // native sigar libraries. we set sigar.install.home
            // here so plugins which use sigar can find it during Sigar.load()

            String path = getClass().getClassLoader().getResource("sigar_bin").getFile();
            _ppm.setProperty("sigar.install.home", path);
        } catch (Exception e) {
            _log.error(e);
        }

        // turn off ready filter asap at shutdown
        // this.stop() won't run until all files are undeploy()ed
        // which may take several minutes.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                setReady(false);
            }
        });

        serverStarted();
    }

    /**
     * 
     */
    // TODO this is never called (used to be an MBean and called by JBoss on
    // going down)
    @ManagedOperation
    public void stop() {
        setReady(false);
        _plugins.clear();
    }

    private void unpackJar(URL url, File destDir, String prefix) throws Exception {

        JarFile jar = new JarFile(url.getFile());
        try {
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
                JarEntry entry = e.nextElement();
                String name = entry.getName();

                if (name.startsWith(prefix)) {
                    name = name.substring(prefix.length());
                    if (name.length() == 0) {
                        continue;
                    }
                    File file = new File(destDir, name);
                    if (entry.isDirectory()) {
                        file.mkdirs();
                    } else {
                        FileUtil.copyStream(jar.getInputStream(entry), new FileOutputStream(file));
                    }
                }
            }
        } finally {
            jar.close();
        }
    }

  
    private void deployHqu(String plugin, URL pluginFile) throws Exception {
        URLClassLoader pluginClassloader = new URLClassLoader(new URL[] {pluginFile});
        final String prefix = HQU + "/";
        URL hqu = pluginClassloader.getResource(prefix);
        if (hqu == null) {
            return;
        }
        File destDir = new File(_hquDir, plugin);
        boolean exists = destDir.exists();
        _log.info("Deploying " + plugin + " " + HQU + " to: " + destDir);

        unpackJar(pluginFile, destDir, prefix);

        if (renditServer.getSysDir() != null) { // rendit.isReady() ?
            if (exists) {
                // update ourselves to avoid having to delete,sleep,unpack
                renditServer.removePluginDir(destDir.getName());
                renditServer.addPluginDir(destDir);
            } // else Rendit watcher will deploy the new plugin
        }
    }

    private void loadPlugins() throws Exception {
        File pluginDir = new File(getPluginDir());
        File[] plugins = pluginDir.listFiles();
        for(File pluginFile : plugins) {
            String plugin = registerPluginJar(pluginFile.toString());
            if (plugin != null) {
                _plugins.add(plugin);
                deployHqu(plugin, pluginFile.toURI().toURL());
            }
        }
    }
}
