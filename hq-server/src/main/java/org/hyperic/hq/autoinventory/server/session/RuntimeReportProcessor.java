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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AIAuditFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.shared.AIConversionUtil;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AIServiceTypeValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.appdef.shared.ServiceTypeFactory;
import org.hyperic.hq.appdef.shared.ServiceValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.hq.measurement.server.session.AgentScheduleSyncZevent;
import org.hyperic.hq.measurement.shared.MeasurementProcessor;
import org.hyperic.hq.product.RuntimeResourceReport;
import org.hyperic.hq.product.ServiceType;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.util.StringUtil;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;

public class RuntimeReportProcessor {
    private final Log log = LogFactory.getLog(RuntimeReportProcessor.class);
    private final AutoinventoryManager aiManager;

    private final PlatformManager platformManager;

    private final ServerManager serverManager;

    private final ServiceManager serviceManager;

    private final ConfigManager configManager;

    private final AuthzSubjectManager subjectManager;

    private final CPropManager cpropManager;

    private final AgentManager agentManager;
    
    private AuditManager auditManager;
    
    private ResourceManager resourceManager;
    
    private MeasurementProcessor measurementProcessor;
    
    private ZeventEnqueuer zEventManager;

    private AuthzSubject _overlord;
    private List<ServiceMergeInfo> _serviceMerges = new ArrayList<ServiceMergeInfo>();
    private Set<ServiceType> serviceTypeMerges = new HashSet<ServiceType>();
    private String _agentToken;
    private ServiceTypeFactory serviceTypeFactory;
    private AIAuditFactory aiAuditFactory;
    private SessionFactory sessionFactory;

    @Autowired
    public RuntimeReportProcessor(AutoinventoryManager aiMgr, PlatformManager platformMgr, ServerManager serverMgr,
                                  ServiceManager serviceMgr, ConfigManager configMgr, AuthzSubjectManager subjectMgr,
                                  CPropManager cpropMgr, AgentManager agentManager,
                                  ServiceTypeFactory serviceTypeFactory, AIAuditFactory aiAuditFactory, AuditManager auditManager,
                                  ResourceManager resourceManager, MeasurementProcessor measurementProcessor, ZeventEnqueuer zEventManager,
                                  SessionFactory sessionFactory) {
        aiManager = aiMgr;
        platformManager = platformMgr;
        serverManager = serverMgr;
        serviceManager = serviceMgr;
        configManager = configMgr;
        subjectManager = subjectMgr;
        cpropManager = cpropMgr;
        this.agentManager = agentManager;
        this.serviceTypeFactory = serviceTypeFactory;
        this.aiAuditFactory = aiAuditFactory;
        this.auditManager = auditManager;
        this.resourceManager = resourceManager;
        this.measurementProcessor = measurementProcessor;
        this.zEventManager = zEventManager;
        this.sessionFactory = sessionFactory;
    }

    public void processRuntimeReport(AuthzSubject subject, String agentToken, CompositeRuntimeResourceReport crrr)
        throws AutoinventoryException, PermissionException, ValidationException, ApplicationException {
        _overlord = subjectManager.getOverlordPojo();
        _agentToken = agentToken;

        Agent agent = agentManager.getAgent(_agentToken);
        Audit audit = aiAuditFactory.newRuntimeImportAudit(agent);
        boolean pushed = false;

        try {
            auditManager.pushContainer(audit);
            pushed = true;
            _processRuntimeReport(subject, crrr);
        } finally {
            if (pushed) {
                auditManager.popContainer(false);
            }
        }
    }

