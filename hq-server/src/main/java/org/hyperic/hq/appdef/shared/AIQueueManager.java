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
package org.hyperic.hq.appdef.shared;

import java.util.List;

import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AIPlatform;
import org.hyperic.hq.autoinventory.AIServer;
import org.hyperic.hq.common.VetoException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for AIQueueManager.
 */
public interface AIQueueManager {
    /**
     * Try to queue a candidate platform discovered via autoinventory.
     * @param aiplatform The platform that we got from the recent autoinventory
     *        data that we are wanting to queue. This may return null if the
     *        appdef platform was removed because the AI platform had a qstat of
     *        "remove" that was approved.
     */
    public AIPlatformValue queue(AuthzSubject subject, AIPlatformValue aiplatform, boolean updateServers,
                                 boolean isApproval, boolean isReport);

    /**
     * Retrieve the contents of the AI queue.
     * @param showIgnored If true, even resources in the AI queue that have the
     *        'ignored' flag set will be returned. By default, resources with
     *        the 'ignored' flag set are excluded when the queue is retrieved.
     * @param showPlaceholders If true, even resources in the AI queue that are
     *        unchanged with respect to appdef will be returned. By default,
     *        resources that are unchanged with respect to appdef are excluded
     *        when the queue is retrieved.
     * @param showAlreadyProcessed If true, even resources that have already
     *        been processed (approved or not approved) will be shown.
     * @return A List of AIPlatformValue objects representing the contents of
     *         the autoinventory queue.
     */
    public PageList<AIPlatformValue> getQueue(AuthzSubject subject, boolean showIgnored, boolean showPlaceholders,
                                              boolean showAlreadyProcessed, PageControl pc);

    /**
     * Get an AIPlatformValue by id.
     * @return An AIPlatformValue with the given id, or null if that platform id
     *         is not present in the queue.
     */
    public AIPlatformValue findAIPlatformById(AuthzSubject subject, int aiplatformID);

    /**
     * Get an AIPlatformValue by FQDN.
     * @return The AIPlatformValue with the given FQDN, or null if that FQDN
     *         does not exist in the queue.
     */
    public AIPlatformValue findAIPlatformByFqdn(AuthzSubject subject, String fqdn);

    /**
     * Get an AIServer by Id.
     * @return The AIServerValue with the given id, or null if that server id
     *         does not exist in the queue.
     */
    public AIServer findAIServerById(AuthzSubject subject, int serverID);

    public void removeAssociatedAIPlatform(Platform platform) throws VetoException;

    /**
     * Get an AIServerValue by name.
     * @return The AIServerValue with the given id, or null if that server name
     *         does not exist in the queue.
     */
    public AIServerValue findAIServerByName(AuthzSubject subject, String name);

    /**
     * Get an AIIp by id.
     * @return The AIIp with the given id, or null if that ip does not exist.
     */
    public AIIpValue findAIIpById(AuthzSubject subject, int ipID);

    /**
     * Get an AIIpValue by address.
     * @return The AIIpValue with the given address, or null if an ip with that
     *         address does not exist in the queue.
     */
    public AIIpValue findAIIpByAddress(AuthzSubject subject, String address);

    /**
     * Process resources in the AI queue. This can be used to approve resources
     * for inclusion into appdef, to ignore or unignore resources in the queue,
     * or to purge resources from the queue.
     * @param platformList A List of aiplatform IDs. This may be null, in which
     *        case it is ignored.
     * @param ipList A List of aiip IDs. This may be null, in which case it is
     *        ignored.
     * @param serverList A List of aiserver IDs. This may be null, in which case
     *        it is ignored.
     * @param action One of the AIQueueCQ _DECISION_XXX constants indicating
     *        what to do with the platforms, ips and servers.
     * @return A List of AppdefResource's that were created as a result of
     *         processing the queue.
     */
    public List<AppdefResource> processQueue(AuthzSubject subject, List<Integer> platformList,
                                             List<Integer> serverList, List<Integer> ipList, int action)
        throws PermissionException, ValidationException, AIQApprovalException;

    /**
     * Remove an AI platform from the queue.
     */
    public void removeFromQueue(AIPlatform aiplatform);

    /**
     * Find a platform given an AI platform id
     */
    public PlatformValue getPlatformByAI(AuthzSubject subject, int aiPlatformID) throws PermissionException,
        PlatformNotFoundException;

    /**
     * Get a platform given an AI platform, returns null if none found
     */
    public AIPlatformValue getAIPlatformByPlatformID(AuthzSubject subject, Integer platformID);

    /**
     * Find an AI platform given an platform
     */
    public Platform getPlatformByAI(AuthzSubject subject, AIPlatform aipLocal) throws PermissionException,
        PlatformNotFoundException;

}
