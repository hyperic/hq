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
package org.hyperic.hq.control.shared;

import java.util.List;
import java.util.concurrent.Future;

import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.control.ControlActionResult;
import org.hyperic.hq.control.GroupControlActionResult;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.quartz.SchedulerException;

/**
 * Local interface for ControlManager.
 */
public interface ControlManager {
    /**
     * Enable an entity for control
     */
    public void configureControlPlugin(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException, PluginException, ConfigFetchException,
        AppdefEntityNotFoundException, AgentNotFoundException;

    /**
     * Immediately execute a single control action on a given entity.
     */
    public void doAction(AuthzSubject subject, AppdefEntityID id, String action, String args)
        throws PluginException, PermissionException;

    /**
     * Immediately execute a single control action on a given entity.
     */
    public void doAction(AuthzSubject subject, AppdefEntityID id, String action)
        throws PluginException, PermissionException;

    /**
     * 
     * Immediately execute a single control action on a given entity, receiving
     * a Future that will provide results once they are received asynchronously
     * from the agent
     * @param subject The user
     * @param id The resource against which to execute the action
     * @param action The control action
     * @param args Arguments to the control action
     * @param waitTimeout Timeout in milliseconds to wait asynchronously for
     *        control action results from the agent
     * @return A Future that will provide control action results once received
     *         from the agent or throw a {@link ControlActionTimeoutException}
     *         if results not received within waitTimeout
     */
    public Future<ControlActionResult> doAction(AuthzSubject subject, AppdefEntityID id,
                                                String action, String args, int waitTimeout)
        throws PluginException, PermissionException;

    /**
     * 
     * Immediately execute a single control action on a given entity, receiving
     * a Future that will provide results once they are received asynchronously
     * from the agent
     * @param subject The user
     * @param id The resource against which to execute the action
     * @param action The control action
     * @param defaultTimeout The default timeout in milliseconds used to wait asynchronously for
     *        control action results from the agent.  This is only used if timeout
     *        is not in the resource's control config.
     * @return A Future that will provide control action results once received
     *         from the agent or throw a {@link ControlActionTimeoutException}
     *         if results not received within waitTimeout
     */
    public Future<ControlActionResult> doAction(AuthzSubject subject, AppdefEntityID id,
                                                String action, int defaultTimeout)
        throws PluginException, PermissionException;

    /**
     * Immediately execute a control action (asynchronously) for a group of
     * given entities, receiving a Future that will provide results for each
     * group member once they are received asynchronously from the agent.
     * 
     * If ids are present in the order array, the Future task will block after
     * executing the action against each resource (using the timeout control
     * config prop), waiting for results. The entire group action will be
     * abandoned and marked as Failed if any action fails.
     * 
     * If there are no ids present in the order array, the action will be
     * executed against each resource in the group asynchronously and the Future
     * will wait for results from all members in the group (using the largest of
     * the timeout control config prop for each group member).
     * 
     * @param subject The user
     * @param id The resource against which to execute the action
     * @param action The control action
     * @param args Arguments to the control action
     * @param defaultResourceTimeout The default per-resource timeout used if timeout
     *        is not in the resource's control config. If the operation is
     *        ordered, this timeout will be used waiting for each synchronous
     *        action. If unordered, the largest of the resource timeouts will be
     *        used to wait for all results.
     * @return A Future that will provide control action results once received
     *         from the agent or throw a {@link ControlActionTimeoutException}
     *         if results not received within timeout. If a single member's
     *         operation fails, status of the {@link GroupControlActionResult}
     *         will be ControlConstants.STATUS_FAILED, else it is
     *         STATUS_COMPLETED. If the operation fails, the message of the
     *         {@link GroupControlActionResult} will be set to the message of
     *         the first failed operation, else it is null
     * 
     */
    public Future<GroupControlActionResult> doGroupAction(AuthzSubject subject, AppdefEntityID id,
                                                          String action, String args, int[] order,
                                                          int defaultResourceTimeout)
        throws PluginException, PermissionException, AppdefEntityNotFoundException,
        GroupNotCompatibleException;

    public void doGroupAction(AuthzSubject subject, AppdefEntityID id, String action, String args,
                              int[] order) throws PluginException, PermissionException,
        AppdefEntityNotFoundException, GroupNotCompatibleException;

    /**
     * Schedule a new control action.
     */
    public void scheduleAction(AuthzSubject subject, AppdefEntityID id, String action,
                               ScheduleValue schedule) throws PluginException, PermissionException,
        SchedulerException;

    /**
     * Schedule a single control action for a group of given entities.
     * @throws SchedulerException
     */
    public void scheduleGroupAction(AuthzSubject subject, AppdefEntityID id, String action,
                                    int[] order, ScheduleValue schedule) throws PluginException,
        PermissionException, SchedulerException, GroupNotCompatibleException,
        AppdefEntityNotFoundException;

    /**
     * Get the supported actions for an appdef entity from the local
     * ControlPluginManager
     */
    public List<String> getActions(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException, PluginNotFoundException, AppdefEntityNotFoundException,
        GroupNotCompatibleException;

    /**
     * Get the supported actions for an appdef entity from the local
     * ControlPluginManager
     */
    public List<String> getActions(AuthzSubject subject, AppdefEntityTypeID aetid)
        throws PluginNotFoundException;

    /**
     * Check if a compatible group's members have been enabled for control. A
     * group is enabled for control if and only if all of its members have been
     * enabled for control.
     * @return flag - true if group is enabled
     */
    public boolean isGroupControlEnabled(AuthzSubject subject, AppdefEntityID id)
        throws AppdefEntityNotFoundException, PermissionException;

    /**
     * Checks with the plugin manager to find out if an entity's resource
     * provides support for control.
     * @param resType - appdef entity (of all kinds inc. groups)
     * @return flag - true if supported
     */
    public boolean isControlSupported(AuthzSubject subject, String resType);

    /**
     * Checks with the plugin manager to find out if an entity's resource
     * provides support for control.
     * @param resType - appdef entity (of all kinds inc. groups)
     * @return flag - true if supported
     */
    public boolean isControlSupported(AuthzSubject subject, AppdefEntityID id, String resType);

    /**
     * Check if a an entity has been enabled for control.
     * @return flag - true if enabled
     */
    public boolean isControlEnabled(AuthzSubject subject, AppdefEntityID id);

    /**
     * Check if an entity has been enabled for control
     */
    public void checkControlEnabled(AuthzSubject subject, AppdefEntityID id) throws PluginException;

    /**
     * Send an agent a plugin configuration. This is needed when agents restart,
     * since they do not persist control plugin configuration.
     * @param pluginName Name of the plugin to get the config for
     * @param merge If true, merge the product and control config data
     */
    public byte[] getPluginConfiguration(String pluginName, boolean merge) throws PluginException;

    /**
     * Receive status information about a previous control action
     */
    public void sendCommandResult(int id, int result, long startTime, long endTime, String message);

    /**
     * Accept an array of appdef entity Ids and verify control permission on
     * each entity for specified subject. Return only the set of entities that
     * have authorization.
     * @return List of entities subject is authz to control NOTE: Returns an
     *         empty list when no resources are found.
     */
    public List<AppdefEntityID> batchCheckControlPermissions(AuthzSubject caller,
                                                             AppdefEntityID[] entities)
        throws AppdefEntityNotFoundException, PermissionException;

    public void removeControlHistory(AppdefEntityID id);
}
