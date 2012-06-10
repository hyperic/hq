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
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.hqu.RenditServer;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginInfo;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.hq.product.shared.PluginManager;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.util.file.FileUtil;
import org.hyperic.util.file.FileWatcher;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

/**
 * ProductPlugin deployer. We accept $PLUGIN_DIR/*.{jar,xml}
 * 
 * 
 */
@ManagedResource("hyperic.jmx:type=Service,name=ProductPluginDeployer")
@Service
public class ProductPluginDeployer implements Comparator<String>, ApplicationContextAware {

    private final Log log = LogFactory.getLog(ProductPluginDeployer.class);

    private static final String HQU = "hqu";

    private final RenditServer renditServer;
    private final ProductManager productManager;

    private ProductPluginManager productPluginManager;

    private final List<File> pluginDirs = new ArrayList<File>(2);
    private File hquDir;

    private final AgentManager agentManager;

    private final PluginManager pluginManager;

    private final TransactionRetry transactionRetry;

    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> pluginExecutorTask ;  

    private FileWatcher fileWatcher ; 
    


    private final Collection<FileEvent> fileEvents = new ArrayList<FileEvent>();

    @Autowired
    public ProductPluginDeployer(RenditServer renditServer, ProductManager productManager,
                                 AgentManager agentManager, PluginManager pluginManager,
                                 @Value("#{scheduler}")TaskScheduler taskScheduler,
                                 TransactionRetry transactionRetry) {
        this.renditServer = renditServer;
        this.productManager = productManager;
        this.agentManager = agentManager;
        this.pluginManager = pluginManager;
        this.transactionRetry = transactionRetry;
        this.taskScheduler = taskScheduler;
    }

    private void initializePlugins(Collection<File> pluginDirs) {
        Map<String, File> plugins = new HashMap<String, File>(0);
        // On startup, it's necessary to load all plugins first due to
        // inter-plugin class dependencies
        try {
            plugins = loadPlugins(pluginDirs);
        } catch (Exception e) {
            log.error("Error loading product plugins", e);
        }
        // Now we can deploy the plugins
        final Map<String, Integer> existing = pluginManager.getAllPluginIdsByName();
        final List<String> keys = new ArrayList<String>(plugins.keySet());
        Collections.sort(keys, this);
        for (final String pluginName : keys) {
            existing.remove(pluginName);
            deployPlugin(pluginName, plugins.get(pluginName));
        }
        final Runnable runner = new Runnable() {
            public void run() {
                pluginManager.markDisabled(existing.values());
            }
        };
        transactionRetry.runTransaction(runner, 3, 1000);
    }

    /**
     * 
     */
    public ProductPluginManager getProductPluginManager() {
        return productPluginManager;
    }

    private Set<String> getPluginNames(String type) throws PluginException {
        return productPluginManager.getPluginManager(type).getPlugins().keySet();
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
        return new ArrayList<String>(productPluginManager.getPlugins().keySet());
    }

    /**
     * 
     */
    @ManagedMetric
    public int getProductPluginCount() throws PluginException {
        return productPluginManager.getPlugins().keySet().size();
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
        productPluginManager.setProperty(name, value);
        log.info("setProperty(" + name + ", " + value + ")");
    }

    /**
     * 
     */
    @ManagedOperation
    public String getProperty(String name) {
        return productPluginManager.getProperty(name);
    }

    /**
     * 
     */
    @ManagedOperation
    public PluginInfo getPluginInfo(String name) throws PluginException {
        PluginInfo info = productPluginManager.getPluginInfo(name);

        if (info == null) {
            throw new PluginException("No PluginInfo found for: " + name);
        }

        return info;
    }

    public int compare(String s1, String s2) {
        int order1 = productPluginManager.getPluginInfo(s1).deploymentOrder;
        int order2 = productPluginManager.getPluginInfo(s2).deploymentOrder;

        return order1 - order2;
    }

    private PluginInfo registerPluginJar(String pluginJar) {
        if (!productPluginManager.isLoadablePluginName(pluginJar)) {
            return null;
        }
        try {
            PluginInfo plugin = productPluginManager.registerPluginJar(pluginJar, null);
            return plugin;
        } catch (Exception e) {
            log.error("Unable to deploy plugin '" + pluginJar + "'", e);
            return null;
        }
    }

    private void deployPlugin(final String pluginName, File dir) {
        try {
            productManager.deploymentNotify(pluginName, dir);
            final Runnable runner = new Runnable() {
                public void run() {
                    pluginManager.markEnabled(pluginName);
                }
            };
            transactionRetry.runTransaction(runner, 3, 1000);
        } catch (Exception e) {
            // HHQ-5390
            final Runnable runner = new Runnable() {
                public void run() {
                    pluginManager.markPluginDisabledByName(pluginName);
                }
            };
            transactionRetry.runTransaction(runner, 3, 1000);
            log.error("Unable to deploy plugin '" + pluginName + "'", e);
        }
    }

