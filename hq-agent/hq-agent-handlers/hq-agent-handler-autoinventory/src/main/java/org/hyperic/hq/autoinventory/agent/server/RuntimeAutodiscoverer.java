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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.diagnostics.AgentDiagnosticObject;
import org.hyperic.hq.agent.diagnostics.AgentDiagnostics;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentRunningException;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.agent.server.ConfigStorage;
import org.hyperic.hq.agent.server.ConfigStorage.Key;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.RuntimeScanner;
import org.hyperic.hq.autoinventory.Scanner;
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

class RuntimeAutodiscoverer implements RuntimeScanner, AgentDiagnosticObject {

    private static final long DEFAULT_SCAN_INTERVAL = 15 * 60000;

    private static final String STORAGE_PREFIX  = "runtimeautodiscovery";
    private static final String STORAGE_KEYLIST = "runtimeAD-keylist";
    private static final String SERVICE_PREFIX  = "service-config";
    private static final String SERVICE_KEYLIST = "service-keylist";
        

    private static Log _log = LogFactory.getLog(RuntimeAutodiscoverer.class);

    private AutoinventoryCommandsServer _aicmd;
    private AgentDaemon                 _agent;
    private AutoinventoryCallbackClient _client;
    private AutoinventoryPluginManager  _apm;
    private ConfigStorage _storage, _serviceStorage;

    //"current" interval can be changed on demand by the
    //trigger methods to scan sooner than the "normal" interval.
    private long _currentScanInterval;
    private long _normalScanInterval;

    private long _currentDefaultScanInterval;
    private long _normalDefaultScanInterval;

    private volatile boolean _isRuntimeScanning = false;
    private volatile Map<Key, ConfigResponse> _insertsDuringScan = new HashMap<Key, ConfigResponse>();

    private final AtomicReference<CompositeRuntimeResourceReport> _lastReport =
        new AtomicReference<CompositeRuntimeResourceReport>();
    private final AtomicBoolean _writeLastReportDiag = new AtomicBoolean(true);

    public RuntimeAutodiscoverer (AutoinventoryCommandsServer aicmd,
                                  AgentStorageProvider storageProvider,
                                  AgentDaemon agent,
                                  AutoinventoryCallbackClient client) 
    {
        AgentDiagnostics.getInstance().addDiagnostic(this);
        _aicmd = aicmd;
        _storage = new ConfigStorage(storageProvider, STORAGE_KEYLIST, 
                                     STORAGE_PREFIX);
                                     
        _serviceStorage = new ConfigStorage(storageProvider, SERVICE_KEYLIST, 
                                            SERVICE_PREFIX);
        _agent = agent;
        _client = client;
        try {
            _apm = (AutoinventoryPluginManager)
                agent.getPluginManager(ProductPlugin.TYPE_AUTOINVENTORY);
        } catch (AgentRunningException are) {
            throw new IllegalStateException("Agent not running? " + are);
        } catch (PluginException gpme) {
            throw new IllegalStateException("Error getting plugin managers: " 
                                            + gpme);
        }

        _currentScanInterval = _normalScanInterval =
            loadScanInterval("runtimeScan");

        triggerScan();

        _currentDefaultScanInterval = _normalDefaultScanInterval =
            loadScanInterval("defaultScan");

        triggerDefaultScan();
    }

    public void updateConfig(AgentRemoteValue args)
        throws AgentRemoteException {

        ConfigStorage configStorage;
        
        int type = Integer.parseInt(args.getValue(ConfigStorage.PROP_TYPE));
        
        if (type == AppdefEntityConstants.APPDEF_TYPE_SERVICE) {
            configStorage = _serviceStorage;
        } else {
            configStorage = _storage;
        }

        ConfigStorage.Key key = configStorage.getKey(args);
        boolean isEnable = (args.getValue("disable.rtad") == null);

        try {
            _lastReport.set(null); //clear cache
            if (isEnable) {
                ConfigResponse config = configStorage.put(key, args);
                if (_isRuntimeScanning) {
                    _log.debug("Scan running while storing config for: " +
                              key);
                    synchronized (_insertsDuringScan) {
                        _insertsDuringScan.put(key, config);
                    }
                } else {
                    _log.debug("Triggering scan after storing config for: " +
                              key);
                    triggerScan();
                }
            } else {
                configStorage.remove(key);
            }
        } catch (AgentStorageException e) {
            String method = isEnable ? "store" : "remove";
            String msg = "Failed to " + method + " config for " + key + 
                ": " + e;
            
            throw new AgentRemoteException(msg, e);
        }
    }

    /** @see org.hyperic.hq.autoinventory.RuntimeScanner#getScanInterval */
    public long getScanInterval() { 
        return _currentScanInterval;
    }

    /** @see org.hyperic.hq.autoinventory.RuntimeScanner#getDefaultScanInterval */
    public long getDefaultScanInterval() {
        return _currentDefaultScanInterval;
    }

    public void triggerScan() {
        _currentScanInterval = 5000;
    }

    public void triggerDefaultScan() {
        _currentDefaultScanInterval = 5000;
    }

    /** @see org.hyperic.hq.autoinventory.RuntimeScanner#scheduleDefaultScan */
    public void scheduleDefaultScan () {
        _aicmd.scheduleDefaultScan();

        // Reset scan interval to the default
        _currentDefaultScanInterval = _normalDefaultScanInterval;
    }

