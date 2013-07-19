/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.bizapp.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ApplicationManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ResourceTypeCleanupZevent;
import org.hyperic.hq.appdef.shared.ResourcesCleanupZevent;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.GroupMember;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.Audit;
import org.hyperic.hq.common.shared.AuditManager;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ResourceCleanupEventListener implements ZeventListener<ResourcesCleanupZevent>, ResourceCleanupEventListenerRegistrar {

    private AppdefBoss appdefBoss;

    private final Log log = LogFactory.getLog(ResourceCleanupEventListener.class);
    
    private ZeventEnqueuer zEventManager;
    
    private AuthzSubjectManager authzSubjectManager;
    
    private ApplicationManager applicationManager;
    
    private ResourceGroupManager resourceGroupManager;
    
    private ServiceManager serviceManager;
    
    private ServerManager serverManager;
    
    private PlatformManager platformManager;
    
    private AgentManager agentManager;
    
    private MeasurementManager measurementManager;

    private ResourceManager resourceManager;

    private AlertManager alertManager;

    private AuditManager auditManager;

    private EscalationManager escalationManager;

    @Autowired 
    public ResourceCleanupEventListener(AppdefBoss appdefBoss, ZeventEnqueuer zEventManager,
                                        AuthzSubjectManager authzSubjectManager,
                                        ApplicationManager applicationManager,
                                        ResourceGroupManager resourceGroupManager,
                                        ServiceManager serviceManager, ServerManager serverManager,
                                        PlatformManager platformManager, AgentManager agentManager,
                                        MeasurementManager measurementManager,
                                        ResourceManager resourceManager, AlertManager alertManager,
                                        AuditManager auditManager, EscalationManager escalationManager) {
        this.appdefBoss = appdefBoss;
        this.zEventManager = zEventManager;
        this.authzSubjectManager = authzSubjectManager;
        this.applicationManager = applicationManager;
        this.resourceGroupManager = resourceGroupManager;
        this.serviceManager = serviceManager;
        this.serverManager = serverManager;
        this.platformManager = platformManager;
        this.agentManager = agentManager;
        this.measurementManager = measurementManager;
        this.resourceManager = resourceManager;
        this.alertManager = alertManager;
        this.auditManager = auditManager;
        this.escalationManager = escalationManager;
    }
    
    /*
     * Field 1: (0-59) second
     * Field 2: (0-59) minute
     * Field 3: (0-23) hour
	 * Field 4: (1-31) day  of  the month
	 * Field 5: (1-12) month of the year
	 * Field 6: (0-6)  day of the week - 1=Monday
     */
    @Scheduled(cron="0 50 9 * * *")
    public void runCleanup() {
        zEventManager.enqueueEventAfterCommit(new ResourcesCleanupZevent());
    }

    public void registerResourceCleanupListener() {
        // Add listener to remove alert definition and alerts after resources
        // are deleted.
        HashSet<Class<? extends Zevent>> events = new HashSet<Class<? extends Zevent>>();
        events.add(ResourcesCleanupZevent.class);
        events.add(ResourceTypeCleanupZevent.class);
        zEventManager.addBufferedListener(events, this);
        zEventManager.enqueueEventAfterCommit(new ResourcesCleanupZevent());
    }

    public void processEvents(List<ResourcesCleanupZevent> events) {
        log.info("starting removeDeletedResources");
        final StopWatch watch = new StopWatch();
        final Collection<String> typeNames = new ArrayList<String>();
        for (final ResourcesCleanupZevent e : events) {
            if (e instanceof ResourceTypeCleanupZevent) {
                typeNames.addAll(((ResourceTypeCleanupZevent) e).getTypeNames());
            }
        }
        if (events != null && !events.isEmpty()) {
            try {
                Map<Integer,List<AppdefEntityID>> agentCache = buildAsyncDeleteAgentCache(events);
                removeDeletedResources(agentCache, typeNames);
                final AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
                if (!typeNames.isEmpty()) {
                    resourceManager.removeResourceTypes(typeNames);
                }
                removeOrphanedPlatforms(overlord);
                removeOrphanedServers(overlord);
                removeOrphanedServices(overlord);
                removeOrphanedAlertDefs();
                removeOrphanedResourceGroupMembers();
                removeOrphanedAudits();
                removeOrphanedEscalationStates(overlord);
                removeOrphanedResources(overlord);
            } catch (Exception e) {
                log.error("removeDeletedResources failed", e);
            }                        
        }
        log.info("completed removeDeletedResources " + watch);
    }

    private void removeOrphanedResources(AuthzSubject overlord) {
        final Collection<Resource> resources = resourceManager.getOrphanedResources();
        if (!resources.isEmpty()) {
            log.info("cleaning up " + resources.size() + " orphaned resources");
        }
        for (final Resource r : resources) {
            try {
                resourceManager.removeResource(overlord, r);
            } catch (VetoException e) {
                log.error(e,e);
            }
        }
    }

    private void removeOrphanedEscalationStates(AuthzSubject overlord) {
        final Collection<EscalationState> escalationStates = escalationManager.getOrphanedEscalationStates();
        if (!escalationStates.isEmpty()) {
            log.info("cleaning up " + escalationStates.size() + " orphaned escalation states");
        }
        for (final EscalationState e : escalationStates) {
            escalationManager.removeEscalationState(e);
        }
    }

    private void removeOrphanedAudits() {
        final Collection<Audit> audits = auditManager.getOrphanedAudits();
        if (!audits.isEmpty()) {
            log.info("cleaning up " + audits.size() + " orphaned audits");
        }
        for (final Audit a : audits) {
            auditManager.deleteAudit(a);
        }
    }

    private void removeOrphanedResourceGroupMembers() {
        final Collection<GroupMember> members = resourceGroupManager.getOrphanedResourceGroupMembers();
        if (!members.isEmpty()) {
            log.info("cleaning up " + members.size() + " orphaned group members");
        }
        for (final GroupMember m : members) {
            resourceGroupManager.removeGroupMember(m);
        }
    }

    private void removeOrphanedAlertDefs() {
        final Collection<AlertDefinition> alertDefs = alertManager.getOrphanedAlertDefs();
        if (!alertDefs.isEmpty()) {
            log.info("cleaning up " + alertDefs.size() + " orphaned alert definitions");
        }
        for (final AlertDefinition def : alertDefs) {
            try {
                alertManager.deleteAlertDef(def);
            } catch (ObjectNotFoundException e) {
                log.warn(e);
                log.debug(e,e);
            }
        }
    }

    private void removeOrphanedPlatforms(AuthzSubject overlord) {
        final Collection<Platform> platforms = platformManager.getOrphanedPlatforms();
        if (!platforms.isEmpty()) {
            log.info("cleaning up " + platforms.size() + " orphaned platforms");
        }
        for (Platform platform : platforms) {
            try {
                platform = platformManager.getPlatformById(platform.getId());
                platformManager.removePlatform(overlord, platform);
            } catch (ObjectNotFoundException e) {
                log.warn(e);
                log.debug(e,e);
            } catch (PlatformNotFoundException e) {
                log.warn(e);
                log.debug(e,e);
            } catch (PermissionException e) {
                log.warn(e);
                log.debug(e,e);
            } catch (VetoException e) {
                log.warn(e);
                log.debug(e,e);
            }
        }
    }

    private void removeOrphanedServers(AuthzSubject overlord) {
        final Collection<Server> servers = serverManager.getOrphanedServers();
        if (!servers.isEmpty()) {
            log.info("cleaning up " + servers.size() + " orphaned servers");
        }
        for (Server server : servers) {
            try {
                server = serverManager.getServerById(server.getId());
                serverManager.removeServer(overlord, server);
            } catch (ObjectNotFoundException e) {
                log.warn(e);
                log.debug(e,e);
            } catch (PermissionException e) {
                log.warn(e);
                log.debug(e,e);
            } catch (VetoException e) {
                log.warn(e);
                log.debug(e,e);
            }
        }
    }

    private void removeOrphanedServices(AuthzSubject overlord) {
        final Collection<Service> services = serviceManager.getOrphanedServices();
        if (!services.isEmpty()) {
            log.info("cleaning up " + services.size() + " orphaned services");
        }
        for (Service service : services) {
            try {
                service = serviceManager.getServiceById(service.getId());
                serviceManager.removeService(overlord, service);
            } catch (ObjectNotFoundException e) {
                log.warn(e);
                log.debug(e,e);
            } catch (PermissionException e) {
                log.warn(e);
                log.debug(e,e);
            } catch (VetoException e) {
                log.warn(e);
                log.debug(e,e);
            }
        }
    }

    /**
     * @param zevents {@link List} of {@link ResourcesCleanupZevent}
     * 
     * @return {@link Map} of {@link Integer} of agentIds 
     * to {@link List} of {@link AppdefEntityID}s
     */
    @SuppressWarnings("unchecked")
    private Map<Integer,List<AppdefEntityID>> buildAsyncDeleteAgentCache(List<ResourcesCleanupZevent> zevents) {
        Map<Integer,List<AppdefEntityID>> masterCache = new HashMap<Integer,List<AppdefEntityID>>();
        
        for (ResourcesCleanupZevent z : zevents) {
            if (z.getAgents() != null) {
                Map<Integer,List<AppdefEntityID>> cache = z.getAgents();
                
                for (Integer agentId : cache.keySet() ) {
                    
                    List<AppdefEntityID> newResources = cache.get(agentId);
                    List<AppdefEntityID> resources = masterCache.get(agentId);
                    if (resources == null) {
                        resources = newResources;
                    } else {
                        resources.addAll(newResources);
                    }
                    masterCache.put(agentId, resources);
                }
            }
        }
        
        return masterCache;
    }
    
    @SuppressWarnings("unchecked")
    private void removeDeletedResources(Map<Integer, List<AppdefEntityID>> agentCache, Collection<String> typeNames)
    throws ApplicationException, VetoException {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        final AuthzSubject subject = authzSubjectManager.findSubjectById(AuthzConstants.overlordId);
        if (debug) watch.markTimeBegin("unscheduleMeasurementsForAsyncDelete");
        unscheduleMeasurementsForAsyncDelete(agentCache);
        if (debug) watch.markTimeEnd("unscheduleMeasurementsForAsyncDelete");
        
        // Look through services, servers, platforms, applications, and groups
        if (debug) watch.markTimeBegin("removeApplications");
        Collection<Application> applications = applicationManager.findDeletedApplications();
        removeApplications(subject, applications);
        if (debug) watch.markTimeEnd("removeApplications");

        if (debug) watch.markTimeBegin("removeResourceGroups");
        Collection<ResourceGroup> groups = resourceGroupManager.findDeletedGroups();
        removeResourceGroups(subject, groups);
        if (debug) watch.markTimeEnd("removeResourceGroups");

        typeNames = (typeNames == null) ? Collections.EMPTY_LIST : typeNames;
        if (debug) watch.markTimeBegin("removeGroupsCompatibleWith");
        for (String name : typeNames) {
            resourceGroupManager.removeGroupsCompatibleWith(name);
        }
        if (debug) watch.markTimeEnd("removeGroupsCompatibleWith");

        Collection<Service> services = serviceManager.findDeletedServices();
        removeServices(subject, services);

        Collection<Server> servers = serverManager.findDeletedServers();
        removeServers(subject, servers);

        if (debug) watch.markTimeBegin("removePlatforms");
        Collection<Platform> platforms = platformManager.findDeletedPlatforms();
        removePlatforms(subject, platforms);
        if (debug) watch.markTimeEnd("removePlatforms");
        if (debug) log.debug("removeDeletedResources: " + watch);
    }
    
    /**
     * Disable measurements and unschedule from the agent in bulk with the agent
     * cache info because the resources have been de-referenced from the agent
     * 
     * @param agentCache {@link Map} of {@link Integer} of agentIds to
     *        {@link List} of {@link AppdefEntityID}s
     */
    private void unscheduleMeasurementsForAsyncDelete(Map<Integer, List<AppdefEntityID>> agentCache) {
        if (agentCache == null) {
            return;
        }

        try {
            AuthzSubject subject = authzSubjectManager.findSubjectById(AuthzConstants.overlordId);

            for (Integer agentId : agentCache.keySet()) {

                Agent agent = agentManager.getAgent(agentId);
                List<AppdefEntityID> resources = agentCache.get(agentId);
               
                measurementManager.disableMeasurementsForDeletion(subject, agent, (AppdefEntityID[]) resources
                    .toArray(new AppdefEntityID[resources.size()]));
            }
        } catch (Exception e) {
            log.error("Error unscheduling measurements during async delete", e);
        }
    }
    
    private final void removeApplications(AuthzSubject subject, Collection<Application> applications) {
        for (Application application : applications) {
            try {
                applicationManager.removeApplication(subject, application.getId());
            } catch (Exception e) {
                log.error("Unable to remove application: " + e, e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Removed " + applications.size() + " applications");
        }
    }
    
    private final void removeResourceGroups(AuthzSubject subject, Collection<ResourceGroup> groups) {
        for (ResourceGroup group : groups) {
            try {
                resourceGroupManager.removeResourceGroup(subject, group.getId());
            } catch (Exception e) {
                log.error("Unable to remove group: " + e, e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Removed " + groups.size() + " resource groups");
        }
    }
    
    private void removePlatforms(AuthzSubject subject, Collection<Platform> platforms) {
        for (Platform platform : platforms) {
            try {
                //removeServers(subject, platform.getServers());
                appdefBoss.removePlatform(subject, platform.getId());
            } catch (Exception e) {
                log.error("Unable to remove platform: " + e, e);
            }
        }
    }
    
    private final void removeServers(AuthzSubject subject, Collection<Server> servers) {
        final StopWatch watch = new StopWatch();
        watch.markTimeBegin("removeServers");
        final List<Server> svrs = new ArrayList<Server>(servers);
        // can't use iterator for loop here. Since we are modifying the
        // internal hibernate collection, which this collection is based on,
        // it will throw a ConcurrentModificationException
        // This occurs even if you disassociate the Collection by trying
        // something like new ArrayList(servers). Not sure why.
        for (int i = 0; i < svrs.size(); i++) {
            try {
                final Server server = svrs.get(i);
                //removeServices(subject, server.getServices());
                appdefBoss.removeServer(subject, server.getId());
            } catch (Exception e) {
                log.error("Unable to remove server: " + e, e);
            }
        }
        watch.markTimeEnd("removeServers");
        if (log.isDebugEnabled()) {
            log.debug("Removed " + servers.size() + " services");
        }
    }
    
    private final void removeServices(AuthzSubject subject, Collection<Service> services) {
        final StopWatch watch = new StopWatch();
        watch.markTimeBegin("removeServices");
        final List<Service> svcs = new ArrayList<Service>(services);
        // can't use iterator for loop here. Since we are modifying the
        // internal hibernate collection, which this collection is based on,
        // it will throw a ConcurrentModificationException
        // This occurs even if you disassociate the Collection by trying
        // something like new ArrayList(services). Not sure why.
        for (int i = 0; i < svcs.size(); i++) {
            try {
                final Service service = svcs.get(i);
                appdefBoss.removeService(subject, service.getId());
            } catch (Exception e) {
                log.error("Unable to remove service: " + e, e);
            }
        }
        watch.markTimeEnd("removeServices");
        if (log.isDebugEnabled()) {
            log.debug("Removed " + services.size() + " services");
        }
    }


    public String toString() {
        return "ResourceCleanupEventListener";
    }
}