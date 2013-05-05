/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.ConfigResponseDB;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.MessagePublisher;
import org.hyperic.hq.control.ControlActionResult;
import org.hyperic.hq.control.ControlEvent;
import org.hyperic.hq.control.GroupControlActionResult;
import org.hyperic.hq.control.agent.client.ControlCommandsClient;
import org.hyperic.hq.control.agent.client.ControlCommandsClientFactory;
import org.hyperic.hq.control.shared.ControlConstants;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.control.shared.ControlScheduleManager;
import org.hyperic.hq.control.shared.ScheduledJobRemoveException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.product.ControlPluginManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginNotFoundException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.shared.ProductManager;
import org.hyperic.hq.scheduler.ScheduleValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.pager.PageControl;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The server-side control system.
 */
@Service
@Transactional
public class ControlManagerImpl implements ControlManager {

    private final Log log = LogFactory.getLog(ControlManagerImpl.class.getName());

    private ProductManager productManager;
    private ControlScheduleManager controlScheduleManager;
    private ControlHistoryDAO controlHistoryDao;
    private ResourceTypeDAO resourceTypeDao;

    private ConfigManager configManager;
    private PlatformManager platformManager;
    private AuthzSubjectManager authzSubjectManager;
    private PermissionManager permissionManager;
    private ControlPluginManager controlPluginManager;

    private MessagePublisher messagePublisher;
    private ControlCommandsClientFactory controlCommandsClientFactory;
    private ControlActionResultsCollector controlActionResultsCollector;
    private AsyncTaskExecutor executor;
    private ControlActionExecutor controlActionExecutor;
    private GroupControlActionExecutor groupControlActionExecutor;
    // Default timeout is 10 minutes. If a plugin does not define its
    // own timeout, this is the value that will be used.
    static final int DEFAULT_RESOURCE_TIMEOUT = 10 * 60 * 1000;

    @Autowired
    public ControlManagerImpl(ProductManager productManager, ControlScheduleManager controlScheduleManager,
                              ControlHistoryDAO controlHistoryDao, ResourceTypeDAO resourceTypeDao,
                              ConfigManager configManager, PlatformManager platformManager,
                              AuthzSubjectManager authzSubjectManager, PermissionManager permissionManager,
                              MessagePublisher messagePublisher,
                              ControlCommandsClientFactory controlCommandsClientFactory, 
                              ControlActionResultsCollector controlActionResultsCollector, 
                              @Value("#{controlExecutor}")AsyncTaskExecutor executor, 
                              ControlActionExecutor controlActionExecutor, 
                              GroupControlActionExecutor groupControlActionExecutor) {
        this.productManager = productManager;
        this.controlScheduleManager = controlScheduleManager;
        this.controlHistoryDao = controlHistoryDao;
        this.resourceTypeDao = resourceTypeDao;
        this.configManager = configManager;
        this.platformManager = platformManager;
        this.authzSubjectManager = authzSubjectManager;
        this.permissionManager = permissionManager;
        this.messagePublisher = messagePublisher;
        this.controlCommandsClientFactory = controlCommandsClientFactory;
        this.controlActionResultsCollector = controlActionResultsCollector;
        this.executor = executor;
        this.controlActionExecutor = controlActionExecutor;
        this.groupControlActionExecutor = groupControlActionExecutor;
    }

    @PostConstruct
    public void createControlPluginManager() {
        // Get reference to the control plugin manager
        try {
            controlPluginManager = (ControlPluginManager) productManager.getPluginManager(ProductPlugin.TYPE_CONTROL);
        } catch (Exception e) {
            this.log.error("Unable to get plugin manager", e);
        }
    }

