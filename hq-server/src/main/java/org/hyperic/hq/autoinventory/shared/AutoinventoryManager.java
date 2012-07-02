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
package org.hyperic.hq.autoinventory.shared;

import java.util.List;
import java.util.Map;

import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.*;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.autoinventory.AIHistory;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.autoinventory.CompositeRuntimeResourceReport;
import org.hyperic.hq.autoinventory.DuplicateAIScanNameException;
import org.hyperic.hq.autoinventory.ScanConfigurationCore;
import org.hyperic.hq.autoinventory.ScanStateCore;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;

/**
 * Local interface for AutoinventoryManager.
 */
public interface AutoinventoryManager {
    /**
     * Get server signatures for a set of servertypes.
     * @param serverTypes A List of ServerTypeValue objects representing the
     *        server types to get signatures for. If this is null, all server
     *        signatures are returned.
     * @return A Map, where the keys are the names of the ServerTypeValues, and
     *         the values are the ServerSignature objects.
     */
    public Map<String, ServerSignature> getServerSignatures(AuthzSubject subject,
                                                            List<ServerTypeValue> serverTypes)
        throws AutoinventoryException;

    /**
     * Check if a given Appdef entity supports runtime auto-discovery.
     * @param id The entity id to check.
     * @return true if the given resource supports runtime auto-discovery.
     */
    public boolean isRuntimeDiscoverySupported(AuthzSubject subject, AppdefEntityID id);

    /**
     * Turn off runtime-autodiscovery for a server that no longer exists. Use
     * this method when you know the appdefentity identified by "id" exists, so
     * that we'll be able to successfully find out which agent we should create
     * our AICommandsClient from.
     * @param id The AppdefEntityID of the resource to turn off runtime config
     *        for.
     */
    public void turnOffRuntimeDiscovery(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException;

    /**
     * Turn off runtime-autodiscovery for a server that no longer exists. We
     * need this as a separate method call because when the server no longer
     * exists, we have to manually specify the agent connection to use.
     * @param id The AppdefEntityID of the resource to turn off runtime config
     *        for.
     * @param agentToken Which agent controls the runtime AI scans for this
     *        resource.
     */
    public void turnOffRuntimeDiscovery(AuthzSubject subject, AppdefEntityID id, String agentToken)
        throws PermissionException;

    /**
     * Toggle Runtime-AI config for the given server.
     */
    public void toggleRuntimeScan(AuthzSubject subject, AppdefEntityID id, boolean enable)
        throws PermissionException, AutoinventoryException, ResourceDeletedException;

    /**
     * Start an autoinventory scan.
     * @param aid The appdef entity whose agent we'll talk to.
     * @param scanConfig The scan configuration to use when scanning.
     * @param scanName The name of the scan - this is ignored (i.e. it can be
     *        null) for immediate, one-time scans.
     * @param scanDesc The description of the scan - this is ignored (i.e. it
     *        can be null) for immediate, one-time scans.
     * @param schedule Described when and how often the scan should run. If this
     *        is null, then the scan will be run as an immediate, one-time only
     *        scan.
     */
    public void startScan(AuthzSubject subject, AppdefEntityID aid,
                          ScanConfigurationCore scanConfig, String scanName, String scanDesc,
                          ScheduleValue schedule) throws AgentConnectionException,
        AgentNotFoundException, AutoinventoryException, DuplicateAIScanNameException,
        ScheduleWillNeverFireException, PermissionException;

    /**
     * Start an autoinventory scan by agentToken
     */
    public void startScan(AuthzSubject subject, String agentToken, ScanConfigurationCore scanConfig)
        throws AgentConnectionException, AgentNotFoundException, AutoinventoryException,
        PermissionException;

    /**
     * Stop an autoinventory scan.
     * @param aid The appdef entity whose agent we'll talk to.
     */
    public void stopScan(AuthzSubject subject, AppdefEntityID aid) throws AutoinventoryException;

    /**
     * Get status for an autoinventory scan.
     * @param aid The appdef entity whose agent we'll talk to.
     */
    public ScanStateCore getScanStatus(AuthzSubject subject, AppdefEntityID aid)
        throws AgentNotFoundException, AgentConnectionException, AgentRemoteException,
        AutoinventoryException;

    /**
     * create AIHistory
     */
    public AIHistory createAIHistory(AppdefEntityID id, Integer groupId, Integer batchId,
                                     String subjectName, ScanConfigurationCore config,
                                     String scanName, String scanDesc, Boolean scheduled,
                                     long startTime, long stopTime, long scheduleTime,
                                     String status, String errorMessage)
        throws AutoinventoryException;

    /**
     * remove AIHistory
     */
    public void removeHistory(AIHistory history);

    /**
     * update AIHistory
     */
    public void updateAIHistory(Integer jobId, long endTime, String status, String message);

    /**
     * Get status for an autoinventory scan, given the agentToken
     */
    public ScanStateCore getScanStatusByAgentToken(AuthzSubject subject, String agentToken)
        throws AgentNotFoundException, AgentConnectionException, AgentRemoteException,
        AutoinventoryException;

    /**
     * Called by agents to report platforms, servers, and services detected via
     * autoinventory scans.
     * @param agentToken The token identifying the agent that sent the report.
     * @param stateCore The ScanState that was detected during the autoinventory
     *        scan.
     */
    public AIPlatformValue reportAIData(String agentToken, ScanStateCore stateCore)
        throws AutoinventoryException;

    /**
     * Called by agents to report resources detected at runtime via
     * monitoring-based autoinventory scans. There are some interesting
     * situations that can occur related to synchronization between the server
     * and agent. If runtime scans are turned off for a server, but the agent is
     * never notified (for example if the agent is not running at the time),
     * then the agent is going to eventually report a runtime scan that includes
     * resources detected by that server's runtime scan. If this happens, we
     * detect it and take the opportunity to tell the agent again that it should
     * not perform runtime AI scans for that server. Any resources reported by
     * that server will be ignored. A similar situation occurs when the appdef
     * server has been deleted but the agent was never notified to turn off
     * runtime AI. We handle this in the same way, by telling the agent to turn
     * off runtime scans for that server, and ignoring anything in the report
     * from that server. This method will process all platform and server
     * merging, given by the report. Any services will be added to Zevent queue
     * to be processed in their own transactions.
     * @param agentToken The token identifying the agent that sent the report.
     * @param crrr The CompositeRuntimeResourceReport that was generated during
     *        the runtime autoinventory scan.
     */
    public void reportAIRuntimeReport(String agentToken, CompositeRuntimeResourceReport crrr)
        throws AutoinventoryException, PermissionException, ValidationException,
        ApplicationException;

    /**
     * Handle ResourceZEvents for enabling runtime autodiscovery.
     * @param events A list of ResourceZevents
     */
    public void handleResourceEvents(List<ResourceZevent> events);

    void invokeAutoApprove(AIPlatformValue aiPlatformValue) throws AutoinventoryException;
}
