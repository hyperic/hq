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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.hqu.RenditServer;
import org.hyperic.hq.product.Plugin;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.roo.file.monitor.event.FileEvent;
import org.springframework.roo.file.monitor.event.FileEventListener;
import org.springframework.roo.file.monitor.event.FileOperation;
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

    private static final String PLUGIN_DIR = "hq-plugins";
    private static final String HQU = "hqu";

    private RenditServer renditServer;
    private ProductManager productManager;

    private ProductPluginManager productPluginManager;

    private List<File> pluginDirs = new ArrayList<File>(2);
    private File hquDir;

    private AgentManager agentManager;

    private PluginManager pluginManager;

    @Autowired
    public ProductPluginDeployer(RenditServer renditServer, ProductManager productManager,
                                 AgentManager agentManager, PluginManager pluginManager) {
        this.renditServer = renditServer;
        this.productManager = productManager;
        this.agentManager = agentManager;
        this.pluginManager = pluginManager;
    }

    private void initializePlugins(File pluginDir) {
        List<String> plugins = new ArrayList<String>(0);
        // On startup, it's necessary to load all plugins first due to
        // inter-plugin class dependencies
        try {
            plugins = loadPlugins(pluginDir);
        } catch (Exception e) {
            log.error("Error loading product plugins", e);
        }

        // Now we can deploy the plugins
        final Map<String, Integer> existing = pluginManager.getAllPluginIdsByName();
        Collections.sort(plugins, this);
        for (String pluginName : plugins) {
            existing.remove(pluginName);
            deployPlugin(pluginName);
        }
        pluginManager.markDisabled(existing.values());
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

    private String registerPluginJar(String pluginJar) {
        if (!productPluginManager.isLoadablePluginName(pluginJar)) {
            return null;
        }
        try {
            String plugin = productPluginManager.registerPluginJar(pluginJar, null).name;
            return plugin;
        } catch (Exception e) {
            log.error("Unable to deploy plugin '" + pluginJar + "'", e);
            return null;
        }
    }

    private void deployPlugin(String plugin) {
        try {
            productManager.deploymentNotify(plugin);
        } catch (Exception e) {
            log.error("Unable to deploy plugin '" + plugin + "'", e);
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
        FileWatcher fileWatcher = new FileWatcher();
        fileWatcher.addFileEventListener(new ProductPluginFileEventListener());
        for(File pluginDir: this.pluginDirs) {
            initializePlugins(pluginDir);
            fileWatcher.addDir(pluginDir.toString(), false);
        }
        if(!(pluginDirs.isEmpty())) {
            fileWatcher.start();
        }
    }

    private void unpackJar(File pluginJarFile, File destDir, String prefix) throws Exception {

        JarFile jar = new JarFile(pluginJarFile);
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

    private List<String> loadPlugins(File pluginDir) throws Exception {
        File[] plugins = pluginDir.listFiles();
        List<String> pluginNames = new ArrayList<String>();
        for (File pluginFile : plugins) {
            String plugin = loadPlugin(pluginFile, true);
            if (plugin != null) {
                pluginNames.add(plugin);
            }
        }
        return pluginNames;
    }

    private void undeployPlugin(File pluginFile) throws Exception {
        log.info("Undeploying plugin: " + pluginFile);
        productPluginManager.removePluginJar(pluginFile.toString());
        agentManager.removePluginFromAgentsInBackground(pluginFile.getName());
    }

    private String loadPlugin(File pluginFile, boolean initializing) throws Exception {
        String plugin = registerPluginJar(pluginFile.toString());
        if (plugin != null) {
            deployHqu(plugin, pluginFile, initializing);
            return plugin;
        }
        return null;
    }

    private void loadAndDeployPlugin(File pluginFile) throws Exception {
        String pluginName = loadPlugin(pluginFile, false);
        if (pluginName != null) {
            log.info("Deploying plugin: " + pluginName);
            deployPlugin(pluginName);
        }
        agentManager.syncPluginToAgentsAfterCommit(pluginFile.getName());
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            this.hquDir = applicationContext.getResource(HQU).getFile();
        } catch (IOException e) {
            log.info("HQU directory not found");
        }
        try {
             File pluginDir = applicationContext.getResource("WEB-INF/" + PLUGIN_DIR).getFile();
             pluginDirs.add(pluginDir);
        } catch (IOException e) {
            log.error("Plugins directory not found", e);
        }
        //Add custom hq-plugins dir at same level as server home
        File workingDirParent = new File(System.getProperty("user.dir")).getParentFile();
        if( workingDirParent != null && new File(workingDirParent,"hq-plugins").exists()) {
            File customPluginDir = new File(workingDirParent,"hq-plugins");
            pluginDirs.add(customPluginDir);
         }  
    }

    private class ProductPluginFileEventListener implements FileEventListener {

        public void onFileEvent(FileEvent fileEvent) {
            if (log.isDebugEnabled()) {
                log.debug("Received product plugin file event: " + fileEvent);
            }
            try {
                if (FileOperation.CREATED.equals(fileEvent.getOperation())) {
                    loadAndDeployPlugin(fileEvent.getFileDetails().getFile());
                } else if (FileOperation.DELETED.equals(fileEvent.getOperation())) {
                    undeployPlugin(fileEvent.getFileDetails().getFile());
                } else if (FileOperation.UPDATED.equals(fileEvent.getOperation()) &&
                           !(pluginDirs.contains(fileEvent.getFileDetails().getFile()))) {
                    undeployPlugin(fileEvent.getFileDetails().getFile());
                    loadAndDeployPlugin(fileEvent.getFileDetails().getFile());
                }
            } catch (Exception e) {
                log.error("Error responding to plugin file event " + fileEvent + ".  Cause: " + e, e);
            }
        }
    }

}
