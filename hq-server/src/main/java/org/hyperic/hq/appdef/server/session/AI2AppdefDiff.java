/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.Ip;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.CPropManager;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AICompare;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.vm.VCManager;
import org.hyperic.hq.vm.VMID;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A utility class for calculating queue status and diff for an AIPlatform.
 */
public class AI2AppdefDiff {

    private static Log _log = LogFactory.getLog(AI2AppdefDiff.class);
    protected VCManager vmMgr;

    
    public AI2AppdefDiff (VCManager vmMgr) {
        this.vmMgr=vmMgr;
    }
    
    /**
     * @param aiplatform The AI platform data, including nested IPs and servers.
     * @return A new AI platform value object, with queuestatus and diff set
     * correctly (including for nested IPs and servers), and only containing the
     * set of IPs and servers that should be queued (IPs and servers that are 
     * already identical to those in appdef are removed from the value object).  
     */
    public AIPlatformValue diffAgainstAppdef(AuthzSubject subject,
                                             PlatformManager pmLH,
                                             ConfigManager cmLocal,
                                             CPropManager cpropMgr,
                                             AIPlatformValue aiplatform)
    {
        AIPlatformValue revisedAIplatform;

        // We know we'll at least need to copy all the platform-level attributes
        revisedAIplatform = new AIPlatformValue(aiplatform);

        // Initially, set platform status to PLACEHOLDER
        revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_PLACEHOLDER);
        revisedAIplatform.setDiff(AIQueueConstants.Q_DIFF_NONE);

        // Get the appdef platform
        Platform appdefPlatform;
        try {
            appdefPlatform = pmLH.getPlatformByAIPlatform(subject, aiplatform);
            // If there was no appdef platform...
            if (appdefPlatform == null) {
                return getAiPlatformToAdd(aiplatform);
            }
        } catch (PermissionException e) {
            _log.error("Error looking up platform", e);
            throw new SystemException(e);
        }

        //when scans are run for a device platform, only the fqdn and ipaddress
        //are available, so we keep the other platform and ip attributes as
        //they already are in appdef (same as the user entered by hand)
        boolean isDevice = revisedAIplatform.isPlatformDevice();
        
        if (isDevice) {
            _log.info("Applying existing appdef attributes for device " +
                      aiplatform.getPlatformTypeName() + "=" +
                      aiplatform.getFqdn());
            if (revisedAIplatform.getCpuCount() == null) {
                revisedAIplatform.setCpuCount(appdefPlatform.getCpuCount());
            }
            
            ConfigResponseDB config =
                cmLocal.getConfigResponse(appdefPlatform.getEntityId());
            
            //if the plugin did not set a config, apply the existing config.
            if (revisedAIplatform.getProductConfig() == null) {
                revisedAIplatform.setProductConfig(config.getProductResponse());
            }
            if (revisedAIplatform.getControlConfig() == null) {
                revisedAIplatform.setControlConfig(config.getControlResponse());
            }
            if (revisedAIplatform.getMeasurementConfig() == null) {
                revisedAIplatform.setMeasurementConfig(config.getMeasurementResponse());
            }

            //XXX might want to do this for all platforms, just checking devices for now.
            if (!configsEqual(revisedAIplatform.getProductConfig(),
                              config.getProductResponse()) ||
                !configsEqual(revisedAIplatform.getControlConfig(),
                              config.getControlResponse()) ||
                !configsEqual(revisedAIplatform.getMeasurementConfig(),
                              config.getMeasurementResponse()))
            {
                revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                addDiff(revisedAIplatform,
                        AIQueueConstants.Q_PLATFORM_PROPERTIES_CHANGED);
                _log.info("ConfigResponse changed for " + aiplatform.getFqdn() +
                          " '" + aiplatform.getPlatformTypeName() + "'");
            }
        }

        // Otherwise, there was an appdef platform that matched, so we
        // go through and compare IPs, servers, and finally the platform
        // attributes.

        // Compare IPs
        if (_log.isDebugEnabled())
            _log.debug("Before IP diff:=" +
                       StringUtil.arrayToString(revisedAIplatform.getAIIpValues()));
        doIpDiffs(appdefPlatform, aiplatform, revisedAIplatform, isDevice);
        if (_log.isDebugEnabled())
            _log.debug("After IP diff:=" +
                      StringUtil.arrayToString(revisedAIplatform.getAIIpValues()));
        
