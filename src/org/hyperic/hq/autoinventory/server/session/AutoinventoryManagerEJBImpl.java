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

package org.hyperic.hq.autoinventory.server.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.shared.AIAppdefResourceValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManagerLocal;
import org.hyperic.hq.appdef.shared.AIQueueManagerUtil;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.CPropManagerLocalHome;
import org.hyperic.hq.appdef.shared.CPropManagerUtil;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerLocalHome;
import org.hyperic.hq.appdef.shared.ConfigManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocalHome;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocalHome;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceManagerLocalHome;
import org.hyperic.hq.appdef.shared.ServiceManagerUtil;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.server.session.AIQueueManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocalHome;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.DuplicateAIScanNameException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanState;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIHistory;
import org.hyperic.hq.autoinventory.agent.client.AICommandsClient;
import org.hyperic.hq.autoinventory.shared.AIScheduleManagerLocal;
import org.hyperic.hq.autoinventory.shared.AIScheduleManagerUtil;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerUtil;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManagerLocal;
import org.hyperic.hq.common.shared.ServerConfigManagerLocalHome;
import org.hyperic.hq.common.shared.ServerConfigManagerUtil;
import org.hyperic.hq.product.AutoinventoryPluginManager;
import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.server.session.ProductManagerEJBImpl;
import org.hyperic.hq.product.shared.ProductManagerLocal;
import org.hyperic.hq.product.shared.ProductManagerUtil;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.hq.dao.AIHistoryDAO;
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
 * @TODO AUTHZ Integration
 */
public class AutoinventoryManagerEJBImpl implements SessionBean {

    protected Log log = LogFactory.getLog(AutoinventoryManagerEJBImpl.
                                          class.getName());
    protected static final String DATASOURCE_NAME = HQConstants.DATASOURCE;

    private AutoinventoryPluginManager aiPluginManager;
    private AIScheduleManagerLocal     aiScheduleManager;
    private SessionContext             sessionCtx;

