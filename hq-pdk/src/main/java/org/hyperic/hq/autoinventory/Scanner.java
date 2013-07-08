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

package org.hyperic.hq.autoinventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.AutoApproveConfig;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.hq.product.HypericOperatingSystem;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;

import org.hyperic.hq.product.AutoinventoryPluginManager;
import org.hyperic.hq.product.PluginNotFoundException;

/**
 * The Scanner class performs the actual auto-inventory system scan.
 */
public class Scanner {

    private static final Log _log
        = LogFactory.getLog(Scanner.class.getName());

    private ScanConfiguration _scanConfig = null;
    private ScanListener _scanListener = null;

    /** The auto-approval configuration instance */
    private AutoApproveConfig _autoApproveConfig;


    private volatile ScanState _state = new ScanState();
    private volatile boolean _isInterrupted = false;

    private AutoinventoryPluginManager _pluginManager = null;

    /**
     * Create a new Scanner with the specified configuration.
     * @param scanConfig The configuration to use when 
     * scanning.
     * @param listener The class to notify when various scan events
     * occur, such as scan completion.
     * @param apm The autoinventory plugin manager.
     */
    public Scanner (ScanConfiguration scanConfig, 
                    ScanListener listener,
                    AutoinventoryPluginManager apm,
                    AutoApproveConfig autoApproveConfig) {

        _scanConfig = scanConfig;
        _scanListener = listener;
        _pluginManager = apm;
        if (_scanConfig.getIsDefaultScan()) _state.setIsDefaultScan(true);
        _autoApproveConfig = autoApproveConfig;
    }

    public boolean getIsInterrupted () { return _isInterrupted; }
    
   

    public static PlatformResource detectPlatform(AutoinventoryPluginManager apm,
                                                  ConfigResponse config)
        throws AutoinventoryException {
       
        String platformType = HypericOperatingSystem.getInstance().getName();
        PlatformDetector detector;
        String type=null;
        boolean isDevice;
        
        if (config != null) {
            type = config.getValue(ProductPlugin.PROP_PLATFORM_TYPE);
        }
        if (type == null) {
            type = platformType;
        }
        isDevice = !type.equals(platformType);

        if (isDevice && _log.isDebugEnabled() && config != null) {
            String fqdn = config.getValue(ProductPlugin.PROP_PLATFORM_FQDN);
            String addr = config.getValue(ProductPlugin.PROP_PLATFORM_IP);
            _log.debug("Running discovery for another platform: " + type + "=" + fqdn + "/" + addr);
        }

        try {
            detector = (PlatformDetector)apm.getPlugin(type);
        } catch (PluginNotFoundException e) {
            if (isDevice) {
                detector = new PlatformDetector(); //default
            }
            else {
                throw new AutoinventoryException("PlatformDetector not found: " +
                                                 type);
            }
        }
        
        try {
            return detector.getPlatformResource(config);
        } catch (PluginException e) {
            throw new AutoinventoryException(e.getMessage(), e);
        }
    }

    /**
     * Get the current state of the scan.  Note that the ScanState
     * object returned from this method may be modified after
     * it is returned.  Callers who want to persist the state should
     * acquire the object's monitor (via a synchronized block) 
     * before writing the object out.
     *
     * @return The current state of the scan.
     */
    public ScanState getScanState () {
        return _state;
    }

