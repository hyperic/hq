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

package org.hyperic.hq.events.server.session;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.CreateException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceOperationsHelper;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.util.jdbc.IDGenerator;
import org.hyperic.util.units.FormattedNumber;

/** Session class superclass, which provides generic utility functions
 */
public abstract class SessionBase {
    protected Log log = LogFactory.getLog(SessionBase.class);

    /** the events database name */
    protected static final String DATASOURCE = HQConstants.DATASOURCE;
    /** the interval between sequence ID's */
    protected static final int SEQUENCE_INTERVAL = 1;

    // Initial context 
    private InitialContext ic = null;
    
    /** the static IDGenerator to generate ID's */
    private static Map idGenerators = new HashMap();

    protected InitialContext getInitialContext() throws NamingException {
        if (ic == null)
            ic = new InitialContext();

        return ic;
    }
    
    /** Get the next ID from the database sequence
     * @throws CreateException if the IDGenerator fails to generate a new ID
     * @return the next ID in the database sequence
     */
    protected Long getNextId(String seq) throws CreateException {
        IDGenerator idGen = (IDGenerator) idGenerators.get(seq);
        if (idGen == null) {
            idGen = new IDGenerator(SessionBase.class.getName(), seq,
                                    SEQUENCE_INTERVAL, DATASOURCE);
            idGenerators.put(seq, idGen);
        }

        try {
            Long id = new Long(idGen.getNewID());
            if ( log.isDebugEnabled() ) {
                log.debug("New ID for " + seq + ": " + id);
            }
            return id;
        } catch (Exception e) {
            log.error("Exception while getting new sequence.", e);
            throw new CreateException(
                "Error getting new ID from IDGenerator: " + e);
        }
    }

    public static AppdefEntityID getAppdefEntityID(AlertDefinitionInterface adi) {
        try {
            return new AppdefEntityID(adi.getResource());
        } catch (IllegalArgumentException e) {
            if (adi instanceof AlertDefinition) {
                AlertDefinition ad = (AlertDefinition) adi;
                return new AppdefEntityTypeID(ad.getAppdefType(), ad.getAppdefId());
            } else {
                throw e;
            }
        }
    }

    /**
     * Check the permission based on resource type, instance ID, and operation
     * @param subjectId the subject trying to perform the operation
     * @param rtName the resource type name
     * @param instId the instance ID
     * @param operation the operation
     * @throws PermissionException if the user is not authorized to perform the
     * operation on the resource
     */
    private static Map resourceTypes = new HashMap();
    private static Map operations = new HashMap();
    
