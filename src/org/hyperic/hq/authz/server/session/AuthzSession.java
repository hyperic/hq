/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc. This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify it under the terms
 * version 2 of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.hq.authz.server.session;

import java.util.HashSet;
import java.util.Set;

import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.context.Bootstrap;

/**
 * This is the parent class for all Authz Session Beans
 */
public abstract class AuthzSession {
    public static final Log log = LogFactory.getLog(AuthzSession.class.getName());

   

    protected SessionContext ctx;

    protected ResourceTypeDAO resourceTypeDAO = Bootstrap.getBean(ResourceTypeDAO.class);
    protected ResourceDAO resourceDAO = Bootstrap.getBean(ResourceDAO.class);
    protected ResourceGroupDAO resourceGroupDAO = Bootstrap.getBean(ResourceGroupDAO.class);
    protected AuthzSubjectDAO authzSubjectDAO = Bootstrap.getBean(AuthzSubjectDAO.class);
    protected RoleDAO roleDAO = Bootstrap.getBean(RoleDAO.class);
    protected OperationDAO operationDAO = Bootstrap.getBean(OperationDAO.class);

    private ResourceRelationDAO resourceRelationDAO = Bootstrap.getBean(ResourceRelationDAO.class);

    protected ResourceTypeDAO getResourceTypeDAO() {
        return resourceTypeDAO;
    }

    protected ResourceDAO getResourceDAO() {
        return resourceDAO;
    }

    protected ResourceGroupDAO getResourceGroupDAO() {
        return resourceGroupDAO;
    }

    protected AuthzSubjectDAO getSubjectDAO() {
        return authzSubjectDAO;
    }

    protected RoleDAO getRoleDAO() {
        return roleDAO;
    }

    protected OperationDAO getOperationDAO() {
        return operationDAO;
    }

    protected ResourceType getRootResourceType() {
        return resourceTypeDAO.findTypeResourceType();
    }

    

    protected Set toPojos(Object[] vals) {
        Set ret = new HashSet();
        if (vals == null || vals.length == 0) {
            return ret;
        }

        RoleDAO roleDao = null;
        ResourceGroupDAO resGrpDao = null;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] instanceof Operation) {
                ret.add(vals[i]);
            } else if (vals[i] instanceof ResourceValue) {
                ret.add(lookupResource((ResourceValue) vals[i]));
            } else if (vals[i] instanceof RoleValue) {
                if (roleDao == null) {
                    roleDao = getRoleDAO();
                }
                ret.add(roleDao.findById(((RoleValue) vals[i]).getId()));
            } else if (vals[i] instanceof ResourceGroupValue) {
                if (resGrpDao == null) {
                    resGrpDao = getResourceGroupDAO();
                }
                ret.add(resGrpDao.findById(((ResourceGroupValue) vals[i]).getId()));
            } else {
                log.error("Invalid type.");
            }

        }

        return ret;
    }

    protected AuthzSubject lookupSubject(Integer id) {
        return authzSubjectDAO.findById(id);
    }

    private Resource lookupResource(ResourceValue resource) {
        if (resource.getId() == null) {
            ResourceType type = resource.getResourceType();
            return resourceDAO.findByInstanceId(type, resource.getInstanceId());
        }
        return resourceDAO.findById(resource.getId());
    }

   

    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
    }

    protected SessionContext getSessionContext() {
        return this.ctx;
    }

    /**
     * 
     * @ejb:interface-method
     */
    public ResourceRelation getContainmentRelation() {
        return getResourceRelation(AuthzConstants.RELATION_CONTAINMENT_ID);
    }

    /**
     * 
     * @ejb:interface-method
     */
    public ResourceRelation getNetworkRelation() {
        return getResourceRelation(AuthzConstants.RELATION_NETWORK_ID);
    }

    private ResourceRelation getResourceRelation(Integer relationId) {
        return resourceRelationDAO.findById(relationId);
    }

    protected Resource findPrototype(AppdefEntityTypeID id) {
        Integer authzType;

        switch (id.getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                authzType = AuthzConstants.authzPlatformProto;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                authzType = AuthzConstants.authzServerProto;
                break;
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                authzType = AuthzConstants.authzServiceProto;
                break;
            default:
                throw new IllegalArgumentException("Unsupported prototype type: " + id.getType());
        }
        return getResourceDAO().findByInstanceId(authzType, id.getId());
    }
}
