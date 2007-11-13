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

package org.hyperic.hq.authz.server.session;

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ejb.FinderException;
import javax.ejb.SessionContext;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocalHome;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.OperationValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocalHome;
import org.hyperic.hq.authz.shared.ResourceGroupManagerUtil;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceManagerLocalHome;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.pager.PageControl;

/**
 * This is the parent class for all Authz Session Beans
 */
public abstract class AuthzSession { 
    public static final Log log
        = LogFactory.getLog(AuthzSession.class.getName());

    protected static final String DATASOURCE = HQConstants.DATASOURCE;
    protected static InitialContext ic = null;

    protected ResourceGroupManagerLocalHome groupMgrHome;
    protected AuthzSubjectManagerLocalHome subjectMgrHome;
    protected ResourceManagerLocalHome resourceMgrHome;

    protected SessionContext ctx;

    protected ResourceTypeDAO getResourceTypeDAO() {
        return DAOFactory.getDAOFactory().getResourceTypeDAO();
    }

    protected ResourceDAO getResourceDAO() {
        return DAOFactory.getDAOFactory().getResourceDAO();
    }

    protected ResourceGroupDAO getResourceGroupDAO() {
        return DAOFactory.getDAOFactory().getResourceGroupDAO();
    }

    protected AuthzSubjectDAO getSubjectDAO() {
        return new AuthzSubjectDAO(DAOFactory.getDAOFactory());
    }

    protected RoleDAO getRoleDAO() {
        return DAOFactory.getDAOFactory().getRoleDAO();
    }

    protected OperationDAO getOperationDAO() {
        return DAOFactory.getDAOFactory().getOperationDAO();
    }

    protected ResourceGroupManagerLocalHome getGroupMgrHome() throws
        NamingException {
        if (groupMgrHome == null) {
            groupMgrHome = ResourceGroupManagerUtil.getLocalHome();
        }
        return groupMgrHome;
    }

    protected ResourceManagerLocalHome getResourceMgrHome() throws
        NamingException {
        if (resourceMgrHome == null) {
            resourceMgrHome = ResourceManagerUtil.getLocalHome();
        }
        return resourceMgrHome;
    }

    protected AuthzSubjectManagerLocalHome getSubjectMgrHome() throws
        NamingException {
        if (subjectMgrHome == null) {
            subjectMgrHome = AuthzSubjectManagerUtil.getLocalHome();
        }
        return subjectMgrHome;
    }

    /** 
     * @return The value-object of the overlord
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public AuthzSubjectValue findOverlord() {
        try {
            return findSubjectByAuth(AuthzConstants.overlordName,
                                     AuthzConstants.overlordDsn)
                                     .getAuthzSubjectValue();
        } catch(SubjectNotFoundException e) {
            throw new SystemException("Unable to find overlord", e);
        }
    }

    protected ResourceType getRootResourceType() {
       return DAOFactory.getDAOFactory().getResourceTypeDAO()
            .findByName(AuthzConstants.typeResourceTypeName); 
    }

    /**
     * Find the subject that has the given name and authentication source.
     * @param name Name of the subject.
     * @param authDsn DSN of the authentication source. Authentication sources
     * are defined externally.
     * @return The value-object of the subject of the given name and authenticating source.
     * @ejb:interface-method
     * @ejb:transaction type="Required"
     */
    public AuthzSubject findSubjectByAuth(String name, String authDsn)
        throws SubjectNotFoundException 
    {
         AuthzSubject subject = new AuthzSubjectDAO(DAOFactory.getDAOFactory())
            .findByAuth(name, authDsn);
        if (subject == null) {
            throw new SubjectNotFoundException(
                "Can't find subject: name=" + name + ",authDsn=" + authDsn);
        }
        return subject;
    }