    /**
     * Enable an entity for control
     **/
    public void configureControlPlugin(AuthzSubject subject, AppdefEntityID id) throws PermissionException,
        PluginException, ConfigFetchException, AppdefEntityNotFoundException, AgentNotFoundException {
        // authz check
        checkModifyPermission(subject, id);

        String pluginName, pluginType;
        ConfigResponse mergedResponse;

        pluginName = id.toString();

        try {
            pluginType = platformManager.getPlatformPluginName(id);
            mergedResponse = configManager.getMergedConfigResponse(subject, ProductPlugin.TYPE_CONTROL, id, true);

            ControlCommandsClient client = controlCommandsClientFactory.getClient(id);
            client.controlPluginAdd(pluginName, pluginType, mergedResponse);
        } catch (EncodingException e) {
            throw new PluginException("Unable to decode config", e);
        } catch (AgentConnectionException e) {
            throw new PluginException("Agent error: " + e.getMessage(), e);
        } catch (AgentRemoteException e) {
            throw new PluginException("Agent error: " + e.getMessage(), e);
        }
    }
    
    public void doAction(AuthzSubject subject, AppdefEntityID id, String action, String args) throws PluginException,
    PermissionException {
        doSingleAction(subject, id, action, args);
    }

    /**
     * Execute a single control action on a given entity.
     */
    private Integer doSingleAction(AuthzSubject subject, AppdefEntityID id, String action, String args) throws PluginException,
        PermissionException {
        // This method doesn't support groups.
        if (id.isGroup()) {
            throw new IllegalArgumentException("Cannot perform single " + "action on a group.");
        }

        checkControlEnabled(subject, id);
        checkControlPermission(subject, id);
        if (log.isDebugEnabled()) {
            log.debug("Running control action for immediate execution: "
                + " {resource=" + id
                + ", action=" + action
                + "}");
        }
        return controlActionExecutor.executeControlAction(id, subject.getName(), new Date(),
                Boolean.FALSE, "", action, args);
    }

    /**
     * Execute a single control action on a given entity.
     */
    public void doAction(AuthzSubject subject, AppdefEntityID id, String action) throws PluginException,
        PermissionException {
        String args = null;
        doAction(subject, id, action, args);
    }
      
    public Future<ControlActionResult> doAction(AuthzSubject subject, AppdefEntityID id,
                                                String action, int waitTimeout)
        throws PluginException, PermissionException {       
        return doAction(subject, id, action, null,waitTimeout);
    }

    public Future<ControlActionResult> doAction(final AuthzSubject subject, final AppdefEntityID id,
                                                String action, String args, final int defaultTimeout)
        throws PluginException, PermissionException {
        final Integer jobId = doSingleAction(subject,id,action, args);
        Future<ControlActionResult> result = executor.submit(new Callable<ControlActionResult>() {
            public ControlActionResult call() throws Exception {
                return controlActionResultsCollector.waitForResult(jobId, 
                    controlActionResultsCollector.getTimeout(subject, id, defaultTimeout));
            }
        });
        return result;
    }

    /**
     * Schedule a new control action.
     */
    public void scheduleAction(AuthzSubject subject, AppdefEntityID id, String action, ScheduleValue schedule)
        throws PluginException, PermissionException, SchedulerException {
        // This method doesn't support groups.
        if (id.isGroup()) {
            throw new IllegalArgumentException("Cannot perform single " + "action on a group.");
        }

        checkControlEnabled(subject, id);
        checkControlPermission(subject, id);

        controlScheduleManager.scheduleAction(id, subject, action, schedule, null);
    }

    /**
     * Single control action for a group of given entities.
     */
    public Future<GroupControlActionResult> doGroupAction(final AuthzSubject subject, final AppdefEntityID id, 
        final String action, final String args, final int[] order, final int defaultResourceTimeout)
        throws PluginException, PermissionException, AppdefEntityNotFoundException, GroupNotCompatibleException {
        List<AppdefEntityID> groupMembers = GroupUtil.getCompatGroupMembers(subject, id, order, PageControl.PAGE_ALL);

        // For each entity in the list, sanity check config and permissions
        for (AppdefEntityID entity : groupMembers) {
            checkControlEnabled(subject, entity);
            checkControlPermission(subject, entity);
        }
        final String subjectName = subject.getName();
        return executor.submit(new Callable<GroupControlActionResult>() {        
            public GroupControlActionResult call() throws Exception {
                return groupControlActionExecutor.executeGroupControlAction(id, subjectName, new Date(), false,
                    "", action, args, order, defaultResourceTimeout);
            }
        });
    }
    