    @PostConstruct
    public void start() throws Exception {
        File propFile = ProductPluginManager.PLUGIN_PROPERTIES_FILE;
        productPluginManager = new ProductPluginManager(propFile);
        productPluginManager.setRegisterTypes(true);

        if (propFile.canRead()) {
            log.info("Loaded custom properties from: " + propFile);
        }

        if (!(pluginDirs.isEmpty())) {
            ProductPluginManager.setPdkPluginsDir(pluginDirs.get(0).getAbsolutePath());
        }
        productPluginManager.init();
        this.fileWatcher = new FileWatcher();
        fileWatcher.addFileEventListener(new ProductPluginFileEventListener());
        for(File pluginDir: this.pluginDirs) {
            fileWatcher.addDir(pluginDir.toString(), false);
        }
        initializePlugins(pluginDirs);
        if(!(pluginDirs.isEmpty())) {
            fileWatcher.start();
        }
        this.pluginExecutorTask = taskScheduler.scheduleWithFixedDelay(new PluginFileExecutor(), new Date(now()+60000l), 5000l);
    }
    
    @PreDestroy 
    public final void destroy() { 
        this.pluginExecutorTask.cancel(true/*mayInterruptIfRunning*/) ; 
        this.fileWatcher.stop() ; 
    }//EOM 
    
    

    private long now() {
        return System.currentTimeMillis();
    }

    private void unpackJar(File pluginJarFile, File destDir, String prefix) throws Exception {

    	JarFile jar = null ; 
        try {
        	jar = new JarFile(pluginJarFile);
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
        }catch(Throwable t) { 
        	t.printStackTrace() ; 
        } finally {
            if(jar != null) jar.close();
        }
    }

    private void deployHqu(String plugin, File pluginFile, boolean initializing) throws Exception {
        URLClassLoader pluginClassloader = new URLClassLoader(new URL[] { pluginFile.toURI().toURL() });
        final String prefix = HQU + "/";
        URL hqu = pluginClassloader.getResource(prefix);
        if (hqu == null) {
            return;
        }
        File destDir = new File(hquDir, plugin);
        boolean exists = destDir.exists();
        log.info("Deploying " + plugin + " " + HQU + " to: " + destDir);

        unpackJar(pluginFile, destDir, prefix);

        if (!(initializing) && exists) {
            // update ourselves to avoid having to delete,sleep,unpack
            renditServer.removePluginDir(destDir.getName());
            renditServer.addPluginDir(destDir);
        } // else Rendit watcher will deploy the new plugin
    }

    private Map<String, File> loadPlugins(Collection<File> pluginDirs) throws Exception {
        Map<String, File> map = new HashMap<String, File>();
        for (File pluginDir : pluginDirs) {
            File[] files = pluginDir.listFiles();
            for (File file : files) {
                String filename = file.getName();
                if (map.containsKey(filename)) {
                    if (isInCustomDir(file)) {
                        log.info("plugin file " + file + " takes precedence over " +
                                 map.get(filename) + " since it is in the custom plugin " +
                                 "deployment dir (this is ok)");
                        map.put(filename, file);
                    } else {
                        log.info("plugin file " + file + " will not be deployed since the custom file " +
                                 map.get(filename) + " takes precedence (this is ok)");
                    }
                } else {
                    map.put(filename, file);
                }
            }
        }
        Collection<File> plugins = map.values();
        Map<String, File> rtn = new HashMap<String, File>();
        for (File pluginFile : plugins) {
            PluginInfo plugin = loadPlugin(pluginFile, true);
            if (plugin != null) {
                rtn.put(plugin.name, pluginFile);
            }
        }
        return rtn;
    }

    private boolean undeployPlugin(final File pluginFile, boolean force) throws Exception {
        if (!force && !isDeployable(pluginFile)) {
            log.info("cannot undeploy " + pluginFile + " since it is over-written in " +
                     getCustomPluginDir() + " (this is ok)");
            return false;
        }
        log.info("Undeploying plugin: " + pluginFile);
        productPluginManager.removePluginJar(pluginFile.toString());
        final Runnable runner = new Runnable() {
            public void run() {
                pluginManager.markDisabled(pluginFile.getName());
            }
        };
        transactionRetry.runTransaction(runner, 3, 1000);
        return true;
    }

    private PluginInfo loadPlugin(File pluginFile, boolean initializing) throws Exception {
        PluginInfo plugin = registerPluginJar(pluginFile.toString());
        if (plugin != null) {
            deployHqu(plugin.name, pluginFile, initializing);
            return plugin;
        }
        return null;
    }

