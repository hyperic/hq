package org.hyperic.hq.autoinventory.server.session;

import java.util.ArrayList;
import java.util.List;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceType;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServiceManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.server.session.RuntimeReportProcessor.ServiceMergeInfo;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.zevents.ZeventManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private AgentReportStatusDAO agentReportStatusDao;
    private ServerManager serverManager;
    private ConfigManager configManager;
    private AgentManager agentManager;

    /**
     * Mapping of (String)agentToken onto (Integer) of # of events in the zevent
     * queue.
     */
    private final Cache _workingCache = CacheManager.getInstance().getCache("ServiceMergerWorking");

    @Autowired
    public ServiceMergerImpl(CPropManager cPropManager, ServiceManager serviceManager,
                             AgentReportStatusDAO agentReportStatusDao, ServerManager serverManager,
                             ConfigManager configManager, AgentManager agentManager) {
        this.cPropManager = cPropManager;
        this.serviceManager = serviceManager;
        this.agentReportStatusDao = agentReportStatusDao;
        this.serverManager = serverManager;
        this.configManager = configManager;
        this.agentManager = agentManager;
    }

    @Transactional
    public void mergeServices(List<ServiceMergeInfo> mergeInfos) throws PermissionException, ApplicationException {
        try {
            for (ServiceMergeInfo sInfo : mergeInfos) {
                AIServiceValue aiservice = sInfo.aiservice;
                Server server = serverManager.getServerById(sInfo.serverId);

                log.info("Checking for existing service: " + aiservice.getName());

                // this is a propagation of a bug that nobody really runs into.
                // Occurs when a set of services under a server have the same
                // name
                // and therefore the AIID is also the same. In a perfect world
                // the
                // AIIDs will be unique, but there is nothing else that comes
                // from
                // the agent that can uniquely identify a service under a
                // server.
                // The get(0), instead of operating on the whole list, enables
                // us to make the least amount of code changes in a messy code
                // path
                // thus reducing the amount of potential problems.
                final List<Service> tmp = serviceManager.getServicesByAIID(server, aiservice.getName());
                Service service = (tmp.size() > 0) ? (Service) tmp.get(0) : null;
                boolean update = false;

                if (service == null) {
                    // CREATE SERVICE
                    log.info("Creating new service: " + aiservice.getName());

                    String typeName = aiservice.getServiceTypeName();
                    ServiceType serviceType = serviceManager.findServiceTypeByName(typeName);
                    service = serviceManager.createService(sInfo.subject, server, serviceType, aiservice.getName(),
                        aiservice.getDescription(), "", null);

                    log.debug("New service created: " + service);
                } else {
                    update = true;
                    // UPDATE SERVICE
                    log.info("Updating service: " + service.getName());
                    final String aiSvcName = aiservice.getName();
                    final String svcName = service.getName();
                    final String aiid = service.getAutoinventoryIdentifier();
                    // if aiid.equals(svcName) this means that the name has
                    // not been manually changed. Therefore it is ok to change
                    // the current resource name
                    if (aiSvcName != null && !aiSvcName.equals(svcName) && aiid.equals(svcName)) {
                        service.setName(aiservice.getName().trim());
                        service.getResource().setName(service.getName());
                    }
                    if (aiservice.getDescription() != null)
                        service.setDescription(aiservice.getDescription().trim());
                }

                // CONFIGURE SERVICE
                configManager.configureResponse(sInfo.subject, service.getConfigResponse(), service.getEntityId(),
                    aiservice.getProductConfig(), aiservice.getMeasurementConfig(), aiservice.getControlConfig(),
                    aiservice.getResponseTimeConfig(), null, update, false);

                // SET CUSTOM PROPERTIES FOR SERVICE
                if (aiservice.getCustomProperties() != null) {
                    int typeId = service.getServiceType().getId().intValue();
                    cPropManager.setConfigResponse(service.getEntityId(), typeId, aiservice.getCustomProperties());
                }
            }
        } finally {
            for (ServiceMergeInfo sInfo : mergeInfos) {
                decrementWorkingCache(sInfo.agentToken);
            }
        }
    }

    @Transactional
    public void markServiceClean(String agentToken) {
        Agent a;

        try {
            a = agentManager.getAgent(agentToken);
        } catch (AgentNotFoundException e) {
            log.error("Agent [" + agentToken + "] not found");
            return;
        }

        markServiceClean(a, true);
    }

    @Transactional
    public void markServiceClean(Agent agent, boolean serviceClean) {

        AgentReportStatus status = agentReportStatusDao.getOrCreate(agent);
        if (serviceClean)
            status.markClean();
        else
            status.markDirty();
    }

    public void processEvents(List<MergeServiceReportZevent> events) {

    }

    private void incrementWorkingCache(String agentToken, int num) {
        log.debug("Adding " + num + " to agent [" + agentToken + "]'s merge queue");

        synchronized (_workingCache) {
            Element e = _workingCache.get(agentToken);

            if (e == null) {
                e = new Element(agentToken, new Integer(num));
                log.debug("Agent [" + agentToken + "] now has " + num + " elements in queue");
            } else {
                Integer val = (Integer) e.getValue();
                val = new Integer(val.intValue() + num);
                e = new Element(agentToken, val);
                log.debug("Agent [" + agentToken + "] now has " + val + " elements in queue");
            }
            _workingCache.put(e);
        }
    }

    public String toString() {
        return "RuntimeAIServiceMerger";
    }

    public void scheduleServiceMerges(final String agentToken, final List<ServiceMergeInfo> serviceMerges) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {   
            public void suspend() {
            }
            
            public void resume() {
            }
            
            public void flush() {
            }
            
            public void beforeCompletion() {
            }
            
            public void beforeCommit(boolean readOnly) {
            }
            
            public void afterCompletion(int status) {
            }
            
            public void afterCommit() {
                incrementWorkingCache(agentToken, serviceMerges.size());
            }
        });

        List<MergeServiceReportZevent> evts = new ArrayList<MergeServiceReportZevent>(serviceMerges.size());

        for (ServiceMergeInfo sInfo : serviceMerges) {
            if (log.isDebugEnabled()) {
                log.debug("Enqueueing service merge for " + sInfo.aiservice.getName() + " on server id=" +
                          sInfo.serverId);
            }

            evts.add(new MergeServiceReportZevent(sInfo));
        }

        ZeventManager.getInstance().enqueueEventsAfterCommit(evts);
    }

    private void decrementWorkingCache(String agentToken) {
        synchronized (_workingCache) {
            Element e = _workingCache.get(agentToken);

            if (e == null) {
                log.error("Expected to find element in working cache");
            } else {
                Integer ival = (Integer) e.getValue();
                int val = ival.intValue();

                if (val == 1) {
                    log.debug("Last event processed for agent [" + agentToken + " ] removing");
                    _workingCache.remove(agentToken);
                    markServiceClean(agentToken);
                } else {
                    val--;
                    e = new Element(agentToken, new Integer(val));
                    log.debug("Processed service for agent [" + agentToken + " ] numLeft= " + val);
                    _workingCache.put(e);
                }
            }
        }
    }

    public boolean currentlyWorkingOn(Agent a) {
        return _workingCache.isKeyInCache(a.getAgentToken());
    }
}
