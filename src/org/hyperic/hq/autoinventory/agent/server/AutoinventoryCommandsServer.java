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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentAPIInfo;
import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.agent.server.AgentNotificationHandler;
import org.hyperic.hq.agent.server.AgentRunningException;
import org.hyperic.hq.agent.server.AgentServerHandler;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.ScanConfiguration;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanListener;
import org.hyperic.hq.autoinventory.ScanManager;
import org.hyperic.hq.autoinventory.ScanMethod;
import org.hyperic.hq.autoinventory.ScanState;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.autoinventory.agent.AICommandsAPI;
import org.hyperic.hq.autoinventory.scanimpl.NullScan;
import org.hyperic.hq.autoinventory.scanimpl.WindowsRegistryScan;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.client.AutoinventoryCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.common.SystemException;
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
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class AutoinventoryCommandsServer 
    implements AgentServerHandler, AgentNotificationHandler, ScanListener 
{
    // max sleep is 1 hour between attempts to send AI report to server.
    public static final long AIREPORT_MAX_SLEEP_WAIT = (60000 * 60);

    // we'll keep trying for 30 days to send our a report.
    public static final long AIREPORT_MAX_TRY_TIME = AIREPORT_MAX_SLEEP_WAIT * 24 * 30;
    
    private AICommandsAPI               _verAPI;
    private AgentDaemon                 _agent;
    private AgentStorageProvider        _storage;        
    private Log                         _log;            
    private AutoinventoryPluginManager  _pluginManager;  
    private RuntimeAutodiscoverer       _rtAutodiscoverer;

    // The CertDN uniquely identifies this agent
    protected String _certDN;

    private ScanManager _scanManager;
    private ScanState   _mostRecentState;
    private ScanState   _lastCompletedDefaultScanState;

    private AutoinventoryCallbackClient _client;

    public AutoinventoryCommandsServer(){
        _verAPI = new AICommandsAPI();
        _log    = LogFactory.getLog(AutoinventoryCommandsServer.class);
    }

    public AgentAPIInfo getAPIInfo(){
        return _verAPI;
    }

    public String[] getCommandSet(){
        return AICommandsAPI.commandSet;
    }

    public AgentRemoteValue dispatchCommand(String cmd, AgentRemoteValue args,
                                            InputStream in, OutputStream out)
        throws AgentRemoteException {

        _log.debug("AICommandsServer: asked to invoke cmd=" + cmd);

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return dispatchCommand_internal(cmd, args);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    private AgentRemoteValue dispatchCommand_internal(String cmd, 
                                                      AgentRemoteValue args)
        throws AgentRemoteException {

        // Anytime we get a request from the server, it means the server
        // is available.  So, if there is a scan sleeping in "scanComplete",
        // wake it up now
        _scanManager.interruptHangingScan();

        if(cmd.equals(_verAPI.command_startScan)){
            
            ScanConfigurationCore scanConfig = null;
            try {
                scanConfig = ScanConfigurationCore.fromAgentRemoteValue(AICommandsAPI.PROP_SCANCONFIG, args);
                if ( scanConfig == null ) {
                    throw new AgentRemoteException("No scan configuration exists.");
                }
                startScan(new ScanConfiguration(scanConfig));

            } catch ( AgentRemoteException are ) {
                throw are;

            } catch ( Exception e ) {
                _log.error("Error starting scan.", e);
                throw new AgentRemoteException("Error starting scan: " + 
                                               e.toString());
            }
            return null;

        } else if(cmd.equals(_verAPI.command_stopScan)){

            try {
                if ( !stopScan() ) {
                    throw new AgentRemoteException("No scan is currently running.");
                }
            } catch ( Exception e ){
                _log.error("Error stopping scan.", e);
                throw new AgentRemoteException("Error stopping scan: " + 
                                               e.toString());
            }
            return null;

        } else if(cmd.equals(_verAPI.command_getScanStatus)){

            AgentRemoteValue rval = new AgentRemoteValue();
            ScanState state = null;
            try {
                state = getScanStatus();

                // Fix bug 7004 -- set endtime so that duration appears 
                // correctly on the serverside when viewing status
                // Fix bug 7134 -- only set endtime if the scan is not yet done
                if ( !state.getIsDone() ) state.initEndTime();

                state.getCore().toAgentRemoteValue("scanState", rval);

            } catch ( Exception e ) {
                _log.error("Error getting scan state.", e);
                throw new AgentRemoteException("Error getting scan status: " + 
                                               e.toString());
            }
            return rval;

        } else if(cmd.equals(_verAPI.command_pushRuntimeDiscoveryConfig)){
            _rtAutodiscoverer.updateConfig(args);
            return null;
        } else {
            throw new AgentRemoteException("Unknown command: " + cmd);
        }
    }

    public void startup (AgentDaemon agent) throws AgentStartException {
        try {
            _agent   = agent;
            _storage = agent.getStorageProvider();
            _client  = setupClient();
            _certDN  = _storage.getValue(AgentDaemon.PROP_CERTDN);
        } catch(AgentRunningException exc){
            throw new AgentAssertionException("Agent should be running here");
        }

        try {
            _pluginManager = (AutoinventoryPluginManager)
                agent.getPluginManager(ProductPlugin.TYPE_AUTOINVENTORY);
        } catch (Exception e) {
            throw new AgentStartException("Unable to get auto inventory " +
                                          "plugin manager: " + 
                                          e.getMessage());
        }

        // Initialize the runtime autodiscoverer
        _rtAutodiscoverer = new RuntimeAutodiscoverer(this, _storage, 
                                                      _agent, _client);

        // Fire up the scan manager
        _scanManager = new ScanManager(this, _log, _pluginManager,  
                                      _rtAutodiscoverer);
        _scanManager.startup();

        // Do we have a provider?
        if ( CommandsAPIInfo.getProvider(_storage) == null ) {
            agent.registerNotifyHandler(this, 
                                        CommandsAPIInfo.NOTIFY_SERVER_SET);
        } else {
            _rtAutodiscoverer.triggerDefaultScan();
        }

        _log.info("Autoinventory Commands Server started up");
    }

    public void handleNotification(String msgClass, String msg) {
        if (msgClass.equals(CommandsAPIInfo.NOTIFY_SERVER_SET)) {
            _scanManager.interruptHangingScan();
            _rtAutodiscoverer.triggerDefaultScan();
        }
    }

    /**
     * This is the scan that's run when the agent first starts up,
     * and periodically thereafter.  This method is called by the
     * ScanManager when the RuntimeAutodiscoverer says it's time for
     * a DefaultScan (by default, every 15 mins)
     */
    protected void scheduleDefaultScan () {
        _log.debug("Scheduling DefaultScan...");
        ScanConfiguration scanConfig = new ScanConfiguration();
        
        scanConfig.setIsDefaultScan(true);

        startScan(scanConfig);
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

    public void shutdown () {
        _log.info("Autoinventory Commands Server shutting down");
        // Give the scan manager 3 seconds to shut down.
        synchronized ( _scanManager ) {
            _scanManager.shutdown(3000);
        }
        _log.info("Autoinventory Commands Server shut down");
    }

    private AutoinventoryCallbackClient setupClient() { 
        StorageProviderFetcher fetcher =
            new StorageProviderFetcher(_storage);

        return new AutoinventoryCallbackClient(fetcher);
    }

    private void addScanners(ScanConfiguration scanConfig,
                             ScanMethod method, ServerSignature[] sigs) {

        ServerSignature[] signatures = scanConfig.getServerSignatures();

        signatures = (ServerSignature[])
            ArrayUtil.combine(signatures, sigs);

        scanConfig.setServerSignatures(signatures);
        scanConfig.setScanMethodConfig(method, new ConfigResponse());
    }

    private void startScan ( ScanConfiguration scanConfig ) {
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
     * @return true if a scan was actually running, false otherwise.
     */
    private boolean stopScan () {

        // A best-effort attempt to grab the most recent scanState we can
        try { getScanStatus(); } catch ( Exception e ) {}

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

    private ScanState getScanStatus () throws AutoinventoryException {

        ScanState scanState = _scanManager.getStatus();
        if ( scanState == null ) {
            if ( _mostRecentState == null ) {
                throw new AutoinventoryException
                    ("No autoinventory scan has been started.");
            } else {
                return _mostRecentState;
            }
        }
        _mostRecentState = scanState;
        return scanState; 
    }

    /**
     * This is where we report our autoinventory-detected data to
     * the EAM server.
     * @see org.hyperic.hq.autoinventory.ScanListener#scanComplete
     */
    public void scanComplete (ScanState scanState) 
        throws AutoinventoryException, SystemException {

        // Special handling for periodic default scans
        if (scanState.getIsDefaultScan()) {
            if (_lastCompletedDefaultScanState != null) {
                try {
                    if (_lastCompletedDefaultScanState.isSameState(scanState)) {
                        // If this default scan is the same as the last one,
                        // don't send anything to the server
                        _log.debug("Default scan didn't find any changes, not "
                                  + "sending report to the server");
                        return;
                    }
                } catch (AutoinventoryException e) {
                    // Just log it and continue, I guess we'll send the report
                    // to the server in this case
                    _log.error("Error comparing default scan states: " + e, e);
                }
            }
            _lastCompletedDefaultScanState = scanState;
        }

        // Anytime a scan completes, we update the most recent state
        _mostRecentState = scanState;

        // Issue a warning if we could not even detect the platform
        if ( scanState.getPlatform() == null ) {
            try {
                ByteArrayOutputStream errInfo = new ByteArrayOutputStream();
                PrintStream errInfoPS = new PrintStream(errInfo);
                scanState.printFullStatus(errInfoPS);
                _log.warn("AICommandsServer: scan completed, but we could not even "
                         + "detect the platform, so nothing will be reported "
                         + "to the server.  Here is some information about the error "
                         + "that occurred: \n" + errInfo.toString() + "\n");
            } catch ( Exception e ) {
                _log.warn("AICommandsServer: scan completed, but we could not even "
                         + "detect the platform, so nothing will be reported "
                         + "to the server.  More information would be provided, "
                         + "but this error occurred just trying to generate more "
                         + "information about the error: " + e, e);
            }
        }

        // But regardless, we always report back to the server, so it
        // knows the scan has been completed.
        scanState.setCertDN(_certDN);

        long sleepWaitMillis = 15000;
        long firstTryTime = System.currentTimeMillis();
        long diffTime;
        while ( true ) {
            try {
                if (_log.isDebugEnabled()) {
                    _log.debug("Sending autoinventory report to server: "
                             + scanState
                    /*+ "\nWITH SERVERS=" + StringUtil.iteratorToString(scanState.getAllServers(null).iterator())*/);

                }
                _client.aiSendReport(scanState);
                _log.info("Autoinventory report " + 
                         "successfully sent to server.");
                break;

            } catch (Exception e) {
                diffTime = System.currentTimeMillis() - firstTryTime;
                if (diffTime > AIREPORT_MAX_TRY_TIME) {
                    final String eMsg = "Unable to send autoinventory " +
                        "platform data to server for maximum time of " +
                        StringUtil.formatDuration(AIREPORT_MAX_TRY_TIME) +
                        ", giving up.  Error was: " + e.getMessage();
                        
                    if(_log.isDebugEnabled()){
                        _log.debug(eMsg, e);
                    } else {
                        _log.error(eMsg);
                    }
                    return;
                }
                final String eMsg = "Unable to send autoinventory " +
                    "platform data to server, sleeping for " +
                    String.valueOf(sleepWaitMillis/1000) + " secs before "+
                    "retrying.  Error: " + e.getMessage();

                if(_log.isDebugEnabled()){
                    _log.debug(eMsg, e);
                } else {
                    _log.error(eMsg);
                }

                try {
                    Thread.sleep(sleepWaitMillis);
                    sleepWaitMillis += (sleepWaitMillis/2);
                    if ( sleepWaitMillis > AIREPORT_MAX_SLEEP_WAIT ) {
                        sleepWaitMillis = AIREPORT_MAX_SLEEP_WAIT;
                    }
                } catch ( InterruptedException ie ) {}
            }
        }
    }
}
