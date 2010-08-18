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

import javax.security.auth.login.LoginException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.control.server.session.ControlSchedule;
import org.hyperic.hq.control.shared.ControlFrequencyValue;
import org.hyperic.hq.control.shared.ScheduledJobNotFoundException;
import org.hyperic.hq.control.shared.ScheduledJobRemoveException;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.quartz.SchedulerException;

/**
 * Local interface for ControlBoss.
 */
public interface ControlBoss {
	/**
	 * Execute a control action immediately on an appdef entity.
	 * 
	 * @param action
	 *            The action to perform
	 */
	public void doAction(int sessionId, AppdefEntityID id, String action,
			String args) throws PluginException, GroupNotCompatibleException,
			SessionNotFoundException, SessionTimeoutException,
			PermissionException, AppdefEntityNotFoundException;

	/**
	 * Execute a control action This is used for doing scheduled control
	 * actions.
	 * 
	 * @param controlJob
	 *            The control job action name
	 * @param schedule
	 *            The control job schedule
	 */
	public void doAction(int sessionId, AppdefEntityID id, String action,
			ScheduleValue schedule) throws PluginException, SchedulerException,
			SessionNotFoundException, SessionTimeoutException,
			PermissionException, AppdefEntityNotFoundException,
			GroupNotCompatibleException, ApplicationException;

	/**
	 * Schedule a control action on a group entity.
	 * 
	 * @param action
	 *            The action to perform
	 */
	public void doGroupAction(int sessionId, AppdefEntityID groupEnt,
			String action, int[] orderSpec, ScheduleValue schedule)
			throws PluginException, SchedulerException,
			SessionNotFoundException, SessionTimeoutException,
			PermissionException, AppdefEntityNotFoundException,
			GroupNotCompatibleException, ApplicationException;

	/**
	 * Execute a control action immediately on a group entity.
	 * 
	 * @param action
	 *            The action to perform
	 */
	public void doGroupAction(int sessionId, AppdefEntityID groupEnt,
			String action, String args, int[] orderSpec)
			throws PluginException, GroupNotCompatibleException,
			SessionNotFoundException, SessionTimeoutException,
			PermissionException, AppdefEntityNotFoundException;

	/**
	 * Get the actions supported for an appdef entity
	 */
	public List<String> getActions(int sessionId, AppdefEntityID id)
			throws PluginNotFoundException, AppdefEntityNotFoundException,
			SessionNotFoundException, SessionTimeoutException,
			PermissionException, GroupNotCompatibleException;

	/**
	 * Get the actions supported for an appdef entity type
	 */
	public List<String> getActions(int sessionId, AppdefEntityTypeID aetid)
			throws PluginNotFoundException, PermissionException,
			SessionNotFoundException, SessionTimeoutException;

	/**
	 * Check if a group has been enabled for control
	 */
	public boolean isGroupControlEnabled(int sessionId, AppdefEntityID id)
			throws AppdefEntityNotFoundException, PermissionException,
			SessionNotFoundException, SessionTimeoutException;

	/**
	 * Check if the entity's resource supports control
	 */
	public boolean isControlSupported(int sessionId, AppdefResourceValue res)
			throws SessionNotFoundException, SessionTimeoutException;

	/**
	 * Check if the entity's resource supports control
	 */
	public boolean isControlSupported(int sessionId, AppdefEntityTypeID tid)
			throws SessionNotFoundException, SessionTimeoutException;

	/**
	 * Check if anything has been enabled for control
	 */
	public boolean isControlEnabled(int sessionId)
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException;

	/**
	 * Check if an entity has been enabled for control
	 */
	public boolean isControlEnabled(int sessionId, AppdefEntityID id)
			throws AppdefEntityNotFoundException, SessionNotFoundException,
			SessionTimeoutException, PermissionException;

	/**
	 * Finder for all of the scheduled jobs for an appdef entity.
	 * 
	 * @return List of scheduled actions
	 */
	public PageList<ControlSchedule> findScheduledJobs(int sessionId, AppdefEntityID id,
			PageControl pc) throws PluginException,
			ScheduledJobNotFoundException, SessionNotFoundException,
			SessionTimeoutException, PermissionException;

	/**
	 * Remove all of the scheduled jobs for an appdef entity.
	 */
	public void removeScheduledJobs(int sessionId, AppdefEntityID id)
			throws SessionNotFoundException, SessionTimeoutException,
			ScheduledJobRemoveException;

	/**
	 * Get a job history based on appdef id
	 * 
	 * @TODO Implement page controls, Authz integration
	 */
	public PageList<ControlHistory> findJobHistory(int sessionId, AppdefEntityID id,
			PageControl pc) throws PluginException, ApplicationException,
			PermissionException, SessionNotFoundException,
			SessionTimeoutException;

