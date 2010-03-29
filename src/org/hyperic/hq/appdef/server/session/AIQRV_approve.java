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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AIConversionUtil;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerLocal;
import org.hyperic.hq.appdef.shared.ServerValue;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.autoinventory.AIIp;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIServer;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;

/**
 * The AIQueueConstants.Q_DECISION_APPROVE means to add the queued
 * resource to the appdef model.  This visitor merges the queued resource
 * into appdef.
 */
public class AIQRV_approve implements AIQResourceVisitor {

    private static Log _log = LogFactory.getLog(AIQRV_approve.class);
    private static final int Q_STATUS_ADDED =
        AIQueueConstants.Q_STATUS_ADDED;
    private static final int Q_STATUS_CHANGED =
        AIQueueConstants.Q_STATUS_CHANGED;
    private static final int Q_STATUS_REMOVED =
        AIQueueConstants.Q_STATUS_REMOVED;
    private static final int Q_STATUS_PLACEHOLDER =
        AIQueueConstants.Q_STATUS_PLACEHOLDER;

    public AIQRV_approve () {}

    public void visitPlatform(AIPlatform aiplatform,
                              AuthzSubject subject,
                              PlatformManagerLocal pmLocal,
                              ConfigManagerLocal configMgr,
                              CPropManagerLocal cpropMgr,
                              List createdResources)
        throws AIQApprovalException, PermissionException {

        Integer id = aiplatform.getId();
        AppdefEntityID aid = null;

        _log.info("Visiting platform: " + id + " fqdn=" + aiplatform.getFqdn());
        AIPlatformValue aiplatformValue = aiplatform.getAIPlatformValue();
        Platform existingPlatform = getExistingPlatform(subject, pmLocal,
                                                        aiplatformValue);
        int qstat = aiplatform.getQueueStatus();
        switch (qstat) {
        case Q_STATUS_PLACEHOLDER:
            // We don't approve placeholders.  Just let them sit
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
                platform =
                    pmLocal.createPlatform(subject,
                                           aiplatform.getAIPlatformValue());
                aid = platform.getEntityId();
                setCustomProperties(aiplatform, platform, cpropMgr);
                _log.info("Created platform " + platform.getName() + " id="
                          + platform.getId());
                createdResources.add(platform);
            } catch (PermissionException e) {
                throw e;
            }
            catch (ValidationException e) {
                throw new AIQApprovalException("Error creating platform from " +
                                               "AI data.", e);
            }
            catch (ApplicationException e) {
                throw new AIQApprovalException(e);
            }

            catch (CreateException e) {
                throw new AIQApprovalException(e);
            }
            
            try {
                configMgr.configureResponse(subject,
                                            platform.getConfigResponse(), aid,
                                            aiplatform.getProductConfig(),
                                            aiplatform.getMeasurementConfig(),
                                            aiplatform.getControlConfig(),
                                            null, null, false);
            } catch (Exception e) {
                _log.warn("Error configuring platform: " + e, e);
            }

            break;

        case Q_STATUS_CHANGED:
            // This platform exists in the queue and in appdef.
            // We wish to sync the appdef attributes to match
            // the queue.
            
            // Check to make sure the platform is still in appdef.

            // Update existing platform attributes.
            try {
                pmLocal.updateWithAI(aiplatformValue, subject);
            } catch (Exception e) {
                throw new AIQApprovalException("Error updating platform using "
                                               + "AI data.", e);
            }

            setCustomProperties(aiplatform, existingPlatform, cpropMgr);

            if (aiplatformValue.isPlatformDevice()) {
                try {
                    configMgr.
                        configureResponse(subject,
                                          existingPlatform.getConfigResponse(),
                                          existingPlatform.getEntityId(),
                                          aiplatform.getProductConfig(),
                                          aiplatform.getMeasurementConfig(),
                                          aiplatform.getControlConfig(),
                                          null, null, false);
                    ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
                    rMan.resourceHierarchyUpdated(
                        subject, Collections.singletonList(existingPlatform.getResource()));
                } catch (Exception e) {
                    _log.warn("Error configuring platform: " + e, e);
                }
            }
            _log.info("Appdef platform updated.");
            break;

        case Q_STATUS_REMOVED:
            // This platform has been removed (in other words, AI no longer
            // detects it) however it is still present in the appdef model.
            // We wish to remove the appdef platform.

            // If the platform has already been removed, do nothing.
            if (existingPlatform == null) {
                _log.warn("Platform has already been removed, cannot " +
                          "remove aiplatform=" + id);
                return;
            }

            // Remove the platform
            try {
                pmLocal.removePlatform(subject, existingPlatform);
            } catch (PermissionException e) {
                throw e;
            } catch (Exception e) {
                throw new SystemException("Error removing platform using " +
                                          "AI data.", e);
            }
            break;

        default:
            _log.error("Unknown queue state: " + qstat);
            throw new SystemException("Unknown queue state: " + qstat);
        }
    }

    public void visitIp(AIIp aiip, AuthzSubject subject,
                        PlatformManagerLocal platformMan)
        throws AIQApprovalException, PermissionException
    {
        Platform platform = getExistingPlatform(
            subject, platformMan, aiip.getAIPlatform().getAIPlatformValue());
        if (platform == null) {
            _log.error("Error finding platform from AIIp Object, AIIpId=" +
                aiip.getId() + ", AIPlatformId=" + aiip.getAIPlatform().getId());
            return;
        }
        int qstat = aiip.getQueueStatus();
        switch (qstat) {
        case Q_STATUS_PLACEHOLDER:
            // Nothing to do
            break;
        case Q_STATUS_ADDED:
            platformMan.addIp(platform, aiip.getAddress(), aiip.getNetmask(),
                aiip.getMacAddress());
            break;
        case Q_STATUS_CHANGED:
            platformMan.updateIp(platform, aiip.getAddress(), aiip.getNetmask(),
                aiip.getMacAddress());
            break;
        case Q_STATUS_REMOVED:
            platformMan.removeIp(platform, aiip.getAddress(), aiip.getNetmask(),
                aiip.getMacAddress());
            break;
        default:
            _log.error("Unknown queue state: " + qstat);
            throw new SystemException("Unknown queue state: " + qstat);
        }
    }

    public void visitServer(AIServer aiserver,
                            AuthzSubject subject,
                            PlatformManagerLocal pmLocal,
                            ServerManagerLocal smLocal,
                            ConfigManagerLocal configMgr,
                            CPropManagerLocal cpropMgr,
                            List createdResources)
        throws AIQApprovalException, PermissionException
    {
        AIPlatform aiplatform = aiserver.getAIPlatform();
        AIPlatformValue aiplatformValue = aiplatform.getAIPlatformValue();
        // Get the aiplatform for this server
        Platform existingPlatform = getExistingPlatform(subject, pmLocal,
                                                        aiplatformValue);
        int qstat = aiserver.getQueueStatus();
        switch (qstat) {
        case Q_STATUS_PLACEHOLDER:
            // We don't approve placeholders.  Just let them sit
            // in the queue.
            break;
        case Q_STATUS_ADDED:
            handleStatusAdded(subject, existingPlatform, aiplatformValue,
                aiserver, configMgr, cpropMgr, createdResources,
                smLocal);
            break;
        case Q_STATUS_CHANGED:
            handleStatusChanged(subject, existingPlatform, aiplatformValue,
                aiserver, configMgr, cpropMgr, createdResources,
                smLocal);
            break;
        case Q_STATUS_REMOVED:
            handleStatusRemoved(subject, existingPlatform, aiplatformValue,
                aiserver, configMgr, cpropMgr, createdResources,
                smLocal);
            break;
        default:
            _log.error("Unknown queue state: " + qstat);
            throw new SystemException("Unknown queue state: " + qstat);
        }
    }
    
    private void handleStatusRemoved(AuthzSubject subject, Platform platform,
                                     AIPlatformValue aiplatformValue,
                                     AIServer aiserver,
                                     ConfigManagerLocal configMgr,
                                     CPropManagerLocal cpropMgr,
                                     List createdResources,
                                     ServerManagerLocal smLocal)
        throws PermissionException
    {
        // If the platform has already been removed, do nothing.
        if (platform == null) {
            _log.warn("Platform has already been removed, cannot " +
                      "remove aiserver=" + aiserver.getId());
            return;
        }
        // This server has been removed (in other words, AI no longer
        // detects it) however it is still present in the appdef model.
        // We wish to remove the appdef platform.
        try {
            Server server = getExistingServer(subject, platform, aiserver, smLocal);
            if (server == null) {
                // Server has already been removed, return.
                _log.warn("Server has already been removed, cannot " +
                          "remove aiserver=" + aiserver.getId());
            }
            _log.info("Removing Server...");
            smLocal.removeServer(subject, server);
       } catch (PermissionException e) {
           throw e;
       } catch (Exception e) {
           throw new SystemException("Error updating platform to remove" +
                                     " server, using AIServer data.", e);
       }
    }

    private void handleStatusChanged(AuthzSubject subject,
                                     Platform platform,
                                     AIPlatformValue aiplatformValue,
                                     AIServer aiserver,
                                     ConfigManagerLocal configMgr,
                                     CPropManagerLocal cpropMgr,
                                     List createdResources,
                                     ServerManagerLocal smLocal)
        throws PermissionException, AIQApprovalException
    {
        if (platform == null) {
            throw new AIQApprovalException("HQ Platform does not exist for" +
                                           " AI Platform ID " +
                                           aiplatformValue.getId());
        }
        // This server exists in the queue and in appdef.
        // We wish to sync the appdef attributes to match
        // the queue.
        try {
            Server server = getExistingServer(subject, platform, aiserver, smLocal);
            if (server == null) {
                // XXX scottmf probably should not blow up here
                // better to change status to added from changed??
                throw new AIQApprovalException("Server id " + aiserver.getId() +
                                               " not found");
            }
            ServerValue serverValue = server.getServerValue();
            AIServerValue aiserverValue = aiserver.getAIServerValue();
            serverValue = AIConversionUtil.mergeAIServerIntoServer(
                aiserverValue, serverValue);
            Server updated = smLocal.updateServer(subject, serverValue);
            try {
                configMgr.configureResponse(subject, server.getConfigResponse(),
                                            serverValue.getEntityId(),
                                            aiserver.getProductConfig(),
                                            aiserver.getMeasurementConfig(),
                                            aiserver.getControlConfig(),
                                            null, null, false);
                ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
                rMan.resourceHierarchyUpdated(
                    subject, Collections.singletonList(server.getResource()));
            } catch (Exception configE) {
                _log.warn("Error configuring server: " + configE, configE);
            }
            setCustomProperties(aiserver, updated, cpropMgr);
            _log.info("Updated server (" + serverValue.getId() + "): " +
                      serverValue);
        } catch (PermissionException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("Error updating platform with " +
                                      "new AIServer data.", e);
        }
    }

    private void handleStatusAdded(AuthzSubject subject,
                                   Platform platform,
                                   AIPlatformValue aiplatformValue,
                                   AIServer aiserver,
                                   ConfigManagerLocal configMgr,
                                   CPropManagerLocal cpropMgr,
                                   List createdResources,
                                   ServerManagerLocal smLocal)
        throws AIQApprovalException, PermissionException
    {
        // If the platform does not exist in appdef, throw an exception
        if (platform == null) {
            throw new AIQApprovalException("HQ Platform does not exist for" +
                                           " AI Platform ID " +
                                           aiplatformValue.getId());
        }
        // This server exists in the queue but not in appdef,
        // so add it to appdef.
        try {
            // Before we add it, make sure it's not already there...
            Server server = getExistingServer(subject, platform, aiserver, smLocal);
            if (server != null) {
                // remove the server if it already exists.
                // probably shouldn't get into the AIQ to begin with but
                // we need to handle it if that is the case.
                removeChild(aiserver.getAIPlatform(), aiserver);
                return;
            }
            AIServerValue aiserverValue = aiserver.getAIServerValue();
            ServerValue serverValue = AIConversionUtil.convertAIServerToServer(
                aiserverValue, smLocal);
            serverValue = AIConversionUtil.mergeAIServerIntoServer(
                aiserverValue, serverValue);

            Integer serverTypePK = serverValue.getServerType().getId();
            server = smLocal.createServer(subject, platform.getId(),
                                          serverTypePK, serverValue);

            try {
                configMgr.configureResponse(subject, server.getConfigResponse(),
                                            server.getEntityId(),
                                            aiserver.getProductConfig(),
                                            aiserver.getMeasurementConfig(),
                                            aiserver.getControlConfig(),
                                            null, /* RT config */ null, false);
            } catch (Exception e) {
                _log.warn("Error configuring server: " + e, e);
            }

            setCustomProperties(aiserver, server, cpropMgr);
            
            createdResources.add(server);
            _log.info("Created server (" + serverValue.getId() + "): " +
                      serverValue);
        } catch (PermissionException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("Error creating platform from " +
                                      "AI data: " + e.getMessage(), e);
        }
    }
    
    private void removeChild(AIPlatform aiPlatform, AIServer server) {
        for (Iterator it=aiPlatform.getAIServers().iterator(); it.hasNext(); ) {
            AIServer aiserver = (AIServer)it.next();
            if (aiserver.getId().equals(server.getId())) {
                it.remove();
            }
        }
    }

    private Server getExistingServer(AuthzSubject subject,
                                     Platform platform,
                                     AIServer aiserver,
                                     ServerManagerLocal smLocal)
        throws PermissionException, FinderException {
        Server server = smLocal.findServerByAIID(
            subject, platform, aiserver.getAutoinventoryIdentifier());
        if (server != null || aiserver.getAIPlatform().isPlatformDevice()) {
            return server;
        }
        ServerType serverType =
            smLocal.findServerTypeByName(aiserver.getServerTypeName());
        // if (virtual == true) for this server type that means that there may
        // only be one server per platform of this type
        if (false == serverType.isVirtual()) {
            return null;
        }
        List servers = smLocal.findServersByType(platform, serverType);
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
                                         PlatformManagerLocal pmLocal,
                                         AIPlatformValue aiplatform) {
        try {
            return pmLocal.getPlatformByAIPlatform(subject, aiplatform);
        } catch (PermissionException e) {
            throw new SystemException(e);
        }
    }

    private static void setCustomProperties(AIPlatform aiplatform,
                                            Platform platform,
                                            CPropManagerLocal cpropMgr) {
        try {
            int typeId =
                platform.getPlatformType().getId().intValue();
            cpropMgr.setConfigResponse(platform.getEntityId(),
                                       typeId,
                                       aiplatform.getCustomProperties());
        } catch (Exception e) {
            _log.warn("Error setting platform custom properties: " + e, e);
        }
    }

    private static void setCustomProperties(AIServer aiserver,
                                            Server server,
                                            CPropManagerLocal cpropMgr) {
        try {
            int typeId = server.getServerType().getId().intValue();
            cpropMgr.setConfigResponse(server.getEntityId(), typeId,
                                       aiserver.getCustomProperties());
        } catch (Exception e) {
            _log.warn("Error setting server custom properties: " + e, e);
        }
    }
}
