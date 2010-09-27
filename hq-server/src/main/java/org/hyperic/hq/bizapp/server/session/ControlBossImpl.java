/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.session;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefManager;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBoss;
import org.hyperic.hq.bizapp.shared.AuthBoss;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.control.server.session.ControlSchedule;
import org.hyperic.hq.control.shared.ControlFrequencyValue;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.control.shared.ScheduledJobNotFoundException;
import org.hyperic.hq.control.shared.ScheduledJobRemoveException;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ControlBossImpl implements ControlBoss {

    private AppdefBoss appdefBoss;
    private SessionManager sessionManager;
    private AppdefManager appdefManager;
    private AuthBoss authBoss;
    private ControlManager controlManager;
    private ControlScheduleManager controlScheduleManager;

    @Autowired
    public ControlBossImpl(SessionManager sessionManager, AppdefManager appdefManager, AuthBoss authBoss,
                           ControlManager controlManager, ControlScheduleManager controlScheduleManager,
                           AppdefBoss appdefBoss) {
        this.appdefManager = appdefManager;
        this.authBoss = authBoss;
        this.controlManager = controlManager;
        this.controlScheduleManager = controlScheduleManager;
        this.sessionManager = sessionManager;
        this.appdefBoss = appdefBoss;
    }

    /**
     * Execute a control action immediately on an appdef entity.
     * 
     * @param action The action to perform
     */
    public void doAction(int sessionId, AppdefEntityID id, String action, String args) throws PluginException,
        GroupNotCompatibleException, SessionNotFoundException, SessionTimeoutException, PermissionException,
        AppdefEntityNotFoundException

    {
        if (id.isGroup()) {
            doGroupAction(sessionId, id, action, args, null);
        } else {
            AuthzSubject subject = sessionManager.getSubject(sessionId);

            controlManager.doAction(subject, id, action, args);
        }
    }

    /**
     * Execute a control action
     * 
     * This is used for doing scheduled control actions.
     * 
     * @param controlJob The control job action name
     * @param schedule The control job schedule
     */
    public void doAction(int sessionId, AppdefEntityID id, String action, ScheduleValue schedule)
        throws PluginException, SchedulerException, SessionNotFoundException, SessionTimeoutException,
        PermissionException, AppdefEntityNotFoundException, GroupNotCompatibleException, ApplicationException {
        if (id.isGroup()) {
            doGroupAction(sessionId, id, action, null, schedule);
        } else {
            AuthzSubject subject = sessionManager.getSubject(sessionId);

            controlManager.scheduleAction(subject, id, action, schedule);
        }
    }

    /**
     * Schedule a control action on a group entity.
     * 
     * @param action The action to perform
     */
    public void doGroupAction(int sessionId, AppdefEntityID groupEnt, String action, int[] orderSpec,
                              ScheduleValue schedule) throws PluginException, SchedulerException,
        SessionNotFoundException, SessionTimeoutException, PermissionException, AppdefEntityNotFoundException,
        GroupNotCompatibleException, ApplicationException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        controlManager.scheduleGroupAction(subject, groupEnt, action, orderSpec, schedule);
    }

    /**
     * Execute a control action immediately on a group entity.
     * 
     * @param action The action to perform
     */
    public void doGroupAction(int sessionId, AppdefEntityID groupEnt, String action, String args, int[] orderSpec)
        throws PluginException, GroupNotCompatibleException, SessionNotFoundException, SessionTimeoutException,
        PermissionException, AppdefEntityNotFoundException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        controlManager.doGroupAction(subject, groupEnt, action, args, orderSpec);
    }

    /**
     * Get the actions supported for an appdef entity
     */
    @Transactional(readOnly=true)
    public List<String> getActions(int sessionId, AppdefEntityID id) throws PluginNotFoundException,
        AppdefEntityNotFoundException, SessionNotFoundException, SessionTimeoutException, PermissionException,
        GroupNotCompatibleException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlManager.getActions(subject, id);
    }

    /**
     * Get the actions supported for an appdef entity type
     */
    @Transactional(readOnly=true)
    public List<String> getActions(int sessionId, AppdefEntityTypeID aetid) throws PluginNotFoundException,
        PermissionException, SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlManager.getActions(subject, aetid);
    }

    /**
     * Check if a group has been enabled for control
     */
    @Transactional(readOnly=true)
    public boolean isGroupControlEnabled(int sessionId, AppdefEntityID id) throws AppdefEntityNotFoundException,
        PermissionException, SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlManager.isGroupControlEnabled(subject, id);
    }

    /**
     * Check if the entity's resource supports control
     */
    @Transactional(readOnly=true)
    public boolean isControlSupported(int sessionId, AppdefResourceValue res) throws SessionNotFoundException,
        SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlManager
            .isControlSupported(subject, res.getEntityId(), res.getAppdefResourceTypeValue().getName());
    }

    /**
     * Check if the entity's resource supports control
     */
    @Transactional(readOnly=true)
    public boolean isControlSupported(int sessionId, AppdefEntityTypeID tid) throws SessionNotFoundException,
        SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlManager.isControlSupported(subject, tid.getAppdefResourceType().getName());
    }

    /**
     * Check if anything has been enabled for control
     * 
     */
    @Transactional(readOnly=true)
    public boolean isControlEnabled(int sessionId) throws SessionNotFoundException, SessionTimeoutException,
        PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        Map<String, AppdefEntityID> platTypes = appdefManager.getControllablePlatformTypes(subject);

        if (platTypes.size() > 0) {
            return true;
        }

        Map<String, AppdefEntityTypeID> svrTypes = appdefManager.getControllableServerTypes(subject);

        if (svrTypes.size() > 0) {
            return true;
        }

        Map<String, AppdefEntityTypeID> svcTypes = appdefManager.getControllableServiceTypes(subject);

        return (svcTypes.size() > 0);
    }

    /**
     * Check if an entity has been enabled for control
     */
    @Transactional(readOnly=true)
    public boolean isControlEnabled(int sessionId, AppdefEntityID id) throws AppdefEntityNotFoundException,
        SessionNotFoundException, SessionTimeoutException, PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlManager.isControlEnabled(subject, id);
    }

    /**
     * Finder for all of the scheduled jobs for an appdef entity.
     * 
     * @return List of scheduled actions
     */
    @Transactional(readOnly=true)
    public PageList<ControlSchedule> findScheduledJobs(int sessionId, AppdefEntityID id, PageControl pc)
        throws PluginException, ScheduledJobNotFoundException, SessionNotFoundException, SessionTimeoutException,
        PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlScheduleManager.findScheduledJobs(subject, id, pc);
    }

    /**
     * Remove all of the scheduled jobs for an appdef entity.
     */
    public void removeScheduledJobs(int sessionId, AppdefEntityID id) throws SessionNotFoundException,
        SessionTimeoutException, ScheduledJobRemoveException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        controlScheduleManager.removeScheduledJobs(subject, id);
    }

    /**
     * Get a job history based on appdef id
     * 
     * @TODO Implement page controls, Authz integration
     */
    @Transactional(readOnly=true)
    public PageList<ControlHistory> findJobHistory(int sessionId, AppdefEntityID id, PageControl pc)
        throws PluginException, ApplicationException, PermissionException, SessionNotFoundException,
        SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlScheduleManager.findJobHistory(subject, id, pc);
    }

    /**
     * Group job history detail on group appdef id
     */
    @Transactional(readOnly=true)
    public PageList<ControlHistory> findGroupJobHistory(int sessionId, AppdefEntityID id, int batchJobId, PageControl pc)
        throws PluginException, ApplicationException, SessionNotFoundException, SessionTimeoutException,
        PermissionException, AppdefGroupNotFoundException {
        if (!id.isGroup()) {
            throw new IllegalArgumentException("Invalid group entity specified");
        }

        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlScheduleManager.findGroupJobHistory(subject, batchJobId, id, pc);
    }

    /**
     * Remove an entry from the control history
     * 
     * @TODO Authz integration
     */
    public void deleteJobHistory(int sessionId, Integer[] ids) throws ApplicationException, PermissionException,
        SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        controlScheduleManager.deleteJobHistory(subject, ids);
    }

    /**
     * Obtain the current action that is being executed. If there is no current
     * running action, null is returned.
     * 
     * @return currently running ControlJob.
     */
    @Transactional(readOnly=true)
    public ControlHistory getCurrentJob(int sessionId, AppdefEntityID id) throws ApplicationException,
        PermissionException, SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlScheduleManager.getCurrentJob(subject, id);
    }

    /**
     * Obtain a control action based on job id
     * 
     * @return last ControlJob that ran
     */
    @Transactional(readOnly=true)
    public ControlHistory getJobByJobId(int sessionId, Integer id) throws ApplicationException, PermissionException,
        SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlScheduleManager.getJobByJobId(subject, id);
    }

    /**
     * Obtain the last control action that fired
     * 
     * @return last ControlJob that ran
     */
    @Transactional(readOnly=true)
    public ControlHistory getLastJob(int sessionId, AppdefEntityID id) throws ApplicationException,
        PermissionException, SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlScheduleManager.getLastJob(subject, id);
    }

    /**
     * Obtain a ControlJob based on an id
     * 
     * @param triggerName The control trigger name
     * 
     * @return The control job that was requested
     */
    @Transactional(readOnly=true)
    public ControlSchedule getControlJob(int sessionId, Integer id) throws PluginException, ApplicationException,
        PermissionException, SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlScheduleManager.getControlJob(subject, id);
    }

    /**
     * Delete a ControlJob based on an id
     * 
     * @param ids Array of job ids to be deleted
     */
    public void deleteControlJob(int sessionId, Integer[] ids) throws PluginException, ApplicationException,
        PermissionException, SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        controlScheduleManager.deleteControlJob(subject, ids);
    }

    // Dashboard routines

    /**
     * Get a list of recent control actions in decending order
     */
    @Transactional(readOnly=true)
    public PageList<ControlHistory> getRecentControlActions(int sessionId, int rows, long window)
        throws ApplicationException, PermissionException, SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlScheduleManager.getRecentControlActions(subject, rows, window);
    }

    /**
     * Get a list of recent control actions in decending order. Called by RSS
     * feed so it does not require valid session ID.
     * 
     * @throws ApplicationException if user is not found
     * @throws LoginException if user account has been disabled
     */
    @Transactional(readOnly=true)
    public PageList<ControlHistory> getRecentControlActions(String user, int rows, long window) throws LoginException,
        ApplicationException {
        int sessionId = authBoss.getUnauthSessionId(user);

        return getRecentControlActions(sessionId, rows, window);
    }

    /**
     * Get a list of pending control actions in decending order
     */
    @Transactional(readOnly=true)
    public PageList<ControlSchedule> getPendingControlActions(int sessionId, int rows) throws ApplicationException,
        PermissionException, SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlScheduleManager.getPendingControlActions(subject, rows);
    }

    /**
     * Get a list of most active control operations
     */
    @Transactional(readOnly=true)
    public PageList<ControlFrequencyValue> getOnDemandControlFrequency(int sessionId, int numToReturn)
        throws ApplicationException, PermissionException, ApplicationException, SessionNotFoundException,
        SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlScheduleManager.getOnDemandControlFrequency(subject, numToReturn);
    }

    /**
     * Accept an array of appdef entity Ids and verify control permission on
     * each entity for specified subject. Return an array containing the set or
     * subset of entities where subject has control authorization.
     * 
     * @return List of entities that are control authorized.
     */
    public List<AppdefEntityID> batchCheckControlPermissions(int sessionId, AppdefEntityID[] entities)
        throws AppdefEntityNotFoundException, PermissionException, SessionNotFoundException, SessionTimeoutException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);

        return controlManager.batchCheckControlPermissions(subject, entities);
    }

    /**
     * Find types of all controllable platforms defined in the system.
     * 
     * @return A map of PlatformType names and AppdefEntityTypeIDs.
     * @throws PermissionException
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityID> findControllablePlatformTypes(int sessionID) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);

        return appdefManager.getControllablePlatformTypes(subject);
    }

    /**
     * Find types of all controllable servers defined in the system.
     * 
     * @return A map of ServerType names and AppdefEntityTypeIDs.
     * @throws PermissionException
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityTypeID> findControllableServerTypes(int sessionID) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);

        return appdefManager.getControllableServerTypes(subject);
    }

    /**
     * Find types of all controllable services defined in the system.
     * 
     * @return A map of ServiceType names and AppdefEntityTypeIDs.
     * @throws PermissionException
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityTypeID> findControllableServiceTypes(int sessionID) throws SessionNotFoundException,
        SessionTimeoutException, PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);

        return appdefManager.getControllableServiceTypes(subject);
    }

    /**
     * Find names of all controllable resources of a given type.
     * 
     * @return A map of Service names and AppdefEntityIDs.
     * @throws PermissionException
     */
    @Transactional(readOnly=true)
    public Map<String, AppdefEntityID> findControllableResourceNames(int sessionID, AppdefEntityTypeID aetid)
        throws SessionNotFoundException, SessionException, PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionID);
        Map<String, AppdefEntityID> result;
        int groupType;

        // Return based on type
        switch (aetid.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                result = appdefManager.getControllablePlatformNames(subject, aetid.getID());
                groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                result = appdefManager.getControllableServerNames(subject, aetid.getID());
                groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                result = appdefManager.getControllableServiceNames(subject, aetid.getID());
                groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC;
                break;
            default:
                throw new IllegalArgumentException("Unsupported appdef type " + aetid.getType());
        }

        try {
            // Get the controllable groups, too

            List<AppdefResourceValue> groups = appdefBoss.findCompatInventory(sessionID, groupType,
                AppdefEntityConstants.APPDEF_TYPE_GROUP, aetid.getType(), aetid.getID(), null, null,
                PageControl.PAGE_ALL);

            for (Iterator<AppdefResourceValue> i = groups.iterator(); i.hasNext();) {
                AppdefResourceValue group = i.next();

                if (isControlSupported(sessionID, group)) {
                    result.put(group.getName(), group.getEntityId());
                }
            }
        } catch (AppdefEntityNotFoundException e) {
            // Nothing to worry about
        } catch (PermissionException e) {
            // Nothing to worry about
        }

        return result;
    }
}