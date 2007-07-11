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

package org.hyperic.hq.control.server.session;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AgentConnectionUtil;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ApplicationManagerUtil;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.ConfigManagerUtil;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.ServerManagerUtil;
import org.hyperic.hq.appdef.shared.ServiceManagerUtil;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.control.agent.client.ControlCommandsClient;
import org.hyperic.hq.control.server.session.ControlHistory;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlScheduleManagerLocal;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.ControlPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.server.session.ProductManagerEJBImpl;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.hq.scheduler.ScheduleWillNeverFireException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;

/** The server-side control system.
 *
 * @ejb:bean name="ControlManager"
 *      jndi-name="ejb/control/ControlManager"
 *      local-jndi-name="LocalControlManager"
 *      view-type="local"
 *      type="Stateless"
 */
public class ControlManagerEJBImpl implements SessionBean {

    private final Log log =
        LogFactory.getLog(ControlManagerEJBImpl.class.getName());

    private ControlPluginManager _controlManager;
    private ControlScheduleManagerLocal _controlScheduleManager;

    private ControlHistoryDAO getControlHistoryDAO() {
        return DAOFactory.getDAOFactory().getControlHistoryDAO();
    }

    /** @ejb:create-method */
    public void ejbCreate() {
       
        // Get reference to the control plugin manager
        try {
            _controlManager = (ControlPluginManager)ProductManagerEJBImpl.
                getOne().getPluginManager(ProductPlugin.TYPE_CONTROL);
        } catch (Exception e) {
            this.log.error("Unable to get plugin manager", e);
        }

        // Get a reference to the control scheduler ejb
        _controlScheduleManager = ControlScheduleManagerEJBImpl.getOne();
    }

    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}

    /**
     * Enable an entity for control
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void configureControlPlugin(AuthzSubjectValue subject,
                                       AppdefEntityID id)
        throws PermissionException, PluginException, ConfigFetchException,
               AppdefEntityNotFoundException, AgentNotFoundException
    {
        // authz check
        checkModifyPermission(subject, id);

        String pluginName, pluginType;
        ConfigResponse mergedResponse;
        
        pluginName = id.toString();
        
        try {
            pluginType = getPlatformManager().getPlatformPluginName(id);
            ConfigManagerLocal cMan = 
                ConfigManagerUtil.getLocalHome().create();
            mergedResponse = 
                cMan.getMergedConfigResponse(subject, 
                                             ProductPlugin.TYPE_CONTROL,
                                             id, true);
            ControlCommandsClient client =
                new ControlCommandsClient(AgentConnectionUtil.getClient(id));
            client.controlPluginAdd(pluginName, pluginType, mergedResponse);
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (EncodingException e) {
            throw new PluginException("Unable to decode config", e);
        } catch (AgentConnectionException e) {
            throw new PluginException("Agent error: " + e.getMessage(), e);
        } catch (AgentRemoteException e) {
            throw new PluginException("Agent error: " + e.getMessage(), e);
        }
    }
                  
    /**
     * Execute a single control action on a given entity.
     *
     * @ejb:interface-method view-type="local"
     * @ejb:transaction type="SUPPORTS"
     */
    public void doAction(AuthzSubjectValue subject, AppdefEntityID id,
                         String action, String args)
        throws PluginException, PermissionException
    {
        // This method doesn't support groups.
        if (id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP)
            throw new IllegalArgumentException ("Cannot perform single "+
                                                "action on a group.");
    
        checkControlEnabled(subject, id);
        checkControlPermission(subject, id);
    
        _controlScheduleManager.doSingleAction(id, subject, action,
                                               args, null);
    }

    /**
     * Schedule a new control action.
     * 
     * @ejb:interface-method view-type="local"
     * @ejb:transaction type="SUPPORTS"
     */
    public void doAction(AuthzSubjectValue subject, AppdefEntityID id,
                         String action, ScheduleValue schedule)
        throws PluginException,
               PermissionException, ScheduleWillNeverFireException
    {
        // This method doesn't support groups.
        if (id.getType() == AppdefEntityConstants.APPDEF_TYPE_GROUP)
            throw new IllegalArgumentException ("Cannot perform single "+
                                                "action on a group.");
    
        checkControlEnabled(subject, id);
        checkControlPermission(subject, id);
    
        _controlScheduleManager.doScheduledAction(id, subject, action,
                                                  schedule, null);
    }

    /**
     * Single control action for a group of given entities. 
     *
     * @ejb:interface-method view-type="local"
     * @ejb:transaction type="SUPPORTS"
     */
    public void doGroupAction(AuthzSubjectValue subject, 
                              AppdefEntityID id, String action, 
                              String args, int[] order)
        throws PluginException, PermissionException, 
               AppdefEntityNotFoundException, GroupNotCompatibleException
    {
        List groupMembers  = GroupUtil.getCompatGroupMembers(subject, id,
                                                             order, 
                                                             PageControl.PAGE_ALL);

        // For each entity in the list, sanity check config and permissions
        for (Iterator i = groupMembers.iterator(); i.hasNext();) {
            AppdefEntityID entity = (AppdefEntityID) i.next();

            checkControlEnabled(subject, entity);
            checkControlPermission(subject, entity);
        }
       
        _controlScheduleManager.doSingleAction(id, subject, action,
                                               args, order);
    }

    /**
     * Schedule a single control action for a group of given entities.
     * 
     * @ejb:interface-method view-type="local"
     * @ejb:transaction type="SUPPORTS"
     */
    public void doGroupAction(AuthzSubjectValue subject, 
                              AppdefEntityID id,
                              String action, int[] order, 
                              ScheduleValue schedule)
        throws PluginException,
               PermissionException, GroupNotCompatibleException,
               AppdefEntityNotFoundException, ScheduleWillNeverFireException
    {
        List groupMembers = GroupUtil.getCompatGroupMembers(subject, id, 
                                                            order,
                                                            PageControl.PAGE_ALL);

        // For each entity in the list, sanity check config and permissions
        for (Iterator i = groupMembers.iterator(); i.hasNext();) {
            AppdefEntityID entity = (AppdefEntityID) i.next();

            checkControlEnabled(subject, entity);
            checkControlPermission(subject, entity);
        }

        _controlScheduleManager.doScheduledAction(id, subject, action,
                                                  schedule, order);
    }

    /**
     * Get the supported actions for an appdef entity from the local
     * ControlPluginManager
     *
     * @ejb:interface-method view-type="local"
     * @ejb:transaction type="SUPPORTS"
     */
    public List getActions(AuthzSubjectValue subject,
                           AppdefEntityID id)
        throws PluginNotFoundException, AppdefEntityNotFoundException
    {
        String pluginName;
   
        pluginName = getPlatformManager().getPlatformPluginName(id);
        return _controlManager.getActions(pluginName);
    }

    /**
     * Get the supported actions for an appdef entity from the local
     * ControlPluginManager
     *
     * @ejb:interface-method view-type="local"
     * @ejb:transaction type="SUPPORTS"
     */
    public List getActions(AuthzSubjectValue subject, AppdefEntityTypeID aetid)
        throws PluginNotFoundException {
        String pluginName = aetid.getAppdefResourceTypeValue().getName();        
        return _controlManager.getActions(pluginName);
    }

    /**
     * Check if a compatible group's members have been enabled for control.
     * A group is enabled for control if and only if all of its members
     * have been enabled for control.
     * @return flag - true if group is enabled
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public boolean isGroupControlEnabled(AuthzSubjectValue subject,
                                         AppdefEntityID id) 
        throws AppdefEntityNotFoundException, PermissionException 
    {
        if (id.getType() != AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            throw new IllegalArgumentException ("Expecting entity of type "+
                                                "group.");
        }

        List members;

        try {
            members = GroupUtil.getCompatGroupMembers(subject, id, null);
        } catch (GroupNotCompatibleException ex) {
            // only compatible groups are controllable
            return false;
        }

        if (members.isEmpty())
            return false;

       for (Iterator i = members.iterator(); i.hasNext();) {
            AppdefEntityID member = (AppdefEntityID) i.next();
            try {
                checkControlEnabled(subject,member);
                return true;
            } catch (PluginException e) {
                //continue
            }
        }
        return false;
    }

    /**
     * Checks with the plugin manager to find out if an entity's
     * resource provides support for control.
     * @param resType - appdef entity (of all kinds inc. groups)
     * @return flag - true if supported
     *
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public boolean isControlSupported (AuthzSubjectValue subject,
                                       AppdefResourceTypeValue resType) {
        try {
            _controlManager.getPlugin(resType.getName());
            return true;
        } catch (PluginNotFoundException e) {
            return false;
        }
    }

    /**
     * Check if a an entity has been enabled for control.
     * @return flag - true if enabled
     *
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public boolean isControlEnabled(AuthzSubjectValue subject,
                                    AppdefEntityID id) {
        try {
            checkControlEnabled(subject, id);
            return true;
        } catch (PluginException e) {
            return false;
        }
    }

    /**
     * Check if an entity has been enabled for control
     *
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public void checkControlEnabled(AuthzSubjectValue subject,
                                    AppdefEntityID id)
        throws PluginException
    {
        ConfigResponseDB config;

        try {
            config = getConfigManager().getConfigResponse(id);
        } catch (Exception e) {
            throw new PluginException(e);
        }

        if (config == null || config.getControlResponse() == null) {
            throw new PluginException("Control not " +
                                      "configured for " + id);
        }
    }

    /**
     * Get the control config response
     *
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ConfigResponse getConfigResponse(AuthzSubjectValue subject,
                                            AppdefEntityID id)
        throws PluginException
    {
        ConfigResponseDB config;
        ConfigResponse configResponse;
        byte[] controlResponse;

        try {
            config = getConfigManager().getConfigResponse(id);
        } catch (Exception e) {
            throw new PluginException(e);
        }

        if (config == null || config.getControlResponse() == null) {
            throw new PluginException("Control not " +
                                      "configured for " + id);
        }

        controlResponse = config.getControlResponse();
        
        try {
            configResponse = ConfigResponse.decode(controlResponse);
        } catch (Exception e) {
            throw new PluginException("Unable to decode configuration");
        }

        return configResponse;
    }
        

    // Remote EJB methods for use by agents.

    /**
     * Send an agent a plugin configuration.  This is needed when agents
     * restart, since they do not persist control plugin configuration.
     *
     * @param pluginName Name of the plugin to get the config for
     * @param merge      If true, merge the product and control config data
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public byte[] getPluginConfiguration(String pluginName, boolean merge)
        throws PluginException
    {
        ConfigManagerLocal cLocal;
        AuthzSubjectValue overlord;
        AppdefEntityID id;

        try {
            id = new AppdefEntityID(pluginName); 
            cLocal = ConfigManagerUtil.getLocalHome().create();

            // We use the overlord to grab config schemas
            overlord = AuthzSubjectManagerUtil.getLocalHome()
                .create().getOverlord();
            
            ConfigResponse config = cLocal.
                getMergedConfigResponse(overlord,
                                        ProductPlugin.TYPE_CONTROL,
                                        id, merge);

            return config.encode();
        } catch (NamingException e) {
            throw new SystemException();
        } catch (Exception e) {
            // XXX: Could be a bit more specific here when catching
            //      exceptions, but ideally this should always
            //      succeed since the agent knows when to pull the
            //      config.
            throw new PluginException("Unable to get plugin " +
                                      "configuration: " +
                                      e.getMessage());
        }
    }

    /**
     * Receive status information about a previous control action
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void sendCommandResult(int id, int result, long startTime, 
                                  long endTime, String message) {
        ControlHistory cLocal;
        String status;
        String msg;

        if (result == 0) {
            status = ControlConstants.STATUS_COMPLETED;
        } else {
            status = ControlConstants.STATUS_FAILED;
        }

        if (message != null && message.length() > 500) {
            // Show last 500 characters from the command output
            msg = message.substring(message.length() - 500);
        } else {
            msg = message;
        }

        // Update the control history
        try {
            ControlHistoryDAO historyLocalHome = getControlHistoryDAO();
            Integer pk = new Integer(id);
            cLocal = historyLocalHome.findById(pk);
        } catch (ObjectNotFoundException e) {
            // We know the ID, this won't happen
            throw new SystemException(e);
        }

        long duration = endTime - startTime;
        cLocal.setStatus(status);
        cLocal.setStartTime(startTime);
        cLocal.setEndTime(endTime);
        cLocal.setDuration(duration);
        cLocal.setMessage(msg);

        // Send a control event
        ControlEvent event = 
            new ControlEvent(cLocal.getSubject(),
                             cLocal.getEntityType().intValue(),
                             cLocal.getEntityId(),
                             cLocal.getAction(),
                             cLocal.getScheduled().booleanValue(),
                             cLocal.getDateScheduled(),
                             status);
        Messenger sender = new Messenger();
        sender.publishMessage("topic/eventsTopic", event);
    }

   /**
    * Accept an array of appdef entity Ids and verify control permission
    * on each entity for specified subject. Return only the set of entities
    * that have authorization.
    *
    * @return    List of entities subject is authz to control
    *            NOTE: Returns an empty list when no resources are found.
    *
    * @ejb:interface-method
    * @ejb:transaction type="SUPPORTS"
    */
    public List batchCheckControlPermissions (AuthzSubjectValue 
        caller, AppdefEntityID[] entities)
        throws AppdefEntityNotFoundException, PermissionException {
        return doBatchCheckControlPermissions(caller,entities);
    }

    protected List doBatchCheckControlPermissions (AuthzSubjectValue caller,
                                                   AppdefEntityID[] entities)
        throws AppdefEntityNotFoundException, PermissionException {
        List resList,opList;
        List retVal;
        ResourceValue[] resArr;
        String[] opArr;

        retVal = new ArrayList();
        resList = new ArrayList();
        opList = new ArrayList();

        // package up the args for verification
        for (int x=0;x<entities.length;x++) {

            // Special case groups. If the group is compatible,
            // pull the members and check each of them. According
            // to Moseley, if any member of a group is control unauthz
            // then the entire group is unauthz.
            if (entities[x].getType() == 
                AppdefEntityConstants.APPDEF_TYPE_GROUP) {
                if (isGroupControlEnabled(caller, entities[x])) {
                    retVal.add(entities[x]);
                }
                continue;
            } 
            // Build up the arguments -- operation name array correlated
            // with resource (i.e. type specific operation names)
            opList.add(getControlPermissionByType(entities[x]));
            ResourceValue rv = new ResourceValue();
            rv.setInstanceId(entities[x].getId());
            rv.setResourceTypeValue( new ResourceTypeValue(false, AppdefUtil
                .appdefTypeIdToAuthzTypeStr(entities[x].getType()),null) );
            resList.add(rv);
        }
        if (resList.size() > 0) {
            opArr = (String[]) opList.toArray(new String[0]);
            resArr= (ResourceValue[]) resList.toArray(new ResourceValue[0]);
    
            // fetch authz resources and add to return list
            try {
                PermissionManager pm = PermissionManagerFactory.getInstance();
                ResourceValue[] authz =
                    pm.findOperationScopeBySubjectBatch(caller,resArr,opArr);
                for (int x=0;x<authz.length;x++) {
                    retVal.add(AppdefUtil.resValToAppdefEntityId(authz[x]));
                }
            } catch (FinderException e) {
                // returns empty list as advertised
            }
        }
        return retVal;
    }

    // Authz Helper Methods

    /**
     * Check control modify permission for an appdef entity
     * Control Modify ops are treated as regular modify operations
     */
    protected void checkModifyPermission(AuthzSubjectValue caller,
                                         AppdefEntityID id)
        throws PermissionException
    {
        try {
            int type = id.getType();
            switch(type) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    getPlatformManager().checkModifyPermission(caller, id);
                    return;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    ServerManagerUtil.getLocalHome().create()
                        .checkModifyPermission(caller, id);
                    return;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    ServiceManagerUtil.getLocalHome().create()
                        .checkModifyPermission(caller, id);
                    return;
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                    ApplicationManagerUtil.getLocalHome().create()
                        .checkModifyPermission(caller, id);
                    return;
                default:
                    throw new InvalidAppdefTypeException("Unknown type: " 
                                                         + type);
            }
        } catch (CreateException e) { 
            // should never happen
            throw new PermissionException("Unable to create AppdefManager: " +
                                          e.getMessage());
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /** Check control permission for an appdef entity */
    protected void checkControlPermission(AuthzSubjectValue caller,
                                          AppdefEntityID id)
        throws PermissionException 
    {
        try {
            int type = id.getType();
            switch(type) {
                case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                    getPlatformManager().checkControlPermission(caller, id);
                    return;
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    ServerManagerUtil.getLocalHome().create()
                        .checkControlPermission(caller, id);
                    return;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    ServiceManagerUtil.getLocalHome().create()
                        .checkControlPermission(caller, id);
                    return;
                case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                    ApplicationManagerUtil.getLocalHome().create()
                        .checkControlPermission(caller, id);
                    return;
                default:
                    throw new InvalidAppdefTypeException("Unknown type: " 
                                                         + type);
            }
        } catch (CreateException e) { 
            // should never happen
            throw new PermissionException("Unable to create AppdefManager: " +
                                          e.getMessage());
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    } 

    // Lookup the appropriate control permission based on entity type.
    // Groups are fetched and appropriate type is returned.
    private String getControlPermissionByType(AppdefEntityID id) {
        switch (id.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            return AuthzConstants.platformOpControlPlatform;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            return AuthzConstants.serverOpControlServer;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            return AuthzConstants.serviceOpControlService;
        case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            return AuthzConstants.appOpControlApplication;
        default:
            throw new IllegalArgumentException("Invalid appdef type:" + 
                                               id.getType());
        }
    }

    private ConfigManagerLocal getConfigManager() {
        return ConfigManagerEJBImpl.getOne();
    }

    private PlatformManagerLocal getPlatformManager() {
        return PlatformManagerEJBImpl.getOne();
    }
}