    public void doGroupAction(AuthzSubject subject, AppdefEntityID id, String action, String args,
                              int[] order) throws PluginException, PermissionException,
        AppdefEntityNotFoundException, GroupNotCompatibleException {
        doGroupAction(subject, id, action, args, order, DEFAULT_RESOURCE_TIMEOUT); 
    }

    /**
     * Schedule a single control action for a group of given entities.
     * @throws SchedulerException
     */
    public void scheduleGroupAction(AuthzSubject subject, AppdefEntityID id, String action, int[] order,
                              ScheduleValue schedule) throws PluginException, PermissionException, SchedulerException,
        GroupNotCompatibleException, AppdefEntityNotFoundException {
        List<AppdefEntityID> groupMembers = GroupUtil.getCompatGroupMembers(subject, id, order, PageControl.PAGE_ALL);

        // For each entity in the list, sanity check config and permissions
        for (AppdefEntityID entity : groupMembers) {
            checkControlEnabled(subject, entity);
            checkControlPermission(subject, entity);
        }

        controlScheduleManager.scheduleAction(id, subject, action, schedule, order);
    }

    /**
     * Get the supported actions for an appdef entity from the local
     * ControlPluginManager
     */
    @Transactional(readOnly=true)
    public List<String> getActions(AuthzSubject subject, AppdefEntityID id) throws PermissionException,
        PluginNotFoundException, AppdefEntityNotFoundException, GroupNotCompatibleException {
        if (id.isGroup()) {
            List<AppdefEntityID> groupMembers = GroupUtil
                .getCompatGroupMembers(subject, id, null, PageControl.PAGE_ALL);

            // For each entity in the list, sanity check permissions
            for (AppdefEntityID entity : groupMembers) {
                checkControlPermission(subject, entity);
            }
        } else {
            checkControlPermission(subject, id);
        }

        String pluginName = platformManager.getPlatformPluginName(id);
        return controlPluginManager.getActions(pluginName);
    }

    /**
     * Get the supported actions for an appdef entity from the local
     * ControlPluginManager
     */
    @Transactional(readOnly=true)
    public List<String> getActions(AuthzSubject subject, AppdefEntityTypeID aetid) throws PluginNotFoundException {
        String pluginName = aetid.getAppdefResourceType().getName();
        return controlPluginManager.getActions(pluginName);
    }

    /**
     * Check if a compatible group's members have been enabled for control. A
     * group is enabled for control if and only if all of its members have been
     * enabled for control.
     * @return flag - true if group is enabled
     */
    @Transactional(readOnly=true)
    public boolean isGroupControlEnabled(AuthzSubject subject, AppdefEntityID id) throws AppdefEntityNotFoundException,
        PermissionException {
        if (!id.isGroup()) {
            throw new IllegalArgumentException("Expecting entity of type " + "group.");
        }

        List<AppdefEntityID> members;

        try {
            members = GroupUtil.getCompatGroupMembers(subject, id, null);
        } catch (GroupNotCompatibleException ex) {
            // only compatible groups are controllable
            return false;
        }

        if (members.isEmpty()) {
            return false;
        }

        for (AppdefEntityID member : members) {
            try {
                checkControlEnabled(subject, member);
                return true;
            } catch (PluginException e) {
                // continue
            }
        }
        return false;
    }