    public void start() {
        ConfigResponse platformConfig = _scanConfig.getConfigResponse();
        _isInterrupted = false;

        try {
            _state.initStartTime();

            // We do this first because we want to make sure the
            // ScanState knows about the scan methods so that
            // if a "status" command is issued rapidly after
            // a "start" command, the client won't get a
            // "scan not yet started" message.  It could still happen of course,
            // but by putting this first we minimize the chances.
            _state.setScanMethods(_scanConfig.getScanMethodNames());
        
            if ( _isInterrupted ) { setStateInterrupted(); return; }
        
            PlatformResource pValue =
                detectPlatform(_pluginManager, platformConfig);

            //default platform config to the platform values we just discovered.
            if (_scanConfig.getIsDefaultScan() && (platformConfig == null)) {
                platformConfig = new ConfigResponse();
                platformConfig.setValue(ProductPlugin.PROP_PLATFORM_FQDN,
                                        pValue.getFqdn());
                platformConfig.setValue(ProductPlugin.PROP_PLATFORM_NAME,
                                        pValue.getFqdn());
                platformConfig.setValue(ProductPlugin.PROP_PLATFORM_TYPE,
                                        pValue.getPlatformTypeName());
            }

            _state.setPlatform(pValue);
        
            if ( _isInterrupted ) { setStateInterrupted(); return; }
            
            ServerSignature[] serverSigs = _scanConfig.getServerSignatures();
            // If there are no server signatures, then stop scanning now, and
            // set the appropriate flag in the scan state.
            if ( serverSigs == null || serverSigs.length == 0 ) {
                _state.setAreServersIncluded(false);
                _log.warn("No server signatures were found.");
                return;
            }
            
            ServerDetector[] serverDetectors = loadDetectors(pValue.getPlatformTypeName(), serverSigs);

            if ( serverDetectors == null || serverDetectors.length == 0 ) {
                _log.warn("No server detectors were loaded.");
            }
            
            ScanMethod scanMethod;
            ScanMethodState[] smStates = _state.getScanMethodStates();
            for ( int i=0; i<smStates.length; i++ ) {
                if ( _isInterrupted ) { setStateInterrupted(); return; }
                
                scanMethod = _state.findScanMethod(smStates[i].getMethodClass());
                scanMethod.init(this, _scanConfig.getScanMethodConfig(scanMethod), _autoApproveConfig);
                try {
                    scanMethod.scan(platformConfig, serverDetectors);
                } catch ( Exception e ) {
                    _log.error("Error during inventory scan: " + e, e);
                    _state.addScanException(scanMethod, e);
                }
            }
        } catch ( Exception global ) {
            _log.error("Global error during inventory scan: " +
                       global, global);
            _state.setGlobalException(global);

        } finally {
            if ( _isInterrupted ) { setStateInterrupted(); return; }
            _state.setIsDone();
            _state.initEndTime();
            notifyScanComplete();
        }
    }
    
    private void setStateInterrupted () {
        _state.setIsInterrupted();
        _state.setIsDone();
    }

    public void stop() {
        _isInterrupted = true;
    }

    /**
     * Load the server detectors for the given set of server signatures.
     * @param serverSigs An array of ServerSignature objects.
     * @return An array of ServerDetector objects.
     */
    private ServerDetector[] loadDetectors(String type,
                                           ServerSignature[] serverSigs) 
        throws AutoinventoryException {

        ServerDetector detector;
        List<ServerDetector> detectorList = new ArrayList<ServerDetector>();
        
        String pluginName = null;
        int i;

        try {
            for ( i=0; i<serverSigs.length; i++ ) {
                pluginName = serverSigs[i].getServerTypeName();
                try {
                    detector = (ServerDetector)_pluginManager.getPlatformPlugin(type, pluginName);
                    detector.setAutoApproveConfig(_autoApproveConfig);
                } catch (PluginNotFoundException ne) {
                    //plugins are not required to support AI
                    _log.warn(ne.getMessage());
                    continue;
                }

                detectorList.add(detector);
            }
        } catch ( Exception e ) {
            String msg =
                "Error loading server detector class for plugin: '" +
                pluginName + "'";
            throw new AutoinventoryException(msg, e);
        }
        
        Collections.sort(detectorList, new Comparator<ServerDetector>() {
            public int compare(ServerDetector detector1, ServerDetector detector2) {
                int order1 = detector1.getScanOrder();
                int order2 = detector2.getScanOrder();
                return order1 - order2;
            }
        });

        return detectorList.toArray(new ServerDetector[detectorList.size()]);
    }

    private void notifyScanComplete () {
        try {
            _scanListener.scanComplete(_state);
        } catch (Exception e) {
            _log.error("Error in ScanListener.scanComplete", e);
        }
    }

    public boolean equals ( Object o ) {
        if (o instanceof Scanner) {
            Scanner s = (Scanner) o;
            return s._scanConfig.equals(_scanConfig) &&
                    s._pluginManager == _pluginManager
                    && s._scanListener == _scanListener;
        }
        return false;
    }
}
