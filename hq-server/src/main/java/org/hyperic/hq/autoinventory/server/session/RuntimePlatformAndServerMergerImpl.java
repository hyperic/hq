/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.server.session.RuntimeReportProcessor.ServiceMergeInfo;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.hq.zevents.ZeventManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RuntimePlatformAndServerMergerImpl implements ApplicationContextAware, RuntimePlatformAndServerMerger {

    private final Log log = LogFactory.getLog(RuntimePlatformAndServerMergerImpl.class);

    private AgentManager agentManager;
    private ServiceMerger serviceMerger;
    private AuthzSubjectManager authzSubjectManager;
    private ProductManager productManager;
    private ApplicationContext applicationContext;

    @Autowired
    public RuntimePlatformAndServerMergerImpl(AgentManager agentManager, 
                                              ServiceMerger serviceMerger, AuthzSubjectManager authzSubjectManager,
                                              ProductManager productManager) {
        this.agentManager = agentManager;
        this.serviceMerger = serviceMerger;
        this.authzSubjectManager = authzSubjectManager;
        this.productManager = productManager;
    }

    /**
     * Merge platforms and servers from the runtime report.
     * 
     * @return a List of {@link ServiceMergeInfo} -- information from the report
     *         about services still needing to be processed
     */
    @Transactional
    public List<ServiceMergeInfo> mergePlatformsAndServers(String agentToken, CompositeRuntimeResourceReport crrr)
        throws ApplicationException, AutoinventoryException {
        AuthzSubject subject = getHQAdmin();

        RuntimeReportProcessor rrp = applicationContext.getBean(RuntimeReportProcessor.class);

        rrp.processRuntimeReport(subject, agentToken, crrr);
        mergeServiceTypes(rrp.getServiceTypeMerges());
        return rrp.getServiceMerges();
    }

    private void mergeServiceTypes(final Set<org.hyperic.hq.product.ServiceType> serviceTypeMerges) {
        if (!serviceTypeMerges.isEmpty()) {
            Map<String, Set<org.hyperic.hq.product.ServiceType>> productTypes = new HashMap<String, Set<org.hyperic.hq.product.ServiceType>>();
            for (org.hyperic.hq.product.ServiceType serviceType : serviceTypeMerges) {
                Set<org.hyperic.hq.product.ServiceType> serviceTypes = productTypes.get(serviceType.getProductName());
                if (serviceTypes == null) {
                    serviceTypes = new HashSet<org.hyperic.hq.product.ServiceType>();
                }
                serviceTypes.add(serviceType);
                log.info("Adding serviceType " + serviceType + " to product type: " + serviceType.getProductName());
                productTypes.put(serviceType.getProductName(), serviceTypes);
            }
            log.info("The size of productTypes: " + productTypes.size());
            for (Map.Entry<String, Set<org.hyperic.hq.product.ServiceType>> serviceTypeEntry : productTypes.entrySet()) {
                try {
                    log.info("Updating dynamic service type plugin");
                    productManager.updateDynamicServiceTypePlugin((String) serviceTypeEntry.getKey(), serviceTypeEntry
                        .getValue());
                } catch (Exception e) {
                    log.error("Error merging dynamic service types for product.  Cause: " + e.getMessage());
                }
            }
        }
    }

    private AuthzSubject getHQAdmin() throws AutoinventoryException {
        try {
            return authzSubjectManager.getSubjectById(AuthzConstants.rootSubjectId);
        } catch (Exception e) {
            throw new AutoinventoryException("Error looking up subject", e);
        }
    }

    public void schedulePlatformAndServerMerges(String agentToken, CompositeRuntimeResourceReport crrr) {
        MergePlatformAndServersZevent event = new MergePlatformAndServersZevent(agentToken, crrr);
        ZeventManager.getInstance().enqueueEventAfterCommit(event);
    }

    @Transactional
    public void reportAIRuntimeReport(String agentToken, CompositeRuntimeResourceReport crrr)
        throws AutoinventoryException, PermissionException, ValidationException, ApplicationException {
        List<ServiceMergeInfo> serviceMerges = mergePlatformsAndServers(agentToken, crrr);

        Agent a = agentManager.getAgent(agentToken);

        serviceMerger.scheduleServiceMerges(agentToken, serviceMerges);
    }

    public String toString() {
        return "RuntimePlatformAndServerMerger";
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