        // Compare servers
        doServerDiffs(appdefPlatform, cmLocal,
                      cpropMgr, aiplatform, revisedAIplatform);

        // Compare platform attributes
        doPlatformAttrDiff(appdefPlatform, revisedAIplatform);

        if (aiplatform.customPropertiesHasBeenSet()) {
            AppdefEntityID aid =
                AppdefEntityID.newPlatformID(appdefPlatform.getId());
            int type =
                appdefPlatform.getPlatformType().getId().intValue();
            // only map the UUID for actual platforms, not for virtual ones discovered by the vc plugin
            if (AuthzConstants.platformPrototypeVmwareVsphereVm.equals(appdefPlatform.getResource().getPrototype().getName())) { 
                updateCprops(cpropMgr, aid, type, aiplatform.getCustomProperties(),null);
            } else {
                Collection<Ip> ips = appdefPlatform.getIps();
                List<String> macs = new ArrayList<String>(ips.size());
                if (ips!=null) {
                    for(Ip ip:ips) {
                        String mac = ip.getMacAddress();
                        if (mac!=null && !mac.isEmpty() && !mac.equals("")) {
                            macs.add(mac);
                        }
                    }
                }
                updateCprops(cpropMgr, aid, type, aiplatform.getCustomProperties(),macs);
            }
        }

