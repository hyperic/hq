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

package org.hyperic.hq.appdef.server.session;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Properties;

import javax.ejb.FinderException;

import org.apache.commons.logging.Log;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.autoinventory.AICompare;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.CPropManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigResponseValue;
import org.hyperic.hq.appdef.shared.PlatformLocal;
import org.hyperic.hq.appdef.shared.IpLocal;
import org.hyperic.hq.appdef.shared.PlatformPK;
import org.hyperic.hq.appdef.shared.PlatformTypePK;
import org.hyperic.hq.appdef.shared.ServerLocal;
import org.hyperic.hq.appdef.shared.ServerPK;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.PlatformLocalHome;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.ServerTypePK;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

/**
 * A utility class for calculating queue status and diff for an AIPlatform.
 */
public class AI2AppdefDiff {

    public AI2AppdefDiff () {}

    /**
     * @param aiplatform The AI platform data, including nested IPs and servers.
     * @return A new AI platform value object, with queuestatus and diff set 
     * correctly (including for nested IPs and servers), and only containing the
     * set of IPs and servers that should be queued (IPs and servers that are 
     * already identical to those in appdef are removed from the value object).  
     */
    public AIPlatformValue diffAgainstAppdef ( Log log,
                                               AuthzSubjectValue subject,
                                               PlatformLocalHome pmLH,
                                               ConfigManagerLocal cmLocal,
                                               CPropManagerLocal cpropMgr,
                                               AIPlatformValue aiplatform ) {
        PlatformLocal appdefPlatform;
        AIPlatformValue revisedAIplatform;
        int i;

        if (log.isDebugEnabled()) {
            log.debug("ai2appdef diff: AIPLATFORM=" + aiplatform);
            log.debug("ai2appdef diff: at start:=" + StringUtil.arrayToString(aiplatform.getAIIpValues()));
        }

        // We know we'll at least need to copy all the platform-level attributes
        revisedAIplatform = new AIPlatformValue();
        revisedAIplatform.setId(aiplatform.getId());
        revisedAIplatform.setPlatformTypeName(aiplatform.getPlatformTypeName());
        revisedAIplatform.setIgnored(aiplatform.getIgnored());
        revisedAIplatform.setCertdn(aiplatform.getCertdn());
        revisedAIplatform.setFqdn(aiplatform.getFqdn());
        revisedAIplatform.setDescription(aiplatform.getDescription());
        revisedAIplatform.setName(aiplatform.getName());
        revisedAIplatform.setCTime(aiplatform.getCTime());
        revisedAIplatform.setMTime(aiplatform.getMTime());
        revisedAIplatform.setLastApproved(aiplatform.getLastApproved());
        revisedAIplatform.setAgentToken(aiplatform.getAgentToken());
        revisedAIplatform.setCpuCount(aiplatform.getCpuCount());
        revisedAIplatform.setCustomProperties(aiplatform.getCustomProperties());
        revisedAIplatform.setProductConfig(aiplatform.getProductConfig());
        revisedAIplatform.setMeasurementConfig(aiplatform.getMeasurementConfig());
        revisedAIplatform.setControlConfig(aiplatform.getControlConfig());

        // Initially, set platform status to PLACEHOLDER
        revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_PLACEHOLDER);
        revisedAIplatform.setDiff(AIQueueConstants.Q_DIFF_NONE);

        // Get the appdef platform
        appdefPlatform = getAppdefPlatform(log, pmLH, subject, aiplatform);

        // If there was no appdef platform...
        if ( appdefPlatform == null ) {
            // If the aiplatform has status "removed", then appdef model is
            // correct and the platform has actually been removed.  In this
            // case we return null, which notifies the caller of this condition.
            if ( aiplatform.getQueueStatus() == AIQueueConstants.Q_STATUS_REMOVED ) {
                return null;
            }

            // Otherwise, recursively mark everything as new, copying 
            // IPs and servers.
            revisedAIplatform = aiplatform;
            revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_ADDED);

            // All scanned IPs must be new
            AIIpValue[] newIps = aiplatform.getAIIpValues();
            revisedAIplatform.removeAllAIIpValues();
            for ( i=0; i<newIps.length; i++ ) {
                newIps[i].setQueueStatus(AIQueueConstants.Q_STATUS_ADDED);
                revisedAIplatform.addAIIpValue(newIps[i]);
            }
 
