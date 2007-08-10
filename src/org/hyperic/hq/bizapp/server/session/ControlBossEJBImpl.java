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

package org.hyperic.hq.bizapp.server.session;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefGroupNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefManagerLocal;
import org.hyperic.hq.appdef.shared.AppdefManagerUtil;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.shared.AppdefBossLocal;
import org.hyperic.hq.bizapp.shared.AppdefBossUtil;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.control.shared.ControlHistoryValue;
import org.hyperic.hq.control.shared.ControlScheduleValue;
import org.hyperic.hq.control.shared.ScheduledJobNotFoundException;
import org.hyperic.hq.control.shared.ScheduledJobRemoveException;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

/** 
 * @ejb:bean name="ControlBoss"
 *      jndi-name="ejb/bizapp/ControlBoss"
 *      local-jndi-name="LocalControlBoss"
 *      view-type="both"
 *      type="Stateless"
 *      
 * @ejb:transaction type="Required"
 */
public class ControlBossEJBImpl extends BizappSessionEJB implements SessionBean
{
    // Our log instance
    private Log log = LogFactory.getLog(ControlBossEJBImpl.class.getName());

    // Session manager
    private SessionManager sessionManager = SessionManager.getInstance();

    AppdefManagerLocal appdefMgr = null;
    public AppdefManagerLocal getAppdefManager() {
        if(appdefMgr == null){
            try {
                appdefMgr = AppdefManagerUtil.getLocalHome().create();
            } catch(Exception exc){
                throw new SystemException(exc);
            }
        }
        return appdefMgr;
    }

    /** @ejb:create-method */
    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
    
    /**
     * Execute a control action immediately on an appdef entity.
     * @ejb:interface-method
     * @param action The action to perform
     */
    public void doAction(int sessionId, AppdefEntityID id, String action,
                         String args)
        throws PluginException, GroupNotCompatibleException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, AppdefEntityNotFoundException
               
    {
        AuthzSubjectValue subject;
        
        if (id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            doGroupAction(sessionId, id, action, args, null);
        } else {
            subject = sessionManager.getSubject(sessionId);

            getControlManager().doAction(subject, id, action, args);
        }
    }

    /**
     * Execute a control action
     *
     * This is used for doing scheduled control actions.
     *
     * @ejb:interface-method
     * @param controlJob The control job action name
     * @param schedule The control job schedule
     */
    public void doAction(int sessionId, AppdefEntityID id,
                         String action, ScheduleValue schedule)
        throws PluginException, ScheduleWillNeverFireException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, AppdefEntityNotFoundException,
               GroupNotCompatibleException, ApplicationException
    {
        AuthzSubjectValue subject;
        if (id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            doGroupAction(sessionId, id, action, null, schedule);
        } else {
            subject = sessionManager.getSubject(sessionId);
            getControlManager().doAction(subject, id, action, schedule);
        }
    }