    private void _processRuntimeReport(AuthzSubject subject, CompositeRuntimeResourceReport crrr)
        throws AutoinventoryException, PermissionException, ValidationException, ApplicationException {
        long startTime = System.currentTimeMillis();

        log.info("Processing Runtime AI Report: " + crrr.simpleSummary());

        // Before we queue or approve anything, verify that all
        // servers used in the scan both (a) exist in appdef
        // and (b) have runtime-ai turned on. If we find ones
        // that don't then we don't trust the platform/servers
        // to queue, because we don't know if some of those were
        // reported by servers that shouldn't be doing runtime scans.
        // (we don't keep track of which server reports other platforms
        // and servers)
        RuntimeResourceReport[] serverReports;
        RuntimeResourceReport serverReport;
        AIPlatformValue[] aiplatforms;
        Integer serverId;

        serverReports = crrr.getServerReports();
        Server[] appdefServers = new Server[serverReports.length];
        for (int i = 0; i < serverReports.length; i++) {

            serverReport = serverReports[i];

            // Check that reporting server still exists.
            serverId = new Integer(serverReport.getServerId());
            appdefServers[i] = serverManager.getServerById(serverId);
            if (!isValid(appdefServers[i])) {
                log.error("Error finding existing server: " + serverId);
                turnOffRuntimeDiscovery(subject, serverId);
                continue;
            }

            // Even if it does exist, make sure runtime reporting is turned
            // on for the server
            if (!appdefServers[i].isRuntimeAutodiscovery()) {
                log.warn("Server reported a runtime report, but " + "autodiscovery should be off, turning off.");
                turnOffRuntimeDiscovery(subject, serverId);
                appdefServers[i] = null;
            }
        }

        // remove resources which are marked as to be deleted once the agent can't discover them
//        Collection<Server> deletableServers = serverManager.getDeletableServers();
//        deletableServers.removeAll(Arrays.asList(appdefServers));
//        for(Server s:deletableServers) {
//            try {
//                serverManager.removeServer(subject, s);
//            }catch(VetoException e) {
//                log.error("failed removing resource " + s.getName() + " with the following exception: " + e.getMessage(),e);
//            }
//        }
        
        // Now, for each server report that had a corresponding appdef server,
        // process that report.
        /*
        +         * {@link Map} of {@link String}=fqdn to {@link Set} of {@link Resource}s
        +         * Represents the resources not checked in from the agent runtime report
        +         */
        Map platformToServers = new HashMap();
        log.info("Merging server reports into appdef (server count=" + appdefServers.length + ")");
        for (int i = 0; i < appdefServers.length; i++) {

            Server appdefServer = appdefServers[i];
            if (appdefServer == null) {
                continue;
            }
            serverReport = serverReports[i];
            aiplatforms = serverReport.getAIPlatforms();
            if (aiplatforms == null) {
                continue;
            }

            log.info("Merging platforms (platform count=" + aiplatforms.length + ", reported by serverId=" +
                     appdefServer.getId() + ") into appdef...");
            for (int j = 0; j < aiplatforms.length; j++) {
                if (aiplatforms[j] != null) {
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
                        // prior to restarting the agent. That is, the
                        // supported operation is
                        // 1. platform is deleted from the ui
                        // 2. stop the agent
                        // 3. delete the "data" directory on the agent
                        // 4. start the agent.

                        aiplatforms[j].setAgentToken(_agentToken);
                    }
                    Set tmp = (Set)platformToServers.get(aiplatforms[j].getFqdn());
                    if (tmp == null) {
                        tmp = new HashSet();
                        platformToServers.put(aiplatforms[j].getFqdn(), tmp);
                    }
                    tmp.addAll(mergePlatformIntoInventory(subject, aiplatforms[j], appdefServer));
                    flushCurrentSession();
                } else {
                    log.error("Runtime Report from server: " + appdefServers[i].getName() +
                              " contained null aiPlatform. Skipping.");
                }
            }
        }
        rescheduleNonReportedServers(subject, platformToServers);
        long endTime = System.currentTimeMillis() - startTime;
        log.info("Completed processing Runtime AI report in: " + endTime / 1000 + " seconds.");
    }
    
    private void flushCurrentSession()
    {
        sessionFactory.getCurrentSession().flush();
    }
    
    private boolean isValid(Server server) {
        if (server == null) {
            return false;
        }
        Platform p = server.getPlatform();
        if (p == null) {
            return false;
        } else {
            Resource r = p.getResource();
            if (r == null || r.isInAsyncDeleteState()) {
                return false;
            }
        }
        return true;
    }

    // [HHQ-3814] AGENT BUG: For some reason the agent does not check in its whole inventory when
    // it sends its runtime report.  Therefore, to be safe, we need to make sure its schedule is
    // up to date with the resources that are in the inventory but have not checked in
    private void rescheduleNonReportedServers(AuthzSubject subject, Map platformToServers) {
        final Set toSchedule = new HashSet();
       
        for (final Iterator it=platformToServers.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry entry = (Map.Entry) it.next();
            final String fqdn = (String) entry.getKey();
            try {
                Platform p = platformManager.findPlatformByFqdn(subject, fqdn);
                if (p == null) {
                    continue;
                }
                Resource r = p.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                toSchedule.add(r);
            } catch (PlatformNotFoundException e) {
                log.warn("could not find platformFqdn=" + fqdn + " to schedule: " + e);
                continue;
            } catch (PermissionException e) {
                log.warn("could not find platformFqdn=" + fqdn + " to schedule: " + e);
                continue;
            }
            final Collection resources = (Collection) entry.getValue();
            for (final Iterator xx=resources.iterator(); xx.hasNext(); ) {
                Resource r = (Resource) xx.next();
                log.warn("Runtime inventory report received from " + fqdn +
                          " didn't include servername=" + r.getName() + 
                          ".  Rescheduling metrics in hierarchy to ensure they are collected");
                toSchedule.add(r);
            }
        }
        measurementProcessor.scheduleHierarchyAfterCommit(toSchedule);
    }

    private List mergePlatformIntoInventory(AuthzSubject subject, AIPlatformValue aiplatform, Server reportingServer)
        throws PermissionException, ValidationException, ApplicationException {

        AIServerValue[] aiservers = aiplatform.getAIServerValues();

        log.info("Merging platform into appdef: " + aiplatform.getFqdn());

        // checks if platform exists by fqdn, certdn, then ipaddr(s)
        Platform appdefPlatform = platformManager.getPlatformByAIPlatform(subject, aiplatform);

        if (appdefPlatform == null) {
            // Add the platform
            log.info("Creating new platform: " + aiplatform);
            appdefPlatform = platformManager.createPlatform(subject, aiplatform);
        } else {
            // Want to make sure that the Resource gets love after commit
            ResourceUpdatedZevent event =
                           new ResourceUpdatedZevent(subject, appdefPlatform.getEntityId());
            zEventManager.enqueueEventAfterCommit(event);
        }

        // Else platform already exists, don't update it, only update servers
        // that are within it.
        if (aiservers == null) {
            return new ArrayList();
        }

        List appdefServers = new ArrayList(appdefPlatform.getServers());

        for (int i = 0; i < aiservers.length; i++) {
            if (aiservers[i] != null) {
                mergeServerIntoInventory(subject, appdefPlatform, aiplatform, aiservers[i], appdefServers,
                    reportingServer);
                flushCurrentSession();
            } else {
                log.error("Platform: " + appdefPlatform.getName() + " reported null aiServer. Skipping.");
            }
        }

        // any servers that we haven't handled, we should mark them
        // as AI-zombies.
        List rtn = new ArrayList(appdefServers.size());
        for (Iterator it = appdefServers.iterator(); it.hasNext();) {
            Server server = (Server) it.next();
            if (server.isWasAutodiscovered()) {
                serverManager.setAutodiscoveryZombie(server, true);
            }
            Resource r = server.getResource();
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
            rtn.add(r);
        }

        log.info("Completed Merging platform into appdef: " + aiplatform.getFqdn());
        return rtn;
    }

    private void updateServiceTypes(AIServerExtValue server, Server foundAppdefServer) {
        final AIServiceTypeValue[] serviceTypes = server.getAiServiceTypes();
        if (serviceTypes != null) {
            for (int i = 0; i < serviceTypes.length; i++) {
                final ServiceType serviceType = serviceTypeFactory.create(serviceTypes[i], foundAppdefServer
                    .getServerType());
                serviceTypeMerges.add(serviceType);
                flushCurrentSession();
            }
        }
    }

    /**
     * @param platform the platform that the server is on
     * @param aiplatform the AIPlatform that is parent to the AI Server
     * @param aiserver the server we're going to merge into inventory
     * @param appdefServers the existing servers on the platform. this method is
     *        expected to remove a server from this collection if the aiserver
     *        is found amongs the appdefServers.
     * @param reportingServer The server that reported the aiserver.
     */
    private void mergeServerIntoInventory(AuthzSubject subject, Platform platform, AIPlatformValue aiplatform,
                                          AIServerValue aiserver, List appdefServers, Server reportingServer)
        throws PermissionException, ValidationException {
        Integer serverTypePK;

        // Does this server exist (by autoinventory identifier) ?
        final Set toSchedule = new HashSet();
        String appdefServerAIID, aiServerAIID;
        aiServerAIID = aiserver.getAutoinventoryIdentifier();
        Integer aiserverId = aiserver.getId();
        AIServerExtValue aiserverExt = null;
        boolean isPlaceholder = false;
        if (aiserver instanceof AIServerExtValue) {
            aiserverExt = (AIServerExtValue) aiserver;
            isPlaceholder = aiserverExt.getPlaceholder();
        }

        log.info("Merging Server into inventory: " + " id=" + aiserver.getId() + "," + " placeholder=" + isPlaceholder +
                 "," + " name=" + aiserver.getName() + "," + " AIIdentifier=" + aiserver.getAutoinventoryIdentifier());

        Server server = null;

        for (int i = 0; i < appdefServers.size(); i++) {
            Server appdefServer = (Server) appdefServers.get(i);

            // We can match either on autoinventory identifier, or if
            // this is the reporting server, we can match on its appdef ID
            appdefServerAIID = appdefServer.getAutoinventoryIdentifier();
            if (appdefServerAIID.equals(aiServerAIID) ||
                (aiserverId != null && aiserverId.equals(reportingServer.getId()) && aiserverId.equals(appdefServer
                    .getId()))) {
                server = serverManager.getServerById(appdefServer.getId());
                log.info("Found existing server: " + server.getName() + " as match for: " +
                         aiserver.getAutoinventoryIdentifier());
                appdefServerAIID = server.getAutoinventoryIdentifier();
                if (!appdefServerAIID.equals(aiServerAIID)) {
                    log.info("Setting AIID to existing=" + appdefServerAIID);
                    aiserver.setAutoinventoryIdentifier(appdefServerAIID);
                }
                appdefServers.remove(i);
                break;
            }
        }

        boolean update;

        try {
            if (update = (server != null)) {
                // UPDATE SERVER
                log.info("Updating server: " + server.getName());

                ServerValue foundAppdefServer = AIConversionUtil.mergeAIServerIntoServer(aiserver, server
                    .getServerValue());
                server = serverManager.updateServer(subject, foundAppdefServer);
            } else {
                if (isPlaceholder) {
                    log.error("Placeholder serverId=" + aiserver.getId() + " not found for platformId=" +
                              platform.getId() + ", fqdn=" + platform.getFqdn());
                    return;
                }
                // CREATE the server
                // replace %serverName% in aisever's name.
                String newServerName = StringUtil
                    .replace(aiserver.getName(), "%serverName%", reportingServer.getName());

                aiserver.setName(newServerName);
                log.info("Creating new server: name: " + aiserver.getName() + "AIIdentifier: " +
                         aiserver.getAutoinventoryIdentifier());
                ServerValue foundAppdefServer = AIConversionUtil.convertAIServerToServer(aiserver, serverManager);

                foundAppdefServer.setWasAutodiscovered(true);

                serverTypePK = foundAppdefServer.getServerType().getId();

                // The server will be owned by whomever owns the platform
                AuthzSubject serverOwner = platform.getResource().getOwner();
                Integer platformPK = platform.getId();
                server = serverManager.createServer(serverOwner, platformPK, serverTypePK, foundAppdefServer);

                log.info("New server created: " + foundAppdefServer.getName() + " (id=" + server.getId() + ")");
            }
        } catch (ApplicationException e) {
            log.error("Failed to merge server: " + aiserver, e);
            log.info("Server: " + aiserver + " will be skipped.");
            return;
        } catch (NotFoundException e) {
            log.error("Failed to merge server: " + aiserver, e);
            log.info("Server: " + aiserver + " will be skipped.");
            return;
        }

        // Only update the server and its config if it is not
        // a placeholder. A placeholder is an AIServerExtValue that
        // exists solely to hold services underneath it.

        if (!isPlaceholder) {
            // CONFIGURE SERVER
            try {
                // Configure resource, telling the config manager to send
                // an update event if this resource has been updated.
                boolean wasUpdated = configManager.configureResponse(subject, server.getConfigResponse(), server.getEntityId(), aiserver
                    .getProductConfig(), aiserver.getMeasurementConfig(), aiserver.getControlConfig(), null, // RT
                    // config
                    null, false);
                if (update && wasUpdated) {
                    Resource r = server.getResource();
                    resourceManager.resourceHierarchyUpdated(subject, Collections.singletonList(r));
                } else {
                   // want to make sure that the server's schedule is correct on the agent
                    toSchedule.add(server.getEntityId());
                }
            } catch (Exception e) {
                log.error("Error configuring server: " + server.getId() + ": " + e, e);
            }
        }

        // setCustomProperties regardless if the server is a placeholder or not.
        // if cprops == null, this is a no-op. else, JBoss for example will get
        // the majority of its cprops via JMX, which it doesnt use until
        // runtime-ai
        try {
            // SET CUSTOM PROPERTIES FOR SERVER
            int typeId = server.getServerType().getId().intValue();
            cpropManager.setConfigResponse(server.getEntityId(), typeId, aiserver.getCustomProperties());
        } catch (Exception e) {
            log.warn("Error setting server custom properties: " + e, e);
        }

        if (aiserverExt != null) {

            log.info("Updating services for server: " + server.getName());

            updateServiceTypes(aiserverExt, server);

            List appdefServices;

            // ServerValue.getServiceValues not working here for some reason,
            // get the services explicitly from the ServiceManager.
            try {
                appdefServices = serviceManager.getServicesByServer(subject, server.getId(), PageControl.PAGE_ALL);
            } catch (Exception e) {
                appdefServices = new ArrayList();
            }

            List aiServices = aiserverExt.getAIServiceValuesAsList();

            // Change the service names if they require expansion.
            for (Iterator i = aiServices.iterator(); i.hasNext();) {
                AIServiceValue aiSvc = (AIServiceValue) i.next();
                String newName = StringUtil.replace(aiSvc.getName(), "%serverName%", server.getName());
                aiSvc.setName(newName);
            }

            String fqdn = aiplatform.getName() == null ? aiplatform.getFqdn() : aiplatform.getName();

            // Filter out and mark zombie services
            for (Iterator i = appdefServices.iterator(); i.hasNext();) {
                ServiceValue tmp = (ServiceValue) i.next();
                final Service service = serviceManager.getServiceById(tmp.getId());
                if (service == null || service.getResource() == null || service.getResource().isInAsyncDeleteState()) {
                    continue;
                }
                final String aiid = service.getAutoinventoryIdentifier();
                boolean found = false;

                AIServiceValue aiSvc = null;
                for (final Iterator j = aiServices.iterator(); j.hasNext();) {
                    aiSvc = (AIServiceValue) j.next();
                    final String ainame = aiSvc.getName();
                    if (found = aiid.equals(ainame)) {
                        break;
                    } else if (aiid.startsWith(fqdn)) {
                        // Get rid of the FQDN
                        final String subname = ainame.substring(fqdn.length());
                        if (found = aiid.endsWith(subname)) {
                            break;
                        }
                    }
                }

                if (found) {
                    // Update name if FQDN changed
                    final String svcName = service.getName();
                    // only change the name if it hasn't been changed already
                    // for example if !svcName.equals(aiid): this means that
                    // the user has explicitly change the service's name
                    if (aiSvc != null && svcName.equals(aiid)) {
                        service.setName(aiSvc.getName());
                    }
                    // this means that the fqdn changed
                    if (aiSvc != null && !aiid.equals(aiSvc.getName())) {
                        service.setAutoinventoryIdentifier(aiSvc.getName());
                    }
                } else {
                    log.info("Service id=" + service.getId() + " name=" + service.getName() + " has become a zombie");
                    serviceManager.updateServiceZombieStatus(_overlord, service, true);
                }
            }

            for (Iterator i = aiServices.iterator(); i.hasNext();) {
                AIServiceValue aiService = (AIServiceValue) i.next();
                ServiceMergeInfo sInfo = new ServiceMergeInfo();
                sInfo.subject = subject;
                sInfo.serverId = server.getId();
                sInfo.aiservice = aiService;
                sInfo.agentToken = _agentToken;
                _serviceMerges.add(sInfo);
                flushCurrentSession();
            }
        }
        zEventManager.enqueueEventAfterCommit(new AgentScheduleSyncZevent(toSchedule));
        log.info("Completed merging server: " + reportingServer.getName() + " into inventory");
    }

    public static class ServiceMergeInfo {
        public AuthzSubject subject;
        public Integer serverId;
        public AIServiceValue aiservice;
        public String agentToken;
    }

    public List<ServiceMergeInfo> getServiceMerges() {
        return _serviceMerges;
    }

    public Set<ServiceType> getServiceTypeMerges() {
        return serviceTypeMerges;
    }

    private boolean turnOffRuntimeDiscovery(AuthzSubject subject, Integer serverId) {
        AppdefEntityID aid = AppdefEntityID.newServerID(serverId);
        log.info("Disabling RuntimeDiscovery for server: " + serverId);
        try {
            aiManager.turnOffRuntimeDiscovery(subject, aid, _agentToken);
            return true;
        } catch (Exception e) {
            log.error("Error turning off runtime scans for server: " + serverId, e);
            return false;
        }
    }
}
