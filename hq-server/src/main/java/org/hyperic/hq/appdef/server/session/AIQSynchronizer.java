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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.autoinventory.AIIp;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIServer;
import org.hyperic.hq.autoinventory.data.AIPlatformRepository;
import org.hyperic.hq.common.SystemException;

/**
 * A utility class to synchronize existing AI queue data with new AI data.
 */
public class AIQSynchronizer {
    private static Log _log = LogFactory.getLog(AIQSynchronizer.class);
    
    public AIQSynchronizer () {}

    /**
     * @param aiPlatform The AI platform to sync into the queue.
     * @return The synchronized platform, or null if the AI data was removed
     * from the queue (this would happen if the platform in the queue matched
     * appdef exactly, such that the data should not be queued).
     */
    public AIPlatformValue sync(AuthzSubject subject,
                                AIQueueManager aiqMgr,
                                AIPlatformRepository aiPlatformLH,
                                AIPlatformValue aiPlatform,
                                boolean updateServers,
                                boolean isApproval,
                                boolean isReport)
    {
        // Is there an entry in the queue for this platform?
        AIPlatform existingQplatform;

        existingQplatform = AIQSynchronizer.getAIQPlatform(aiPlatformLH,
                                                           aiPlatform);

        // If the platform was unchanged with respect to appdef...
        if(aiPlatform.getQueueStatus() == AIQueueConstants.Q_STATUS_PLACEHOLDER
           || aiPlatform.getQueueStatus() == AIQueueConstants.Q_STATUS_REMOVED){
            // Was it in the queue?
            if (existingQplatform == null) {
                // Not in the queue, so nothing to do.

            } else {
                // Remove from queue, but only if nothing has been ignored.
                AIServerValue[] servers = aiPlatform.getAIServerValues();
                for (int i = 0; i < servers.length; i++) {
                    AIServerValue s = servers[i];
                    if (s.getIgnored()) {
                        _log.info("Platform " + existingQplatform.getName() +
                                  " has ignored servers, leaving in queue.");

                        if (aiPlatform.getQueueStatus() != existingQplatform.getQueueStatus()) {
                            _log.info("Updating queue status of existing platform: " +
                                      aiPlatform.getFqdn());

                            updateQueueState(existingQplatform, aiPlatform,
                                updateServers, isApproval, isReport,aiPlatformLH);
                        }

                        return aiPlatform;
                    }
                }

                _log.info("Removing unchanged " + existingQplatform.getName() +
                          " from queue.");
                aiqMgr.removeFromQueue(existingQplatform);
            }

            return aiPlatform;
        }

        // If the platform is new or changed, then make sure it (and everything 
        // else underneath it) is in the queue.
        if (aiPlatform.getQueueStatus() == AIQueueConstants.Q_STATUS_ADDED ||
            aiPlatform.getQueueStatus() == AIQueueConstants.Q_STATUS_CHANGED) {

            if ( existingQplatform == null ) {
                // No existing queued platform, so we'll queue everything up.
                _log.info("Queueing new platform: " + aiPlatform.getFqdn());
                AIPlatform newQPlatform = create(aiPlatformLH,aiPlatform);
                return newQPlatform.getAIPlatformValue();
            } else {
                _log.info("Updating existing platform: " +
                           aiPlatform.getFqdn());
                updateQueueState(existingQplatform,
                                              aiPlatform,
                                              updateServers,
                                              isApproval,
                                              isReport, aiPlatformLH);
            }
        }
        return existingQplatform.getAIPlatformValue();
    }
    
    /**
     * legacy method
     * 
     * @param aiplatform The new AI data to update this platform with.
     * @param updateServers If true, servers will be updated too.
     */
    private void updateQueueState(AIPlatform aip, AIPlatformValue aiplatform, boolean updateServers,
                                 boolean isApproval, boolean isReport, AIPlatformRepository aiPlatformDao) {
        // reassociate platform
        aip = aiPlatformDao.findById(aip.getId());

        long nowTime = System.currentTimeMillis();
        aip.setFqdn(aiplatform.getFqdn());
        aip.setName(aiplatform.getName());
        aip.setPlatformTypeName(aiplatform.getPlatformTypeName());
        aip.setAgentToken(aiplatform.getAgentToken());
        aip.setQueueStatus(aiplatform.getQueueStatus());
        aip.setDescription(aiplatform.getDescription());
        aip.setDiff(aiplatform.getDiff());
        aip.setCpuCount(aiplatform.getCpuCount());
        aip.setCustomProperties(aiplatform.getCustomProperties());
        aip.setProductConfig(aiplatform.getProductConfig());
        aip.setMeasurementConfig(aiplatform.getMeasurementConfig());
        aip.setControlConfig(aiplatform.getControlConfig());

        if (isReport || isApproval) {
            if (isApproval) {
                aiplatform.setLastApproved(new Long(nowTime + 1));
            } else {
                Long lastApproved = aiplatform.getLastApproved();
                if (lastApproved == null)
                    lastApproved = new Long(0);
                aiplatform.setLastApproved(lastApproved);
            }
            aip.setLastApproved(aiplatform.getLastApproved());
        }

        // Sanitize name
        if (aip.getName() == null) {
            aip.setName(aip.getFqdn());
        }
        updateIpSet(aip, aiplatform);
        if (updateServers) {
            updateServerSet(aip, aiplatform);
        }
        aiPlatformDao.save(aip);
    }
    