	/**
	 * Group job history detail on group appdef id
	 */
	public PageList<ControlHistory> findGroupJobHistory(int sessionId, AppdefEntityID id,
			int batchJobId, PageControl pc) throws PluginException,
			ApplicationException, SessionNotFoundException,
			SessionTimeoutException, PermissionException,
			AppdefGroupNotFoundException;

	/**
	 * Remove an entry from the control history
	 * 
	 * @TODO Authz integration
	 */
	public void deleteJobHistory(int sessionId, Integer[] ids)
			throws ApplicationException, PermissionException,
			SessionNotFoundException, SessionTimeoutException;

	/**
	 * Obtain the current action that is being executed. If there is no current
	 * running action, null is returned.
	 * 
	 * @return currently running ControlJob.
	 */
	public ControlHistory getCurrentJob(int sessionId, AppdefEntityID id)
			throws ApplicationException, PermissionException,
			SessionNotFoundException, SessionTimeoutException;

	/**
	 * Obtain a control action based on job id
	 * 
	 * @return last ControlJob that ran
	 */
	public ControlHistory getJobByJobId(int sessionId, Integer id)
			throws ApplicationException, PermissionException,
			SessionNotFoundException, SessionTimeoutException;

	/**
	 * Obtain the last control action that fired
	 * 
	 * @return last ControlJob that ran
	 */
	public ControlHistory getLastJob(int sessionId, AppdefEntityID id)
			throws ApplicationException, PermissionException,
			SessionNotFoundException, SessionTimeoutException;

	/**
	 * Obtain a ControlJob based on an id
	 * 
	 * @param triggerName
	 *            The control trigger name
	 * @return The control job that was requested
	 */
	public ControlSchedule getControlJob(int sessionId, Integer id)
			throws PluginException, ApplicationException, PermissionException,
			SessionNotFoundException, SessionTimeoutException;

	/**
	 * Delete a ControlJob based on an id
	 * 
	 * @param ids
	 *            Array of job ids to be deleted
	 */
	public void deleteControlJob(int sessionId, Integer[] ids)
			throws PluginException, ApplicationException, PermissionException,
			SessionNotFoundException, SessionTimeoutException;

	/**
	 * Get a list of recent control actions in decending order
	 */
	public PageList<ControlHistory> getRecentControlActions(int sessionId, int rows, long window)
			throws ApplicationException, PermissionException,
			SessionNotFoundException, SessionTimeoutException;

	/**
	 * Get a list of recent control actions in decending order. Called by RSS
	 * feed so it does not require valid session ID.
	 * 
	 * @throws ApplicationException
	 *             if user is not found
	 * @throws LoginException
	 *             if user account has been disabled
	 */
	public PageList<ControlHistory> getRecentControlActions(String user, int rows, long window)
			throws LoginException, ApplicationException;

	/**
	 * Get a list of pending control actions in decending order
	 */
	public PageList<ControlSchedule> getPendingControlActions(int sessionId, int rows)
			throws ApplicationException, PermissionException,
			SessionNotFoundException, SessionTimeoutException;

	/**
	 * Get a list of most active control operations
	 */
	public PageList<ControlFrequencyValue> getOnDemandControlFrequency(int sessionId, int numToReturn)
			throws ApplicationException, PermissionException,
			ApplicationException, SessionNotFoundException,
			SessionTimeoutException;

	/**
	 * Accept an array of appdef entity Ids and verify control permission on
	 * each entity for specified subject. Return an array containing the set or
	 * subset of entities where subject has control authorization.
	 * 
	 * @return List of entities that are control authorized.
	 */
	public List<AppdefEntityID> batchCheckControlPermissions(int sessionId,
			AppdefEntityID[] entities) throws AppdefEntityNotFoundException,
			PermissionException, SessionNotFoundException,
			SessionTimeoutException;

	/**
	 * Find types of all controllable platforms defined in the system.
	 * 
	 * @return A map of PlatformType names and AppdefEntityTypeIDs.
	 * @throws PermissionException
	 */
	public Map<String,AppdefEntityID> findControllablePlatformTypes(int sessionID)
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException;

	/**
	 * Find types of all controllable servers defined in the system.
	 * 
	 * @return A map of ServerType names and AppdefEntityTypeIDs.
	 * @throws PermissionException
	 */
	public Map<String,AppdefEntityTypeID> findControllableServerTypes(int sessionID)
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException;

	/**
	 * Find types of all controllable services defined in the system.
	 * 
	 * @return A map of ServiceType names and AppdefEntityTypeIDs.
	 * @throws PermissionException
	 */
	public Map<String,AppdefEntityTypeID> findControllableServiceTypes(int sessionID)
			throws SessionNotFoundException, SessionTimeoutException,
			PermissionException;

	/**
	 * Find names of all controllable resources of a given type.
	 * 
	 * @return A map of Service names and AppdefEntityIDs.
	 * @throws PermissionException
	 */
	public Map<String,AppdefEntityID> findControllableResourceNames(int sessionID,
			AppdefEntityTypeID aetid) throws SessionNotFoundException,
			SessionException, PermissionException;

}
