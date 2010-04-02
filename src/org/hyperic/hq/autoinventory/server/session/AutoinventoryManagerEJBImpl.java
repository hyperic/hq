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

package org.hyperic.hq.autoinventory.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AIQueueManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.AgentCreateCallback;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.CPropManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AIAppdefResourceValue;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManagerLocal;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.autoinventory.AIHistory;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.DuplicateAIScanNameException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanState;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.agent.client.AICommandsClient;
import org.hyperic.hq.autoinventory.agent.client.AICommandsClientFactory;
import org.hyperic.hq.autoinventory.server.session.RuntimeReportProcessor.ServiceMergeInfo;
import org.hyperic.hq.autoinventory.shared.AIScheduleManagerLocal;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerUtil;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.dao.AIHistoryDAO;
import org.hyperic.hq.measurement.server.session.AgentScheduleSyncZevent;
import org.hyperic.hq.measurement.server.session.MeasurementProcessorEJBImpl;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocal;
import org.hyperic.hq.product.AutoinventoryPluginManager;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.server.session.ProductManagerEJBImpl;
import org.hyperic.hq.product.shared.ProductManagerLocal;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * This class is responsible for managing Autoinventory objects in autoinventory
 * and their relationships
 * @ejb:bean name="AutoinventoryManager"
 *      jndi-name="ejb/autoinventory/AutoinventoryManager"
 *      local-jndi-name="LocalAutoinventoryManager"
 *      view-type="both"
 *      type="Stateless"
 * @ejb:util generate="physical"
 */
public class AutoinventoryManagerEJBImpl implements SessionBean {
    private Log _log = 
        LogFactory.getLog(AutoinventoryManagerEJBImpl.class.getName());
                          
    protected final String DATASOURCE_NAME = HQConstants.DATASOURCE;

    private AutoinventoryPluginManager aiPluginManager;
    private AIScheduleManagerLocal     aiScheduleManager;

    /**
     * Get server signatures for a set of servertypes.
     * @param serverTypes A List of ServerTypeValue objects representing the
     * server types to get signatures for.  If this is null, all server
     * signatures are returned.
     * @return A Map, where the keys are the names of the ServerTypeValues,
     * and the values are the ServerSignature objects.
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public Map getServerSignatures(AuthzSubject subject,
                                   List serverTypes)
        throws FinderException, AutoinventoryException
    {
        // Plug server type names into a map for quick retrieval
        HashMap stNames = null;
        if ( serverTypes != null ) {
            stNames = new HashMap();
            ServerTypeValue stValue;
            for ( int i=0; i<serverTypes.size(); i++ ) {
                stValue = (ServerTypeValue) serverTypes.get(i);
                stNames.put(stValue.getName(), stValue);
            }
        }
        
        Map plugins = aiPluginManager.getPlugins();
        Iterator iter = plugins.keySet().iterator();
        Map results = new HashMap();
        String pluginName;
        GenericPlugin plugin;
        while (iter.hasNext()) {
            plugin = (GenericPlugin) plugins.get(iter.next());
            pluginName = plugin.getName();
            if (!(plugin instanceof ServerDetector)) {
                _log.debug("skipping non-server AI plugin: " + pluginName);
                continue;
            }
            if (stNames != null &&
                stNames.get(pluginName) == null) {
                _log.debug("skipping unrequested AI plugin: " + pluginName);
                continue;
            }
            results.put(pluginName, 
                        ((ServerDetector) plugin).getServerSignature());
        }

        return results;
    }

    /**
     * Check if a given Appdef entity supports runtime auto-discovery.
     *
     * @param id The entity id to check.
     * @return true if the given resource supports runtime auto-discovery.
     * @ejb:interface-method
     */
    public boolean isRuntimeDiscoverySupported(AuthzSubject subject,
                                               AppdefEntityID id) {
        boolean retVal;
        AutoinventoryPluginManager aiPluginManager ;
        ProductManagerLocal productManager = ProductManagerEJBImpl.getOne();
        ServerManagerLocal serverManager = ServerManagerEJBImpl.getOne();

        try {
            Server server = serverManager.getServerById(id.getId());
            if (server == null) {
                return false;
            }
            
            String pluginName = server.getServerType().getName();
            aiPluginManager = (AutoinventoryPluginManager)productManager.
                getPluginManager(ProductPlugin.TYPE_AUTOINVENTORY);
            GenericPlugin plugin = aiPluginManager.getPlugin(pluginName);

            if (plugin instanceof ServerDetector) {
                retVal =
                    ((ServerDetector)plugin).isRuntimeDiscoverySupported();
            } else {
                retVal = false ;
            }
        } catch (PluginNotFoundException pne) {
            return false;
        } catch (PluginException e) {
            _log.error("Error getting plugin", e);
            return false;
        }

        return retVal;
    }
    
