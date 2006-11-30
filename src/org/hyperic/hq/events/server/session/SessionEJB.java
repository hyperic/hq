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

import javax.ejb.CreateException;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.authz.server.session.Operation;
import org.hyperic.hq.authz.server.session.OperationDAO;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.server.session.ResourceTypeDAO;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.util.jdbc.IDGenerator;

/** Entity EJB superclass, which provides generic utility
 * functions
 */
public abstract class SessionEJB {
    protected Log log = LogFactory.getLog(SessionEJB.class);

    /** the events database name */
    protected static final String DATASOURCE = HQConstants.DATASOURCE;
    /** the interval between sequence ID's */
    protected static final int SEQUENCE_INTERVAL = 1;

    // Initial context 
    private InitialContext ic = null;

    /** the static IDGenerator to generate ID's */
    private static HashMap idGenerators = new HashMap();

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
            idGen =
                new IDGenerator(
                    SessionEJB.class.getName(),
                    seq,
                    SEQUENCE_INTERVAL,
                    DATASOURCE);
            idGenerators.put(seq, idGen);
        }

        try {
            // Cast from long to Integer
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

    protected AppdefEntityID getAppdefEntityID(AlertDefinition ad) {
        return new AppdefEntityID(ad.getAppdefType(), ad.getAppdefId());
    }

    /**
     * Check the permission based on resource type, instance ID, and operation
     * @param subject the subject trying to perform the operation
     * @param rtName the resource type name
     * @param instId the instance ID
     * @param operation the operation
     * @throws PermissionException if the user is not authorized to perform the
     * operation on the resource
     */
    private void checkPermission(AuthzSubjectValue subject, String rtName,
                                 Integer instId, String operation)
        throws PermissionException {
        PermissionManager permMgr = PermissionManagerFactory.getInstance();
        // note, using the "SLOWER" permission check
        permMgr.check(subject.getId(), rtName, instId, operation);
        log.debug("Permission Check Succesful");
    }

    /**
     * Check a permission 
     * @param subject - who
     * @param resourceType - type of resource 
     * @param instance Id - the id of the object
     * @param operation - the name of the operation to perform
     */
    private void checkAlerting(AuthzSubjectValue subject, AppdefEntityID id,
                               String operation)
        throws PermissionException {
        log.debug("Checking Permission for Operation: "
            + operation + " ResourceType: " + id.getTypeName() +
            " Instance Id: " + id + " Subject: " + subject);
        checkPermission(subject, id.getTypeName(), id.getId(), operation);
    }

    protected void canManageAlerts(AuthzSubjectValue who, AlertDefinition ad)
        throws PermissionException {
        Integer parentId = ad.getParent() != null ? ad.getParent().getId()
                : null;
        if (!EventConstants.TYPE_ALERT_DEF_ID.equals(parentId))
            canManageAlerts(who, getAppdefEntityID(ad));
    }

    /**
     * Check for manage alerts permission for a given resource
     */
    protected void canManageAlerts(AuthzSubjectValue subject, AppdefEntityID id)
        throws PermissionException {
        int type = id.getType();
        String opName = null;
        switch (type) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                opName = AuthzConstants.platformOpManageAlerts;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                opName = AuthzConstants.serverOpManageAlerts;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                opName = AuthzConstants.serviceOpManageAlerts;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                opName = AuthzConstants.appOpManageAlerts;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                opName = AuthzConstants.groupOpManageAlerts;
                break;                
            default:
                throw new InvalidAppdefTypeException("Unknown type: " +
                    type);
        }
        // now check
        checkAlerting(subject, id, opName);
    }
    
    private void checkEscalation(AuthzSubjectValue subject, String operation) 
        throws PermissionException {
        ResourceType rt =
            DAOFactory.getDAOFactory().getResourceTypeDAO()
            .findByName(AuthzConstants.escalationResourceTypeName);

        checkPermission(subject, AuthzConstants.rootResType, rt.getId(),
                        operation);        
    }
    
    protected void canCreateEscalation(AuthzSubjectValue subj)
        throws PermissionException {
        checkEscalation(subj, AuthzConstants.escOpCreateEscalation);
    }
    
    protected void canViewEscalation(AuthzSubjectValue subj)
        throws PermissionException {
        checkEscalation(subj, AuthzConstants.escOpViewEscalation);
    }
    
    protected void canModifyEscalation(AuthzSubjectValue subj)
        throws PermissionException {
        checkEscalation(subj, AuthzConstants.escOpModifyEscalation);
    }
    
    protected void canRemoveEscalation(AuthzSubjectValue subj)
        throws PermissionException {
        checkEscalation(subj, AuthzConstants.escOpRemoveEscalation);
    }
}
