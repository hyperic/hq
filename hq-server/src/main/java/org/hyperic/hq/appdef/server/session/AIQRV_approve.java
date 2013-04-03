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

package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AIConversionUtil;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.CPropKeyNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.autoinventory.AIIp;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIServer;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.vm.VCManager;
import org.hyperic.hq.vm.VMID;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * The AIQueueConstants.Q_DECISION_APPROVE means to add the queued resource to
 * the appdef model. This visitor merges the queued resource into appdef.
 */
@Component
public class AIQRV_approve implements AIQResourceVisitor, ApplicationContextAware {

    private final Log log = LogFactory.getLog(AIQRV_approve.class);
    private static final int Q_STATUS_ADDED = AIQueueConstants.Q_STATUS_ADDED;
    private static final int Q_STATUS_CHANGED = AIQueueConstants.Q_STATUS_CHANGED;
    private static final int Q_STATUS_REMOVED = AIQueueConstants.Q_STATUS_REMOVED;
    private static final int Q_STATUS_PLACEHOLDER = AIQueueConstants.Q_STATUS_PLACEHOLDER;
    private PlatformManager platformManager;
    private ConfigManager configMgr;
    private CPropManager cpropMgr;
    private ServerManager serverManager;
    private ResourceManager resourceManager;
    private ApplicationContext appContext;
    