    /**
     * Turn off runtime-autodiscovery for a server that no longer
     * exists.  Use this method when you know the appdefentity identified
     * by "id" exists, so that we'll be able to successfully find out
     * which agent we should create our AICommandsClient from.
     * @param id The AppdefEntityID of the resource to turn
     * off runtime config for.
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void turnOffRuntimeDiscovery(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException
    {
        AICommandsClient client;

        try {
            client = AICommandsClientFactory.getInstance().getClient(id);
        } catch ( AgentNotFoundException e ) {
            throw new SystemException("Error looking up agent for resource " +
                                      "(" + id + "): " + e);
        }

        try {
            client.pushRuntimeDiscoveryConfig(id.getType(), id.getID(),
                                              null, null, null);            
        } catch (AgentRemoteException e) {
            throw new SystemException("Error turning off runtime-autodiscovery " +
            		                  "for resource ("+id+"): "+e);
        }

    }

    /**
     * Turn off runtime-autodiscovery for a server that no longer
     * exists.  We need this as a separate method call because when
     * the server no longer exists, we have to manually specify
     * the agent connection to use.
     * @param id The AppdefEntityID of the resource to turn
     * off runtime config for.
     * @param agentToken Which agent controls the runtime AI scans for 
     * this resource.
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void turnOffRuntimeDiscovery(AuthzSubject subject,
                                        AppdefEntityID id,
                                        String agentToken)
        throws PermissionException
    {
        AICommandsClient client;
        
        try {
            client = AICommandsClientFactory.getInstance().getClient(agentToken);
        } catch (AgentNotFoundException e) {
            throw new SystemException("Error looking up agent for resource " +
                                      "(" + id + "): " + e);
        }
        
        try {
            client.pushRuntimeDiscoveryConfig(id.getType(), id.getID(),
                                              null, null, null);            
        } catch (AgentRemoteException e) {
            throw new SystemException("Error turning off runtime-autodiscovery " +
                                      "for resource ("+id+"): "+e);
        }
    }

    /**
     * Toggle Runtime-AI config for the given server.
     * @ejb:interface-method
     */
    public void toggleRuntimeScan(AuthzSubject subject,
                                  AppdefEntityID id, boolean enable)
        throws PermissionException, AutoinventoryException,
               ResourceDeletedException {
        ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
        Resource res = rMan.findResource(id);
        // if resource is asynchronously deleted ignore
        if (res == null || res.isInAsyncDeleteState()) {
            final String m = id + " is asynchronously deleted";
            throw new ResourceDeletedException(m);
        }
        if (!id.isServer()) {
            _log.warn("toggleRuntimeScan() called for non-server type=" + id);
            return;
        }

        if (!isRuntimeDiscoverySupported(subject, id)) {
            return;
        }

        ConfigManagerLocal cman = ConfigManagerEJBImpl.getOne();
        ServerManagerLocal serverManager = ServerManagerEJBImpl.getOne();
        try {
            Server server = serverManager.findServerById(id.getId());
            server.setRuntimeAutodiscovery(enable);

            ConfigResponse metricConfig =
                cman.getMergedConfigResponse(subject,
                                             ProductPlugin.TYPE_MEASUREMENT,
                                             id, true);

            pushRuntimeDiscoveryConfig(subject, server, metricConfig);
        } catch (ConfigFetchException e) {
            // No config, no need to turn off auto-discovery.
        } catch (Exception e) {
            throw new AutoinventoryException("Error enabling Runtime-AI for " +
                                             "server: " + e.getMessage(), e);
        }
    }

    /**
     * Push the metric ConfigResponse out to an agent so it can perform 
     * runtime-autodiscovery
     * @param res The appdef entity ID of the server.
     * @param response The configuration info.
     */
    private void pushRuntimeDiscoveryConfig(AuthzSubject subject,
                                            AppdefResource res,
                                            ConfigResponse response)
        throws PermissionException
    {
        AppdefEntityID aeid = res.getEntityId();
        if (!isRuntimeDiscoverySupported(subject, aeid)) {
            return;
        }

        AICommandsClient client;

        if (aeid.isServer()) {
            // Setting the response to null will disable runtime
            // autodiscovery at the agent.
            if (!AppdefUtil.areRuntimeScansEnabled((Server) res)) {
                response = null;
            }
        }

        try {
            client = AICommandsClientFactory.getInstance().getClient(aeid);
        } catch ( AgentNotFoundException e ) {
            throw new SystemException("Error looking up agent for server " +
                                      "(" + res + "): " + e);
        }
        String typeName = res.getAppdefResourceType().getName();
        String name = null;
        if (!aeid.isServer()) {
            name = res.getName();
        }
        
        try {
            client.pushRuntimeDiscoveryConfig(aeid.getType(), aeid.getID(),
                                              typeName, name, response);            
        } catch (AgentRemoteException e) {
            throw new SystemException("Error pushing metric config response to " +
            		                  "agent for server ("+res+"): "+e);
        }

    }

