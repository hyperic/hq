/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfiguration;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanManager;
import org.hyperic.hq.autoinventory.ScanMethod;
import org.hyperic.hq.autoinventory.ScanState;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.autoinventory.agent.client.AICommandsClient;
import org.hyperic.hq.autoinventory.agent.client.AICommandsUtils;
import org.hyperic.hq.autoinventory.scanimpl.NullScan;
import org.hyperic.hq.autoinventory.scanimpl.WindowsRegistryScan;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.AutoinventoryPluginManager;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * The AI Commands service.
 */
public class AICommandsService implements AICommandsClient {
    
    private static final Log _log = LogFactory.getLog(AICommandsService.class);
    
    private final AutoinventoryPluginManager _pluginManager;
    private final RuntimeAutodiscoverer _rtAutodiscoverer;
    private final ScanManager _scanManager;
    
    private final Object _lock = new Object();
    
    private ScanState _mostRecentState;
    
    
    public AICommandsService(AutoinventoryPluginManager pluginManager, 
                             RuntimeAutodiscoverer rtAutodiscoverer,
                             ScanManager scanManager) {
        _pluginManager = pluginManager;
        _rtAutodiscoverer = rtAutodiscoverer;
        _scanManager = scanManager;
    }

    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#getScanStatus()
     */
    public ScanStateCore getScanStatus()  throws AgentRemoteException {
        return getScanStatus(true);
    }
    
    ScanStateCore getScanStatus(boolean interruptHangingScan) 
        throws AgentRemoteException {

        if (interruptHangingScan) {
            _scanManager.interruptHangingScan();            
        }
        
        ScanState state;
        
        try {
            state = doGetScanStatus();            
        
            // Fix bug 7004 -- set endtime so that duration appears 
            // correctly on the serverside when viewing status
            // Fix bug 7134 -- only set endtime if the scan is not yet done
            if ( !state.getIsDone() ) state.initEndTime();
        } catch (Exception e) {
            _log.error("Error getting scan state.", e);
            throw new AgentRemoteException("Error getting scan status: " + 
                                            e.toString());
        }
                
        return state.getCore();
    }
    
    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#pushRuntimeDiscoveryConfig(int, int, java.lang.String, java.lang.String, org.hyperic.util.config.ConfigResponse)
     */
    public void pushRuntimeDiscoveryConfig(int type, 
                                           int id, 
                                           String typeName,
                                           String name, 
                                           ConfigResponse response) 
        throws AgentRemoteException {
                
        AgentRemoteValue args = 
            AICommandsUtils.createArgForRuntimeDiscoveryConfig(type, 
                                                               id, 
                                                               typeName, 
                                                               name, 
                                                               response);
        pushRuntimeDiscoveryConfig(args, true);
    }
    
    void pushRuntimeDiscoveryConfig(AgentRemoteValue args, 
                                    boolean interruptHangingScan) 
        throws AgentRemoteException {

        if (interruptHangingScan) {
            _scanManager.interruptHangingScan();            
        }
        
        _rtAutodiscoverer.updateConfig(args);
    }
    
    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#startScan(org.hyperic.hq.autoinventory.ScanConfigurationCore)
     */
    public void startScan(ScanConfigurationCore scanConfigCore)
            throws AgentRemoteException {
        
        startScan(scanConfigCore, true);
    }
    
    void startScan(ScanConfigurationCore scanConfigCore, 
                   boolean interruptHangingScan) 
        throws AgentRemoteException {
        
        if (interruptHangingScan) {
            _scanManager.interruptHangingScan();            
        }
        
        if ( scanConfigCore == null ) {
            throw new AgentRemoteException("No scan configuration exists.");
        }
                
        ScanConfiguration scanConfig = new ScanConfiguration(scanConfigCore);
        
        startScan(scanConfig);        
    }
    
    void startScan(ScanConfiguration scanConfig) {
        ConfigResponse platformConfig = scanConfig.getConfigResponse();
        String platformType = null;
        boolean isDefault = scanConfig.getIsDefaultScan();
        String type = isDefault ? "auto" : "user";

        ServerSignature[] autoSigs, rgySigs;

        autoSigs = rgySigs = scanConfig.getServerSignatures();

        //user scan, default to all if no servers specified.
        boolean userDefault =
            ((autoSigs == null) || (autoSigs.length == 0));

        if (platformConfig != null) {
            platformType =
                platformConfig.getValue(ProductPlugin.PROP_PLATFORM_TYPE);
        }
        if (platformType == null) {
            platformType = OperatingSystem.getInstance().getName();
        }

        boolean isWin32 = PlatformDetector.isWin32(platformType);

        if (isDefault || userDefault) {
            autoSigs = getAutoScanners(platformType);

            if (isWin32) {
                rgySigs = getWindowsRegistryScanners();
            }
        }

        addScanners(scanConfig, new NullScan(), autoSigs);
        if (isWin32 && (rgySigs != null)) {
            addScanners(scanConfig, new WindowsRegistryScan(), rgySigs);
        }

        if (_log.isDebugEnabled()) {
            ServerSignature[] sigs = scanConfig.getServerSignatures();
            ArrayList types = new ArrayList();
            for (int i=0; i<sigs.length; i++) {
                types.add(sigs[i].getServerTypeName());
            }
            _log.debug(type + " scan for: " + types);
        }

        synchronized ( _scanManager ) {
            _scanManager.queueScan(scanConfig);
        }        
    }