    private void updateServerSet(AIPlatform p, AIPlatformValue aiplatform) {
        Map newServers = getServersMap(Arrays.asList(aiplatform.getAIServerValues()));
        // XXX, scottmf need to get Platform from AIPlatform
        // then find a way to correlate the old aiid with the new aiid for
        // each server
        Collection serverSet = p.getAIServers();
        for (Iterator i = serverSet.iterator(); i.hasNext();) {
            AIServer qserver = (AIServer) i.next();
            String aiid = qserver.getAutoinventoryIdentifier();
            AIServerValue aiserver = (AIServerValue) newServers.remove(aiid);

            if (aiserver == null) {
                // keep the user specified ignored value
                boolean qIgnored = qserver.getIgnored();
                qserver.setAIServerValue(qserver.getAIServerValue());
                qserver.setIgnored(qIgnored);
            }
        }

        for (Iterator i = newServers.values().iterator(); i.hasNext();) {
            AIServerValue aiserver = (AIServerValue) i.next();
            AIServer ais = new AIServer();
            ais.setAIServerValue(aiserver);
            p.addAIServer(ais);
        }
    }
    
    private void updateIpSet(AIPlatform p, AIPlatformValue aiplatform) {
        List newIPs = new ArrayList(Arrays.asList(aiplatform.getAIIpValues()));
        Collection ipSet = p.getAIIps();
        for (Iterator i = ipSet.iterator(); i.hasNext();) {
            AIIp qip = (AIIp) i.next();
            AIIpValue aiip = findAndRemoveAIIp(newIPs, qip.getAddress());
            if (aiip == null) {
                i.remove();
            } else {
                boolean qIgnored = qip.isIgnored();
                qip.setQueueStatus(new Integer(aiip.getQueueStatus()));
                qip.setDiff(aiip.getDiff());
                qip.setIgnored(aiip.getIgnored());
                qip.setAddress(aiip.getAddress());
                qip.setMacAddress(aiip.getMACAddress());
                qip.setNetmask(aiip.getNetmask());
                qip.setIgnored(qIgnored);
            }
        }

        // Add remaining IPs
        for (Iterator i = newIPs.iterator(); i.hasNext();) {
            AIIpValue aiip = (AIIpValue) i.next();
            AIIp ip = new AIIp();
            ip.setQueueStatus(new Integer(aiip.getQueueStatus()));
            ip.setDiff(aiip.getDiff());
            ip.setIgnored(aiip.getIgnored());
            ip.setAddress(aiip.getAddress());
            ip.setMacAddress(aiip.getMACAddress());
            ip.setNetmask(aiip.getNetmask());
            ip.setAIPlatform(p);
            ipSet.add(ip);
        }
    }
    
    private AIIpValue findAndRemoveAIIp(List ips, String addr) {
        AIIpValue aiip;
        for (int i = 0; i < ips.size(); i++) {
            aiip = (AIIpValue) ips.get(i);
            if (aiip.getAddress().equals(addr)) {
                return (AIIpValue) ips.remove(i);
            }
        }
        return null;
    }

    private Map getServersMap(List servers) {
        Map rtn = new HashMap();
        for (Iterator it = servers.iterator(); it.hasNext();) {
            AIServerValue server = (AIServerValue) it.next();
            rtn.put(server.getAutoinventoryIdentifier(), server);
        }
        return rtn;
    }

    
    
    private AIPlatform create(AIPlatformRepository aiPlatformDao, AIPlatformValue apv) {
        AIPlatform p = new AIPlatform(apv);
        fixdata(p);

        // handle IP's in the value object
        // Since this is a new value object
        // I'll assume we need add all ips
        AIIpValue[] newIPs = apv.getAIIpValues();
        Collection ipSet = new HashSet();
        p.setAIIps(ipSet);

        for (int i = 0; i < newIPs.length; i++) {
            AIIpValue ipVal = newIPs[i];
            AIIp ip = new AIIp(ipVal);
            ip.setAIPlatform(p);
            ipSet.add(ip);
        }

        // handle Server's in the value object
        // Since this is a new value object
        // I'll assume we need add all servers
        AIServerValue[] newServers = apv.getAIServerValues();
        Collection serverSet = new HashSet();
        p.setAIServers(serverSet);

        // XXX hack around bug in xcraplet's generated
        // removeAllXXX methods (they actually don't remove anything)
        // The AIQueueManagerImpl.queue method relies on this working.
        HashSet xdocletServerHackSet = new HashSet();
        for (int i = 0; i < newServers.length; i++) {
            AIServerValue serverVal = newServers[i];
            AIServer s = new AIServer(serverVal);
            s.setAIPlatform(p);
            serverSet.add(s);
        }
        aiPlatformDao.save(p);
        return p;
    }
    
    private void fixdata(AIPlatform p) {
        String name = p.getName();
        if (name == null || "".equals(name.trim())) {
            p.setName(p.getFqdn());
        }
    }

    public static AIPlatform getAIQPlatform(AIPlatformRepository aiPlatformLH,
                                            AIPlatformValue aiPlatformValue)
        throws SystemException {

        // Is there another platform in the queue with the same certdn?
        AIPlatform aiPlatform;
        String certdn = aiPlatformValue.getCertdn();
        String fqdn = aiPlatformValue.getFqdn();
        // Try FQDN first
        AIPlatform fqdnMatch = aiPlatformLH.findByFqdn(fqdn);

        if (fqdnMatch == null) {
            aiPlatform = aiPlatformLH.findByCertdn(certdn);
            return aiPlatform;
        } else {
            return fqdnMatch;
        }
    }
}
