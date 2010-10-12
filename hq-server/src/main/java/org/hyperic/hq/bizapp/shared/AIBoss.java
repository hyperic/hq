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
package org.hyperic.hq.bizapp.shared;

import java.util.List;
import java.util.Map;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIQApprovalException;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerTypeValue;
import org.hyperic.hq.appdef.shared.UpdateException;
import org.hyperic.hq.appdef.shared.ValidationException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AIHistory;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.DuplicateAIScanNameException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.autoinventory.shared.AIScheduleValue;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/**
 * Local interface for AIBoss.
 */
public interface AIBoss {
    /**
     * Finder for all of the scheduled AI scans for an appdef entity.
     */
    public PageList<AIScheduleValue> findScheduledJobs(int sessionId, AppdefEntityID id, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    public AIScheduleValue findScheduledJobById(int sessionId, Integer id) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException;

    /**
     * Get a job history based on appdef id
     */
    public PageList<AIHistory> findJobHistory(int sessionId, AppdefEntityID id, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException;

    /**
     * Delete a AIJob based on an id
     * @param ids Array of job ids to be deleted
     */
    public void deleteAIJob(int sessionId, java.lang.Integer[] ids) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException, AutoinventoryException;

    /**
     * Get server signatures for a set of servertypes.
     * @param serverTypes A List of ServerTypeValue objects representing the
     *        server types to get signatures for. If this is null, all server
     *        signatures are returned.
     * @return A Map, where the keys are the names of the ServerTypeValues, and
     *         the values are the ServerSignature objects.
     */
    public Map<String, ServerSignature> getServerSignatures(int sessionID, List<ServerTypeValue> serverTypes)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException, AutoinventoryException;

    /**
     * Start an autoinventory scan on a group of platforms
     * @param groupID The ID of the group of platforms to scan.
     * @param scanConfig The scan configuration to use when scanning.
     * @param scanName The name of the scan - this is ignored (i.e. it can be
     *        null) for immediate, one-time scans.
     * @param scanDesc The description of the scan - this is ignored (i.e. it
     *        can be null) for immediate, one-time scans.
     * @param schedule Describes when the scan should be run. If this is null,
     *        then the scan is run as an immediate, one-time only scan.
     */
    public void startGroupScan(int sessionID, int groupID, ScanConfigurationCore scanConfig, String scanName,
                               String scanDesc, ScheduleValue schedule) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, AutoinventoryException, AgentConnectionException,
        AgentNotFoundException, DuplicateAIScanNameException, ScheduleWillNeverFireException,
        GroupNotCompatibleException;

    /**
     * Start an autoinventory scan.
     * @param platformID The platform ID of the platform to scan.
     * @param scanConfig The scan configuration to use when scanning.
     * @param scanName The name of the scan - this is ignored (i.e. it can be
     *        null) for immediate, one-time scans.
     * @param scanDesc The description of the scan - this is ignored (i.e. it
     *        can be null) for immediate, one-time scans.
     * @param schedule Describes when the scan should be run. If this is null,
     *        then the scan is run as an immediate, one-time only scan.
     */
    public void startScan(int sessionID, int platformID, ScanConfigurationCore scanConfig, String scanName,
                          String scanDesc, ScheduleValue schedule) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, AutoinventoryException, AgentConnectionException,
        AgentNotFoundException, DuplicateAIScanNameException, ScheduleWillNeverFireException;

    public void startScan(int sessionID, String agentToken, ScanConfigurationCore scanConfig)
        throws SessionTimeoutException, SessionNotFoundException, PermissionException, AutoinventoryException,
        AgentConnectionException, AgentNotFoundException;

    /**
     * Stop an autoinventory scan.
     * @param platformID The platform ID of the platform to stop scanning on.
     */
    public void stopScan(int sessionID, int platformID) throws SessionTimeoutException, SessionNotFoundException,
        PermissionException, AutoinventoryException, AgentConnectionException, AgentNotFoundException;

    /**
     * Get status for a running autoinventory scan.
     * @param platformID The platform ID of the platform to get scan status for.
     */
    public ScanStateCore getScanStatus(int sessionID, int platformID) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, AgentNotFoundException, AgentConnectionException,
        AgentRemoteException, AutoinventoryException;

    /**
     * Get status for a running autoinventory scan given the agentToken
     */
    public ScanStateCore getScanStatusByAgentToken(int sessionID, String agentToken) throws SessionTimeoutException,
        SessionNotFoundException, PermissionException, AgentNotFoundException, AgentConnectionException,
        AgentRemoteException, AutoinventoryException;

    /**
     * Get the contents of the AI queue.
     * @param showIgnored If true, even resources in the AI queue that have the
     *        'ignored' flag set will be returned. By default, resources with
     *        the 'ignored' flag set are excluded when the queue is retrieved.
     * @param showPlaceholders If true, even resources in the AI queue that are
     *        unchanged with respect to appdef will be returned. By default,
     *        resources that are unchanged with respect to appdef are excluded
     *        when the queue is retrieved.
     * @param pc How the results should be sorted/paged.
     * @return A List of AIPlatformValue objects representing the contents of
     *         the autoinventory queue.
     */
    public PageList<AIPlatformValue> getQueue(int sessionID, boolean showIgnored, boolean showPlaceholders,
                                              PageControl pc) throws SessionNotFoundException, SessionTimeoutException;

    /**
     * Get the contents of the AI queue.
     * @param showIgnored If true, even resources in the AI queue that have the
     *        'ignored' flag set will be returned. By default, resources with
     *        the 'ignored' flag set are excluded when the queue is retrieved.
     * @param showPlaceholders If true, even resources in the AI queue that are
     *        unchanged with respect to appdef will be returned. By default,
     *        resources that are unchanged with respect to appdef are excluded
     *        when the queue is retrieved.
     * @param showAlreadyProcessed If true, even resources that have already
     *        been processed (approved or not approved) will be shown.
     * @param pc How the results should be sorted/paged.
     * @return A List of AIPlatformValue objects representing the contents of
     *         the autoinventory queue.
     */
    public PageList<AIPlatformValue> getQueue(int sessionID, boolean showIgnored, boolean showPlaceholders,
                                              boolean showAlreadyProcessed, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException;

    /**
     * Get details on a single platform from the AI queue, by aiplatformID
     */
    public AIPlatformValue findAIPlatformById(int sessionID, int aiplatformID) throws SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Get details on a single server from the AI queue, by serverID
     */
    public AIServerValue findAIServerById(int sessionID, int serverID) throws SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Get details on a single server from the AI queue, by name
     */
    public AIServerValue findAIServerByName(int sessionID, String name) throws SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Get details on a single ip from the AI queue, by ipID
     */
    public AIIpValue findAIIpById(int sessionID, int ipID) throws SessionNotFoundException, SessionTimeoutException;

    /**
     * Get details on a single ip from the AI queue, by address
     */
    public AIIpValue findAIIpByAddress(int sessionID, String address) throws SessionNotFoundException,
        SessionTimeoutException;

    /**
     * Process queued AI resources.
     * @param platformList A List of platform IDs
     * @param serverList A List of server IDs
     * @param ipList A List of ip IDs
     * @param action One of the AIQueueConstants.Q_DECISION_XXX constants
     *        indicating what to do with the platforms, ips and servers.
     */
    public void processQueue(int sessionID, List<Integer> platformList, List<Integer> serverList, List<Integer> ipList,
                             int action) throws SessionNotFoundException, SessionTimeoutException,
        AIQApprovalException, PermissionException, ValidationException;

    /**
     * Process queued AI resources.
     * @param id The server to enable runtime-AI for.
     * @param doEnable If true, runtime autodiscovery will be enabled, if false,
     *        it will be disabled.
     */
    public void toggleRuntimeScan(AuthzSubject subject, AppdefEntityID id, boolean doEnable)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException, AppdefEntityNotFoundException,
        AppdefGroupNotFoundException, GroupNotCompatibleException, UpdateException, ConfigFetchException,
        EncodingException;

    /**
     * Find an AI Platform from an appdef platform
     */
    public AIPlatformValue findAIPlatformByPlatformID(int sessionId, Integer platformID)
        throws SessionNotFoundException, SessionTimeoutException, PermissionException, PlatformNotFoundException;

}