    /**
     * Schedule a control action on a group entity.
     * 
     * @ejb:interface-method
     * @param action The action to perform
     */
    public void doGroupAction(int sessionId, AppdefEntityID groupEnt,
                              String action, int[] orderSpec, 
                              ScheduleValue schedule)
        throws PluginException, ScheduleWillNeverFireException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, AppdefEntityNotFoundException,
               GroupNotCompatibleException, ApplicationException
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);

        getControlManager().doGroupAction(subject, groupEnt, action, orderSpec,
                                          schedule);
    }

    /**
     * Execute a control action immediately on a group entity.
     * 
     * @ejb:interface-method
     * @param action The action to perform
     */
    public void doGroupAction(int sessionId, AppdefEntityID groupEnt,
                              String action, String args, int[] orderSpec)
        throws PluginException, GroupNotCompatibleException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, AppdefEntityNotFoundException
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);

        getControlManager().doGroupAction(subject, groupEnt, action, args,
                                          orderSpec);
    }

    /**
     * Get the actions supported for an appdef entity
     * @ejb:interface-method
     */
    public List getActions(int sessionId, AppdefEntityID id)
        throws PluginNotFoundException, AppdefEntityNotFoundException,
               SessionNotFoundException, SessionTimeoutException
    {
        // fix 5874
        // controlManager.getActions needs to look up the platform
        // this needs to be done by the overlord since the caller may have
        // control for the server, but lack view for the platform
        return getControlManager().getActions(getOverlord(), id);
    }
    
    /**
     * Get the actions supported for an appdef entity type
     * @ejb:interface-method
     */
    public List getActions(int sessionId, AppdefEntityTypeID aetid)
        throws PluginNotFoundException, PermissionException,
               SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = sessionManager.getSubject(sessionId);
    
        return getControlManager().getActions(subject, aetid);
    }

    /**
     * Check if a group has been enabled for control
     * @ejb:interface-method
     */
    public boolean isGroupControlEnabled(int sessionId, AppdefEntityID id)
        throws AppdefEntityNotFoundException, PermissionException,
               SessionNotFoundException, SessionTimeoutException
   {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);

        return getControlManager().isGroupControlEnabled(subject, id);
    }

    /**
     * Check if the entity's resource supports control
     * @ejb:interface-method
     */
    public boolean isControlSupported (int sessionId, AppdefResourceValue res)
        throws SessionNotFoundException, SessionTimeoutException
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);

        return getControlManager().
            isControlSupported(subject,
                               res.getAppdefResourceTypeValue());
    }

    /**
     * Check if the entity's resource supports control
     * @ejb:interface-method
     */
    public boolean isControlSupported (int sessionId, AppdefEntityTypeID tid)
        throws SessionNotFoundException, SessionTimeoutException               
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);

        return getControlManager().
            isControlSupported(subject,
                               tid.getAppdefResourceTypeValue());
    }

    /**
     * Check if anything has been enabled for control
     * @ejb:interface-method
     */
    public boolean isControlEnabled(int sessionId)
        throws SessionNotFoundException,  SessionTimeoutException,
               PermissionException
    {
        AuthzSubjectValue subject = sessionManager.getSubject(sessionId);

        Map platTypes =
            getAppdefManager().getControllablePlatformTypes(subject);
        
        if (platTypes.size() > 0)
            return true;
        
        Map svrTypes = getAppdefManager().getControllableServerTypes(subject);
        
        if (svrTypes.size() > 0)
            return true;
        
        Map svcTypes = getAppdefManager().getControllableServiceTypes(subject);
        
        return (svcTypes.size() > 0);
    }
    /**
     * Check if an entity has been enabled for control
     * @ejb:interface-method
     */
    public boolean isControlEnabled(int sessionId, AppdefEntityID id)
        throws AppdefEntityNotFoundException, SessionNotFoundException, 
               SessionTimeoutException, PermissionException
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);
    
        return getControlManager().isControlEnabled(subject, id);
    }

    /**
     * Finder for all of the scheduled jobs for an appdef entity.
     * @ejb:interface-method
     * @return List of scheduled actions
     */
    public PageList findScheduledJobs(int sessionId, AppdefEntityID id,
                                      PageControl pc)
        throws PluginException, ScheduledJobNotFoundException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);
     
        return getControlScheduleManager().findScheduledJobs(subject, id, pc);
    }
    
    /**
     * Remove all of the scheduled jobs for an appdef entity.
     * @ejb:interface-method
     */
    public void removeScheduledJobs(int sessionId, AppdefEntityID id)
        throws SessionNotFoundException, SessionTimeoutException,
               ScheduledJobRemoveException {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);
     
        getControlScheduleManager().removeScheduledJobs(subject, id);
    }

    /**
     * Get a job history based on appdef id
     *
     * @ejb:interface-method
     *
     * @TODO Implement page controls, Authz integration
     */
    public PageList findJobHistory(int sessionId, AppdefEntityID id, 
                                   PageControl pc)
        throws PluginException, ApplicationException, PermissionException,
               SessionNotFoundException, SessionTimeoutException               
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);
        
        return getControlScheduleManager().findJobHistory(subject, id, pc);
    }

    /**
     * Group job history detail on group appdef id
     *
     * @ejb:interface-method
     */
    public PageList findGroupJobHistory(int sessionId, AppdefEntityID id, 
                                        int batchJobId, PageControl pc)
        throws PluginException, ApplicationException,
               SessionNotFoundException, SessionTimeoutException,
               PermissionException, AppdefGroupNotFoundException
    {
        AuthzSubjectValue subject;

        if (id.getType() != AppdefEntityConstants.APPDEF_TYPE_GROUP)
          throw new IllegalArgumentException ("Invalid group entity specified");

        subject = sessionManager.getSubject(sessionId);
     
        return getControlScheduleManager().findGroupJobHistory(subject,
                                                               batchJobId,
                                                               id, pc);
    }

    /**
     * Remove an entry from the control history
     *
     * @ejb:interface-method
     *
     * @TODO Authz integration
     */
    public void deleteJobHistory(int sessionId, Integer[] ids)
        throws ApplicationException, PermissionException,
               SessionNotFoundException, SessionTimeoutException               
    {
        AuthzSubjectValue subject;

        subject = sessionManager.getSubject(sessionId);

        getControlScheduleManager().deleteJobHistory(subject, ids);
    }
   
    /**
     * Obtain the current action that is being executed.  If there is
     * no current running action, null is returned.
     *
     * @ejb:interface-method
     *
     * @return currently running ControlJob.
     */
     public ControlHistory getCurrentJob(int sessionId, AppdefEntityID id)
         throws ApplicationException, PermissionException,
                SessionNotFoundException, SessionTimeoutException                
    {
        AuthzSubjectValue subject;

        subject = sessionManager.getSubject(sessionId);

        return getControlScheduleManager().getCurrentJob(subject, id);
    }

    /**
     * Obtain a control action based on job id
     *
     * @ejb:interface-method
     *
     * @return last ControlJob that ran
     */
    public ControlHistory getJobByJobId(int sessionId, Integer id)
        throws ApplicationException, PermissionException,
               SessionNotFoundException, SessionTimeoutException
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);
     
        return getControlScheduleManager().getJobByJobId(subject, id);
    }

    /**
     * Obtain the last control action that fired
     *
     * @ejb:interface-method
     *
     * @return last ControlJob that ran
     */
    public ControlHistory getLastJob(int sessionId, AppdefEntityID id)
        throws ApplicationException, PermissionException,
               SessionNotFoundException, SessionTimeoutException
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);
     
        return getControlScheduleManager().getLastJob(subject, id);
    }

    /**
     * Obtain a ControlJob based on an id
     *
     * @ejb:interface-method
     * @param triggerName The control trigger name
     *
     * @return The control job that was requested
     */
    public ControlScheduleValue getControlJob(int sessionId, 
                                              Integer id)
        throws PluginException, ApplicationException, PermissionException,
               SessionNotFoundException, SessionTimeoutException               
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);
     
        return getControlScheduleManager().getControlJob(subject, id);
    }

    /**
     * Delete a ControlJob based on an id
     *
     * @ejb:interface-method
     * @param ids Array of job ids to be deleted
     */
    public void deleteControlJob(int sessionId, Integer[] ids)
        throws PluginException, ApplicationException, PermissionException,
               SessionNotFoundException, SessionTimeoutException
    {
        AuthzSubjectValue subject;
        
        subject = sessionManager.getSubject(sessionId);

        getControlScheduleManager().deleteControlJob(subject, ids);
    }

    // Dashboard routines

    /**
     * Get a list of recent control actions in decending order
     *
     * @ejb:interface-method
     */
    public PageList getRecentControlActions(int sessionId, int rows,
                                            long window)
        throws ApplicationException, PermissionException,
               SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject = sessionManager.getSubject(sessionId);
        
        return getControlScheduleManager().getRecentControlActions(subject,
                                                                   rows,
                                                                   window);
    }


    /**
     * Get a list of recent control actions in decending order.  Called by RSS
     * feed so it does not require valid session ID.
     * @throws ApplicationException if user is not found
     * @throws LoginException if user account has been disabled
     *
     * @ejb:interface-method
     */
    public PageList getRecentControlActions(String user, int rows, long window)
        throws LoginException, ApplicationException {
        int sessionId = getAuthManager().getUnauthSessionId(user);
        return getRecentControlActions(sessionId, rows, window);
    }
    
    /**
     * Get a list of pending control actions in decending order
     * 
     * @ejb:interface-method
     */
    public PageList getPendingControlActions(int sessionId, int rows)
        throws ApplicationException, PermissionException,
               SessionNotFoundException, SessionTimeoutException
    {
        AuthzSubjectValue subject = sessionManager.getSubject(sessionId);
        
        return getControlScheduleManager().getPendingControlActions(subject,
                                                                    rows);
    }

    /**
     * Get a list of most active control operations
     *
     * @ejb:interface-method
     */
    public PageList getOnDemandControlFrequency(int sessionId, int numToReturn)
        throws ApplicationException, PermissionException, ApplicationException,
               SessionNotFoundException, SessionTimeoutException
    {
        AuthzSubjectValue subject = sessionManager.getSubject(sessionId);

        return getControlScheduleManager().
            getOnDemandControlFrequency(subject, numToReturn);
    }

   /**
    * Accept an array of appdef entity Ids and verify control permission
    * on each entity for specified subject. Return an array containing 
    * the set or subset of entities where subject has control authorization.
    *
    * @return    List of entities that are control authorized.
    * @ejb:interface-method
    */
    public List batchCheckControlPermissions(int sessionId,
                                             AppdefEntityID[] entities)
        throws AppdefEntityNotFoundException, PermissionException,
               SessionNotFoundException, SessionTimeoutException {
        AuthzSubjectValue subject;
        subject = sessionManager.getSubject(sessionId);

        return getControlManager().batchCheckControlPermissions(subject,
                                                                entities);
    }

    /**
     * Find types of all controllable platforms defined in the system.
     * 
     * @return A map of PlatformType names and AppdefEntityTypeIDs.
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map findControllablePlatformTypes(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        AuthzSubjectValue subject = sessionManager.getSubject(sessionID);
        return getAppdefManager().getControllablePlatformTypes(subject);
    }
    
    /**
     * Find types of all controllable servers defined in the system.
     *
     * @return A map of ServerType names and AppdefEntityTypeIDs.
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map findControllableServerTypes(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        AuthzSubjectValue subject = sessionManager.getSubject(sessionID);
        return getAppdefManager().getControllableServerTypes(subject);
    }
    /**
     * Find types of all controllable services defined in the system.
     *
     * @return A map of ServiceType names and AppdefEntityTypeIDs.
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map findControllableServiceTypes(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        AuthzSubjectValue subject = sessionManager.getSubject(sessionID);
        return getAppdefManager().getControllableServiceTypes(subject);
    }
    /**
     * Find names of all controllable resources of a given type.
     *
     * @return A map of Service names and AppdefEntityIDs.
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map findControllableResourceNames(int sessionID,
                                             AppdefEntityTypeID aetid)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException {
        AuthzSubjectValue subject = sessionManager.getSubject(sessionID);
        
        Map ret;
        int groupType;
        
        // Return based on type
        switch (aetid.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            ret = getAppdefManager()
                .getControllablePlatformNames(subject, aetid.getID());
            groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            ret = getAppdefManager()
                .getControllableServerNames(subject, aetid.getID());
            groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_PS;
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            ret = getAppdefManager()
                .getControllableServiceNames(subject, aetid.getID());
            groupType = AppdefEntityConstants.APPDEF_TYPE_GROUP_COMPAT_SVC;
            break;
        default:
            throw new IllegalArgumentException
                ( "Unsupported appdef type " + aetid.getType() );
        }
        
        try {
            // Get the controllable groups, too
            AppdefBossLocal aboss = AppdefBossUtil.getLocalHome().create();
            
            List groups = aboss.findCompatInventory(
                sessionID, groupType, AppdefEntityConstants.APPDEF_TYPE_GROUP,
                aetid.getType(), aetid.getID(), null, null,
                PageControl.PAGE_ALL);
            
            for (Iterator it = groups.iterator(); it.hasNext(); ) {
                AppdefResourceValue grp = (AppdefResourceValue) it.next();
                if (isControlSupported(sessionID, grp))
                    ret.put(grp.getName(), grp.getEntityId());
            }
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (AppdefEntityNotFoundException e) {
            // Nothing to worry about
        } catch (PermissionException e) {
            // Nothing to worry about
        }
        
        return ret;
    }
}