    protected Set toPojos(Object[] vals) {
        Set ret = new HashSet();
        if (vals == null || vals.length == 0) {
            return ret;
        }
        
        OperationDAO operDao = null;
        AuthzSubjectDAO subjDao = 
            new AuthzSubjectDAO(DAOFactory.getDAOFactory());
        ResourceDAO resDao = null;
        RoleDAO roleDao = null;
        ResourceGroupDAO resGrpDao = null;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] instanceof OperationValue) {
                if (operDao == null) {
                    operDao = getOperationDAO();
                }
                ret.add(operDao.findById(((OperationValue) vals[i]).getId()));
            }
            else if (vals[i] instanceof AuthzSubjectValue) {
                ret.add(subjDao.findById(((AuthzSubjectValue)vals[i]).getId()));
            }
            else if (vals[i] instanceof ResourceValue) {
                if (resDao == null) {
                    resDao = getResourceDAO();
                }
                ret.add(resDao.findById(((ResourceValue) vals[i]).getId()));
            }
            else if (vals[i] instanceof RoleValue) {
                if (roleDao == null) {
                    roleDao = getRoleDAO();
                }
                ret.add(roleDao.findById(((RoleValue) vals[i]).getId()));
            }
            else if (vals[i] instanceof ResourceGroupValue) {
                if (resGrpDao == null) {
                    resGrpDao = getResourceGroupDAO();
                }
                ret.add(resGrpDao.findById(
                    ((ResourceGroupValue) vals[i]).getId()));
            }
            else {
                log.error("Invalid type.");
            }

        }
                
        return ret;
    }
    
    protected Object[] fromPojos(Collection pojos, Class c) {
        Object[] values = (Object[]) Array.newInstance(c, pojos.size());
        
        int i = 0;
        for (Iterator it = pojos.iterator(); it.hasNext(); i++) {
            AuthzNamedBean ent = (AuthzNamedBean) it.next();
            values[i] = ent.getValueObject();
            
            // Verify that it's the expected class
            if (!c.isInstance(values[i]))
                log.error("Invalid type: " + values[i].getClass() +
                          " when expecting " + c);
        }
        
        return values;
    }
    
    protected Object[] fromLocals(Collection locals, Class c) {
        Object[] values = null;
        Iterator it = locals.iterator();
        int counter = 0;
        boolean isOperation = false;
        boolean isResource = false;
        boolean isResourceGroup = false;
        boolean isRole = false;
        boolean isAuthzSubject = false;

        if (c.equals(OperationValue.class)) {
            values = new OperationValue[locals.size()];
            isOperation = true;
        } else if (c.equals(ResourceValue.class)) {
            values = new ResourceValue[locals.size()];
            isResource = true;
        } else if (c.equals(ResourceGroupValue.class)) {
            values = new ResourceGroupValue[locals.size()];
            isResourceGroup = true;
        } else if (c.equals(RoleValue.class)) {
            values = new RoleValue[locals.size()];
            isRole = true;
        } else if (c.equals(AuthzSubjectValue.class)) {
            values = new AuthzSubjectValue[locals.size()];
            isAuthzSubject = true;
        } else {
            throw new SystemException("Unknown type");
        }

        while (it.hasNext()) {
            if (isOperation) {
                values[counter] =
                    ((Operation)it.next()).getOperationValue();
            } else if (isResource) {
                values[counter] =
                    ((Resource)it.next()).getResourceValue();
            } else if (isResourceGroup) {
                values[counter] =
                    ((ResourceGroup)it.next()).getResourceGroupValue();
            } else if (isRole) {
                values[counter] =
                    ((Role)it.next()).getRoleValue();
            } else if (isAuthzSubject) {
                values[counter] =
                    ((AuthzSubject)it.next()).getAuthzSubjectValue();
            } else {
                throw new SystemException("Unknown type");
            }
            counter++;
        }
        return values;
    }

    protected AuthzSubject lookupSubject(AuthzSubjectValue subject) {
        return getSubjectDAO().findById(subject.getId());
    }

    protected AuthzSubject lookupSubject(Integer id) {
        return getSubjectDAO().findById(id);
    }

    protected AuthzSubject lookupSubjectPojo(AuthzSubjectValue subject) {
        return lookupSubjectPojo(subject.getId());
    }

    protected AuthzSubject lookupSubjectPojo(Integer id) {
        return new AuthzSubjectDAO(DAOFactory.getDAOFactory()).findById(id);
    }

    protected ResourceType lookupType(ResourceTypeValue type) {
        return getResourceTypeDAO().findById(type.getId());
    }

    protected ResourceGroup lookupGroup(ResourceGroupValue group) {
        return getResourceGroupDAO().findById(group.getId());
    }

    protected ResourceGroup lookupGroup(Integer id) {
        return getResourceGroupDAO().findById(id);
    }

    protected Resource lookupResource(ResourceValue resource) {
        if (resource.getId() == null) {
            String typeName = resource.getResourceTypeValue().getName();
            ResourceType typeLocal = getResourceTypeDAO().findByName(typeName);
            return getResourceDAO().findByInstanceId(typeLocal,
                                                     resource.getInstanceId());
        } 
        return getResourceDAO().findById(resource.getId());
    }

    protected Resource lookupResourcePojoByInstance(String resTypeName,
                                                    Integer instId) {
        ResourceType type = getResourceTypeDAO().findByName(resTypeName);
        return getResourceDAO().findByInstanceId(type, instId);
    }

    protected Resource lookupResourcePojo(ResourceValue resource) {
        if (resource.getId() == null) {
            ResourceType type = getResourceTypeDAO()
                .findByName(resource.getResourceTypeValue().getName());
            return getResourceDAO().findByInstanceId(type,
                                                     resource.getInstanceId());
        }
        return getResourceDAO().findById(resource.getId());
    }

    /**
     * Filter a collection of groupLocal objects to only include those viewable
     * by the specified user
     */
    protected Collection filterViewableGroups(AuthzSubjectValue who,
                                              Collection groups)
        throws PermissionException, FinderException
    {
        // finally scope down to only the ones the user can see
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List viewable = pm.findOperationScopeBySubject(who,
           AuthzConstants.groupOpViewResourceGroup,
           AuthzConstants.groupResourceTypeName,
           PageControl.PAGE_ALL);
        
        for(Iterator i = groups.iterator(); i.hasNext();) {
            ResourceGroup resGrp = (ResourceGroup) i.next();
            
            if (!viewable.contains(resGrp.getId())) {
                i.remove();
            }
        }
        return groups;
    }

    protected InitialContext getInitialContext() throws NamingException {
        if (ic == null) ic = new InitialContext();
        return ic;
    }

    protected Connection getDBConn() throws SQLException {
        return DAOFactory.getDAOFactory().getCurrentSession().connection();
    }
    
    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
    }
    
    protected SessionContext getSessionContext() {
        return this.ctx;
    }
}
