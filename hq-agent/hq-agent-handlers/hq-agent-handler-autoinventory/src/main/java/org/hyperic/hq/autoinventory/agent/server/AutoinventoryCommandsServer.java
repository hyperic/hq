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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.*;
import org.hyperic.hq.agent.server.*;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.autoinventory.*;
import org.hyperic.hq.autoinventory.agent.AICommandsAPI;
import org.hyperic.hq.autoinventory.agent.client.AICommandsClient;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.client.AutoinventoryCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.product.AutoinventoryPluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.AutoApproveConfig;
import org.hyperic.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class AutoinventoryCommandsServer implements AgentServerHandler, AgentNotificationHandler, ScanListener {

    // max sleep is 1 hour between attempts to send AI report to server.
    public static final long AIREPORT_MAX_SLEEP_WAIT = (60000 * 60);

    // we'll keep trying for 30 days to send our a report.
    public static final long AIREPORT_MAX_TRY_TIME = AIREPORT_MAX_SLEEP_WAIT * 24 * 30;

    private AICommandsAPI               _verAPI;
    private AgentDaemon                 _agent;
    private AgentStorageProvider        _storage;
    private Log                         _log;
    private RuntimeAutodiscoverer       _rtAutodiscoverer;
    private AICommandsService           _aiCommandsService;

    private AutoApproveConfig _autoApproveConfig;

    // The CertDN uniquely identifies this agent
    protected String _certDN;

    private ScanManager _scanManager;
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

    public AgentRemoteValue dispatchCommand(String cmd, AgentRemoteValue args, InputStream in, OutputStream out)
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

    public void startup (AgentDaemon agent) throws AgentStartException {
        try {
            _agent   = agent;
            _storage = agent.getStorageProvider();
            _client  = setupClient();
            _certDN  = _storage.getValue(AgentDaemon.PROP_CERTDN);
        } catch(AgentRunningException exc){
            throw new AgentAssertionException("Agent should be running here");
        }

        // Read the auto-approve configuration.
        _autoApproveConfig = new AutoApproveConfig(
                _agent.getBootConfig().getConfDirName(), AgentConfig.DEFAULT_PROP_ENC_KEY_FILE);

        AutoinventoryPluginManager pluginManager;

        try {
            pluginManager = (AutoinventoryPluginManager)
                agent.getPluginManager(ProductPlugin.TYPE_AUTOINVENTORY);
        } catch (Exception e) {
            throw new AgentStartException("Unable to get auto inventory " +
                                          "plugin manager: " +
                                          e.getMessage());
        }

        // Initialize the runtime autodiscoverer
        _rtAutodiscoverer = new RuntimeAutodiscoverer(this, _storage, _agent, _client);

        // Fire up the scan manager
        _scanManager = new ScanManager(this, pluginManager, _rtAutodiscoverer, _autoApproveConfig);

        _aiCommandsService = new AICommandsService(pluginManager, _rtAutodiscoverer, _scanManager);

        AgentTransportLifecycle agentTransportLifecycle;

        try {
            agentTransportLifecycle = agent.getAgentTransportLifecycle();
        } catch (Exception e) {
            throw new AgentStartException("Unable to get agent transport lifecycle: "+
                                            e.getMessage());
        }

        _log.info("Registering AI Commands Service with Agent Transport");

        try {
            agentTransportLifecycle.registerService(AICommandsClient.class, _aiCommandsService);
        } catch (Exception e) {
            throw new AgentStartException("Failed to register AI Commands Service.", e);
        }

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
    
    public final void postInitActions() throws AgentStartException { /*do nothing*/ }//EOM

    public void shutdown () {
        _log.info("Autoinventory Commands Server shutting down");
        // Give the scan manager 3 seconds to shut down.
        synchronized ( _scanManager ) {
            _scanManager.shutdown(3000);
        }
        _log.info("Autoinventory Commands Server shut down");
    }

    /**
     * This is where we report our autoinventory-detected data to
     * the EAM server.
     * @see org.hyperic.hq.autoinventory.ScanListener#scanComplete
     */
    public void scanComplete (ScanState scanState) throws AutoinventoryException, SystemException {

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
        _aiCommandsService.setMostRecentState(scanState);

        // Issue a warning if we could not even detect the platform
        AIPlatformValue aiPlatformValue = scanState.getPlatform();
        if (aiPlatformValue == null) {
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

        // Handle auto approval
        applyAutoApproval(aiPlatformValue);

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
                } catch ( InterruptedException ie ) { /* ignore */ }
            }
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

        _aiCommandsService.startScan(scanConfig);
    }

    private AgentRemoteValue dispatchCommand_internal(String cmd, AgentRemoteValue args)
            throws AgentRemoteException {

        // Anytime we get a request from the server, it means the server
        // is available.  So, if there is a scan sleeping in "scanComplete",
        // wake it up now
        _scanManager.interruptHangingScan();

        if(cmd.equals(AICommandsAPI.command_startScan)){
            ScanConfigurationCore scanConfig;

            try {
                scanConfig = ScanConfigurationCore.fromAgentRemoteValue(AICommandsAPI.PROP_SCANCONFIG, args);
            } catch ( Exception e ) {
                _log.error("Error starting scan.", e);
                throw new AgentRemoteException("Error starting scan: " +
                        e.toString());
            }

            _aiCommandsService.startScan(scanConfig, false);
            return null;

        } else if(cmd.equals(AICommandsAPI.command_stopScan)){
            _aiCommandsService.stopScan(false);
            return null;

        } else if(cmd.equals(AICommandsAPI.command_getScanStatus)){
            AgentRemoteValue rval = new AgentRemoteValue();
            ScanStateCore state = _aiCommandsService.getScanStatus(false);

            try {
                state.toAgentRemoteValue("scanState", rval);
            } catch ( Exception e ) {
                _log.error("Error getting scan state.", e);
                throw new AgentRemoteException("Error getting scan status: " +
                        e.toString());
            }

            return rval;

        } else if(cmd.equals(AICommandsAPI.command_pushRuntimeDiscoveryConfig)){
            _aiCommandsService.pushRuntimeDiscoveryConfig(args, false);
            return null;
        } else {
            throw new AgentRemoteException("Unknown command: " + cmd);
        }
    }

    private AutoinventoryCallbackClient setupClient() {
        StorageProviderFetcher fetcher =
                new StorageProviderFetcher(_storage);

        return new AutoinventoryCallbackClient(fetcher);
    }

    private void applyAutoApproval(AIPlatformValue aiPlatformValue) {
        // If the auto-approve configuration wasn't provided then exit.
        if (!_autoApproveConfig.exists()) {
            _log.info("no resource is auto approved");
            return;
        }

        // Platform auto-approval
        boolean approvePlatform = _autoApproveConfig.isAutoApproved(AutoApproveConfig.PLATFORM_PROPERTY_NAME);
        aiPlatformValue.setAutoApprove(approvePlatform);

        // Servers auto-approval
        AIServerValue[] aiServerValues = aiPlatformValue.getAIServerValues();
        if (aiServerValues != null) {
            for (AIServerValue aiServerValue : aiServerValues) {
                boolean approveServer = _autoApproveConfig.isAutoApproved(aiServerValue.getName());
                _log.info("--- Auto-Approve for Server: [" + aiServerValue.getName() + "] is: " + approveServer);
                aiServerValue.setAutoApprove(approveServer);
            }
        }
    }
}