    /**
     * Checks with the plugin manager to find out if an entity's resource
     * provides support for control.
     * @param resType - appdef entity (of all kinds inc. groups)
     * @return flag - true if supported
     */
    @Transactional(readOnly=true)
    public boolean isControlSupported(AuthzSubject subject, String resType) {
        try {
            controlPluginManager.getPlugin(resType);
            return true;
        } catch (PluginNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks with the plugin manager to find out if an entity's resource
     * provides support for control.
     * @param resType - appdef entity (of all kinds inc. groups)
     * @return flag - true if supported
     */
    @Transactional(readOnly=true)
    public boolean isControlSupported(AuthzSubject subject, AppdefEntityID id, String resType) {
        try {
            if (id.isGroup()) {
                List<AppdefEntityID> members = GroupUtil.getCompatGroupMembers(subject, id, null);

                if (members.isEmpty()) {
                    return false;
                } 
                AppdefEntityID groupMemberID = (AppdefEntityID) members.get(0);
                checkControlPermission(subject, groupMemberID);
                // HHQ-5788 - we dont display control tab for compatible groups of type "platform"
                if (groupMemberID.getType() == AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
                    return false;
                }
                // Check with the plugin manager whether resource provides support for control
                controlPluginManager.getPlugin(resType);
            } else {
                checkControlPermission(subject, id);
                // Check with the plugin manager whether resource provides support for control
                controlPluginManager.getPlugin(resType);
                // Check if an entity has been enabled for control
                // f.e some of the platforms have a "reset vm" control action and some do not  
                checkControlEnabled(subject, id) ; 
            }
            return true;
        } catch (PluginNotFoundException e) {
            // return false
        } catch (PermissionException e) {
            // return false
        } catch (AppdefEntityNotFoundException e) {
            // return false
        } catch (GroupNotCompatibleException e) {
            // return false
        }catch(PluginException pe) { 
            // return false
        }
        return false;
    }

    /**
     * Check if a an entity has been enabled for control.
     * @return flag - true if enabled
     */
    @Transactional(readOnly=true)
    public boolean isControlEnabled(AuthzSubject subject, AppdefEntityID id) {
        try {
            checkControlEnabled(subject, id);
            return true;
        } catch (PluginException e) {
            return false;
        }
    }

    /**
     * Check if an entity has been enabled for control
     */
    @Transactional(readOnly=true)
    public void checkControlEnabled(AuthzSubject subject, AppdefEntityID id) throws PluginException {
        ConfigResponseDB config;

        try {
            config = configManager.getConfigResponse(id);
        } catch (IllegalArgumentException iae) {
            throw new PluginException(iae);
        } catch (Exception e) {
            throw new PluginException(e);
        }

        if (config == null || config.getControlResponse() == null) {
            throw new PluginException("Control not " + "configured for " + id);
        }
    }

   

    /**
     * Send an agent a plugin configuration. This is needed when agents restart,
     * since they do not persist control plugin configuration.
     * 
     * @param pluginName Name of the plugin to get the config for
     * @param merge If true, merge the product and control config data
     */
    @Transactional(readOnly=true)
    public byte[] getPluginConfiguration(String pluginName, boolean merge) throws PluginException {
        try {
            AppdefEntityID id = new AppdefEntityID(pluginName);

            AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

            ConfigResponse config = configManager.getMergedConfigResponse(overlord, ProductPlugin.TYPE_CONTROL, id,
                merge);

            return config.encode();
        } catch (Exception e) {
            // XXX: Could be a bit more specific here when catching
            // exceptions, but ideally this should always
            // succeed since the agent knows when to pull the
            // config.
            throw new PluginException("Unable to get plugin configuration: " + e.getMessage());
        }
    }
    
   
    /**
     * Receive status information about a previous control action
     */
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void sendCommandResult(int id, int result, long startTime, long endTime, String message) {
        String status;
        if (result == 0) {
            status = ControlConstants.STATUS_COMPLETED;
        } else {
            status = ControlConstants.STATUS_FAILED;
        }

        String msg;
        if (message != null && message.length() > 500) {
            // Show last 500 characters from the command output
            msg = message.substring(message.length() - 500);
        } else {
            msg = message;
        }

        Integer pk = new Integer(id);
        
        ControlHistory cLocal = null ; 
        for(int i=0; i < 3; i++) { 
            cLocal = controlHistoryDao.get(pk);
            if(cLocal != null) break ;     
        }//EO while there are more retries
        
        if (cLocal == null) {
            // We know the ID, this should not happen
            throw new SystemException(
                "Failure getting control history id=" + id
                + ". Could not update history {status=" + status
                + ", startTime=" + startTime
                + ", endTime=" + endTime
                + ", message=" + msg
                + "}");
        }

        cLocal.setStatus(status);
        cLocal.setStartTime(startTime);
        cLocal.setEndTime(endTime);
        cLocal.setMessage(msg);

        // Send a control event
        ControlEvent event = new ControlEvent(cLocal.getSubject(), cLocal.getEntityType().intValue(), cLocal
            .getEntityId(), cLocal.getAction(), cLocal.getScheduled().booleanValue(), cLocal.getDateScheduled(), status);
        event.setMessage(msg);
        messagePublisher.publishMessage(EventConstants.EVENTS_TOPIC, event);
    }

    @Transactional
    public void removeControlHistory(AppdefEntityID id) {
        controlHistoryDao.removeByEntity(id.getType(), id.getID());
    }

    /**
     * Accept an array of appdef entity Ids and verify control permission on
     * each entity for specified subject. Return only the set of entities that
     * have authorization.
     * 
     * @return List of entities subject is authz to control NOTE: Returns an
     *         empty list when no resources are found.
     */
    public List<AppdefEntityID> batchCheckControlPermissions(AuthzSubject caller, AppdefEntityID[] entities)
        throws AppdefEntityNotFoundException, PermissionException {
        return doBatchCheckControlPermissions(caller, entities);
    }

    protected List<AppdefEntityID> doBatchCheckControlPermissions(AuthzSubject caller, AppdefEntityID[] entities)
        throws AppdefEntityNotFoundException, PermissionException {
        List<ResourceValue> resList = new ArrayList<ResourceValue>();
        List<String> opList = new ArrayList<String>();
        List<AppdefEntityID> retVal = new ArrayList<AppdefEntityID>();
        ResourceValue[] resArr;
        String[] opArr;

        // package up the args for verification
        for (AppdefEntityID entity : entities) {

            // Special case groups. If the group is compatible,
            // pull the members and check each of them. According
            // to Moseley, if any member of a group is control unauthz
            // then the entire group is unauthz.
            if (entity.isGroup()) {
                if (isGroupControlEnabled(caller, entity)) {
                    retVal.add(entity);
                }
                continue;
            }
            // Build up the arguments -- operation name array correlated
            // with resource (i.e. type specific operation names)
            opList.add(getControlPermissionByType(entity));
            ResourceValue rv = new ResourceValue();
            rv.setInstanceId(entity.getId());
            rv.setResourceType(resourceTypeDao.findByName(AppdefUtil.appdefTypeIdToAuthzTypeStr(entity.getType())));
            resList.add(rv);
        }
        if (resList.size() > 0) {
            opArr = (String[]) opList.toArray(new String[0]);
            resArr = (ResourceValue[]) resList.toArray(new ResourceValue[0]);

            // fetch authz resources and add to return list
            try {
                PermissionManager pm = PermissionManagerFactory.getInstance();
                Resource[] authz = pm.findOperationScopeBySubjectBatch(caller, resArr, opArr);
                for (int x = 0; x < authz.length; x++) {
                    retVal.add(AppdefUtil.newAppdefEntityId(authz[x]));
                }
            } catch (ApplicationException e) {
                // returns empty list as advertised
            }
        }
        return retVal;
    }

    // Authz Helper Methods

    /**
     * Check control modify permission for an appdef entity Control Modify ops
     * are treated as regular modify operations
     */
    protected void checkModifyPermission(AuthzSubject caller, AppdefEntityID id) throws PermissionException {
        int type = id.getType();
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                permissionManager.checkModifyPermission(caller, id);
                return;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                permissionManager.checkModifyPermission(caller, id);
                return;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                permissionManager.checkModifyPermission(caller, id);
                return;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                permissionManager.checkModifyPermission(caller, id);
                return;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + type);
        }
    }

    /** Check control permission for an appdef entity */
    protected void checkControlPermission(AuthzSubject caller, AppdefEntityID id) throws PermissionException {
        int type = id.getType();
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                permissionManager.checkControlPermission(caller, id);
                return;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                permissionManager.checkControlPermission(caller, id);
                return;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                permissionManager.checkControlPermission(caller, id);
                return;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                permissionManager.checkControlPermission(caller, id);
                return;
            default:
                throw new InvalidAppdefTypeException("Unknown type: " + type);
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
                throw new IllegalArgumentException("Invalid appdef type:" + id.getType());
        }
    }
}
