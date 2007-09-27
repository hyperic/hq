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

package org.hyperic.hq.autoinventory.agent.server;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentRunningException;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.agent.server.ConfigStorage;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.RuntimeScanner;
import org.hyperic.hq.autoinventory.Scanner;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.AutoinventoryCallbackClient;
import org.hyperic.hq.product.AutoinventoryPluginManager;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.util.PluginLoader;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.timer.StopWatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class RuntimeAutodiscoverer implements RuntimeScanner {

    private static final long DEFAULT_SCAN_INTERVAL = 15 * 60000;

    private static final String STORAGE_PREFIX =
        "runtimeautodiscovery";

    private static final String STORAGE_KEYLIST =
        "runtimeAD-keylist";

    private static final String SERVICE_PREFIX =
        "service-config";
    
    private static final String SERVICE_KEYLIST =
        "service-keylist";

    private static Log log =
        LogFactory.getLog(RuntimeAutodiscoverer.class.getName());

    private AutoinventoryCommandsServer aicmd;
    private AgentDaemon agent;
    private AutoinventoryCallbackClient client;
    private AutoinventoryPluginManager apm;
    private ConfigStorage storage, serviceStorage;

    //"current" interval can be changed on demand by the
    //trigger methods to scan sooner than the "normal" interval.
    private long currentScanInterval;
    private long normalScanInterval;

    private long currentDefaultScanInterval;
    private long normalDefaultScanInterval;

    private volatile boolean isRuntimeScanning = false;
    private volatile Map insertsDuringScan = new HashMap();

    private CompositeRuntimeResourceReport lastReport = null;

    public RuntimeAutodiscoverer (AutoinventoryCommandsServer aicmd,
                                  AgentStorageProvider storageProvider,
                                  AgentDaemon agent,
                                  AutoinventoryCallbackClient client) {
        this.aicmd = aicmd;
        this.storage =
            new ConfigStorage(storageProvider,
                              STORAGE_KEYLIST, STORAGE_PREFIX);
        this.serviceStorage =
            new ConfigStorage(storageProvider,
                              SERVICE_KEYLIST, SERVICE_PREFIX);
        this.agent = agent;
        this.client = client;
        try {
            this.apm =
                (AutoinventoryPluginManager)agent.
                    getPluginManager(ProductPlugin.TYPE_AUTOINVENTORY);
        } catch (AgentRunningException are) {
            throw new IllegalStateException("Agent not running? " + are);
        } catch (PluginException gpme) {
            throw new IllegalStateException("Error getting plugin managers: " + gpme);
        }

        currentScanInterval = normalScanInterval =
            loadScanInterval("runtimeScan");

        triggerScan();

        currentDefaultScanInterval = normalDefaultScanInterval =
            loadScanInterval("defaultScan");

        triggerDefaultScan();
    }

    public void updateConfig(AgentRemoteValue args)
        throws AgentRemoteException {

        ConfigStorage configStorage;
        
        int type =
            Integer.parseInt(args.getValue(ConfigStorage.PROP_TYPE));
        
        if (type == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            configStorage = this.serviceStorage;
        }
        else {
            configStorage = this.storage;
        }

        ConfigStorage.Key key = configStorage.getKey(args);
        boolean isEnable = (args.getValue("disable.rtad") == null);

        try {
            lastReport = null; //clear cache
            if (isEnable) {
                ConfigResponse config = configStorage.put(key, args);
                if (this.isRuntimeScanning) {
                    log.debug("Scan running while storing config for: " +
                              key);
                    synchronized (this.insertsDuringScan) {
                        this.insertsDuringScan.put(key, config);
                    }
                }
                else {
                    log.debug("Triggering scan after storing config for: " +
                              key);
                    triggerScan();
                }
            }
            else {
                configStorage.remove(key);
            }
        } catch (AgentStorageException e) {
            String method = isEnable ? "store" : "remove";
            String msg =
                "Failed to " + method + " config for " +
                key + ": " + e;
            throw new AgentRemoteException(msg, e);
        }
    }

    /** @see org.hyperic.hq.autoinventory.RuntimeScanner#getScanInterval */
    public long getScanInterval() { 
        return currentScanInterval;
    }

    /** @see org.hyperic.hq.autoinventory.RuntimeScanner#getDefaultScanInterval */
    public long getDefaultScanInterval() {
        return currentDefaultScanInterval;
    }

    public void triggerScan() {
        currentScanInterval = 5000;
    }

    public void triggerDefaultScan() {
        currentDefaultScanInterval = 5000;
    }

    /** @see org.hyperic.hq.autoinventory.RuntimeScanner#scheduleDefaultScan */
    public void scheduleDefaultScan () {
        aicmd.scheduleDefaultScan();

        // Reset scan interval to the default
        currentDefaultScanInterval = normalDefaultScanInterval;
    }

    /** @see org.hyperic.hq.autoinventory.RuntimeScanner#doRuntimeScan */
    public void doRuntimeScan() throws AutoinventoryException {
        //This Map is a copy, we can do with it as we please.
        Map configs = this.storage.load();
        
        this.isRuntimeScanning = (configs.size() > 0);

        while (this.isRuntimeScanning) {
            // Always sleep for 5 seconds before running a scan.  This
            // prevents lock contention on the server for runtime scans
            // that are very quick.
            try { Thread.sleep(5000); } catch ( InterruptedException ie ) {}

            doRuntimeScan_internal(configs);

            // If any configs were inserted while we were scanning
            // go and scan those now.
            synchronized (this.insertsDuringScan) {
                int size = this.insertsDuringScan.size();
                if (size == 0) {
                    this.isRuntimeScanning = false;
                    break;
                }
                else {
                    log.debug("Processing " + size +
                              " configs inserted while scan was running");
                    // reset flag, scan again
                    configs.clear();
                    configs.putAll(this.insertsDuringScan);
                    this.insertsDuringScan.clear();
                }
            }
        }
    }

    private void doRuntimeScan_internal(Map configs) throws AutoinventoryException {
        Map serviceConfigs = this.serviceStorage.load();

        //drop service configs into the plugin manager so they can
        //be used by plugins to discover cprops for services
        for (Iterator it = serviceConfigs.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            ConfigStorage.Key key = (ConfigStorage.Key)entry.getKey();
            ConfigResponse config = (ConfigResponse)entry.getValue();
            String type = key.getTypeName();
            this.apm.addServiceConfig(type, config);
        }
        
        CompositeRuntimeResourceReport compositeReport =
            new CompositeRuntimeResourceReport();
        
        for (Iterator it = configs.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            ConfigStorage.Key key = (ConfigStorage.Key)entry.getKey();
            ConfigResponse config = (ConfigResponse)entry.getValue();
            String type = key.getTypeName();
            ServerDetector detector;
            RuntimeDiscoverer discoverer;

            try {
                detector = (ServerDetector)apm.getPlugin(type);
            } catch (PluginNotFoundException e) {
                //plugins are not required to support AI
                log.debug("Plugin does not support server detection: " + type);
                continue;
            }

            if (!detector.isRuntimeDiscoverySupported()) {
                log.debug("Plugin does not support runtime discovery: " + type);
                continue;
            }

            PluginLoader.setClassLoader(detector);
            try {
                discoverer = detector.getRuntimeDiscoverer();
                log.info("Running runtime autodiscovery for " + type);

                PlatformResource platform =
                    Scanner.detectPlatform(apm, config);
                StopWatch timer = new StopWatch();

                RuntimeResourceReport report =
                    discoverer.discoverResources(key.getId(), 
                                                 platform, 
                                                 config);

                log.info(key.getTypeName() + " discovery took " + timer);

                compositeReport.addServerReport(report);
            } catch (PluginException e) {
                log.warn("Error running autodiscoverer for plugin: " +
                         type + ": " + e.getMessage(), e);
                continue;
            } catch (Exception e) {
                log.error("Unexpected error running autodiscoverer for plugin: "
                          + type + ": " + e.getMessage(), e);
                continue;
            } catch (NoClassDefFoundError e) {
                log.error("Unable to run autodiscoverer for plugin: "
                          + type + " (consult product setup help): "
                          + e.getMessage(), e);
                log.debug("Current ClassLoader=" + PluginLoader.getClassLoader());
                continue;
            } finally {
                PluginLoader.resetClassLoader(detector);
            }
        }

        if (compositeReport.isSameReport(lastReport)) {
            log.debug("No changes detected, not sending runtime report");
        }
        else {
            final String errMsg = "Error sending runtime report to server: ";
            lastReport = compositeReport;
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Sending RuntimeReport: " +
                              compositeReport.simpleSummary());
                }
                client.aiSendRuntimeReport(compositeReport);
            } catch(AgentCallbackClientException e) {
                if(log.isDebugEnabled()) {
                    log.error(errMsg + e, e);
                } else {
                    log.error(errMsg + e);
                }
            } catch (Exception e) {
                log.error(errMsg + e, e);
            }
        }

        // Reset scan interval to the default
        currentScanInterval = normalScanInterval;
    }

    private long loadScanInterval(String type) {
        // get scan intervals from agent.properties.
        Properties bootProps = 
            this.agent.getBootConfig().getBootProperties();

        //XXX ought to support human readable format like "15min", "1d"
        //rather than millis.
        String prop  = "autoinventory." + type + ".interval.millis";
        String value = bootProps.getProperty(prop);

        long interval;

        if (value == null) {
            interval = DEFAULT_SCAN_INTERVAL;
        }
        else {
            try {
                interval = Long.parseLong(value);
            } catch (NumberFormatException e) {
                String msg = prop + " value not a number '" + value + "'";
                throw new IllegalArgumentException(msg);
            }
            // -1 means never scan unless told to.
            // while Long.MAX_VALUE is not infinite, would take years before a scan would run.
            if (interval == -1) {
                interval = Long.MAX_VALUE;
            }
        }

        return interval;
    }
}