        return revisedAIplatform;
    }

    
    private AIPlatformValue getAiPlatformToAdd(AIPlatformValue aiplatform) {
        // If the aiplatform has status "removed", then appdef model is
        // correct and the platform has actually been removed.  In this
        // case we return null, which notifies the caller of this condition.
        if (aiplatform.getQueueStatus() == AIQueueConstants.Q_STATUS_REMOVED) {
            return null;
        }
        // Otherwise, recursively mark everything as new, copying 
        // IPs and servers.
        AIPlatformValue revisedAIplatform = aiplatform;
        revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_ADDED);
        // All scanned IPs must be new
        AIIpValue[] newIps = aiplatform.getAIIpValues();
        revisedAIplatform.removeAllAIIpValues();
        for (int i=0; i<newIps.length; i++) {
            newIps[i].setQueueStatus(AIQueueConstants.Q_STATUS_ADDED);
            revisedAIplatform.addAIIpValue(newIps[i]);
        }
        // All scanned servers must be new
        AIServerValue[] newServers = aiplatform.getAIServerValues();
        revisedAIplatform.removeAllAIServerValues();
        for (int i=0; i<newServers.length; i++) {
            newServers[i].setQueueStatus(AIQueueConstants.Q_STATUS_ADDED);
            revisedAIplatform.addAIServerValue(newServers[i]);
        }
        return revisedAIplatform;
    }

    private void doIpDiffs(Platform appdefPlatform,
                           AIPlatformValue aiPlatform,
                           AIPlatformValue revisedAIplatform,
                           boolean isDevice)
    {
        // Compare IP addresses between appdef and AI data.
        // We iterate over the IPs in the AI data, removing them from
        // the appdef list as we find them.  In the end, the IPs that are
        // left in appdef but not in the AI data are the IPs that have been
        // removed from the platform.
        List appdefIps  = new ArrayList(appdefPlatform.getIps());
        List scannedIps = Arrays.asList(aiPlatform.getAIIpValues());
        if (_log.isDebugEnabled())
            _log.debug("appdefIps=" + StringUtil.listToString(appdefIps) +
                       " scannedIps=" + StringUtil.listToString(scannedIps));
        revisedAIplatform.removeAllAIIpValues();
        
        Ip appdefIp = null;
        AIIpValue scannedIp = null;
        Iterator i = scannedIps.iterator();
        while ( i.hasNext() ) {
            scannedIp = (AIIpValue) i.next();
            appdefIp = findAndRemoveAppdefIp(scannedIp.getAddress(), appdefIps);

            if (scannedIp.getQueueStatus()==AIQueueConstants.Q_STATUS_REMOVED) {
                if (appdefIp == null) {
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
                        scannedIp.setMACAddress(appdefIp.getMacAddress());
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
                if ( !objectsEqual(scannedIp.getMACAddress(), appdefIp.getMacAddress()) ) {
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
            appdefIp = (Ip) i.next();
            scannedIp = new AIIpValue();
            scannedIp.setAddress(appdefIp.getAddress());
            scannedIp.setNetmask(appdefIp.getNetmask());
            scannedIp.setMACAddress(appdefIp.getMacAddress());
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
    private Ip findAndRemoveAppdefIp(String address, List appdefIps) {
        // Is the appdef ip in the scan state?
        int size = appdefIps.size();
        for (int i=0; i<size; i++ ) {
            Ip appdefIp = (Ip) appdefIps.get(i);
            if ( appdefIp.getAddress().equals(address) ) {

                // Found a match based on address, remove it from
                // the scanned ip list and return it.
                appdefIps.remove(i);
                return appdefIp;
            }
        }
        return null;
    }
    
    private boolean shouldUpdateConfig(byte[] newConfig, byte[] existingConfig, boolean userManaged) {
        if (configsEqual(newConfig, existingConfig)) {
            return false;
        }        
 
        if (!userManaged  ) {
            // config are different and not user managed
            return true;
        }

        if (existingConfig == null) {
            // configs are different and userManaged - return true - only if existing config is null
            return true;
        }
        
        // config diff, userMnaged==true and exisitingConfig is not null
        return false;
    }


    private void doServerDiffs(Platform appdefPlatform,
                               ConfigManager cmLocal,
                               CPropManager cpropMgr,
                               AIPlatformValue aiPlatform,
                               AIPlatformValue revisedAIplatform) {

        // Compare servers between appdef and AI data.
        // We iterate over the servers in the AI data, removing them from
        // the appdef list as we find them.  In the end, the servers that are
        // left in appdef but not in the AI data are the servers that have been
        // removed from the platform.
    	        
        // Force initialization to ensure AIQ server diffs are up-to-date
        Hibernate.initialize(appdefPlatform.getServersBag());

        List appdefServers  = new ArrayList();
        appdefServers.addAll(appdefPlatform.getServers());
        List scannedServers = new ArrayList();
        scannedServers.addAll(Arrays.asList(aiPlatform.getAIServerValues()));
        if (_log.isDebugEnabled())
            _log.debug(" appdefServers=" + StringUtil.listToString(appdefServers) +
                       " scannedServers=" + StringUtil.listToString(scannedServers));
        Server appdefServer;
        AIServerValue scannedServer;
        Iterator i = scannedServers.iterator();
        while (i.hasNext()) {
            scannedServer = (AIServerValue) i.next();
            appdefServer
                = findAndRemoveAppdefServer(scannedServer, appdefServers);
            if (scannedServer.getQueueStatus()==AIQueueConstants.Q_STATUS_REMOVED) {
                if (appdefServer == null) {
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
            if (appdefServer == null) {
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
                String scannedInstallPath = scannedServer.getInstallPath();
                if ((scannedInstallPath == null && appdefServer.getInstallPath() != null) ||
                    (scannedInstallPath != null && !scannedInstallPath.equals(appdefServer.getInstallPath()))){
                    // InstallPath has changed
                    scannedServer.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(scannedServer, AIQueueConstants.Q_SERVER_INSTALLPATH_CHANGED); 

                    // Push changes up to platform
                    revisedAIplatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                    addDiff(revisedAIplatform, AIQueueConstants.Q_PLATFORM_SERVERS_CHANGED);
                }

                AppdefEntityID aID =
                    AppdefEntityID.newServerID(appdefServer.getId());
                boolean configChanged = false;
                
                // Look at configs
                ConfigResponseDB config = cmLocal.getConfigResponse(aID);
                boolean userManaged = config.getUserManaged() ;
                if ( shouldUpdateConfig(scannedServer.getProductConfig(), config.getProductResponse(), userManaged) ||
                     shouldUpdateConfig(scannedServer.getControlConfig(), config.getControlResponse(), userManaged)||
                     shouldUpdateConfig(scannedServer.getMeasurementConfig(), config.getMeasurementResponse(), userManaged)||
                     shouldUpdateConfig(scannedServer.getResponseTimeConfig(), config.getResponseTimeResponse(), userManaged)) {
                    // config was changed (and is NOT user-managed - or exisiting config is null)                    
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
                    int type = appdefServer.getServerType().getId().intValue();
                    updateCprops(cpropMgr, aID, type,
                                 scannedServer.getCustomProperties(),null);
                }

                revisedAIplatform.addAIServerValue(scannedServer);
            }
        }
    }

    /**
     * Find (and remove) a scanned server within a list of appdef servers.
     * @param scannedServer The server to look for, by autoinventory identifier
     * @param appdefServers The appdef servers to search.
     * @return The appdefServer if it was found, null if it was not.  If the
     * appdefServer was found, it is also removed from the appdefServers list.
     */
    private Server findAndRemoveAppdefServer(AIServerValue scannedServer,
                                             List appdefServers ) {
        // Is the appdef server in the scan state?
        String aiid = scannedServer.getAutoinventoryIdentifier();
        int size = appdefServers.size();
        Server appdefServer;
        for (int i=0; i<size; i++) {
            appdefServer = (Server) appdefServers.get(i);
            if (appdefServer.getAutoinventoryIdentifier().equals(aiid)) {

                // Found a match based on aiid, remove it from
                // the scanned server list and return it.
                appdefServers.remove(i);
                return appdefServer;
            }
        }

        // No matches.
        return null;
    }

    private void doPlatformAttrDiff(Platform appdefPlatform,
                                    AIPlatformValue aiPlatform)
    {
        // Compare AI platform against appdefmeasurementManager data.
        if (!appdefPlatform.getFqdn().equals(aiPlatform.getFqdn())) {
            aiPlatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
            addDiff(aiPlatform, AIQueueConstants.Q_PLATFORM_FQDN_CHANGED);
        }

        // cpu count can be null in appdef
        if (!objectsEqual(aiPlatform.getCpuCount(), appdefPlatform.getCpuCount())) {
            aiPlatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
            addDiff(aiPlatform, AIQueueConstants.Q_PLATFORM_PROPERTIES_CHANGED);
            _log.info("CpuCount changed for " + aiPlatform.getFqdn() +
                      " from: " +
                      appdefPlatform.getCpuCount() +
                      ", to: " +
                      aiPlatform.getCpuCount());
        }
        
        // Pickup the appdef name attribute if it's not set
        if (aiPlatform.getName() == null) {
            aiPlatform.setName(appdefPlatform.getName());
        }

        String aiDescr = aiPlatform.getDescription();
        String appdefDescr = appdefPlatform.getDescription();

        if ((appdefDescr == null) ||
            (appdefDescr.trim().length() == 0) ||
            //e.g. may have vmguest info appended
            ((aiDescr != null) && aiDescr.startsWith(appdefDescr + " ")))
        {
            if (aiDescr != null) {
                aiPlatform.setQueueStatus(AIQueueConstants.Q_STATUS_CHANGED);
                addDiff(aiPlatform,
                        AIQueueConstants.Q_PLATFORM_PROPERTIES_CHANGED);
                _log.info("Description changed for " + aiPlatform.getFqdn() +
                          " from: '" +
                          appdefDescr +
                          "', to: '" +
                          aiDescr + "'");
            }
        } else {
            //don't overwrite existing appdef description
            aiPlatform.setDescription(appdefDescr);
        }
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

    //TODO this seems odd to update something in a method
    //that is checking for differences.  however, at this point
    //we have the ai object, the existing appdef object and the cpropMgr.
    //this is simply the easiest place until server AI code is refactored.
    private void updateCprops(CPropManager cpropMgr,
                              AppdefEntityID id, int type, byte[] data, List<String> macs)
    {
        if (data == null) {
            return;
        }

        ConfigResponse aicprops;
        Properties existing;
        try {
            aicprops = ConfigResponse.decode(data);
        } catch (EncodingException e) {
            _log.error("Error decoding cprops for: " + id);
            return;
        }

        if (macs!=null) {
            VMID vmid = this.vmMgr.getVMID(macs);
            if (vmid!=null) {
                aicprops.setValue(HQConstants.MOID, vmid.getMoref());
                aicprops.setValue(HQConstants.VCUUID, vmid.getVcUUID());
            }
        }
        try {
            existing = cpropMgr.getEntries(id);
        } catch (Exception e) {
            _log.error("Error looking up cprops for: " + id, e);
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
                    _log.error("Error updating custom properties for: " +
                               id, e);
                }
            }
        }
        String un = isChanged ? "" : "un";
        _log.debug("Custom Properties " + un + "changed for: " + id);
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