    /** @see org.hyperic.hq.autoinventory.RuntimeScanner#doRuntimeScan */
    public void doRuntimeScan() throws AutoinventoryException {
        //This Map is a copy, we can do with it as we please.
        Map<Key, ConfigResponse> configs = _storage.load();
        
        _isRuntimeScanning = (configs.size() > 0);

        while (_isRuntimeScanning) {
            doRuntimeScan_internal(configs);

            // If any configs were inserted while we were scanning
            // go and scan those now.
            synchronized (_insertsDuringScan) {
                int size = _insertsDuringScan.size();
                if (size == 0) {
                    _isRuntimeScanning = false;
                    break;
                } else {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Processing " + size + " configs inserted while scan was running");
                    }
                    // reset flag, scan again
                    configs.clear();
                    configs.putAll(_insertsDuringScan);
                    _insertsDuringScan.clear();
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void doRuntimeScan_internal(Map<ConfigStorage.Key, ConfigResponse> configs) throws AutoinventoryException {
        Map<ConfigStorage.Key, ConfigResponse> serviceConfigs = _serviceStorage.load();

        //drop service configs into the plugin manager so they can
        //be used by plugins to discover cprops for services
        for (Entry<ConfigStorage.Key, ConfigResponse> entry : serviceConfigs.entrySet()) {
            ConfigStorage.Key key = entry.getKey();
            ConfigResponse config = entry.getValue();
            String type = key.getTypeName();
            _apm.addServiceConfig(type, config);
        }
        
        CompositeRuntimeResourceReport compositeReport = new CompositeRuntimeResourceReport();
        
        for (Entry<ConfigStorage.Key, ConfigResponse> entry : configs.entrySet()) {
            ConfigStorage.Key key = entry.getKey();
            ConfigResponse config = entry.getValue();
            String type = key.getTypeName();
            ServerDetector detector;
            RuntimeDiscoverer discoverer;

            try {
                detector = (ServerDetector)_apm.getPlugin(type);
            } catch (PluginNotFoundException e) {
                //plugins are not required to support AI
                if (_log.isDebugEnabled()) {
                    _log.debug("Plugin does not support server detection: " + type, e);
                }
                continue;
            }

            if (!detector.isRuntimeDiscoverySupported()) {
                if (_log.isDebugEnabled()) {
                    _log.debug("Plugin does not support runtime discovery: " + type);
                }
                continue;
            }

            PluginLoader.setClassLoader(detector);
            try {
                discoverer = detector.getRuntimeDiscoverer();
                _log.info("Running runtime autodiscovery for " + type);
                PlatformResource platform = Scanner.detectPlatform(_apm, config);
                StopWatch timer = new StopWatch();
                RuntimeResourceReport report = discoverer.discoverResources(key.getId(), platform, config);  
                _log.info(key.getTypeName() + " discovery took " + timer);
                compositeReport.addServerReport(report);
            } catch (Exception e) {
                _log.error("Unexpected error running autodiscoverer for plugin: " + type + ": " + e, e);
                continue;
            } catch (NoClassDefFoundError e) {
                _log.error("Unable to run autodiscoverer for plugin: "
                          + type + " (consult product setup help): " + e, e);
                _log.debug("Current ClassLoader=" + PluginLoader.getClassLoader());
                continue;
            } finally {
                PluginLoader.resetClassLoader(detector);
            }
        }

        compositeReport = loadReportFilter().filterReport(compositeReport);
        if (compositeReport.isSameReport(_lastReport.get())) {
            _log.debug("No changes detected, not sending runtime report");
            _writeLastReportDiag.set(false);
        } else {
            final String errMsg = "Error sending runtime report to server: ";
            _lastReport.set(compositeReport);
            _writeLastReportDiag.set(true);
            try {
                if (_log.isDebugEnabled()) {
                    _log.debug("Sending RuntimeReport: " + compositeReport.simpleSummary());
                }
                _client.aiSendRuntimeReport(compositeReport);
            } catch (Exception e) {
                _log.error(errMsg + e, e);
            }
        }

        // Reset scan interval to the default
        _currentScanInterval = _normalScanInterval;
    }

    private long loadScanInterval(String type) {
        // get scan intervals from agent.properties.
        Properties bootProps = _agent.getBootConfig().getBootProperties(); 

        String prop  = "autoinventory." + type + ".interval.millis";
        String value = bootProps.getProperty(prop);

        long interval;

        if (value == null) {
            interval = DEFAULT_SCAN_INTERVAL;
        } else {
            try {
                interval = Long.parseLong(value);
            } catch (NumberFormatException e) {
                String msg = prop + " value not a number '" + value + "'";
                throw new IllegalArgumentException(msg);
            }
            // -1 means never scan unless told to.
            // while Long.MAX_VALUE is not infinite, would take years before 
            // a scan would run.
            if (interval == -1) {
                interval = Long.MAX_VALUE;
            }
        }

        return interval;
    }
    
    private RuntimeReportFilter loadReportFilter() {
        RuntimeReportFilter defaultFilter = new RuntimeReportFilter() {
            public CompositeRuntimeResourceReport filterReport(CompositeRuntimeResourceReport r)  {
                return r;
            }
        };
        String val = _agent.getBootConfig().getBootProperties().getProperty("autoinventory.reportFilter");
        if (val == null) {
            return defaultFilter;
        }
        try {
            Class<?> c = Class.forName(val);
            return (RuntimeReportFilter)c.newInstance();
        } catch(Throwable t) {
            _log.error("Unable to create autoinventory.reportFilter [" +  val + "], using default", t);
            return defaultFilter;
        }
    }

    public String getDiagStatus() {
        if (!_writeLastReportDiag.get() || _lastReport.get() == null) {
            return "Discovery report has not changed";
        }
        _writeLastReportDiag.set(false);
        return _lastReport.get().fullSummary();
    }

    public String getDiagName() {
        return "Runtime Report Diagnostic";
    }
}
