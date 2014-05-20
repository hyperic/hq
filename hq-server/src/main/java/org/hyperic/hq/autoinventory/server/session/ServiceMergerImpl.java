/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.autoinventory.server.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.autoinventory.server.session.RuntimeReportProcessor.ServiceMergeInfo;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.zevents.ZeventManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merges in services which have been discovered via runtime AI.
 * 
 * This class also has the responsibility of keeping state about which services
 * are in the queue, waiting to be processed, and notifying the agent that it
 * still needs to get a runtime service scan.
 */
@Component
public class ServiceMergerImpl implements ServiceMerger {

    private final Log log = LogFactory.getLog(ServiceMergerImpl.class);

    private CPropManager cPropManager;
    private ServiceManager serviceManager;
    private ServerManager serverManager;
    private ConfigManager configManager;
    private ResourceManager resourceManager;
    private AuthzSubjectManager authzSubjectManager;
    private SRNManager srnManager;

    @Autowired
    public ServiceMergerImpl(CPropManager cPropManager, ServiceManager serviceManager,
                             ServerManager serverManager, ConfigManager configManager,
                             ResourceManager resourceManager, SRNManager srnManager,
                             AuthzSubjectManager authzSubjectManager) {
        this.cPropManager = cPropManager;
        this.serviceManager = serviceManager;
        this.serverManager = serverManager;
        this.configManager = configManager;
        this.resourceManager = resourceManager;
        this.authzSubjectManager = authzSubjectManager;
        this.srnManager = srnManager;
    }

    @Transactional
    public void mergeServices(List<ServiceMergeInfo> mergeInfos) throws PermissionException, ApplicationException {
        final boolean debug = log.isDebugEnabled();
        final Set<Resource> updatedResources = new HashSet<Resource>();
        final Set<AppdefEntityID> toSchedule = new HashSet<AppdefEntityID>();
        AuthzSubject creator = null;
        for (ServiceMergeInfo sInfo : mergeInfos) {
            // this is hacky, but mergeInfos will never be called with multiple
            // subjects
            // and hence the method probably shouldn't be written the way it is
            // anyway.
            AIServiceValue aiservice = sInfo.aiservice;
            Server server = serverManager.getServerById(sInfo.serverId);
			if (server == null  || server.getResource() == null || server.getResource().isInAsyncDeleteState()) {
				continue;
			}

        	// HHQ-4646: The owner of the service should be the same
			// as the owner of the server
			creator = authzSubjectManager.findSubjectByName(server.getModifiedBy());

			if(log.isDebugEnabled()){
				log.debug("Checking for existing service: " + aiservice.getName());
			}
            // this is a propagation of a bug that nobody really runs into.
            // Occurs when a set of services under a server have the same name
            // and therefore the AIID is also the same. In a perfect world the
            // AIIDs will be unique, but there is nothing else that comes from
            // the agent that can uniquely identify a service under a server.
            // The get(0), instead of operating on the whole list, enables
            // us to make the least amount of code changes in a messy code path
            // thus reducing the amount of potential problems.
            final List<Service> tmp = serviceManager.getServicesByAIID(server, aiservice.getName());
            Service service = (tmp.size() > 0) ? (Service) tmp.get(0) : null;
            boolean update = false;

            if (service == null) {
                // CREATE SERVICE
                log.info("Creating new service: " + aiservice.getName());
                
                String typeName = aiservice.getServiceTypeName();
                ServiceType serviceType = serviceManager.findServiceTypeByName(typeName);
                service = serviceManager.createService(creator, server, serviceType,
                    aiservice.getName(), aiservice.getDescription(), "", null);
                log.debug("New service created: " + service);
            } else {
                final String aiSvcName = aiservice.getName();
                final String svcName = service.getName();
                final String aiid = service.getAutoinventoryIdentifier();
                // if aiid.equals(svcName) this means that the name has
                // not been manually changed. Therefore it is ok to change
                // the current resource name
                if (aiSvcName != null && !aiSvcName.equals(svcName) && aiid.equals(svcName)) {
                    // UPDATE SERVICE
                    update = true;
                    if(log.isDebugEnabled()){
                    	log.debug("Updating service: " + service.getName());
                    }
                    service.setName(aiservice.getName().trim());
                    service.getResource().setName(service.getName());
                }
                if (aiservice.getDescription() != null)
                    service.setDescription(aiservice.getDescription().trim());
            }

            // CONFIGURE SERVICE
            final boolean wasUpdated = configManager.configureResponse(creator, service.getConfigResponse(),
                                                                       service.getEntityId(),
                                                                       aiservice.getProductConfig(),
                                                                       aiservice.getMeasurementConfig(),
                                                                       aiservice.getControlConfig(),
                                                                       aiservice.getResponseTimeConfig(), null, false);
            if (update && wasUpdated) {
                updatedResources.add(service.getResource());
            }

            // SET CUSTOM PROPERTIES FOR SERVICE
            if (aiservice.getCustomProperties() != null) {
                int typeId = service.getServiceType().getId().intValue();
                cPropManager.setConfigResponse(service.getEntityId(), typeId, aiservice.getCustomProperties());
            }
        }
        if (!toSchedule.isEmpty()) {
            resourceManager.resourceHierarchyUpdated(creator, updatedResources);
            srnManager.scheduleInBackground(toSchedule, true, true);
        }
    }

    public String toString() {
        return "RuntimeAIServiceMerger";
    }

    public void scheduleServiceMerges(final String agentToken,
                                      final List<ServiceMergeInfo> serviceMerges) {
       
        List<MergeServiceReportZevent> evts = new ArrayList<MergeServiceReportZevent>(serviceMerges
            .size());

        for (ServiceMergeInfo sInfo : serviceMerges) {
            if (log.isDebugEnabled()) {
                log.debug("Enqueueing service merge for " + sInfo.aiservice.getName() +
                          " on server id=" + sInfo.serverId);
            }

            evts.add(new MergeServiceReportZevent(sInfo));
        }

        ZeventManager.getInstance().enqueueEventsAfterCommit(evts);
    }

}
