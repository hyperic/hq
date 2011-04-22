/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.hqu.rendit.helpers


import org.hyperic.hq.appdef.shared.AIPlatformValue
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.appdef.shared.AIQApprovalException
import org.hyperic.hq.appdef.shared.AIQueueConstants
import org.hyperic.hq.appdef.shared.AIQueueManager;
import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.autoinventory.AIServer
import org.hyperic.util.pager.PageControl

/**
 * The AutodiscoveryHelper can be used to list and approve resources into
 * the inventory.
 */
class AutodiscoveryHelper extends BaseHelper {

    private aiqMan = Bootstrap.getBean(AIQueueManager.class);
    AutodiscoveryHelper(AuthzSubject user) {
        super(user)
    }

    /**
      * Return a List of {@link AIPlatformValue}s in the queue.
      */
    public List getQueue() {
        aiqMan.getQueue(user, true, true, false, PageControl.PAGE_ALL)
    }

    /**
     * Find an AIPlatformValue by fqdn.
     *
     * @param fqdn The platform fqdn to find.
     * @return A {@link AIPlatformValue} with the given fqdn or null if a
     * queued platform with the given fqdn does not exist.
     *
     */
    public AIPlatformValue findByFqdn(String fqdn) {
        aiqMan.findAIPlatformByFqdn(user, fqdn)
    }

    /**
     * Find an AIPlatformValue by id.
     * @param id The id to look up.
     * @return A {@link AIPlatformValue} with the given id or null if a
     * queued platform with the given id does not exist.
     */
    public AIPlatformValue findById(int id) {
        aiqMan.findAIPlatformById(user, id)
    }

    /**
     * Find an AIServer by id.
     * @param id The id to look up.
     * @return A {@link AIServerValue} with the given id or null if a
     * queued server with the given id does not exist.
     */
    public AIServer findServerById(int id) {
    	aiqMan.findAIServerById(user, id)
    }
    
    /**
     * Approve the platform with the given fqdn.
     * @param fqdn The platform fqdn to approve
     * @return A List of {@link org.hyperic.hq.appdef.server.session.AppdefResource}s
     * that were created as a result of processing the queue.
     */
    public List approve(AIPlatformValue platform) {
        // If a platform is a placeholder, don't attempt to approve it.
        List platformIds = []
        if (platform.queueStatus != AIQueueConstants.Q_STATUS_PLACEHOLDER) {
            platformIds.add(platform.id)
        }

        // Only approve servers that are not marked ignored
        List serverIds = platform.AIServerValues.findAll { !it.ignored }.id

        // All IP changes get auto-approved
        List ipIds = platform.AIIpValues.id

        aiqMan.processQueue(user, platformIds, serverIds, ipIds,
                            AIQueueConstants.Q_DECISION_APPROVE)
    }
    
    /**
     * Approve the server.
     * @param server The server to approve
     * @return A List of {@link org.hyperic.hq.appdef.server.session.AppdefResource}s
     * that were created as a result of processing the queue.
     */
    public List approve(AIServer server) throws AIQApprovalException {
        // Only approve servers that are not marked ignored
        if (server.ignored) {
        	throw new AIQApprovalException("Cannot approve an ignored server")
        }
        
        List platformIds = []
        List serverIds = []
        
        serverIds.add(server.id)
        platformIds.add(server.getAIPlatform().id)

        aiqMan.processQueue(user, platformIds, serverIds, null,
                            AIQueueConstants.Q_DECISION_APPROVE)
    }
}