    @Autowired
    public AIQRV_approve(PlatformManager platformManager, ConfigManager configMgr,
                         CPropManager cpropMgr, ServerManager serverManager,
                         ResourceManager resourceManager) {
        this.platformManager = platformManager;
        this.configMgr = configMgr;
        this.cpropMgr = cpropMgr;
        this.serverManager = serverManager;
        this.resourceManager = resourceManager;
    }
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }


    public void visitPlatform(AIPlatform aiplatform, AuthzSubject subject, List createdResources)
        throws AIQApprovalException, PermissionException {

        Integer id = aiplatform.getId();
        AppdefEntityID aid = null;

        log.info("Visiting platform: " + id + " fqdn=" + aiplatform.getFqdn());
        AIPlatformValue aiplatformValue = aiplatform.getAIPlatformValue();
        Platform existingPlatform = getExistingPlatform(subject, aiplatformValue);
        int qstat = aiplatform.getQueueStatus();
        switch (qstat) {
            case Q_STATUS_PLACEHOLDER:
                // We don't approve placeholders. Just let them sit
                // in the queue.
                break;

            case Q_STATUS_ADDED:
                // This platform exists in the queue but not in appdef,
                // so add it to appdef.

                if (existingPlatform != null) {

                    throw new AIQApprovalException("Platform already added at id " +
                                                   existingPlatform.getId());
                }
                // Add the AI platform to appdef
                Platform platform;
                try {
                    platform = platformManager.createPlatform(subject, aiplatform
                        .getAIPlatformValue());
                    aid = platform.getEntityId();
                    setCustomProperties(aiplatform, platform);
                    log.info("Created platform " + platform.getName() + " id=" + platform.getId());
                    createdResources.add(platform);
                } catch (PermissionException e) {
                    throw e;
                } catch (ValidationException e) {
                    throw new AIQApprovalException("Error creating platform from " + "AI data.", e);
                } catch (ApplicationException e) {
                    throw new AIQApprovalException(e);
                }

                try {
                    configMgr.configureResponse(subject, platform.getConfigResponse(), aid,
                        aiplatform.getProductConfig(), aiplatform.getMeasurementConfig(),
                        aiplatform.getControlConfig(), null, null, false);
                } catch (Exception e) {
                    log.warn("Error configuring platform: " + e, e);
                }

                break;

            case Q_STATUS_CHANGED:
                // This platform exists in the queue and in appdef.
                // We wish to sync the appdef attributes to match
                // the queue.

                // Check to make sure the platform is still in appdef.

                // Update existing platform attributes.
                try {
                    platformManager.updateWithAI(aiplatformValue, subject);
                } catch (Exception e) {
                    throw new AIQApprovalException("Error updating platform using " + "AI data.", e);
                }

                setCustomProperties(aiplatform, existingPlatform);

                if (aiplatformValue.isPlatformDevice()) {
                    try {
                        configMgr.configureResponse(subject, existingPlatform.getConfigResponse(),
                            existingPlatform.getEntityId(), aiplatform.getProductConfig(),
                            aiplatform.getMeasurementConfig(), aiplatform.getControlConfig(), null,
                            null, false);
                         resourceManager.resourceHierarchyUpdated(
                             subject, Collections.singletonList(existingPlatform.getResource()));
                    } catch (Exception e) {
                        log.warn("Error configuring platform: " + e, e);
                    }
                }
                log.info("Appdef platform updated.");
                break;

            case Q_STATUS_REMOVED:
                // This platform has been removed (in other words, AI no longer
                // detects it) however it is still present in the appdef model.
                // We wish to remove the appdef platform.

                // If the platform has already been removed, do nothing.
                if (existingPlatform == null) {
                    log.warn("Platform has already been removed, cannot " + "remove aiplatform=" +
                              id);
                    return;
                }

                // Remove the platform
                try {
                    platformManager.removePlatform(subject, existingPlatform);
                } catch (PermissionException e) {
                    throw e;
                } catch (Exception e) {
                    throw new SystemException("Error removing platform using " + "AI data.", e);
                }
                break;

            default:
                log.error("Unknown queue state: " + qstat);
                throw new SystemException("Unknown queue state: " + qstat);
        }
    }

    public void visitIp(AIIp aiip, AuthzSubject subject) throws AIQApprovalException,
        PermissionException {
        Platform platform = getExistingPlatform(subject, aiip.getAIPlatform().getAIPlatformValue());
        if (platform == null) {
            log.error("Error finding platform from AIIp Object, AIIpId=" + aiip.getId() +
                       ", AIPlatformId=" + aiip.getAIPlatform().getId());
            return;
        }
        int qstat = aiip.getQueueStatus();
        switch (qstat) {
            case Q_STATUS_PLACEHOLDER:
                // Nothing to do
                break;
            case Q_STATUS_ADDED:
                platformManager.addIp(platform, aiip.getAddress(), aiip.getNetmask(), aiip
                    .getMacAddress());
                break;
            case Q_STATUS_CHANGED:
                platformManager.updateIp(platform, aiip.getAddress(), aiip.getNetmask(), aiip
                    .getMacAddress());
                break;
            case Q_STATUS_REMOVED:
                platformManager.removeIp(platform, aiip.getAddress(), aiip.getNetmask(), aiip
                    .getMacAddress());
                break;
            default:
                log.error("Unknown queue state: " + qstat);
                throw new SystemException("Unknown queue state: " + qstat);
        }
    }

    public void visitServer(AIServer aiserver, AuthzSubject subject, List createdResources)
        throws AIQApprovalException, PermissionException {
        AIPlatform aiplatform = aiserver.getAIPlatform();
        AIPlatformValue aiplatformValue = aiplatform.getAIPlatformValue();
        // Get the aiplatform for this server
        Platform existingPlatform = getExistingPlatform(subject, aiplatformValue);
        int qstat = aiserver.getQueueStatus();
        switch (qstat) {
            case Q_STATUS_PLACEHOLDER:
                // We don't approve placeholders. Just let them sit
                // in the queue.
                break;
            case Q_STATUS_ADDED:
                handleStatusAdded(subject, existingPlatform, aiplatformValue, aiserver,
                    createdResources);
                break;
            case Q_STATUS_CHANGED:
                handleStatusChanged(subject, existingPlatform, aiplatformValue, aiserver,
                    createdResources);
                break;
            case Q_STATUS_REMOVED:
                handleStatusRemoved(subject, existingPlatform, aiplatformValue, aiserver,
                    createdResources);
                break;
            default:
                log.error("Unknown queue state: " + qstat);
                throw new SystemException("Unknown queue state: " + qstat);
        }
    }

    private void handleStatusRemoved(AuthzSubject subject, Platform platform,
                                     AIPlatformValue aiplatformValue, AIServer aiserver,

                                     List createdResources) throws PermissionException {
        // If the platform has already been removed, do nothing.
        if (platform == null) {
            log.warn("Platform has already been removed, cannot " + "remove aiserver=" +
                      aiserver.getId());
            return;
        }
        // This server has been removed (in other words, AI no longer
        // detects it) however it is still present in the appdef model.
        // We wish to remove the appdef platform.
        try {
            Server server = getExistingServer(subject, platform, aiserver);
            if (server == null) {
                // Server has already been removed, return.
                log.warn("Server has already been removed, cannot " + "remove aiserver=" +
                          aiserver.getId());
            }
            log.info("Removing Server...");
            serverManager.removeServer(subject, server);
        } catch (PermissionException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("Error updating platform to remove"
                                      + " server, using AIServer data.", e);
        }
    }

    private void handleStatusChanged(AuthzSubject subject, Platform platform,
                                     AIPlatformValue aiplatformValue, AIServer aiserver,

                                     List createdResources) throws PermissionException,
        AIQApprovalException {
        if (platform == null) {
            throw new AIQApprovalException("HQ Platform does not exist for" + " AI Platform ID " +
                                           aiplatformValue.getId());
        }
        // This server exists in the queue and in appdef.
        // We wish to sync the appdef attributes to match
        // the queue.
        try {
            Server server = getExistingServer(subject, platform, aiserver);
            if (server == null) {
                // just ignore this and move on.
                return;
            }
            ServerValue serverValue = server.getServerValue();
            AIServerValue aiserverValue = aiserver.getAIServerValue();
            serverValue = AIConversionUtil.mergeAIServerIntoServer(aiserverValue, serverValue);
            Server updated = serverManager.updateServer(subject, serverValue);
            try {
                configMgr.configureResponse(subject, server.getConfigResponse(), serverValue
                    .getEntityId(), aiserver.getProductConfig(), aiserver.getMeasurementConfig(),
                    aiserver.getControlConfig(), null, null, false);
                resourceManager.resourceHierarchyUpdated(
                    subject, Collections.singletonList(server.getResource()));
            } catch (Exception configE) {
                log.warn("Error configuring server: " + configE, configE);
            }
            setCustomProperties(aiserver, updated);
            log.info("Updated server (" + serverValue.getId() + "): " + serverValue);
        } catch (PermissionException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("Error updating platform with " + "new AIServer data.", e);
        }
    }

    private void handleStatusAdded(AuthzSubject subject, Platform platform,
                                   AIPlatformValue aiplatformValue, AIServer aiserver,

                                   List createdResources) throws AIQApprovalException,
        PermissionException {
        // If the platform does not exist in appdef, throw an exception
        if (platform == null) {
            throw new AIQApprovalException("HQ Platform does not exist for" + " AI Platform ID " +
                                           aiplatformValue.getId());
        }
        // This server exists in the queue but not in appdef,
        // so add it to appdef.
        try {
            // Before we add it, make sure it's not already there...
            Server server = getExistingServer(subject, platform, aiserver);
            if (server != null) {
                // remove the server if it already exists.
                // probably shouldn't get into the AIQ to begin with but
                // we need to handle it if that is the case.
                removeChild(aiserver.getAIPlatform(), aiserver);
                return;
            }
            AIServerValue aiserverValue = aiserver.getAIServerValue();
            ServerValue serverValue = AIConversionUtil.convertAIServerToServer(aiserverValue,
                serverManager);
            serverValue = AIConversionUtil.mergeAIServerIntoServer(aiserverValue, serverValue);

            Integer serverTypePK = serverValue.getServerType().getId();
            server = serverManager.createServer(subject, platform.getId(), serverTypePK,
                serverValue);

            try {
                configMgr.configureResponse(subject, server.getConfigResponse(), server
                    .getEntityId(), aiserver.getProductConfig(), aiserver.getMeasurementConfig(),
                    aiserver.getControlConfig(), null, /* RT config */
                    null, false);
            } catch (Exception e) {
                log.warn("Error configuring server: " + e, e);
            }

            setCustomProperties(aiserver, server);

            createdResources.add(server);
            log.info("Created server (" + serverValue.getId() + "): " + serverValue);
        } catch (PermissionException e) {
            throw e;
        } catch (NotFoundException e) {
            // just ignore and keep moving, this could happen if a plugin was removed from HQ
            log.warn(e);
            log.debug(e,e);
        } catch (Exception e) {
            throw new SystemException("Error creating platform from " + "AI data: " +
                                      e.getMessage(), e);
        }
    }

    private void removeChild(AIPlatform aiPlatform, AIServer server) {
        for (Iterator it = aiPlatform.getAIServers().iterator(); it.hasNext();) {
            AIServer aiserver = (AIServer) it.next();
            if (aiserver.getId().equals(server.getId())) {
                it.remove();
            }
        }
    }

    private Server getExistingServer(AuthzSubject subject, Platform platform, AIServer aiserver)
        throws PermissionException, NotFoundException {
        Server server = serverManager.findServerByAIID(subject, platform, aiserver
            .getAutoinventoryIdentifier());
        if (server != null || aiserver.getAIPlatform().isPlatformDevice()) {
            return server;
        }
        ServerType serverType = serverManager.findServerTypeByName(aiserver.getServerTypeName());
        // if (virtual == true) for this server type that means that there may
        // only be one server per platform of this type
        if (false == serverType.isVirtual()) {
            return null;
        }
        List servers = serverManager.findServersByType(platform, serverType);
        // servers.size() > 1 must be false if
        // serverType.isVirtual() == true
        // Unfortunately this is not enforced anywhere so we should do something
        // to handle it just in case
        if (servers.size() > 0) {
            return (Server) servers.get(0);
        }
        return null;
    }

    private Platform getExistingPlatform(AuthzSubject subject,

    AIPlatformValue aiplatform) {
        try {
            return platformManager.getPlatformByAIPlatform(subject, aiplatform);
        } catch (PermissionException e) {
            throw new SystemException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void setCustomProperties(AIPlatform aiplatform, Platform platform) {
        try {
            int typeId = platform.getPlatformType().getId().intValue();
            Collection<AIIp> ips = aiplatform.getAIIps();
            List<String> macs = new ArrayList<String>(ips.size());
            if (ips!=null) {
                for(AIIp ip:ips) {
                    String mac = ip.getMacAddress();
                    if (mac!=null && !mac.isEmpty() && !mac.equals("")) {
                        macs.add(mac);
                    }
                }
            }
                        
            ConfigResponse cprops = null;

            try {
                cprops = ConfigResponse.decode(aiplatform.getCustomProperties());
            } catch (EncodingException e) {
                throw new SystemException(e);
            }

            if (macs!=null) {
                VMID vmid = appContext.getBean(VCManager.class).getVMID(macs);
                if (vmid!=null) {
                    cprops.setValue(HQConstants.MOID, vmid.getMoref());
                    cprops.setValue(HQConstants.VCUUID, vmid.getVcUUID());
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("cprops=" + cprops);
                log.debug("aID=" + platform.getEntityId().toString() + ", typeId=" + typeId);
            }

            for (Iterator<String> it = cprops.getKeys().iterator(); it.hasNext();) {
                String key = it.next();
                String val = cprops.getValue(key);

                try {
                    cpropMgr.setValue(platform.getEntityId(), typeId, key, val);
                } catch (CPropKeyNotFoundException e) {
                    log.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Error setting platform custom properties: " + e, e);
        }
    }

    private void setCustomProperties(AIServer aiserver, Server server) {
        try {
            int typeId = server.getServerType().getId().intValue();
            cpropMgr
                .setConfigResponse(server.getEntityId(), typeId, aiserver.getCustomProperties());
        } catch (Exception e) {
            log.warn("Error setting server custom properties: " + e, e);
        }
    }
}
