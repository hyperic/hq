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
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AIConversionUtil;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.appdef.shared.AppdefDuplicateNameException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManagerLocal;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.server.session.AIAudit;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.CPropManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManagerLocal;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.server.session.AuditManagerEJBImpl;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.hyperic.util.StringUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.hibernate.Util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class RuntimeReportProcessor {
    private Log log = LogFactory.getLog(RuntimeReportProcessor.class);
    private AutoinventoryManagerLocal aiMgr = 
        AutoinventoryManagerEJBImpl.getOne();
    private PlatformManagerLocal platformMgr = 
        PlatformManagerEJBImpl.getOne();
    private ServerManagerLocal  serverMgr = 
        ServerManagerEJBImpl.getOne();
    private ServiceManagerLocal  serviceMgr = 
        ServiceManagerEJBImpl.getOne();
    private ConfigManagerLocal configMgr = 
        ConfigManagerEJBImpl.getOne();
    private AuthzSubjectManagerLocal subjectMgr =
        AuthzSubjectManagerEJBImpl.getOne();
    private CPropManagerLocal cpropMgr =
        CPropManagerEJBImpl.getOne();
    
    public RuntimeReportProcessor () {}

    public void processRuntimeReport(AuthzSubjectValue subject,
                                     String agentToken,
                                     CompositeRuntimeResourceReport crrr)
        throws AutoinventoryException, CreateException, 
               PermissionException, ValidationException, 
               ApplicationException 
    {
        Agent agent = AgentManagerEJBImpl.getOne().getAgentPojo(agentToken);
        Audit audit = AIAudit.newRuntimeImportAudit(agent); 
        boolean pushed = false;
        
        try {
            AuditManagerEJBImpl.getOne().pushContainer(audit);
            pushed = true; 
            _processRuntimeReport(subject, agentToken, crrr); 
        } finally {
            if (pushed) {
                AuditManagerEJBImpl.getOne().popContainer(false);
            }
        }
    }

    private void _processRuntimeReport(AuthzSubjectValue subject,
                                       String agentToken,
                                       CompositeRuntimeResourceReport crrr)
        throws AutoinventoryException, CreateException, 
               PermissionException, ValidationException, 
               ApplicationException 
    {
        long startTime = System.currentTimeMillis();

        log.info("Processing Runtime AI Report: " + crrr.simpleSummary());

        // Before we queue or approve anything, verify that all
        // servers used in the scan both (a) exist in appdef
        // and (b) have runtime-ai turned on.  If we find ones
        // that don't then we don't trust the platform/servers
        // to queue, because we don't know if some of those were
        // reported by servers that shouldn't be doing runtime scans.
        // (we don't keep track of which server reports other platforms
        // and servers)
        int i, j;
        ServerValue[] appdefServers;
        RuntimeResourceReport[] serverReports;
        RuntimeResourceReport serverReport;
        AIPlatformValue[] aiplatforms;
        Integer serverId;

        serverReports = crrr.getServerReports();
        appdefServers = new ServerValue[serverReports.length];
        for (i=0; i<serverReports.length; i++) {

            serverReport = serverReports[i];

            // Check that reporting server still exists.
            serverId = new Integer(serverReport.getServerId());
            Server server = serverMgr.getServerById(serverId);
            if (server == null) {
                log.error("Error finding existing server: " + serverId);
                turnOffRuntimeDiscovery(subject, serverId, 
                                        agentToken);
                appdefServers[i] = null;
                continue;
            } else {
                appdefServers[i] = server.getServerValue();
            }

            // Even if it does exist, make sure runtime reporting is turned
            // on for the server
            if (!appdefServers[i].getRuntimeAutodiscovery()) {
                log.warn("Server reported a runtime report, but " +
                         "autodiscovery should be off, turning off.");
                turnOffRuntimeDiscovery(subject, serverId, 
                                        agentToken);
                appdefServers[i] = null;
            }
        }

        // Now, for each server report that had a corresponding appdef server,
        // process that report.
        log.info("Merging server reports into appdef " +
                 "(server count=" + appdefServers.length + ")");
        for (i=0; i<appdefServers.length; i++) {

            if (appdefServers[i] == null) continue;
            serverReport = serverReports[i];
            aiplatforms = serverReport.getAIPlatforms();
            if (aiplatforms == null) continue;

            log.info("Merging platforms " +
                     "(platform count=" + aiplatforms.length +
                     ", reported by serverId=" + appdefServers[i].getId() +
                     ") into appdef...");
            for (j=0; j<aiplatforms.length; j++) {
                if(aiplatforms[j] != null) {
                    if (aiplatforms[j].getAgentToken() == null) {
                        // reassociate agent to the auto discoverred platform
                        // one situation this condition occurs is when
                        // 1. platform is deleted from the ui
                        // 2. agent for that platform is stopped and started
                        //
                        // BTW: there is lot more going on here, then
                        // just setting the agent token. As of today,
                        // inventory rediscovery is supported only if
                        // the "data" directory on the agent is deleted
                        // prior to restarting the agent.  That is, the
                        // supported operation is
                        // 1. platform is deleted from the ui
                        // 2. stop the agent
                        // 3. delete the "data" directory on the agent
                        // 4. start the agent.

                        aiplatforms[j].setAgentToken(agentToken);
                    }
                    mergePlatformIntoInventory(subject, aiplatforms[j],
                                               appdefServers[i]); 
                    Util.flushCurrentSession();
                } else {
                    log.error("Runtime Report from server: " + appdefServers[i].getName() 
                            + " contained null aiPlatform. Skipping.");
                }
            }
        }
        long endTime = System.currentTimeMillis() - startTime;
        log.info("Completed processing Runtime AI report in: " +
                 endTime/1000 + " seconds.");
    }

    private void mergePlatformIntoInventory(AuthzSubjectValue subject,
                                            AIPlatformValue aiplatform,
                                            ServerValue reportingServer)
        throws CreateException, PermissionException, ValidationException,
               ApplicationException {

        AIServerValue[] aiservers = aiplatform.getAIServerValues();

        log.info("Merging platform into appdef: " + aiplatform.getFqdn());

        // Does this platform exist (by fqdn) ?
        PlatformValue appdefPlatform;
        String fqdn = aiplatform.getFqdn();
        try {
            Platform p = platformMgr.findPlatformByFqdn(subject, fqdn);
            appdefPlatform = p.getPlatformValue();
        } catch (PlatformNotFoundException e) {
            // Platform doesn't exist by fqdn, so let's try by IP address.
            // This is needed for servers like weblogic, which report platforms 
            // for node servers with an IP address, and not an FQDN.
            PageList platforms = platformMgr.findPlatformsByIpAddr(subject, 
                                                                   fqdn, null);
            if (platforms.size() == 1) {
                appdefPlatform = (PlatformValue) platforms.get(0);

            } else if (platforms.size() > 1) {
                log.warn("Multiple platforms have IP address " + fqdn +
                         ", could not determine a definitive platform");
                appdefPlatform = null;

            } else {
                log.warn("Could not find a platform with FQDN or IP " +
                         "address that matched: " + fqdn);
                appdefPlatform = null;
            }
        }

        if (appdefPlatform == null) {
            // Add the platform
            log.info("Creating new platform: " + aiplatform);
            Platform platform =
                platformMgr.createPlatform(subject, aiplatform);
            appdefPlatform = platform.getPlatformValue();
        } else {
            // Platform already exists, don't update it, only update servers
            // that are within it.
        }

        PageList appdefServerList =
            serverMgr.getServersByPlatform(subject, appdefPlatform.getId(),
                                           false, PageControl.PAGE_ALL);

        if (aiservers == null) return;

        for (int i=0; i<aiservers.length; i++) {
            if(aiservers[i] != null) {
                mergeServerIntoInventory(subject, appdefPlatform, aiservers[i],
                                         appdefServerList, reportingServer);
                Util.flushCurrentSession();
            } else {
                log.error("Platform: " + appdefPlatform.getName() + 
                          " reported null aiServer. Skipping.");
            }
        }

        // any servers that we haven't handled, we should mark them
        // as AI-zombies.
        for (int i=0; i<appdefServerList.size(); i++) {
            ServerValue appdefServer = (ServerValue) appdefServerList.get(i);

            // Annoying - serverMgr doesn't support updateServer on 
            // ServerLightValue objects, so we lookup the full value object.
            ServerValue serverValue = null;
            try {
                serverValue = serverMgr.getServerById(subject,
                                                      appdefServer.getId());
                if (serverValue.getWasAutodiscovered()) {
                    serverValue.setAutodiscoveryZombie(true);
                    serverMgr.updateServer(subject, serverValue);
                }
            } catch (AppdefDuplicateNameException e) {
                log.warn("Error updating server: "
                         + serverValue + ": " + e.getMessage());
            } catch (UpdateException e) {
                log.error("Error updating server: " + serverValue, e);
            } catch (ServerNotFoundException e) {
                log.error("Error updating server: " + serverValue, e);
            }
        }
        log.info("Completed Merging platform into appdef: " +
                 aiplatform.getFqdn());
    }

    /**
     * @param platform the platform that the server is on
     * @param aiserver the server we're going to merge into inventory
     * @param appdefServers the existing servers on the platform.
     * this method is expected to remove a server from this collection
     * if the aiserver is found amongs the appdefServers.
     * @param reportingServer The server that reported the aiserver.
     */
    private void mergeServerIntoInventory(AuthzSubjectValue subject,
                                          PlatformValue platform,
                                          AIServerValue aiserver,
                                          List appdefServers,
                                          ServerValue reportingServer)
        throws CreateException, PermissionException, ValidationException
    {
        int i;
        ServerValue appdefServer;
        ServerValue foundAppdefServer = null;
        Integer serverTypePK;

        // Does this server exist (by autoinventory identifier) ?
        String appdefServerAIID, aiServerAIID;
        aiServerAIID = aiserver.getAutoinventoryIdentifier();
        Integer aiserverId = aiserver.getId();

        log.info("Merging Server into inventory: name: " +
                 aiserver.getName() + " AIIdentifier: " +
                 aiserver.getAutoinventoryIdentifier());
        for (i=0; i<appdefServers.size(); i++) {
            appdefServer = (ServerValue) appdefServers.get(i);

            // We can match either on autoinventory identifier, or if
            // this is the reporting server, we can match on its appdef ID
            appdefServerAIID = appdefServer.getAutoinventoryIdentifier();
            if (appdefServerAIID.equals(aiServerAIID) ||
                (aiserverId != null &&
                 aiserverId.equals(reportingServer.getId()) &&
                 aiserverId.equals(appdefServer.getId()))) {
                try {
                    foundAppdefServer
                        = serverMgr.getServerById(subject,
                                                  appdefServer.getId());
                    log.info("Found existing server: " +
                             foundAppdefServer.getName() + " as match for: " +
                             aiserver.getAutoinventoryIdentifier());
                    appdefServerAIID =
                        foundAppdefServer.getAutoinventoryIdentifier();
                    if (!appdefServerAIID.equals(aiServerAIID)) {
                        log.info("Setting AIID to existing=" +
                                 appdefServerAIID);
                        aiserver.setAutoinventoryIdentifier(appdefServerAIID);
                    }
                } catch (ServerNotFoundException e) {
                    log.error("Error finding server: " + appdefServer);
                    throw new SystemException(e);
                }
                appdefServers.remove(i);
                break;
            }
        }

        boolean update;

        try {
            if (foundAppdefServer == null) {
                update = false;
                // CREATE the server
                // replace %serverName% in aisever's name.
                String newServerName =
                    StringUtil.replace(aiserver.getName(), "%serverName%",
                                       reportingServer.getName());

                aiserver.setName(newServerName);
                log.info("Creating new server: name: " + aiserver.getName() +
                         "AIIdentifier: " +
                         aiserver.getAutoinventoryIdentifier());
                foundAppdefServer
                    = AIConversionUtil.convertAIServerToServer(aiserver, 
                                                               serverMgr);
            
                foundAppdefServer.setWasAutodiscovered(true);

                serverTypePK = foundAppdefServer.getServerType().getId();
                
                // The server will be owned by whomever owns the platform
                String serverOwnerName = platform.getOwner();
                AuthzSubjectValue serverOwner
                    = subjectMgr.findSubjectByName(subject, serverOwnerName);
                Integer platformPK = platform.getId();
                Server server = serverMgr.createServer(serverOwner,
                                                       platformPK,
                                                       serverTypePK,
                                                       foundAppdefServer);

                log.info("New server created: " + foundAppdefServer.getName() +
                         " (id=" + server.getId() + ")");
                // Refresh the light value to include all attributes
                foundAppdefServer = server.getServerValue();
            } else {
                update = true;
                // UPDATE SERVER
                log.info("Updating server: " + foundAppdefServer.getName());

                foundAppdefServer
                    = AIConversionUtil.mergeAIServerIntoServer(aiserver,
                                                               foundAppdefServer);
                serverMgr.updateServer(subject, foundAppdefServer);
            }
        } catch (ApplicationException e) {
            log.error("Failed to merge server: " + aiserver, e);
            log.info("Server: " + aiserver + " will be skipped.");
            return;
        } catch (FinderException e) {
            log.error("Failed to merge server: " + aiserver, e);
            log.info("Server: " + aiserver + " will be skipped.");
            return;
        }

        // Only update the server and its config if it is not
        // a placeholder.  A placeholder is an AIServerExtValue that
        // exists solely to hold services underneath it.
        AIServerExtValue aiserverExt = null;
        if (aiserver instanceof AIServerExtValue) {
            aiserverExt = (AIServerExtValue) aiserver;
        }

        if (!isPlaceholder(aiserverExt)) {             
            // CONFIGURE SERVER
            try {
                // Configure resource, telling the config manager to send
                // an update event if this resource has been updated.
                configMgr.configureResource(subject,
                                            foundAppdefServer.getEntityId(),
                                            aiserver.getProductConfig(),
                                            aiserver.getMeasurementConfig(),
                                            aiserver.getControlConfig(),
                                            null, //RT config
                                            null,
                                            update,
                                            false);
            } catch (Exception e) {
                log.error("Error configuring server: " 
                          + foundAppdefServer.getId() + ": " + e, e);
            }
        }

        //setCustomProperties regardless if the server is a placeholder or not.
        //if cprops == null, this is a no-op.  else, JBoss for example will get
        //the majority of its cprops via JMX, which it doesnt use until
        // runtime-ai
        try {
            // SET CUSTOM PROPERTIES FOR SERVER
            int typeId =
                foundAppdefServer.getServerType().getId().intValue();
            cpropMgr.setConfigResponse(foundAppdefServer.getEntityId(),
                                       typeId,
                                       aiserver.getCustomProperties());
        } catch (Exception e) {
            log.warn("Error setting server custom properties: " + e, e);
        }

        if (aiserverExt != null) {

            log.info("Updating services for server: " +
                     foundAppdefServer.getName());

            AIServiceValue[] aiservices;
            List appdefServices;

            aiservices = ((AIServerExtValue) aiserver).getAIServiceValues();
            if (aiservices != null) {
                // ServerValue.getServiceValues not working here for some reason,
                // get the services explicitly from the ServiceManager.
                try {
                    appdefServices = 
                        serviceMgr.getServicesByServer(subject,
                                                       foundAppdefServer.getId(),
                                                       PageControl.PAGE_ALL);
                } catch (Exception e) {
                    appdefServices = new ArrayList();
                }

                for (i=0; i<aiservices.length; i++) {
                    if(aiservices[i] != null) {
                        mergeServiceIntoInventory(subject, foundAppdefServer,
                                                  aiservices[i],
                                                  appdefServices,
                                                  reportingServer);
                        Util.flushCurrentSession();
                    } else {
                        log.error("Server: " + reportingServer.getName() + 
                                  " reported null aiservice. Skipping.");
                    }
                }

                // any services that we haven't handled, we should mark them
                // as AI-zombies.
                ServiceValue appdefService;
                for (i=0; i<appdefServices.size(); i++) {
                    appdefService = (ServiceValue) appdefServices.get(i);
                    try {
                        appdefService.setAutodiscoveryZombie(true);
                        serviceMgr.updateService(subject, appdefService);
                        Util.flushCurrentSession();
                    } catch (ApplicationException e) {
                        log.error("Error marking service as zombie: " +
                                  appdefService.getName(), e);
                    } 
                }
            }
        }
        log.info("Completed merging server: " + reportingServer.getName() +
                 " into inventory");
    }

    private void mergeServiceIntoInventory(AuthzSubjectValue subject,
                                           ServerValue server,
                                           AIServiceValue aiservice,
                                           List appdefServices,
                                           ServerValue reportingServer)
        throws CreateException, PermissionException, ValidationException {

        int i;
        ServiceValue appdefService;
        ServiceValue foundAppdefService = null;

        String newServerName = StringUtil.replace(aiservice.getName(),
                                                  "%serverName%",
                                                  server.getName());
        aiservice.setName(newServerName);

        // Does this service exist (by name) ?
        for (i=0; i<appdefServices.size(); i++) {
            appdefService = (ServiceValue) appdefServices.get(i);
            if (appdefService.getName().equals(aiservice.getName())) {
                foundAppdefService = appdefService;
                // Remove from list so all that's left are zombies
                appdefServices.remove(i);
                break;
            }
        }

        boolean update;

        try {
            if (foundAppdefService == null) {
                update = false;
                // CREATE SERVICE
                log.info("Creating new service: " + aiservice.getName());

                try {
                    foundAppdefService
                        = AIConversionUtil.convertAIServiceToService(aiservice,
                                                                     serviceMgr);
                } catch (FinderException e) {
                    // Most likely a plugin bug
                    log.error("Unable to find reported resource type: " + 
                              aiservice.getServiceTypeName() + " for " +
                              "resource: " + aiservice.getName() +
                              ", ignoring");
                    return;
                }

                Integer serviceTypePK
                    = foundAppdefService.getServiceType().getId();
                String serviceOwnerName = server.getOwner();
                AuthzSubjectValue serviceOwner
                    = subjectMgr.findSubjectByName(subject, serviceOwnerName);
                Integer pk  = serviceMgr.createService(serviceOwner,
                                                       server.getId(),
                                                       serviceTypePK,
                                                       foundAppdefService);
                try {
                    foundAppdefService = serviceMgr.getServiceById(serviceOwner,
                                                               pk);
                } catch (ServiceNotFoundException e) {
                    log.fatal("Unable to find service we just created.", e);
                    throw new SystemException("Unable to find service we "
                                                 + "just created", e);
                }
                log.debug("New service created: " + foundAppdefService);
            } else {
                update = true;
                // UPDATE SERVICE
                log.info("Updating service: " + foundAppdefService.getName());

                foundAppdefService
                    = AIConversionUtil.mergeAIServiceIntoService(aiservice,
                                                                 foundAppdefService);
                serviceMgr.updateService(subject, foundAppdefService);
            }
            
            // CONFIGURE SERVICE
            configMgr.configureResource(subject,
                                        foundAppdefService.getEntityId(),
                                        aiservice.getProductConfig(),
                                        aiservice.getMeasurementConfig(),
                                        aiservice.getControlConfig(),
                                        aiservice.getResponseTimeConfig(),
                                        null,
                                        update,
                                        false);
            
            // SET CUSTOM PROPERTIES FOR SERVICE
            if (aiservice.getCustomProperties() != null) {
                int typeId =
                    foundAppdefService.getServiceType().getId().intValue();
                cpropMgr.setConfigResponse(foundAppdefService.getEntityId(),
                                           typeId,
                                           aiservice.getCustomProperties());            
            }
        } catch (ApplicationException e) {
            log.error("Failed to merge service: " + aiservice, e);
            log.info("Skipping merging of service: " + aiservice);
        } catch (FinderException e) {
            log.error("Failed to merge service: " + aiservice, e);
            log.info("Skipping merging of service: " + aiservice);
        }
    }

    private boolean turnOffRuntimeDiscovery(AuthzSubjectValue subject,
                                            Integer serverId,
                                            String agentToken ) {
        AppdefEntityID aid =
            new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_SERVER,
                               serverId);

        log.info("Disabling RuntimeDiscovery for server: " + serverId);
        try {
            aiMgr.turnOffRuntimeDiscovery(subject, aid, agentToken);
            return true;
            
        } catch (Exception e) {
            log.error("Error turning off runtime scans for " +
                      "server: " + serverId, e);
            return false;
        }
    }

    private boolean isPlaceholder(AIServerExtValue aiserverExt) {
        return (aiserverExt != null && aiserverExt.getPlaceholder());
    }
}
