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

import java.util.Collection;
import java.util.Iterator;

import javax.ejb.RemoveException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManagerLocal;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.dao.AIPlatformDAO;

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
                                AIQueueManagerLocal aiqMgr,
                                AIPlatformDAO aiPlatformLH,
                                AIPlatformValue aiPlatform,
                                boolean updateServers,
                                boolean isApproval,
                                boolean isReport)
        throws RemoveException
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
                AIPlatform newQPlatform = aiPlatformLH.create(aiPlatform);
                return newQPlatform.getAIPlatformValue();
            } else {
                _log.info("Updating existing platform: " +
                           aiPlatform.getFqdn());
                aiPlatformLH.updateQueueState(existingQplatform,
                                              aiPlatform,
                                              updateServers,
                                              isApproval,
                                              isReport);
            }
        }
        return existingQplatform.getAIPlatformValue();
    }

    public static AIPlatform getAIQPlatform(AIPlatformDAO aiPlatformLH,
                                            AIPlatformValue aiPlatformValue)
        throws SystemException {

        // Is there another platform in the queue with the same certdn?
        AIPlatform aiPlatform;
        String certdn = aiPlatformValue.getCertdn();
        String fqdn = aiPlatformValue.getFqdn();
        Collection fqdnMatches;
        // Try FQDN first
        fqdnMatches = aiPlatformLH.findByFQDN(fqdn);

        if (fqdnMatches.size() != 1) {
            aiPlatform = aiPlatformLH.findByCertDN(certdn);
            if (aiPlatform == null) {
                if (fqdnMatches.size() > 1) {
                    _log.warn("Multiple platforms matched FQDN: " +
                              fqdn + " [" + fqdnMatches + "]");
                }
                return null;
            }
            return aiPlatform;
        }

        Iterator i = fqdnMatches.iterator();
        return (AIPlatform) i.next();
    }
}
