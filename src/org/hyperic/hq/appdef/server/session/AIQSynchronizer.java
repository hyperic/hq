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

import java.util.Collection;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AIPlatformLocal;
import org.hyperic.hq.appdef.shared.AIPlatformLocalHome;
import org.hyperic.hq.appdef.shared.AIPlatformPK;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQueueConstants;
import org.hyperic.hq.appdef.shared.AIQueueManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEvent;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformManagerUtil;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniPlatformNode;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniResourceTree;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniServerNode;
import org.hyperic.hq.appdef.shared.miniResourceTree.MiniServiceNode;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;

import org.apache.commons.logging.Log;

/**
 * A utility class to synchronize existing AI queue data with new AI data.
 */
public class AIQSynchronizer {
    private PlatformManagerLocal pm = null;
    
    private PlatformManagerLocal getPlatformMan() {
        if (pm == null) {
            try {
                pm = PlatformManagerUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }
        return pm;
    }

    public AIQSynchronizer () {}

    /**
     * @param aiPlatform The AI platform to sync into the queue.
     * @return The synchronized platform, or null if the AI data was removed 
     * from the queue (this would happen if the platform in the queue matched
     * appdef exactly, such that the data should not be queued).
     */
    public AIPlatformValue sync (Log log,
                                 AuthzSubjectValue subject,
                                 AIQueueManagerLocal aiqMgr,
                                 AIPlatformLocalHome aiPlatformLH,
                                 AIPlatformValue aiPlatform,
                                 boolean updateServers,
                                 boolean isApproval,
                                 boolean isReport) 
        throws CreateException, RemoveException, NamingException {

        // Is there an entry in the queue for this platform?
        AIPlatformLocal existingQplatform;

        existingQplatform = AIQSynchronizer.getAIQPlatform(log,
                                                           aiPlatformLH, 
                                                           aiPlatform);

        // If the platform was unchanged with respect to appdef...
        if(aiPlatform.getQueueStatus() == AIQueueConstants.Q_STATUS_PLACEHOLDER
           || aiPlatform.getQueueStatus() == AIQueueConstants.Q_STATUS_REMOVED){
            // Was it in the queue?
            if ( existingQplatform == null ) {
                // Not in the queue, so nothing to do.

            } else {
                Integer existingId = ((AIPlatformPK)existingQplatform.getPrimaryKey()).getId();
                // Leave it in the queue in case something changes later
                // aiqMgr.removeFromQueue(existingQplatform);
                existingQplatform.updateQueueState(aiPlatform, 
                                                   updateServers,
                                                   isApproval,
                                                   isReport);
                // FIXME why set id again?!?
                aiPlatform.setId(existingId);
            }

            return aiPlatform;
        }

        // If the platform is new or changed, then make sure it (and everything 
        // else underneath it) is in the queue.
        if ( aiPlatform.getQueueStatus() == AIQueueConstants.Q_STATUS_ADDED ||
             aiPlatform.getQueueStatus() == AIQueueConstants.Q_STATUS_CHANGED ) {

            if ( existingQplatform == null ) {
                // No existing queued platform, so we'll queue everything up.
                log.info("AIQmgr: Queueing new platform: " + aiPlatform.getFqdn());
                AIPlatformLocal newQPlatform = aiPlatformLH.create(aiPlatform);
                AIPlatformValue newQPlatformValue
                    = newQPlatform.getAIPlatformValue();
                return newQPlatformValue;
            } else {
                log.info("AIQmgr: Updating existing platform: " + aiPlatform.getFqdn());
                existingQplatform.updateQueueState(aiPlatform, 
                                                   updateServers,
                                                   isApproval,
                                                   isReport);

                // sending an update event
                // only if there is an approval action actually taking place
                if(isApproval) {
                    this.sendAppdefEvent(log, subject,
                                         aiPlatform, AppdefEvent.ACTION_UPDATE);
                }
            }
        }
        return existingQplatform.getAIPlatformValue();
    }
    
    private void sendAppdefEvent(Log log,
                                 AuthzSubjectValue subject,
                                 AIPlatformValue aiPlatform, int eventType)
        throws NamingException {
        PlatformValue platform;
        try {
            platform =
                getPlatformMan().getPlatformByAIPlatform(subject, aiPlatform);
        } catch (PermissionException e) {
            if (log.isDebugEnabled()) {
                log.debug("sendAppdefEvent(): " + subject.getName() +
                          " does not have permission to view platform " +
                          "for certdn: " + aiPlatform.getCertdn() + " fqdn: " +
                          aiPlatform.getFqdn());
            }
            return;
        }

        if (platform == null) {
            // I'm not sure why the platform could be null, if there's
            // supposedly an existing platform when we call this - clee
            if (log.isDebugEnabled()) {
                log.debug("sendAppdefEvent(): Existing platform not found " +
                          "for certdn: " + aiPlatform.getCertdn() + " fqdn: " +
                          aiPlatform.getFqdn());
            }
            
            return;
        }

        // We'll have to iterate through everything in the platform and send
        // appdef events
        AppdefEntityID[] pid = new AppdefEntityID[] { platform.getEntityId() };
        try {
            MiniResourceTree tree =
                getPlatformMan().getMiniResourceTree(subject, pid, 0);
            
            for (Iterator p = tree.getPlatformIterator(); p.hasNext(); ) {
                MiniPlatformNode pn = (MiniPlatformNode) p.next();
                
                pm.sendAppdefEvent(subject, pn.getPlatform().getEntityId(),
                                   eventType);
                
                for(Iterator s = pn.getServerIterator(); s.hasNext();) {
                    MiniServerNode sn = (MiniServerNode) s.next();
                    pm.sendAppdefEvent(subject, sn.getServer().getEntityId(),
                                       eventType);
                    
                    for(Iterator sv = sn.getServiceIterator(); sv.hasNext();) {
                        MiniServiceNode svn = (MiniServiceNode)sv.next();
                        pm.sendAppdefEvent(subject,
                                           svn.getService().getEntityId(),
                                           eventType);
                    }
                }
            }
        } catch (AppdefEntityNotFoundException e1) {
            log.error("Existing platform not found: " + platform.getEntityId());
        }
    }

    public static AIPlatformLocal getAIQPlatform ( Log log,
                                                   AIPlatformLocalHome aiPlatformLH,
                                                   AIPlatformValue aiPlatformValue )
        throws SystemException {

        // Is there another platform in the queue with the same certdn?
        AIPlatformLocal aiPlatform = null;
        String certdn = aiPlatformValue.getCertdn();
        String fqdn = aiPlatformValue.getFqdn();
        Collection fqdnMatches = null;
        // Try FQDN first
        try {
            fqdnMatches = aiPlatformLH.findByFQDN(fqdn);
        } catch ( FinderException fe ) {
            log.warn("FindByFQDN failed: " + fe, fe);
        }

        if (fqdnMatches == null || fqdnMatches.size() != 1) {
            try {
                aiPlatform = aiPlatformLH.findByCertDN(certdn);
                return aiPlatform;
            } catch (FinderException fe) {
                // Hope that we actually found some by FQDN
                if (fqdnMatches == null || fqdnMatches.size() == 0)
                    log.warn("FindByFQDN and FindByCertDN both failed: " + fe.getMessage());
                else
                    log.warn("Multiple platforms matched FQDN: " +
                             fqdn + " [" + fqdnMatches + "]");
                return null;
            }
        }

        Iterator i = fqdnMatches.iterator();
        return (AIPlatformLocal) i.next();
    }

}