    /**
     * @see org.hyperic.hq.autoinventory.agent.client.AICommandsClient#stopScan()
     */
    public void stopScan() throws AgentRemoteException {
        stopScan(true);
    }
    
    void stopScan(boolean interruptHangingScan) throws AgentRemoteException {
        if (interruptHangingScan) {
            _scanManager.interruptHangingScan();            
        }
        
        try {
            if ( !doStopScan() ) {
                throw new AgentRemoteException("No scan is currently running.");
            }
        } catch ( Exception e ){
            _log.error("Error stopping scan.", e);
            throw new AgentRemoteException("Error stopping scan: " + 
                                           e.toString());
        }        
    }
    
    void setMostRecentState(ScanState scanState) {
        synchronized (_lock) {
            _mostRecentState = scanState;            
        }
    }
    
    private ServerSignature[] getAutoScanners(String type) {
        ArrayList sigs = new ArrayList();
        Map plugins = _pluginManager.getPlatformPlugins(type);
        //XXX hack.  we want the jboss plugin to run before tomcat
        //so jboss can drop a hint about the embedded tomcat.
        TreeSet detectors =
            new TreeSet(new Comparator() {
                public int compare(Object o1, Object o2) {
                    String name1 = ((GenericPlugin)o1).getName();
                    String name2 = ((GenericPlugin)o2).getName();
                    return name1.compareTo(name2);
                }
            });

        for (Iterator i = plugins.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            ServerDetector detector;

            if (!(entry.getValue() instanceof ServerDetector)) {
                continue;
            }

            detector = (ServerDetector)entry.getValue();

            TypeInfo info = ((GenericPlugin)detector).getTypeInfo();

            if (info.getType() != TypeInfo.TYPE_SERVER) {
                continue;
            }

            if (!(detector instanceof AutoServerDetector)) {
                continue;
            }

            detectors.add(detector);
        }

        for (Iterator i = detectors.iterator(); i.hasNext();) {
            ServerDetector detector = (ServerDetector)i.next();
            sigs.add(detector.getServerSignature());
        }

        return (ServerSignature[])sigs.toArray(new ServerSignature[0]);
    }

    private ServerSignature[] getWindowsRegistryScanners() {
        ArrayList sigs = new ArrayList();
        Map plugins = _pluginManager.getPlatformPlugins();

        for (Iterator it = plugins.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            ServerDetector detector;

            if (!(entry.getValue() instanceof RegistryServerDetector)) {
                continue;
            }

            detector = (ServerDetector)entry.getValue();

            TypeInfo info = ((GenericPlugin)detector).getTypeInfo();

            if (info.getType() != TypeInfo.TYPE_SERVER) {
                continue;
            }

            sigs.add(detector.getServerSignature());
        }

        return (ServerSignature[])sigs.toArray(new ServerSignature[0]);
    }
    
    private void addScanners(ScanConfiguration scanConfig,
            ScanMethod method, ServerSignature[] sigs) {

        ServerSignature[] signatures = scanConfig.getServerSignatures();

        signatures = (ServerSignature[])
        ArrayUtil.combine(signatures, sigs);

        scanConfig.setServerSignatures(signatures);
        scanConfig.setScanMethodConfig(method, new ConfigResponse());
    } 
    
    /**
     * @return true if a scan was actually running, false otherwise.
     */
    private boolean doStopScan() {

        // A best-effort attempt to grab the most recent scanState we can
        try { doGetScanStatus(); } catch ( Exception e ) {}

        synchronized ( _scanManager ) {
            try {
                return _scanManager.stopScan();
                
            } catch ( AutoinventoryException e ) {
                // Error stopping scan - restart the entire scan manager
                _log.error("Error stopping scan, restarting scan manager: " + e);
                _scanManager.shutdown(1000);
                _scanManager.startup();
            }
            return true;
        }
    }

    private ScanState doGetScanStatus() throws AutoinventoryException {

        ScanState scanState = _scanManager.getStatus();
        
        synchronized (_lock) {
            if ( scanState == null ) {
                if ( _mostRecentState == null ) {
                    throw new AutoinventoryException
                        ("No autoinventory scan has been started.");
                } else {
                    return _mostRecentState;
                }
            }
            _mostRecentState = scanState;            
        }
        
        return scanState; 
    }    
    
}