    /**
     * Get server signatures for a set of servertypes.
     * @param serverTypes A List of ServerTypeValue objects representing the
     * server types to get signatures for.  If this is null, all server
     * signatures are returned.
     * @return A Map, where the keys are the names of the ServerTypeValues,
     * and the values are the ServerSignature objects.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public Map getServerSignatures(AuthzSubjectValue subject,
                                   List serverTypes)
        throws NamingException, FinderException, 
               CreateException, AutoinventoryException
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
                log.debug("skipping non-server AI plugin: " + pluginName);
                continue;
            }
            if (stNames != null &&
                stNames.get(pluginName) == null) {
                log.debug("skipping unrequested AI plugin: " + pluginName);
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
    public boolean isRuntimeDiscoverySupported(AuthzSubjectValue subject,
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
            log.error("Error getting plugin", e);
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
     * @ejb:transaction type="REQUIRED"
     */
    public void turnOffRuntimeDiscovery(AuthzSubjectValue subject,
                                        AppdefEntityID id)
        throws PermissionException
    {
        AICommandsClient client;

        try {
            client = AIUtil.getClient(id);
        } catch ( AgentNotFoundException e ) {
            throw new SystemException("Error looking up agent for resource " +
                                      "(" + id + "): " + e);
        }

        client.pushRuntimeDiscoveryConfig(id.getType(), id.getID(),
                                          null, null, null);
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
     * @ejb:transaction type="REQUIRED"
     */
    public void turnOffRuntimeDiscovery(AuthzSubjectValue subject,
                                        AppdefEntityID id,
                                        String agentToken)
        throws PermissionException
    {
        AICommandsClient client;
        
        try {
            client = AIUtil.getClient(agentToken);
        } catch (AgentNotFoundException e) {
            throw new SystemException("Error looking up agent for resource " +
                                      "(" + id + "): " + e);
        }

        client.pushRuntimeDiscoveryConfig(id.getType(), id.getID(),
                                          null, null, null);
    }

    /**
     * Toggle Runtime-AI config for the given server.
     * @ejb:interface-method
     */
    public void toggleRuntimeScan(AuthzSubjectValue subject,
                                  AppdefEntityID id, boolean enable)
        throws PermissionException, AutoinventoryException
    {
        if (!id.isServer()) {
            log.warn("toggleRuntimeScan() called for non-server type=" + id);
            return;
        }

        if (!isRuntimeDiscoverySupported(subject, id)) {
            return;
        }

        ConfigManagerLocal cman = ConfigManagerEJBImpl.getOne();
        ServerManagerLocal serverManager = ServerManagerEJBImpl.getOne();
        try {
            Server server = serverManager.findServerById(id.getId());
            server.setRuntimeAutodiscovery(true);

            ConfigResponse metricConfig =
                cman.getMergedConfigResponse(subject,
                                             ProductPlugin.TYPE_MEASUREMENT,
                                             id, true);

            pushRuntimeDiscoveryConfig(subject, id, metricConfig);
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
     * @param id The appdef entity ID of the server.
     * @param response The configuration info.
     */
    private void pushRuntimeDiscoveryConfig(AuthzSubjectValue subject,
                                            AppdefEntityID id,
                                            ConfigResponse response)
        throws PermissionException
    {
        if (!isRuntimeDiscoverySupported(subject, id)) {
            return;
        }

        AICommandsClient client;
        AppdefEntityValue aval = new AppdefEntityValue(id, subject);

        try {
            if (id.isServer()) {
                ServerValue server = (ServerValue) aval.getResourceValue();
                // Setting the response to null will disable runtime
                // autodiscovery at the agent.
                if (!AppdefUtil.areRuntimeScansEnabled(server)) {
                    response = null;
                }
            }
            else if (id.isService()) {
                aval.getResourceValue();
            }
        } catch (AppdefEntityNotFoundException e) {
            throw new SystemException("Error looking up resource " +
                                      "(" + id + "): " + e);
        }

        try {
            client = AIUtil.getClient(id);
        } catch ( AgentNotFoundException e ) {
            throw new SystemException("Error looking up agent for server " +
                                      "(" + id + "): " + e);
        }
        String typeName = null, name = null;
        try {
            typeName = aval.getTypeName();
            if (!id.isServer()) {
                name = aval.getName();
            }
        } catch (AppdefEntityNotFoundException e) {
            throw new SystemException("Error looking up type name for " +
                                      "resource (" + id + "): " + e);
        }
        
        client.pushRuntimeDiscoveryConfig(id.getType(), 
                                          id.getID(),
                                          typeName,
                                          name,
                                          response);
    }

    // XXX hack, see usage in startScan method below.
    private static final
        AIQueueManagerEJBImpl authzChecker = new AIQueueManagerEJBImpl();

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
     * @ejb:transaction type="REQUIRED"
     */
    public void startScan(AuthzSubjectValue subject,
                          AppdefEntityID aid,
                          ScanConfigurationCore scanConfig,
                          String scanName, String scanDesc,
                          ScheduleValue schedule )
        throws AgentConnectionException, AgentNotFoundException,
               AutoinventoryException, DuplicateAIScanNameException,
               ScheduleWillNeverFireException, PermissionException
    {
        try {
            // XXX XXX XXX
            // Dude, this is a totally silly, ugly hack.  In my defense,
            // implementing these security checks as methods in AppdefSessionEJB
            // was a bad idea, they are not what inheritance was meant for.
            // They should be static utilities in some other class.  But I'm not
            // about to make waves that big, this close to release.
            // For now, this works.
            authzChecker.checkAIScanPermission(subject, aid);

            ConfigResponse config =
                getConfigManagerLocalHome().create().
                    getMergedConfigResponse(subject, 
                                            ProductPlugin.TYPE_MEASUREMENT, 
                                            aid, false);

            if (log.isDebugEnabled()) {
                log.debug("startScan config=" + config);
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
            log.warn("Error starting scan: " + StringUtil.getStackTrace(ae));
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
     * @ejb:transaction type="REQUIRED"
     */
    public void startScan(AuthzSubjectValue subject,
                          String agentToken,
                          ScanConfigurationCore scanConfig)
        throws AgentConnectionException, AgentNotFoundException,
               AutoinventoryException, PermissionException {

        log.info("AutoinventoryManager.startScan called");

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
            startScan(
                subject, new AppdefEntityID(
                AppdefEntityConstants.APPDEF_TYPE_PLATFORM, pValue.getId()),
                scanConfig, null, null, null);
            return;

        } catch (PlatformNotFoundException e) {
            log.warn("startScan: no platform exists for queued AIPlatform: "
                     + aipLocal.getId() + ": " + e);
        } catch (Exception e) {
            log.error("startScan: error starting scan for AIPlatform: "
                     + aipLocal.getId() + ": " + e, e);
            throw new SystemException(e);
        }

        try {
            AICommandsClient client = AIUtil.getClient(agentToken);
            client.startScan(scanConfig);
        } catch (AgentRemoteException e) {
            throw new AutoinventoryException(e);
        }
    }

    /**
     * Stop an autoinventory scan.
     * @param aid The appdef entity whose agent we'll talk to.
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void stopScan(AuthzSubjectValue subject,
                         AppdefEntityID aid)
        throws AutoinventoryException {

        log.info("AutoinventoryManager.stopScan called");
        try { 
            AICommandsClient client = AIUtil.getClient(aid);
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
     * @ejb:transaction type="REQUIRED"
     */
    public ScanStateCore getScanStatus(AuthzSubjectValue subject,
                                       AppdefEntityID aid)
        throws AgentNotFoundException, 
               AgentConnectionException, AgentRemoteException,
               AutoinventoryException {
        
        log.info("AutoinventoryManager.getScanStatus called");
        ScanStateCore core;
        try {
            AICommandsClient client = AIUtil.getClient(aid);
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
            throw new SystemException("Error getting scan status " +
                                      "for agent: " + e, e);
        }
        return core;
    }

    /**
     * create AIHistory
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
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
     * @ejb:transaction type="REQUIRED"
     */
    public void removeHistory(AIHistory history) {
        getHistoryDAO().remove(history);
    }

    /**
     * update AIHistory
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void updateAIHistory(Integer jobId, long endTime,
                                String status, String message)
        throws FinderException, CreateException, NamingException
    {
        AIHistory local = getHistoryDAO().findById(jobId);

        local.setEndTime(endTime);
        local.setDuration(endTime - local.getStartTime());
        local.setStatus(status);
        local.setMessage(message);
    }

    protected AIHistoryDAO getHistoryDAO()
    {
        return DAOFactory.getDAOFactory().getAIHistoryDAO();
    }

    /**
     * Get status for an autoinventory scan, given the agentToken
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public ScanStateCore getScanStatusByAgentToken(AuthzSubjectValue subject,
                                                   String agentToken)
        throws AgentNotFoundException,  AgentConnectionException,
               AgentRemoteException, AutoinventoryException
    {
        log.info("AutoinventoryManager.getScanStatus called");
        ScanStateCore core;
        try {
            AICommandsClient client = AIUtil.getClient(agentToken);
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

    /**
     * Called by agents to report platforms, servers, and services
     * detected via autoinventory scans.
     * @param agentToken The token identifying the agent that sent 
     * the report.
     * @param stateCore The ScanState that was detected during the autoinventory
     * scan.
     * 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public void reportAIData(String agentToken, ScanStateCore stateCore)
        throws AutoinventoryException {

        ScanState state = new ScanState(stateCore);

        log.info("Received auto-inventory report from " +
                 state.getPlatform().getFqdn() +
                 " (" + state.getAllServers(log).size() + 
                 " servers)");

        if (log.isDebugEnabled()) {
            log.debug("AutoinventoryManager.reportAIData called, "
                      + "scan state=" + state);
            log.debug("AISERVERS=" + state.getAllServers(log));
        }

        // This could happen if there was a serious error in the scan,
        // and not even the platform could be detected.
        if ( state.getPlatform() == null ) {
            log.warn("ScanState did not even contain a platform, ignoring.");
            return;
        }

        // In the future we may want this method to act as
        // another user besides "admin".  It might make sense to have 
        // a user per-agent, so that actions that are agent-initiated 
        // can be tracked.  Of course, this will be difficult when the
        // agent is reporting itself to the server for the first time.
        // In that case, we'd have to act as admin and be careful about 
        // what we allow that codepath to do.
        AuthzSubjectValue subject = getOverlord();

        AIPlatformValue aiPlatform = state.getPlatform();
        aiPlatform.setAgentToken(agentToken);

        if (log.isDebugEnabled()) {
            log.debug("AImgr.reportAIData: state.getPlatform()=" + aiPlatform);
        }
        
        if (stateCore.getAreServersIncluded()) {
            ServerManagerLocal serverLocal;
            try {
                serverLocal = getServerManagerLocalHome().create();
            } catch (NamingException e) {
                throw new SystemException(e);
            } catch (CreateException e) {
                throw new SystemException(e);
            }

            Set serverSet = state.getAllServers(log);

            Iterator aiservers = serverSet.iterator();
            while (aiservers.hasNext()) {
                AIServerValue aiServer = (AIServerValue)aiservers.next();

                // Ensure the server reported has a valid appdef type
                try {
                    serverLocal.
                        findServerTypeByName(aiServer.getServerTypeName());
                } catch (FinderException e) {
                    this.log.error("Ignoring non-existent server type: " +
                                   aiServer.getServerTypeName(), e);
                    continue;
                }

                aiPlatform.addAIServerValue(aiServer);
            }
        }

        AIQueueManagerLocal aiqLocal;
        try {
            aiqLocal = getAIQueueManagerLocal();
        } catch (Exception e) {
            throw new SystemException(e);
        }

        try {
            aiPlatform = aiqLocal.queue(subject, aiPlatform, 
                                        stateCore.getAreServersIncluded(), 
                                        false, true);
        } catch ( SystemException cse ) {
            throw cse;
        } catch ( Exception e ) {
            throw new SystemException(e);
        }

        if (aiPlatform.isPlatformDevice()) {
            log.info("Auto-approving inventory for " + aiPlatform.getFqdn());
            List platforms = new ArrayList();
            platforms.add(aiPlatform.getId());
            List ips =
                buildAIResourceIds(aiPlatform.getAIIpValues());
            List servers =
                buildAIResourceIds(aiPlatform.getAIServerValues());
            
            try {
                aiqLocal.processQueue(subject,
                                      platforms, servers, ips, 
                                      AIQueueConstants.Q_DECISION_APPROVE);
            } catch (SystemException cse) {
                throw cse;
            } catch (Exception e) {
                throw new SystemException(e);
            }
        }
    }

    /**
     * Called by agents to report resources detected at runtime via 
     * monitoring-based autoinventory scans.
     * There are some interesting situations that can occur related
     * to synchronization between the server and agent.  If runtime scans
     * are turned off for a server, but the agent is never notified (for
     * example if the agent is not running at the time), then the agent
     * is going to eventually report a runtime scan that includes resources
     * detected by that server's runtime scan.  If this happens, we detect
     * it and take the opportunity to tell the agent again that it should not
     * perform runtime AI scans for that server.
     * Any resources reported by that server will be ignored.
     * A similar situation occurs when the appdef server
     * has been deleted but the agent was never notified to turn off
     * runtime AI.  We handle this in the same way, by telling the agent
     * to turn off runtime scans for that server, and ignoring anything in
     * the report from that server.
     * @param agentToken The token identifying the agent that sent 
     * the report.
     * @param crrr The CompositeRuntimeResourceReport that was generated
     * during the runtime autoinventory scan.
     * 
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void reportAIRuntimeReport(String agentToken,
                                      CompositeRuntimeResourceReport crrr)
        throws AutoinventoryException, PermissionException, ValidationException,
               ApplicationException {

        PlatformManagerLocal platformMgr;
        ServerManagerLocal serverMgr;
        ServiceManagerLocal serviceMgr;
        AutoinventoryManagerLocal aiMgr;
        ConfigManagerLocal configMgr;
        ServerConfigManagerLocal serverConfigMgr;
        CPropManagerLocal cpropMgr;

        AuthzSubjectManagerLocal subjectMgr;
        try {
            platformMgr
                = getPlatformManagerLocalHome().create();
            serverMgr
                = getServerManagerLocalHome().create();
            serviceMgr
                = getServiceManagerLocalHome().create();
            aiMgr
                = (AutoinventoryManagerLocal) sessionCtx.getEJBLocalObject();
            configMgr
                = getConfigManagerLocalHome().create();
            cpropMgr
                = getCPropManagerLocalHome().create();
            serverConfigMgr
                = getServerConfigManagerLocalHome().create();
            subjectMgr
                = getAuthzSubjectManagerLocalHome().create();

        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }


        // In the future we may want this method to act as
        // another user besides "admin".  It might make sense to have 
        // a user per-agent, so that actions that are agent-initiated 
        // can be tracked.  Of course, this will be difficult when the
        // agent is reporting itself to the server for the first time.
        // In that case, we'd have to act as admin and be careful about 
        // what we allow that codepath to do.
        AuthzSubjectValue subject = getOverlord();

        RuntimeReportProcessor rrp = new RuntimeReportProcessor();
        try {
            rrp.processRuntimeReport(subject, agentToken, crrr, aiMgr, platformMgr,
                                     serverMgr, serviceMgr, 
                                     configMgr, cpropMgr, subjectMgr);
            serverConfigMgr.vacuumAppdef();
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    public void setSessionContext(javax.ejb.SessionContext ctx) { 
        sessionCtx = ctx;
    }

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
            ProductManagerLocal productManager = 
                ProductManagerUtil.getLocalHome().create();
            this.aiPluginManager = 
                (AutoinventoryPluginManager)productManager.
                getPluginManager(ProductPlugin.TYPE_AUTOINVENTORY);

        } catch (Exception e) {
            this.log.error("Unable to initialize session beans: " +
                           e.getMessage());
        }
        // Get a reference to the control scheduler ejb
        try {
            this.aiScheduleManager =
                AIScheduleManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            this.log.error("Unable to get autoinventory schedule manager: " +
                           e.getMessage());
        }
    }
    public void ejbRemove() { }
    public void ejbActivate() { }
    public void ejbPassivate() { }
    
    private AuthzSubjectValue getOverlord () throws AutoinventoryException {
        AuthzSubjectValue subject;
        try {
            subject = getAuthzSubjectManagerLocalHome().create().findOverlord();
        } catch ( Exception e ) {
            throw new AutoinventoryException("Error looking up subject", e);
        }
        return subject;
    }

    /**
     * If we ever have more than this single session EJB, this method
     * ought to be placed in a superclass, kinda like appdef has the
     * AppdefSessionEJB as a base class for all other appdef session EJBs.
     */
    protected PlatformManagerLocalHome getPlatformManagerLocalHome() 
        throws NamingException {
        if(pmanagerLHome == null) {
            pmanagerLHome = PlatformManagerUtil.getLocalHome();
        }
        return pmanagerLHome;
    }
    protected PlatformManagerLocalHome pmanagerLHome;

    /**
     * If we ever have more than this single session EJB, this method
     * ought to be placed in a superclass, kinda like appdef has the
     * AppdefSessionEJB as a base class for all other appdef session EJBs.
     */
    protected AIQueueManagerLocal getAIQueueManagerLocal() 
        throws NamingException, CreateException {
        if(aiqLocal == null) {
            aiqLocal = AIQueueManagerUtil.getLocalHome().create();
        }
        return aiqLocal;
    }
    protected AIQueueManagerLocal aiqLocal;

    protected ServerConfigManagerLocalHome getServerConfigManagerLocalHome() 
        throws NamingException {
        if(serverConfigLHome == null) {
           serverConfigLHome = ServerConfigManagerUtil.getLocalHome();
        }
        return serverConfigLHome;
    }
    protected ServerConfigManagerLocalHome serverConfigLHome;

    protected ServerManagerLocalHome getServerManagerLocalHome() 
        throws NamingException {
        if(serverManagerLHome == null) {
           serverManagerLHome = ServerManagerUtil.getLocalHome();
        }
        return serverManagerLHome;
    }
    protected ServerManagerLocalHome serverManagerLHome;

    protected ServiceManagerLocalHome getServiceManagerLocalHome() 
        throws NamingException {
        if(serviceManagerLHome == null) {
           serviceManagerLHome = ServiceManagerUtil.getLocalHome();
        }
        return serviceManagerLHome;
    }
    protected ServiceManagerLocalHome serviceManagerLHome;


    protected ConfigManagerLocalHome getConfigManagerLocalHome() 
        throws NamingException {
        if(configManagerLHome == null) {
           configManagerLHome = ConfigManagerUtil.getLocalHome();
        }
        return configManagerLHome;
    }
    protected ConfigManagerLocalHome configManagerLHome;

    protected CPropManagerLocalHome getCPropManagerLocalHome() 
        throws NamingException {
        if(cpropManagerLHome == null) {
            cpropManagerLHome = CPropManagerUtil.getLocalHome();
        }
        return cpropManagerLHome;
    }
    protected CPropManagerLocalHome cpropManagerLHome;

    protected AuthzSubjectManagerLocalHome getAuthzSubjectManagerLocalHome() 
        throws NamingException {
        if(authzSubjectManagerLHome == null) {
           authzSubjectManagerLHome = AuthzSubjectManagerUtil.getLocalHome();
        }
        return authzSubjectManagerLHome;
    }
    protected AuthzSubjectManagerLocalHome authzSubjectManagerLHome;
}