    private static void checkPermission(Integer subjectId,
                                        String rtName, Integer instId,
                                        String opName)
        throws PermissionException {
        // ...check permission if user is NOT a super user...
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(subjectId)) {
            PermissionManager permMgr = PermissionManagerFactory.getInstance();
    
            if (!resourceTypes.containsKey(rtName)) {
                resourceTypes.put(rtName, 
                new ResourceTypeDAO(DAOFactory.getDAOFactory()).findByName(rtName));
            }
            ResourceType resType = (ResourceType) resourceTypes.get(rtName);
            
            if (!operations.containsKey(opName)) {
                operations.put(opName,
                               new OperationDAO(DAOFactory.getDAOFactory())
                                    .findByTypeAndName(resType, opName));
            }
            Operation operation = (Operation) operations.get(opName);
            
            permMgr.check(subjectId, resType.getId(), instId, operation.getId());
            // Permission Check Succesful
        }
    }

    protected Resource findResource(AppdefEntityID id) {
        return ResourceManagerEJBImpl.getOne().findResource(id);
    }

    public static void canViewResourceTypeAlertDefinitionTemplate(AuthzSubject user)
    throws PermissionException {
        // ...right now, you have to be a member of the super user's role to do anything with
        // resource type alert templates...
        // TODO ...if this changes in the future, we can make the change here and the rest should just work...
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(user.getId())) {
            throw new PermissionException("User must be in Super User role to manage resource type alert definitions");
        }
    }
    
    public static void canModifyResourceTypeAlertDefinitionTemplate(AuthzSubject user)
    throws PermissionException {
        // ...right now, you have to be a member of the super user's role to do anything with
        // resource type alert templates...
        // TODO ...if this changes in the future, we can make the change here and the rest should just work...
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(user.getId())) {
            throw new PermissionException("User must be in Super User role to manage resource type alert definitions");
        }
    }
    
    public static void canCreateResourceTypeAlertDefinitionTemplate(AuthzSubject user)
    throws PermissionException {
        // ...right now, you have to be a member of the super user's role to do anything with
        // resource type alert templates...
        // TODO ...if this changes in the future, we can make the change here and the rest should just work...
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(user.getId())) {
            throw new PermissionException("User must be in Super User role to manage resource type alert definitions");
        }
    }
    
    public static void canDeleteResourceTypeAlertDefinitionTemplate(AuthzSubject user)
    throws PermissionException {
        // ...right now, you have to be a member of the super user's role to do anything with
        // resource type alert templates...
        // TODO ...if this changes in the future, we can make the change here and the rest should just work...
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(user.getId())) {
            throw new PermissionException("User must be in Super User role to manage resource type alert definitions");
        }
    }
    
    public static void canViewAlertDefinition(AuthzSubject user, AppdefEntityID entityId)
    throws PermissionException {
        // ...we need to check the resource associated with the alert definition to determine 
        // if the user can view the alert definition.  Must have read permission on resource...
        checkAlertDefinitionPermission(user, entityId, ResourceOperationsHelper.getReadOperation(entityId.getType()));
    }
    
    public static void canModifyAlertDefinition(AuthzSubject user, AppdefEntityID entityId)
    throws PermissionException {
        // ...we need to check the resource associated with the alert definition to determine 
        // if the user can modify the alert definition.  Must have modify permission on resource...
        checkAlertDefinitionPermission(user, entityId, ResourceOperationsHelper.getUpdateOperation(entityId.getType()));
    }
    
    public static void canCreateAlertDefinition(AuthzSubject user, AppdefEntityID entityId)
    throws PermissionException {
        // ...we need to check the resource associated with the alert definition to determine 
        // if the user can modify the alert definition.  Must have modify permission on resource...
        // TODO ...If we introduce finer grained permission for Alert definition, we can make the change here
        // and the rest should just work...
        checkAlertDefinitionPermission(user, entityId, ResourceOperationsHelper.getUpdateOperation(entityId.getType()));
    }

    public static void canDeleteAlertDefinition(AuthzSubject user, AppdefEntityID entityId)
    throws PermissionException {
        // ...we need to check the resource associated with the alert definition to determine 
        // if the user can modify the alert definition.  Must have modify permission on resource...
        // TODO ...If we introduce finer grained permission for Alert definition, we can make the change here
        // and the rest should just work...
        checkAlertDefinitionPermission(user, entityId, ResourceOperationsHelper.getUpdateOperation(entityId.getType()));
    }

    private static void checkAlertDefinitionPermission(AuthzSubject user, AppdefEntityID id, String operationName) 
    throws PermissionException {
        int resourceType = id.getType();
        String resourceTypeLabel;
        
        switch (resourceType) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                resourceTypeLabel = AuthzConstants.platformResType;

                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                resourceTypeLabel = AuthzConstants.serverResType;

                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                resourceTypeLabel = AuthzConstants.serviceResType;

                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP: 
                resourceTypeLabel = AuthzConstants.groupResType;

                break;
            default:
                throw new PermissionException("Unknown type: " + resourceType);
        }
        
        // ...check based on resource type to see if we have the requested permission...
        checkPermission(user.getId(), resourceTypeLabel, id.getId(), operationName);
    }
    
    public static void canFixAcknowledgeAlerts(AuthzSubject who, AlertDefinitionInterface adi)
    throws PermissionException {
        if (adi.isDeleted()) {    // Don't need to check deleted alert defs
            return;
        }
        
        Integer parentId = null;

        if (adi instanceof AlertDefinition) {
            AlertDefinition ad = (AlertDefinition) adi;
            parentId = ad.getParent() != null ? ad.getParent().getId() : null;
        }
        
        if (!EventConstants.TYPE_ALERT_DEF_ID.equals(parentId)) {
            canFixAcknowledgeAlerts(who, new AppdefEntityID(adi.getResource()));
        }
    }
    
    /**
     * Check for manage alerts permission for a given resource 
     * 
     * By manage, we mean the ability to fix/acknowledge alerts & pause escalations...
     */
    public static void canFixAcknowledgeAlerts(AuthzSubject user, AppdefEntityID entityId)
    throws PermissionException {
        try {
            canModifyAlertDefinition(user, entityId);
        } catch(PermissionException e) {
            // ...first check that we can view the alert...
            canViewAlertDefinition(user, entityId);
    
            int resourceTypeId = entityId.getType();
            
            // ...then check if we have fix/acknowledge permissions on alert...
            checkPermission(user.getId(), 
                            ResourceOperationsHelper.getResourceType(resourceTypeId), 
                            entityId.getId(), 
                            ResourceOperationsHelper.getManageAlertOperation(resourceTypeId));            
        }
    }
    
    protected String describeCondition(AlertCondition cond, Measurement dm) {
        StringBuffer text = new StringBuffer();
        switch (cond.getType()) {
        case EventConstants.TYPE_THRESHOLD:
        case EventConstants.TYPE_BASELINE:
            text.append(cond.getName()).append(" ")
                .append(cond.getComparator()).append(" ");
    
            if (cond.getType() == EventConstants.TYPE_BASELINE) {
                text.append(cond.getThreshold());
                text.append("% of ");
    
                if (MeasurementConstants.BASELINE_OPT_MAX.equals(cond
                        .getOptionStatus())) {
                    text.append("Max Value");
                } else if (MeasurementConstants.BASELINE_OPT_MIN
                        .equals(cond.getOptionStatus())) {
                    text.append("Min Value");
                } else {
                    text.append("Baseline");
                }
            } else {
                FormattedNumber th =
                    UnitsConvert.convert(cond.getThreshold(),
                                         dm.getTemplate().getUnits());
                text.append(th.toString());
            }
            break;
        case EventConstants.TYPE_CONTROL:
            text.append(cond.getName());
            break;
        case EventConstants.TYPE_CHANGE:
            text.append(cond.getName()).append(" value changed");
            break;
        case EventConstants.TYPE_CUST_PROP:
            text.append(cond.getName()).append(" value changed");
            break;
        case EventConstants.TYPE_LOG:
            text.append("Event/Log Level(")
                    .append(ResourceLogEvent.getLevelString(Integer
                                    .parseInt(cond.getName())))
                    .append(")");
            if (cond.getOptionStatus() != null
                    && cond.getOptionStatus().length() > 0) {
                text.append(" and matching substring ").append('"')
                    .append(cond.getOptionStatus()).append('"');
            }
            break;
        case EventConstants.TYPE_CFG_CHG:
            text.append("Config changed");
            if (cond.getOptionStatus() != null
                    && cond.getOptionStatus().length() > 0) {
                text.append(": ")
                    .append(cond.getOptionStatus());
            }
            break;
        default:
            break;
        }
        
        return text.toString();
    }

    private static void checkEscalation(Integer subjectId,
                                        String operation) 
        throws PermissionException {
        // The escalation resource type is looked up for its ID to be used
        // instance ID.  The reason is that escalations are global, and we're
        // not applying escalation permission per appdef resource.
        ResourceType rt = new ResourceTypeDAO(DAOFactory.getDAOFactory())
            .findByName(AuthzConstants.escalationResourceTypeName);

        checkPermission(subjectId, AuthzConstants.rootResType, rt.getId(),
                        operation);        
    }
    
    public static void canCreateEscalation(Integer subjectId)
        throws PermissionException {
        checkEscalation(subjectId, AuthzConstants.escOpCreateEscalation);
    }
    
    public static void canViewEscalation(Integer subjectId) 
    throws PermissionException {
        checkEscalation(subjectId, AuthzConstants.escOpViewEscalation);
    }
    
    public static void canModifyEscalation(Integer subjectId)
        throws PermissionException {
        checkEscalation(subjectId, AuthzConstants.escOpModifyEscalation);
    }
    
    public static void canRemoveEscalation(Integer subjectId)
        throws PermissionException {
        checkEscalation(subjectId, AuthzConstants.escOpRemoveEscalation);
    }
}