    /**
     * Start an autoinventory scan.
     * @param aid The appdef entity whose agent we'll talk to.
     * @param scanConfig The scan configuration to use when scanning.
     * @param scanName The name of the scan - this is ignored (i.e. it can be 
     * null) for immediate, one-time scans.
     * @param scanDesc The description of the scan - this is ignored (i.e. it 
     * can be null) for immediate, one-time scans.
     * @param schedule Described when and how often the scan should run.  If 
     * this is null, then the scan will be run as an immediate, one-time only 
     * scan.
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void startScan(AuthzSubject subject,
                          AppdefEntityID aid,
                          ScanConfigurationCore scanConfig,
                          String scanName, String scanDesc,
                          ScheduleValue schedule )
        throws AgentConnectionException, AgentNotFoundException,
               AutoinventoryException, DuplicateAIScanNameException,
               ScheduleWillNeverFireException, PermissionException
    {
        try {
            final AIQueueManagerEJBImpl authzChecker =
                new AIQueueManagerEJBImpl();
            final ConfigManagerLocal cfgMan = ConfigManagerEJBImpl.getOne(); 

            authzChecker.checkAIScanPermission(subject, aid);

            ConfigResponse config = cfgMan.
                    getMergedConfigResponse(subject, 
                                            ProductPlugin.TYPE_MEASUREMENT, 
                                            aid, false);

            if (_log.isDebugEnabled()) {
                _log.debug("startScan config=" + config);
            }

            scanConfig.setConfigResponse(config);

            // All scans go through the scheduler.
            aiScheduleManager.doScheduledScan(subject,
                                              aid,
                                              scanConfig,
                                              scanName,
                                              scanDesc,
                                              schedule);
        } catch (ScheduleWillNeverFireException e) {
            throw e;
        } catch (DuplicateAIScanNameException ae) {
            throw ae;
        } catch (AutoinventoryException ae) {
            _log.warn("Error starting scan: " + StringUtil.getStackTrace(ae));
            throw ae;
        } catch (PermissionException ae) {
            throw ae;
        } catch (Exception e) {
            throw new SystemException("Error starting scan " +
                                      "for agent: " + e, e);
        }
    }

    /**
     * Start an autoinventory scan by agentToken
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void startScan(AuthzSubject subject,
                          String agentToken,
                          ScanConfigurationCore scanConfig)
        throws AgentConnectionException, AgentNotFoundException,
               AutoinventoryException, PermissionException {

        _log.info("AutoinventoryManager.startScan called");

        // Is there an already-approved platform with this agent token?  If so,
        // re-call using the other startScan method
        AIPlatform aipLocal =
            DAOFactory.getDAOFactory()
                .getAIPlatformDAO().findByAgentToken(agentToken);
        if (aipLocal == null) {
            throw new AutoinventoryException("No platform in auto-discovery " +
                                             "queue with agentToken=" +
                                             agentToken);
        }
        PlatformValue pValue;
        try {
            pValue = getAIQueueManagerLocal().getPlatformByAI
                (subject, aipLocal.getId().intValue());

            // It does exist.  Call the other startScan method so that 
            // authz checks will apply
            startScan(subject,
                      AppdefEntityID.newPlatformID(pValue.getId()),
                      scanConfig, null, null, null);
            return;

        } catch (PlatformNotFoundException e) {
            _log.warn("startScan: no platform exists for queued AIPlatform: "
                     + aipLocal.getId() + ": " + e);
        } catch (Exception e) {
            _log.error("startScan: error starting scan for AIPlatform: "
                     + aipLocal.getId() + ": " + e, e);
            throw new SystemException(e);
        }

        try {
            AICommandsClient client = 
                AICommandsClientFactory.getInstance().getClient(agentToken);
            
            client.startScan(scanConfig);
        } catch (AgentRemoteException e) {
            throw new AutoinventoryException(e);
        }
    }

    /**
     * Stop an autoinventory scan.
     * @param aid The appdef entity whose agent we'll talk to.
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void stopScan(AuthzSubject subject, AppdefEntityID aid)
        throws AutoinventoryException {

        _log.info("AutoinventoryManager.stopScan called");
        try { 
            AICommandsClient client = 
                AICommandsClientFactory.getInstance().getClient(aid);
            client.stopScan();
        } catch (Exception e) {
            throw new AutoinventoryException("Error stopping scan " +
                                             "for agent: " + e, e);
        }
    }

    /**
     * Get status for an autoinventory scan.
     * @param aid The appdef entity whose agent we'll talk to.
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ScanStateCore getScanStatus(AuthzSubject subject, AppdefEntityID aid)
        throws AgentNotFoundException, AgentConnectionException,
               AgentRemoteException, AutoinventoryException {
        
        _log.info("AutoinventoryManager.getScanStatus called");
        ScanStateCore core;
        try {
            AICommandsClient client = 
                AICommandsClientFactory.getInstance().getClient(aid);
            core = client.getScanStatus();
        } 
        catch (AgentNotFoundException ae) {
            throw ae;
        } catch (AgentRemoteException ae) {
            throw ae;
        } catch (AgentConnectionException ae) {
            throw ae;
        } catch (AutoinventoryException ae) {
            throw ae;
        } catch (Exception e) {
            throw new SystemException("Error getting scan status for agent: " +
                                      e, e);
        }
        return core;
    }

    /**
     * create AIHistory
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public AIHistory createAIHistory(AppdefEntityID id,
                                     Integer groupId,
                                     Integer batchId,
                                     String subjectName,
                                     ScanConfigurationCore config,
                                     String scanName,
                                     String scanDesc,
                                     Boolean scheduled,
                                     long startTime,
                                     long stopTime,
                                     long scheduleTime,
                                     String status,
                                     String errorMessage)
        throws AutoinventoryException {
        return getHistoryDAO().create(id, groupId, batchId, subjectName,
                                      config, scanName, scanDesc,
                                      scheduled, startTime,
                                      stopTime, scheduleTime,
                                      status, null /*description*/,
                                      errorMessage);
    }

    /**
     * remove AIHistory
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void removeHistory(AIHistory history) {
        getHistoryDAO().remove(history);
    }

    /**
     * update AIHistory
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void updateAIHistory(Integer jobId, long endTime,
                                String status, String message) {
        AIHistory local = getHistoryDAO().findById(jobId);

        local.setEndTime(endTime);
        local.setDuration(endTime - local.getStartTime());
        local.setStatus(status);
        local.setMessage(message);
    }

    protected AIHistoryDAO getHistoryDAO()
    {
        return new AIHistoryDAO(DAOFactory.getDAOFactory());
    }

    /**
     * Get status for an autoinventory scan, given the agentToken
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public ScanStateCore getScanStatusByAgentToken(AuthzSubject subject,
                                                   String agentToken)
        throws AgentNotFoundException,  AgentConnectionException,
               AgentRemoteException, AutoinventoryException
    {
        _log.info("AutoinventoryManager.getScanStatus called");
        ScanStateCore core;
        try {
            AICommandsClient client = 
                AICommandsClientFactory.getInstance().getClient(agentToken);
            core = client.getScanStatus();
        } catch (AgentNotFoundException ae) {
            throw ae;
        } catch (AgentRemoteException ae) {
            throw ae;
        } catch (AgentConnectionException ae) {
            throw ae;
        } catch (AutoinventoryException ae) {
            throw ae;
        } catch (Exception e) {
            throw new SystemException("Error getting scan status " +
                                      "for agent: " + e, e);
        }
        return core;
    }

    private static List buildAIResourceIds(AIAppdefResourceValue[] aiResources)
    {
        List ids = new ArrayList();
        for (int i=0; i<aiResources.length; i++) {
            Integer id = aiResources[i].getId();
            if (id == null) {
                continue; //unchanged?
            }
            ids.add(id);
        }
        return ids;
    }
    
    private String getIps(Collection aiipValues) {
        StringBuilder rtn = new StringBuilder();
        for (Iterator it=aiipValues.iterator(); it.hasNext(); ) {
            AIIpValue aiip = (AIIpValue)it.next();
            rtn.append(aiip.getAddress()).append(',');
        }
        return rtn.substring(0, rtn.length()-1);
    }

    /**
     * Called by agents to report platforms, servers, and services
     * detected via autoinventory scans.
     * @param agentToken The token identifying the agent that sent 
     * the report.
     * @param stateCore The ScanState that was detected during the autoinventory
     * scan.
     * 
     * @ejb:interface-method
     * @ejb:transaction type="RequiresNew"
     */
    public void reportAIData(String agentToken, ScanStateCore stateCore)
        throws AutoinventoryException {
        final boolean debug = _log.isDebugEnabled();
        ScanState state = new ScanState(stateCore);
        AIPlatformValue aiPlatform = state.getPlatform();
        // This could happen if there was a serious error in the scan,
        // and not even the platform could be detected.
        if ( state.getPlatform() == null ) {
            _log.warn("ScanState did not even contain a platform, ignoring.");
            return;
        }
        _log.info("Received auto-inventory report from " + aiPlatform.getFqdn() +
                 "; IPs -> " + getIps(aiPlatform.getAddedAIIpValues()) + 
                 "; CertDN -> " + aiPlatform.getCertdn() +
                 "; (" + state.getAllServers().size() +  " servers)");
        if (debug) {
            _log.debug("AutoinventoryManager.reportAIData called, "
                      + "scan state=" + state);
            _log.debug("AISERVERS=" + state.getAllServers());
        }
        // In the future we may want this method to act as
        // another user besides "admin".  It might make sense to have 
        // a user per-agent, so that actions that are agent-initiated 
        // can be tracked.  Of course, this will be difficult when the
        // agent is reporting itself to the server for the first time.
        // In that case, we'd have to act as admin and be careful about 
        // what we allow that codepath to do.
        AuthzSubject subject = getHQAdmin();
        aiPlatform.setAgentToken(agentToken);
        if (debug) {
            _log.debug("AImgr.reportAIData: state.getPlatform()=" + aiPlatform);
        }
        addAIServersToAIPlatform(stateCore, state, aiPlatform);
        AIQueueManagerLocal aiqLocal = getAIQueueManagerLocal();
        aiPlatform = aiqLocal.queue(subject, aiPlatform, 
                                    stateCore.getAreServersIncluded(), 
                                    false, true);
        approvePlatformDevice(subject, aiPlatform);
        checkAgentAssignment(subject, agentToken, aiPlatform);
    }

    private void addAIServersToAIPlatform(ScanStateCore stateCore,
                                          ScanState state,
                                          AIPlatformValue aiPlatform)
    throws AutoinventoryException {
        if (stateCore.getAreServersIncluded()) {
            Set serverSet = state.getAllServers();
            final ServerManagerLocal svrMan = ServerManagerEJBImpl.getOne();
            for (Iterator it = serverSet.iterator(); it.hasNext();) {
                AIServerValue aiServer = (AIServerValue)it.next();
                // Ensure the server reported has a valid appdef type
                try {
                    svrMan.findServerTypeByName(aiServer.getServerTypeName());
                } catch (FinderException e) {
                    _log.error("Ignoring non-existent server type: " +
                               aiServer.getServerTypeName(), e);
                    continue;
                }
                aiPlatform.addAIServerValue(aiServer);
            }
        }
    }

    private void approvePlatformDevice(AuthzSubject subject,
                                       AIPlatformValue aiPlatform) {
        if (aiPlatform.isPlatformDevice()) {
            _log.info("Auto-approving inventory for " + aiPlatform.getFqdn());
            List ips = buildAIResourceIds(aiPlatform.getAIIpValues());
            List servers = buildAIResourceIds(aiPlatform.getAIServerValues());
            List platforms = Collections.singletonList(aiPlatform.getId());
            try {
                AIQueueManagerLocal aiqLocal = getAIQueueManagerLocal();
                aiqLocal.processQueue(subject, platforms, servers,
                                      ips, AIQueueConstants.Q_DECISION_APPROVE);
            } catch (Exception e) {
                throw new SystemException(e);
            }
        }
    }

    private void checkAgentAssignment(AuthzSubject subj, String agentToken,
                                      AIPlatformValue aiPlatform) {
        try {
            PlatformManagerLocal pMan = PlatformManagerEJBImpl.getOne();
            Platform platform = pMan.getPlatformByAIPlatform(subj, aiPlatform);
            if (platform != null) {
                Agent agent = platform.getAgent();
                if (agent == null || !agent.getAgentToken().equals(agentToken)) {
                    Agent newAgent = AgentManagerEJBImpl.getOne().getAgent(agentToken);
                    String fqdn = platform.getFqdn();
                    Integer pid = platform.getId();
                    _log.info("reassigning platform agent (fqdn=" + fqdn +
                              ",id=" + pid + ") from=" + agent +
                              " to=" + newAgent);
                    platform.setAgent(newAgent);
                    MeasurementProcessorLocal mProc =
                        MeasurementProcessorEJBImpl.getOne();
                    mProc.scheduleHierarchyAfterCommit(platform.getResource());
                }
            }
        } catch (PermissionException e) {
            // using admin, this should not happen
            _log.error(e,e);
        } catch (AgentNotFoundException e) {
            // this is a problem since the agent should already exist in our
            // inventory before it gets here.
            _log.error(e,e);
        }
    }

    /**
     * Called by agents to report resources detected at runtime via 
     * monitoring-based autoinventory scans.
     * 
     * There are some interesting situations that can occur related
     * to synchronization between the server and agent.  If runtime scans
     * are turned off for a server, but the agent is never notified (for
     * example if the agent is not running at the time), then the agent
     * is going to eventually report a runtime scan that includes resources
     * detected by that server's runtime scan.  If this happens, we detect
     * it and take the opportunity to tell the agent again that it should not
     * perform runtime AI scans for that server.
     * Any resources reported by that server will be ignored.
     * 
     * A similar situation occurs when the appdef server has been deleted but 
     * the agent was never notified to turn off runtime AI.  We handle this in 
     * the same way, by telling the agent to turn off runtime scans for that 
     * server, and ignoring anything in the report from that server.
     * 
     * This method will process all platform and server merging, given by
     * the report.  Any services will be added to Zevent queue to be 
     * processed in their own transactions.
     * 
     * @param agentToken The token identifying the agent that sent 
     * the report.
     * @param crrr The CompositeRuntimeResourceReport that was generated
     * during the runtime autoinventory scan.
     * 
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void reportAIRuntimeReport(String agentToken,
                                      CompositeRuntimeResourceReport crrr)
        throws AutoinventoryException, PermissionException, ValidationException,
               ApplicationException {
        RuntimePlatformAndServerMerger.schedulePlatformAndServerMerges(
            agentToken, crrr);
    }

    /**
     * Should only be called from RuntimePlatformAndServerMerger
     * @ejb:interface-method
     * @ejb:transaction type="RequiresNew"
     */
    public void _reportAIRuntimeReport(String agentToken,
                                      CompositeRuntimeResourceReport crrr)
        throws AutoinventoryException, PermissionException, ValidationException,
               ApplicationException 
    {
        List serviceMerges = mergePlatformsAndServers(agentToken, crrr);
        
        Agent a = AgentManagerEJBImpl.getOne().getAgent(agentToken);
        AgentReportStatusDAO statDAO = 
            new AgentReportStatusDAO(DAOFactory.getDAOFactory());
        AgentReportStatus status = statDAO.getOrCreate(a);
        
        if (serviceMerges.isEmpty()) {
            _log.debug("Agent [" + agentToken + "] reported no services.  " +
                       "Marking clean");
            status.markClean();
        } else {
            _log.debug("Agent [" + agentToken + "] reported " + 
                       serviceMerges.size() + " services.  Marking dirty");
            status.markDirty();
        }
        
        ServiceMerger.scheduleServiceMerges(agentToken, serviceMerges);
    }
    
    /**
     * Merge platforms and servers from the runtime report.  
     * 
     * @return a List of {@link ServiceMergeInfo} -- information from the
     *         report about services still needing to be processed
     * 
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public List mergePlatformsAndServers(String agentToken, 
                                         CompositeRuntimeResourceReport crrr)
        throws ApplicationException, AutoinventoryException
    {
        AuthzSubject subject = getHQAdmin();

        RuntimeReportProcessor rrp = new RuntimeReportProcessor();
        try {
            rrp.processRuntimeReport(subject, agentToken, crrr);
            mergeServiceTypes(rrp.getServiceTypeMerges());
            return rrp.getServiceMerges();
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }
    
    private void mergeServiceTypes(final Set serviceTypeMerges) {
    	if(! serviceTypeMerges.isEmpty()) {
    		final ProductManagerLocal productManager = ProductManagerEJBImpl.getOne();
    		Map productTypes = new HashMap();
    		for(Iterator iterator = serviceTypeMerges.iterator();iterator.hasNext();) {
    			final org.hyperic.hq.product.ServiceType serviceType = (org.hyperic.hq.product.ServiceType) iterator
    	 			.next();
    			Set serviceTypes = (Set)productTypes.get(serviceType.getProductName());
    			if(serviceTypes == null) {
    				serviceTypes = new HashSet();
    			}
    			serviceTypes.add(serviceType);
    			_log.info("Adding serviceType " + serviceType + " to product type: " + serviceType.getProductName());
    			productTypes.put(serviceType.getProductName(), serviceTypes);
    		}
    		_log.info("The size of productTypes: " + productTypes.size());
    		for(Iterator iterator = productTypes.entrySet().iterator();iterator.hasNext();)  {
    			try {
    				Map.Entry serviceTypeEntry = (Map.Entry)iterator.next();
    				_log.info("Updating dynamic service type plugin");
    				productManager.updateDynamicServiceTypePlugin((String)serviceTypeEntry.getKey(), (Set)serviceTypeEntry.getValue());
    			} catch (Exception e) {
    				_log.error("Error merging dynamic service types for product.  Cause: " + e.getMessage());
    			} 
    		}
    	}
    }
    
    
    /**
     * Merge a list of {@link ServiceMergeInfo}s in HQ's appdef model
     * 
     * @ejb:interface-method
     * @ejb:transaction type="RequiresNew"
     */
    public void mergeServices(List mergeInfos)
        throws PermissionException, ApplicationException {
        final ServerManagerLocal svrMan = ServerManagerEJBImpl.getOne();
        final CPropManagerLocal cpropMan = CPropManagerEJBImpl.getOne();
        final Set updatedResources = new HashSet();
        final Set toSchedule = new HashSet();
        AuthzSubject subj = AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
    
        for (final Iterator i = mergeInfos.iterator(); i.hasNext();) {
            ServiceMergeInfo sInfo = (ServiceMergeInfo) i.next();
            // this is hacky, but mergeInfos will never be called with multiple subjects
            // and hence the method probably shouldn't be written the way it is anyway.
            subj = sInfo.subject;
            AIServiceValue aiservice = sInfo.aiservice;
            Server server = svrMan.getServerById(sInfo.serverId);
            
            _log.info("Checking for existing service: " + aiservice.getName());
            
            final ServiceManagerLocal svcMan = ServiceManagerEJBImpl.getOne();
            // this is a propagation of a bug that nobody really runs into.
            // Occurs when a set of services under a server have the same name
            // and therefore the AIID is also the same.  In a perfect world the
            // AIIDs will be unique, but there is nothing else that comes from
            // the agent that can uniquely identify a service under a server.
            // The get(0), instead of operating on the whole list, enables
            // us to make the least amount of code changes in a messy code path
            // thus reducing the amount of potential problems.
            final List tmp =
                svcMan.getServicesByAIID(server, aiservice.getName());
            Service service = (tmp.size() > 0) ? (Service)tmp.get(0) : null;
            boolean update = false;
            
            if (service == null) {
                // CREATE SERVICE
                _log.info("Creating new service: " + aiservice.getName());
            
                String typeName = aiservice.getServiceTypeName();
                ServiceType serviceType = 
                    svcMan.findServiceTypeByName(typeName);
                service = svcMan.createService(sInfo.subject, server,
                                               serviceType, aiservice.getName(),
                                               aiservice.getDescription(), "",
                                               null);
            
                _log.debug("New service created: " + service);
            } else {
                update = true;
                // UPDATE SERVICE
                _log.info("Updating service: " + service.getName());
                final String aiSvcName = aiservice.getName();
                final String svcName = service.getName();
                final String aiid = service.getAutoinventoryIdentifier();
                // if aiid.equals(svcName) this means that the name has
                // not been manually changed.  Therefore it is ok to change
                // the current resource name
                if (aiSvcName != null && !aiSvcName.equals(svcName) && aiid.equals(svcName)) {
                    service.setName(aiservice.getName().trim());
                    service.getResource().setName(service.getName());
                }
                if (aiservice.getDescription() != null)
                    service.setDescription(aiservice.getDescription().trim());
            }
                    
            // CONFIGURE SERVICE
            final ConfigManagerLocal cfgMan = ConfigManagerEJBImpl.getOne();
            final boolean wasUpdated = cfgMan.configureResponse(sInfo.subject,
                                         service.getConfigResponse(),
                                         service.getEntityId(),
                                         aiservice.getProductConfig(),
                                         aiservice.getMeasurementConfig(),
                                         aiservice.getControlConfig(),
                                         aiservice.getResponseTimeConfig(),
                                         null, false);
            if (update && wasUpdated) {
                updatedResources.add(service.getResource());
            } else {
                // make sure the service's schedule is up to date on the agent side
                toSchedule.add(new AppdefEntityID(service.getResource()));
            }
                    
            // SET CUSTOM PROPERTIES FOR SERVICE
            if (aiservice.getCustomProperties() != null) {
                int typeId = service.getServiceType().getId().intValue();
                cpropMan.setConfigResponse(service.getEntityId(),
                                            typeId,
                                            aiservice.getCustomProperties());            
            }
        }
        if (!toSchedule.isEmpty()) {
            ResourceManagerEJBImpl.getOne().resourceHierarchyUpdated(subj, updatedResources);
        	ZeventManager.getInstance().enqueueEventAfterCommit(
        	    new AgentScheduleSyncZevent(toSchedule));
        }
    }
    
    /**
     * Returns a list of {@link Agent}s which still need to send in a 
     * runtime scan (their last runtime scan was unsuccessfully processed)
     *  
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public List findAgentsRequiringRuntimeScan() {
        AgentReportStatusDAO statDAO = 
            new AgentReportStatusDAO(DAOFactory.getDAOFactory());
        
        Collection dirties = statDAO.findDirtyStatus();
        List res = new ArrayList(dirties.size());
        
        _log.info("Found " + dirties.size() + " agents with " +
                  "serviceDirty = true");
        
        for (Iterator i=dirties.iterator(); i.hasNext(); ) {
            AgentReportStatus s = (AgentReportStatus)i.next();
            
            if (!ServiceMerger.currentlyWorkingOn(s.getAgent())) {
                _log.debug("Agent [" + s.getAgent().getAgentToken() +
                          "] is serviceDirty");
                res.add(s.getAgent());
            } else {
                _log.debug("Agent [" + s.getAgent().getAgentToken() + 
                          "] is serviceDirty, but in process");
            }
        }
        return res;
    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void notifyAgentsNeedingRuntimeScan() {
        List agents = findAgentsRequiringRuntimeScan();
        
        for (Iterator i=agents.iterator(); i.hasNext(); ) {
            Agent a = (Agent)i.next();
            AICommandsClient client;
            
            try {
                client = 
                    AICommandsClientFactory.getInstance().getClient(a.getAgentToken());
            } catch(AgentNotFoundException e) {
                _log.warn("Unable to find agent [" + a.getAgentToken() + "]");
                continue;
            }
            
            int type = AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
            ConfigResponse cfg = new ConfigResponse();
            
            try {
                client.pushRuntimeDiscoveryConfig(type, 0, null, null, cfg);                
            } catch (AgentRemoteException e) {
                _log.warn("Unable to notify agent needing runtime scan ["+
                          a.getAgentToken()+"]");
                continue;
            }
        }
    }
    
    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void markServiceClean(String agentToken) {
        Agent a;
        
        try {
            a = AgentManagerEJBImpl.getOne().getAgent(agentToken);
        } catch(AgentNotFoundException e) {
            _log.error("Agent [" + agentToken + "] not found");
            return;
        }
        
        markServiceClean(a, true);
    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void markServiceClean(Agent agent, boolean serviceClean) {
        AgentReportStatusDAO statDAO = 
            new AgentReportStatusDAO(DAOFactory.getDAOFactory());
        
        AgentReportStatus status = statDAO.getOrCreate(agent);
        if (serviceClean)
            status.markClean();
        else
            status.markDirty();
    }
    
    /**
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void startup() {
        AgentCreateCallback listener = new AgentCreateCallback() {
            public void agentCreated(Agent agent) {
                markServiceClean(agent, false);
            }
        };
        HQApp.getInstance().registerCallbackListener(AgentCreateCallback.class,
                                                     listener);
    }

    /**
     * Handle ResourceZEvents for enabling runtime autodiscovery.
     *
     * @param events A list of ResourceZevents
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public void handleResourceEvents(List events)
    {
        ServerManagerLocal serverMgr = ServerManagerEJBImpl.getOne();
        AuthzSubjectManagerLocal azMan = AuthzSubjectManagerEJBImpl.getOne();

        for (Iterator i = events.iterator(); i.hasNext(); ) {
            ResourceZevent zevent = (ResourceZevent) i.next();
            AppdefEntityID id = zevent.getAppdefEntityID();
            boolean isUpdate = zevent instanceof ResourceUpdatedZevent;

            // Only servers have runtime AI.
            if (!id.isServer()) {
                continue;
            }

            // Need to look up the AuthzSubject POJO
            AuthzSubject subj = 
                azMan.findSubjectById(zevent.getAuthzSubjectId());
            if (isUpdate) {
                Server s = serverMgr.getServerById(id.getId());
                _log.info("Toggling Runtime-AI for " + id);
                try {
                    toggleRuntimeScan(subj, id, s.isRuntimeAutodiscovery());
                } catch (ResourceDeletedException e) {
                    _log.debug(e);
                } catch (Exception e) {
                    _log.warn("Error toggling runtime-ai for server [" +
                              id + "]", e);
                }
            } else {
                _log.info("Enabling Runtime-AI for " + id);
                try {
                    toggleRuntimeScan(subj, id, true);
                } catch (ResourceDeletedException e) {
                    _log.debug(e);
                } catch (Exception e) {
                    _log.warn("Error enabling runtime-ai for server [" +
                              id + "]", e);
                }
            }
        }
    }

    public void setSessionContext(SessionContext ctx) {} 

    public static AutoinventoryManagerLocal getOne() {
        try {
            return AutoinventoryManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * Create an autoinventory manager session bean.
     * @exception CreateException If an error occurs creating the pager
     * for the bean.
     */
    public void ejbCreate() throws CreateException {
        // Get reference to the AI plugin manager
        try {
            ProductManagerLocal productManager = ProductManagerEJBImpl.getOne();
            aiPluginManager = 
                (AutoinventoryPluginManager) productManager.
                getPluginManager(ProductPlugin.TYPE_AUTOINVENTORY);

        } catch (Exception e) {
            _log.error("Unable to initialize session beans.", e);
        }
        // Get a reference to the control scheduler ejb
        try {
            aiScheduleManager = AIScheduleManagerEJBImpl.getOne();
        } catch (Exception e) {
            _log.error("Unable to get autoinventory schedule manager: " +
                           e.getMessage());
        }
    }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    
    private AuthzSubject getHQAdmin() throws AutoinventoryException {
        try {
             return AuthzSubjectManagerEJBImpl.getOne()
                 .getSubjectById(AuthzConstants.rootSubjectId);
        } catch ( Exception e ) {
            throw new AutoinventoryException("Error looking up subject", e);
        }
    }

    /**
     * If we ever have more than this single session EJB, this method
     * ought to be placed in a superclass, kinda like appdef has the
     * AppdefSessionEJB as a base class for all other appdef session EJBs.
     */
    protected AIQueueManagerLocal getAIQueueManagerLocal() {
        return AIQueueManagerEJBImpl.getOne();
    }
}