            // All scanned servers must be new
            AIServerValue[] newServers = aiplatform.getAIServerValues();
            revisedAIplatform.removeAllAIServerValues();
            for ( i=0; i<newServers.length; i++ ) {
                newServers[i].setQueueStatus(AIQueueConstants.Q_STATUS_ADDED);
                revisedAIplatform.addAIServerValue(newServers[i]);
            }
            
            return revisedAIplatform;
        }

        //when scans are run for a device platform, only the fqdn and ipaddress
        //are available, so we keep the other platform and ip attributes as
        //they already are in appdef (same as the user entered by hand)
        boolean isDevice = revisedAIplatform.isPlatformDevice();
        
        if (isDevice) {
            log.info("Applying existing appdef attributes for device " +
                     aiplatform.getPlatformTypeName() + "=" + aiplatform.getFqdn());
            if (revisedAIplatform.getCpuCount() == null) {
                revisedAIplatform.setCpuCount(appdefPlatform.getCpuCount());
            }
            
            ConfigResponseValue crValue;
            try {
                crValue =
                    cmLocal.getConfigResponseValue(appdefPlatform.getEntityId());
            } catch (AppdefEntityNotFoundException e) {
                // Should not happen, unless the platform was deleted since
                // we just looked it up moments ago.
                throw new SystemException(e);
            }
            //if the plugin did not set a config, apply the existing config.
            if (revisedAIplatform.getProductConfig() == null) {
                revisedAIplatform.setProductConfig(crValue.getProductResponse());
            }
            if (revisedAIplatform.getControlConfig() == null) {
                revisedAIplatform.setControlConfig(crValue.getControlResponse());
            }
            if (revisedAIplatform.getMeasurementConfig() == null) {
                revisedAIplatform.setMeasurementConfig(crValue.getMeasurementResponse());
            }

            //XXX might want to do this for all platforms, just checking devices for now.
            if (!configsEqual(revisedAIplatform.getProductConfig(),
                              crValue.getProductResponse()) ||
                !configsEqual(revisedAIplatform.getControlConfig(),
                              crValue.getControlResponse()) ||
                !configsEqual(revisedAIplatform.getMeasurementConfig(),
                              crValue.getMeasurementResponse()))
            {
                revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_PROPERTIES_CHANGED);
                log.info("ConfigResponse changed for " + aiplatform.getFqdn() +
                         " '" + aiplatform.getPlatformTypeName() + "'");
            }
        }

        // Otherwise, there was an appdef platform that matched, so we
        // go through and compare IPs, servers, and finally the platform
        // attributes.

        // Compare IPs
        if (log.isDebugEnabled())
            log.debug("ai2appdef diff: before IP diff:=" +
                      StringUtil.arrayToString(revisedAIplatform.getAIIpValues()));
        doIpDiffs(log, appdefPlatform, aiplatform, revisedAIplatform, isDevice);
        if (log.isDebugEnabled())
            log.debug("ai2appdef diff: after IP diff:=" +
                      StringUtil.arrayToString(revisedAIplatform.getAIIpValues()));
        
        // Compare servers
        doServerDiffs(log, appdefPlatform, cmLocal, 
                      cpropMgr, aiplatform, revisedAIplatform);

        // Compare platform attributes
        doPlatformAttrDiff(log, appdefPlatform, revisedAIplatform);

        if (aiplatform.customPropertiesHasBeenSet()) {
            int id =
                ((PlatformPK)appdefPlatform.getPrimaryKey()).getId().intValue();
            AppdefEntityID aid = 
                new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_PLATFORM, id);
            int type =
                ((PlatformTypePK)appdefPlatform.getPlatformType().getPrimaryKey()).
                    getId().intValue();
            updateCprops(log, cpropMgr, aid, type,
                         aiplatform.getCustomProperties());
        }

        return revisedAIplatform;
    }

    private void doIpDiffs ( Log log,
                             PlatformLocal appdefPlatform,
                             AIPlatformValue aiPlatform,
                             AIPlatformValue revisedAIplatform,
                             boolean isDevice ) {

        // Compare IP addresses between appdef and AI data.
        // We iterate over the IPs in the AI data, removing them from
        // the appdef list as we find them.  In the end, the IPs that are
        // left in appdef but not in the AI data are the IPs that have been
        // removed from the platform.
        List appdefIps  = new ArrayList();
        appdefIps.addAll(appdefPlatform.getIps());
        List scannedIps = new ArrayList();
        scannedIps.addAll(Arrays.asList(aiPlatform.getAIIpValues()));
        if (log.isDebugEnabled())
            log.debug("AI2AppdefDiff: doIpDiffs:" +
                     " appdefIps=" + StringUtil.listToString(appdefIps) +
                     " scannedIps=" + StringUtil.listToString(scannedIps));
        IpLocal appdefIp = null;
        AIIpValue scannedIp = null;
        Iterator i = scannedIps.iterator();
        while ( i.hasNext() ) {
            scannedIp = (AIIpValue) i.next();
            appdefIp = findAndRemoveAppdefIp(scannedIp.getAddress(), appdefIps);

            if (scannedIp.getQueueStatus()==AIQueueConstants.Q_STATUS_REMOVED) {
                if ( appdefIp == null ) {
                    // scannedIp not found in appdef, and AI thinks it's been
                    // removed, so we're OK.  No need to add it anywhere, just
                    // continue on, and when this while loop is finished it will
                    // get added to the revisedAIplatform as "removed".

                } else {
                    // scannedIp is found in appdef, and AI thinks it's been
                    // removed, so just add it back to the appdef
                    // list so when this while loop is finished it will get
                    // added to the revisedAIplatform as "removed".
                    appdefIps.add(appdefIp);
                }
                continue;
            }

            scannedIp.setQueueStatus(AIQueueConstants.Q_STATUS_PLACEHOLDER);
            if ( appdefIp == null ) {
                // scannedIp was not found amongst appdefIps, therefore
                // it it a new IP.
                scannedIp.setQueueStatus(AIQueueConstants.Q_STATUS_ADDED);

                // Push changes up to platform
                revisedAIplatform.addAIIpValue(scannedIp);
                revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_IPS_CHANGED);
                
            } else {
                if (isDevice) {
                    if (scannedIp.getNetmask() == null) {
                        scannedIp.setNetmask(appdefIp.getNetmask());
                    }
                    if (scannedIp.getMACAddress() == null) {
                        scannedIp.setMACAddress(appdefIp.getMACAddress());
                    }
                }
                // Scanned IP does exist in appdef, do comparison 
                if ( !objectsEqual(scannedIp.getNetmask(), appdefIp.getNetmask()) ) {
                    // Netmask has changed
                    scannedIp.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(scannedIp, AIQueueConstants.Q_IP_NETMASK_CHANGED); 

                    // Push changes up to platform
                    revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_IPS_CHANGED);

                }
                if ( !objectsEqual(scannedIp.getMACAddress(), appdefIp.getMACAddress()) ) {
                    // MAC has changed
                    scannedIp.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(scannedIp, AIQueueConstants.Q_IP_MAC_CHANGED);

                    // Push changes up to platform
                    revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_IPS_CHANGED);
                }
                revisedAIplatform.addAIIpValue(scannedIp);
            }
        }

        // Whatever appdef IPs are left were not found in the scannedIps,
        // so they must have been removed from the platform.
        i = appdefIps.iterator();
        while ( i.hasNext() ) {
            appdefIp = (IpLocal) i.next();
            scannedIp = new AIIpValue();
            scannedIp.setAddress(appdefIp.getAddress());
            scannedIp.setNetmask(appdefIp.getNetmask());
            scannedIp.setMACAddress(appdefIp.getMACAddress());
            scannedIp.setQueueStatus(AIQueueConstants.Q_STATUS_REMOVED);
            revisedAIplatform.addAIIpValue(scannedIp);

            // Push changes up to platform
            revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
            addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_IPS_CHANGED);
        }
    }

    /**
     * Find (and remove) a scanned IP within a list of appdef IPs.
     * @param address The IP address to look for.
     * @param appdefIps The appdef IPs to search.
     * @return The appdefIp if it was found, null if it was not.  If the
     * appdefIp was found, it is also removed from the appdefIps list.
     */
    private IpLocal findAndRemoveAppdefIp(String address, List appdefIps) {
        // Is the appdef ip in the scan state?
        int size = appdefIps.size();
        IpLocal appdefIp;
        for (int i=0; i<size; i++ ) {
            appdefIp = (IpLocal) appdefIps.get(i);
            if ( appdefIp.getAddress().equals(address) ) {

                // Found a match based on address, remove it from
                // the scanned ip list and return it.
                appdefIps.remove(i);
                return appdefIp;
            }
        }
        return null;
    }

    private void doServerDiffs ( Log log,
                                 PlatformLocal appdefPlatform,
                                 ConfigManagerLocal cmLocal,
                                 CPropManagerLocal cpropMgr,
                                 AIPlatformValue aiPlatform,
                                 AIPlatformValue revisedAIplatform ) {

        // Compare servers between appdef and AI data.
        // We iterate over the servers in the AI data, removing them from
        // the appdef list as we find them.  In the end, the servers that are
        // left in appdef but not in the AI data are the servers that have been
        // removed from the platform.
        List appdefServers  = new ArrayList();
        appdefServers.addAll(appdefPlatform.getServers());
        List scannedServers = new ArrayList();
        scannedServers.addAll(Arrays.asList(aiPlatform.getAIServerValues()));
        if (log.isDebugEnabled())
            log.debug("AI2AppdefDiff: doServerDiffs:" +
                     " appdefServers=" + StringUtil.listToString(appdefServers) +
                     " scannedServers=" + StringUtil.listToString(scannedServers));
        ServerLocal appdefServer;
        AIServerValue scannedServer;
        Iterator i = scannedServers.iterator();
        while ( i.hasNext() ) {
            scannedServer = (AIServerValue) i.next();
            appdefServer
                = findAndRemoveAppdefServer(scannedServer, appdefServers);
            if (scannedServer.getQueueStatus()==AIQueueConstants.Q_STATUS_REMOVED) {
                if ( appdefServer == null ) {
                    // scannedServer not found in appdef, and AI thinks it's been
                    // removed, so we're OK.  No need to add it anywhere, just
                    // continue on, and when this while loop is finished it will
                    // get added to the revisedAIplatform as "removed".

                } else {
                    // scannedServer is found in appdef, and AI thinks it's been
                    // removed, so just add it back to the appdef
                    // list so when this while loop is finished it will get
                    // added to the revisedAIplatform as "removed".

                    appdefServers.add(appdefServer);
                }
                continue;
            }

            scannedServer.setQueueStatus(AIQueueConstants.Q_STATUS_PLACEHOLDER);
            if ( appdefServer == null ) {
                // scannedServer was not found amongst appdefServers, therefore
                // it it a new Server.
                scannedServer.setQueueStatus(AIQueueConstants.Q_STATUS_ADDED);
                
                // Only set status changed if the server is not ignored
                if (!scannedServer.getIgnored()) {
                    revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_SERVERS_CHANGED);
                }

                revisedAIplatform.addAIServerValue(scannedServer);
            } else {
                // Scanned Server does exist in appdef, do comparison 
                /* A modified name doesn't mean that the server has actually
                 * changed.  The plugins report a new name everytime, because
                 * they all use UUIDs to ensure uniqueness.
                 * if ( !scannedServer.getName().equals(appdefServer.getName()) ) {
                 *   // Name has changed
                 *   scannedServer.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                 *   addDiff(scannedServer, AIQueueConstants.Q_SERVER_NAME_CHANGED); 

                 *   // Push changes up to platform
                 *   revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                 *   addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_SERVERS_CHANGED);
                 * }
                 */
                if ( !scannedServer.getInstallPath().equals(appdefServer.getInstallPath()) ) {
                    // InstallPath has changed
                    scannedServer.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(scannedServer, AIQueueConstants.Q_SERVER_INSTALLPATH_CHANGED); 

                    // Push changes up to platform
                    revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_SERVERS_CHANGED);
                }
                
                AppdefEntityID aID =
                    new AppdefEntityID((ServerPK) appdefServer.getPrimaryKey());
                boolean configChanged = false;
                
                // Look at configs
                ConfigResponseValue crValue;
                try {
                    crValue = cmLocal.getConfigResponseValue(aID);
                } catch (AppdefEntityNotFoundException e) {
                    // Should not happen, unless the server was deleted since
                    // we just looked it up moments ago.
                    throw new SystemException(e);
                }
                if ( !crValue.getUserManaged() && (
                     !configsEqual(scannedServer.getProductConfig(), crValue.getProductResponse()) ||
                     !configsEqual(scannedServer.getControlConfig(), crValue.getControlResponse()) ||
                     !configsEqual(scannedServer.getMeasurementConfig(), crValue.getMeasurementResponse()) ||
                     !configsEqual(scannedServer.getResponseTimeConfig(), crValue.getResponseTimeResponse())))
                {
                    // config was changed (and is NOT user-managed)
                    configChanged = true;
                }

                if (configChanged) {
                    scannedServer.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(scannedServer, AIQueueConstants.Q_SERVER_CONFIG_CHANGED);
                    // Push changes up to platform
                    revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_SERVERS_CHANGED);
                }

                if (scannedServer.customPropertiesHasBeenSet()) {
                    int type = ((ServerTypePK) appdefServer.getServerType()
                        .getPrimaryKey()).getId().intValue();
                    updateCprops(log, cpropMgr, aID, type,
                                 scannedServer.getCustomProperties());
                }

                revisedAIplatform.addAIServerValue(scannedServer);
            }
        }

        // Whatever appdef Servers are left were not found in the scannedServers,
        // so they must have been removed from the platform.
        /* NOTE: disabled so that removed servers do not affect platform queue 
           status.
        i = appdefServers.iterator();
        while ( i.hasNext() ) {
            appdefServer = (ServerLocal) i.next();
            scannedServer = new AIServerValue();
            scannedServer.setName(appdefServer.getName());
            scannedServer.setServerTypeName
                (appdefServer.getServerType().getName());
            scannedServer.setInstallPath(appdefServer.getInstallPath());
            scannedServer.setAutoinventoryIdentifier(appdefServer.getAutoinventoryIdentifier());
            scannedServer.setCTime(appdefServer.getCTime());
            scannedServer.setMTime(appdefServer.getMTime());
            scannedServer.setQueueStatus(AIQueueConstants.Q_STATUS_REMOVED);
            revisedAIplatform.addAIServerValue(scannedServer);

            // Push changes up to platform
            revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
            addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_SERVERS_CHANGED);
        }
        */
    }

    /**
     * Find (and remove) a scanned server within a list of appdef servers.
     * @param aiid The server to look for, by autoinventory identifier
     * @param appdefServers The appdef servers to search.
     * @return The appdefServer if it was found, null if it was not.  If the
     * appdefServer was found, it is also removed from the appdefServers list.
     */
    private ServerLocal findAndRemoveAppdefServer ( AIServerValue scannedServer,
                                                    List appdefServers ) {
        // Is the appdef server in the scan state?
        String aiid = scannedServer.getAutoinventoryIdentifier();
        int size = appdefServers.size();
        ServerLocal appdefServer;
        for (int i=0; i<size; i++ ) {
            appdefServer = (ServerLocal) appdefServers.get(i);
            if ( appdefServer.getAutoinventoryIdentifier().equals(aiid) ) {

                // Found a match based on aiid, remove it from
                // the scanned server list and return it.
                appdefServers.remove(i);
                return appdefServer;
            }
        }

        // No matches.
        return null;
    }

    private void doPlatformAttrDiff ( Log log,
                                      PlatformLocal appdefPlatform,
                                      AIPlatformValue aiPlatform ) {
        // Compare AI platform against appdef data.
        if ( !appdefPlatform.getFqdn().equals(aiPlatform.getFqdn()) ) {
            aiPlatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
            addDiff(aiPlatform, AIQueueConstants.Q_PLATFORM_FQDN_CHANGED);
        }

        // cpu count can be null in appdef
        if (!objectsEqual(aiPlatform.getCpuCount(), appdefPlatform.getCpuCount())) {
            aiPlatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
            addDiff(aiPlatform, AIQueueConstants.Q_PLATFORM_PROPERTIES_CHANGED);
            log.info("CpuCount changed for " + aiPlatform.getFqdn() +
                     " from: " +
                     appdefPlatform.getCpuCount() +
                     ", to: " +
                     aiPlatform.getCpuCount());
        }
        
        // Pickup the appdef name attribute if it's not set
        if ( aiPlatform.getName() == null ) {
            aiPlatform.setName(appdefPlatform.getName());
        }
        
        String description = appdefPlatform.getDescription();
        if ((description == null) || (description.trim().length() == 0)) {
            if (aiPlatform.getDescription() != null) {
                aiPlatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                addDiff(aiPlatform, AIQueueConstants.Q_PLATFORM_PROPERTIES_CHANGED);
                log.info("Description changed for " + aiPlatform.getFqdn() +
                         " from: '" +
                         appdefPlatform.getDescription() +
                         "', to: '" +
                         aiPlatform.getDescription() + "'");
            }
        }
        else {
            //don't overwrite existing appdef description
            aiPlatform.setDescription(description);
        }
    }

    /**
     * Figure out which platform an autoinventory report is
     * referring to, by comparing the CertDN in the reported
     * platform data to existing platforms.
     *
     * @param aiPlatform the AI platform to find in appdef
     * @return The PlatformLocal for the platform that the scan came from.
     */
    private PlatformLocal getAppdefPlatform ( Log log, PlatformLocalHome pmLH,
                                              AuthzSubjectValue subject,
                                              AIPlatformValue aiPlatform ) {
        PlatformLocal platform = null;
        String certdn = aiPlatform.getCertdn();
        String fqdn = aiPlatform.getFqdn();
        try {
            // First try to find by fqdn
            platform = pmLH.findByFQDN(fqdn);
        } catch ( FinderException fe ) {
            // Now try to find by certdn
            try {
                platform = pmLH.findByCertDN(certdn);
            } catch ( FinderException fe2 ) {
                if (log.isDebugEnabled()) {
                    log.debug("FindByCertDN failed: " + fe2);
                }
                return null;
            }

            return platform;
        } catch ( Exception e ) {
            log.error("Error finding platform by fqdn: " + e, e);
            throw new SystemException(e);
        }

        return platform;
    }

    private static boolean objectsEqual(Object o1, Object o2) {
        if (o1 == o2) {
            return true;
        }
        if ((o1 == null) || (o2 == null)) {
            return false;
        }
        return o1.equals(o2);
    }

    //XXX this seems odd to update something in a method
    //that is checking for differences.  however, at this point
    //we have the ai object, the existing appdef object and the cpropMgr.
    //this is simply the easiest place until server AI code is refactored.
    private void updateCprops(Log log,
                              CPropManagerLocal cpropMgr,
                              AppdefEntityID id,
                              int type,
                              byte[] data)
    {
        if (data == null) {
            return;
        }

        ConfigResponse aicprops;
        Properties existing;
        try {
            aicprops = ConfigResponse.decode(data);
        } catch (EncodingException e) {
            log.error("Error decoding cprops for: " + id);
            return;
        }

        try {
            existing = cpropMgr.getEntries(id);
        } catch (Exception e) {
            log.error("Error looking up cprops for: " + id, e);
            return;
        }

        boolean isChanged = false;
        for (Iterator it=aicprops.getKeys().iterator(); it.hasNext();) {
            String key = (String)it.next();
            String value = aicprops.getValue(key);
            String current = existing.getProperty(key);

            //modified version of cpropMgr.setConfigResponse
            //here only setValue() for new or changed values
            if ((current == null) || !value.equals(current)) {
                try {
                    cpropMgr.setValue(id, type, key, value);
                    isChanged = true;
                } catch (Exception e) {
                    log.error("Error updating custom properties for: " + id, e);
                }
            }
        }
        String un = isChanged ? "" : "un";
        log.debug("Custom Properties " + un + "changed for: " + id);
    }

    private static boolean configsEqual(byte[] c1, byte[] c2)  {
        return AICompare.configsEqual(c1, c2);
    }

    private void addDiff ( AIPlatformValue aiPlatform, long diff ) {
        aiPlatform.setDiff(aiPlatform.getDiff() | diff);
    }

    private void addDiff ( AIIpValue aiIp, long diff ) {
        aiIp.setDiff(aiIp.getDiff() | diff);
    }

    private void addDiff ( AIServerValue aiServer, long diff ) {
        aiServer.setDiff(aiServer.getDiff() | diff);
    }
}