    private boolean loadAndDeployPlugin(File pluginFile) throws Exception {
        if (!isDeployable(pluginFile)) {
            log.info("cannot deploy " + pluginFile + " since it is over-written in " +
                     getCustomPluginDir() + " (this is ok)");
            return false;
        }
        PluginInfo pluginInfo = loadPlugin(pluginFile, false);
        if (pluginInfo != null) {
            log.info("Deploying plugin: " + pluginFile.getAbsolutePath());
            deployPlugin(pluginInfo.name, pluginFile.getParentFile());
        }
        return true;
    }

    /**
     * simply checks if the pluginFile is in the serverPluginDir and if there is a duplicate
     * filename in the customPluginDir.  If those conditions are met then return false.
     */
    private boolean isDeployable(File pluginFile) {
        if (isInCustomDir(pluginFile)) {
            return true;
        }
        if (new File(getCustomPluginDir().getAbsoluteFile(), pluginFile.getName()).exists()) {
            return false;
        }
        return true;
    }

    /**
     * @return true if pluginFile is located in customPluginDir
     */
    private boolean isInCustomDir(File file) {
        return file.getAbsoluteFile().getParent().startsWith(getCustomPluginDir().getAbsolutePath());
    }

    private File getServerPluginDir() {
        return pluginManager.getServerPluginDir();
    }

    private File getCustomPluginDir() {
        return pluginManager.getCustomPluginDir();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            this.hquDir = applicationContext.getResource(HQU).getFile();
        } catch (IOException e) {
            log.info("HQU directory not found");
        }
        pluginDirs.add(getServerPluginDir());
        // Add custom hq-plugins dir at same level as server home
        final File customPluginDir = getCustomPluginDir();
        if (!customPluginDir.exists()) {
            customPluginDir.mkdirs();
        }
        if( customPluginDir != null && customPluginDir.exists() && customPluginDir.isDirectory()) {
            pluginDirs.add(customPluginDir);
        } else {
            log.error("custom plugin directory " + customPluginDir.getAbsolutePath() +
                      " does not exist.  Without this directory users " +
                      "will not be able to deploy plugins from the HQ Plugin Manager",
                      new Throwable());
        }
    }
    
    private class PluginFileExecutor implements Runnable {
        public void run() {
            try {
                handleFileEvents();
            } catch (Throwable t) {
                log.error(t,t);
            }
        }
        public void handleFileEvents() {
            final Collection<String> toSync = new ArrayList<String>();
            final Collection<FileEvent> events;
            synchronized (fileEvents) {
                events = new ArrayList<FileEvent>(fileEvents);
                fileEvents.clear();
            }
            if (events.isEmpty()) {
                return;
            }
            final boolean debug = log.isDebugEnabled();
            for (final FileEvent fileEvent : events) {
                try {
                    if (debug) log.debug("Received product plugin file event: " + fileEvent);
                    final File pluginFile = fileEvent.getFileDetails().getFile();
                    if (FileOperation.CREATED.equals(fileEvent.getOperation())) {
                        File serverPlugin = new File(getServerPluginDir(), pluginFile.getName());
                        if (!serverPlugin.equals(pluginFile)) {
                            // plugin was deployed in the custom dir but already exists in the
                            // server dir.  redeploy it!
                            undeployPlugin(serverPlugin, true);
                        }
                        boolean deployed = loadAndDeployPlugin(pluginFile);
                        if (deployed) {
                            toSync.add(pluginFile.getName());
                        }
                    } else if (FileOperation.DELETED.equals(fileEvent.getOperation())) {
                        File customPlugin = new File(getCustomPluginDir(), pluginFile.getName());
                        if (customPlugin.exists()) {
                            // do nothing, the file existed in both custom dir and server
                            // then the server file was deleted
                        } else {
                            undeployPlugin(pluginFile, false);
                        }
                    } else if (FileOperation.UPDATED.equals(fileEvent.getOperation()) &&
                               !(pluginDirs.contains(fileEvent.getFileDetails().getFile()))) {
                        boolean undeployed = undeployPlugin(pluginFile, false);
                        if (undeployed) {
                            boolean deployed = loadAndDeployPlugin(pluginFile);
                            if (deployed) {
                                toSync.add(pluginFile.getName());
                            }
                        }
                    }
                } catch (Throwable e) {
                    log.error("Error responding to plugin file event " + fileEvent + ": " + e, e);
                }
            }
            agentManager.syncPluginToAgentsAfterCommit(toSync);
        }
    }

    private class ProductPluginFileEventListener implements FileEventListener {
        public void onFileEvent(FileEvent fileEvent) {
            synchronized (fileEvents) {
                fileEvents.add(fileEvent);
            }
        }
    }

}
