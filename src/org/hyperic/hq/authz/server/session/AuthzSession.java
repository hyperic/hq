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
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.dao.AuthzSubjectDAO;
import org.hyperic.hibernate.dao.OperationDAO;
import org.hyperic.hibernate.dao.ResourceDAO;
import org.hyperic.hibernate.dao.ResourceGroupDAO;
import org.hyperic.hibernate.dao.ResourceTypeDAO;
import org.hyperic.hibernate.dao.RoleDAO;
import org.hyperic.hq.authz.AuthzNamedEntity;
import org.hyperic.hq.authz.AuthzSubject;
import org.hyperic.hq.authz.Resource;
import org.hyperic.hq.authz.ResourceGroup;
import org.hyperic.hq.authz.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectLocalHome;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocalHome;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.OperationLocal;
import org.hyperic.hq.authz.shared.OperationLocalHome;
import org.hyperic.hq.authz.shared.OperationUtil;
import org.hyperic.hq.authz.shared.OperationValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupLocal;
import org.hyperic.hq.authz.shared.ResourceGroupLocalHome;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocalHome;
import org.hyperic.hq.authz.shared.ResourceGroupManagerUtil;
import org.hyperic.hq.authz.shared.ResourceGroupPK;
import org.hyperic.hq.authz.shared.ResourceGroupUtil;
import org.hyperic.hq.authz.shared.ResourceGroupValue;
import org.hyperic.hq.authz.shared.ResourceLocal;
import org.hyperic.hq.authz.shared.ResourceLocalHome;
import org.hyperic.hq.authz.shared.ResourceManagerLocalHome;
import org.hyperic.hq.authz.shared.ResourceManagerUtil;
import org.hyperic.hq.authz.shared.ResourceTypeLocal;
import org.hyperic.hq.authz.shared.ResourceTypeLocalHome;
import org.hyperic.hq.authz.shared.ResourceTypeUtil;
import org.hyperic.hq.authz.shared.ResourceTypeValue;
import org.hyperic.hq.authz.shared.ResourceUtil;
import org.hyperic.hq.authz.shared.ResourceValue;
import org.hyperic.hq.authz.shared.RoleLocal;
import org.hyperic.hq.authz.shared.RoleLocalHome;
import org.hyperic.hq.authz.shared.RolePK;
import org.hyperic.hq.authz.shared.RoleUtil;
import org.hyperic.hq.authz.shared.RoleValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hibernate.ObjectNotFoundException;

/**
 * This is the parent class for all Authz Session Beans
 */
public abstract class AuthzSession {

    public static final Log log
        = LogFactory.getLog(AuthzSession.class.getName());

    protected static final String DATASOURCE = HQConstants.DATASOURCE;
    protected static InitialContext ic = null;

    // Homes for the entities
    protected ResourceLocalHome resourceHome;
    protected ResourceTypeLocalHome resTypeHome;
    protected ResourceGroupLocalHome groupHome;
    protected AuthzSubjectLocalHome subjectHome;
    protected OperationLocalHome operationHome;
    protected RoleLocalHome roleHome;
    
    // Homes for the sessions
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
        return DAOFactory.getDAOFactory().getAuthzSubjectDAO();
    }

    protected RoleDAO getRoleDAO() {
        return DAOFactory.getDAOFactory().getRoleDAO();
    }

    protected OperationDAO getOperationDAO() {
        return DAOFactory.getDAOFactory().getOperationDAO();
    }

    /** now the home cache methods **/
    protected ResourceLocalHome getResourceHome() {
        try {
            if (resourceHome == null) {
                resourceHome = ResourceUtil.getLocalHome();
            }
            return resourceHome;
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    protected ResourceTypeLocalHome getResourceTypeHome() throws NamingException {
        if (resTypeHome == null) {
            resTypeHome = ResourceTypeUtil.getLocalHome();
        }
        return resTypeHome;
    }

    private ResourceGroupLocalHome getGroupHome() throws NamingException {
        if (groupHome == null) {
            groupHome = ResourceGroupUtil.getLocalHome();
        }
        return groupHome;
    }

    protected AuthzSubjectLocalHome getSubjectHome() throws NamingException {
        if (subjectHome == null) {
            subjectHome = AuthzSubjectUtil.getLocalHome();
        }
        return subjectHome;
    }

    protected OperationLocalHome getOperationHome() throws NamingException {    
        if (operationHome == null) {
            operationHome = OperationUtil.getLocalHome();
        }
        return operationHome;
    }

    protected RoleLocalHome getRoleHome() throws NamingException {
        if (roleHome == null) {
            roleHome = RoleUtil.getLocalHome();
        }
        return roleHome;
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
    public AuthzSubjectValue findOverlord()
        throws NamingException, FinderException {
        return findSubjectByAuth(AuthzConstants.overlordName,
                                 AuthzConstants.overlordDsn);
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
    public AuthzSubjectValue findSubjectByAuth(String name, String authDsn)
    {
         AuthzSubject subject = DAOFactory.getDAOFactory().getAuthzSubjectDAO()
            .findByAuth(name, authDsn);
        if (subject == null) {
            throw new ObjectNotFoundException(
                name, "Can't find subject: name="+name+",authDsn="+authDsn);
        }
        return subject.getAuthzSubjectValue();
    }

    protected Set toLocals(Object[] values)
        throws NamingException, FinderException {
        Set locals = new HashSet();
        boolean isOperation = false;
        boolean isResource = false;
        boolean isResourceGroup = false;
        boolean isRole = false;
        boolean isAuthzSubject = false;

        OperationLocalHome opLome = null;
        ResourceLocalHome rLome = null;
        ResourceGroupLocalHome groupLome = null;
        AuthzSubjectLocalHome subjectLome = null;
        RoleLocalHome roleLome = null;
        if (values != null && (values.length > 0)) {
            int counter = 0;
            Object value = values[0];
            if (value instanceof
                org.hyperic.hq.authz.shared.OperationValue) {
                opLome = OperationUtil.getLocalHome();
                isOperation = true;
            } else if (value instanceof
                       org.hyperic.hq.authz.shared.ResourceValue) {
                rLome = ResourceUtil.getLocalHome();
                isResource = true;
            } else if (value instanceof
                       org.hyperic.hq.authz.shared.ResourceGroupValue) {
                groupLome = ResourceGroupUtil.getLocalHome();
                isResourceGroup = true;
            } else if (value instanceof
                       org.hyperic.hq.authz.shared.RoleValue) {
                roleLome = RoleUtil.getLocalHome();
                isRole = true;
            } else if (value instanceof
                       org.hyperic.hq.authz.shared.AuthzSubjectValue) {
                subjectLome = AuthzSubjectUtil.getLocalHome();
                isAuthzSubject = true;
            } else {
                log.error("Invalid type.");
            }
            for (counter = 0; counter < values.length; counter++) {
                if (isOperation) {
                    OperationValue op = (OperationValue)values[counter];
                    locals.add(opLome.findByPrimaryKey(op.getPrimaryKey()));
                } else if (isResource) {
                    ResourceValue r = (ResourceValue)values[counter];
                    locals.add(rLome.findByPrimaryKey(r.getPrimaryKey()));
                } else if (isResourceGroup) {
                    ResourceGroupValue group = (ResourceGroupValue)values[counter];
                    locals.add(groupLome.findByPrimaryKey(group.getPrimaryKey()));
                } else if (isRole) {
                    RoleValue role = (RoleValue)values[counter];
                    locals.add(roleLome.findByPrimaryKey(role.getPrimaryKey()));
                } else if (isAuthzSubject) {
                    AuthzSubjectValue subject = (AuthzSubjectValue)values[counter];
                    locals.add(subjectLome.findByPrimaryKey(subject.getPrimaryKey()));
                } else {
                    log.error("Invalid type.");
                }
            }
        }
        return locals;
    }

    protected Set toPojos(Object[] vals) {
        final int OPER_HASH = OperationValue.class.hashCode();
        final int SUBJ_HASH = AuthzSubjectValue.class.hashCode();

        Set ret = new HashSet();
        if (vals == null || vals.length == 0) {
            return ret;
        }
        
        int hashCode = vals[0].hashCode();
        
        OperationDAO operDao = null;
        AuthzSubjectDAO subjDao = null;
        for (int i = 0; i < vals.length; i++) {
            if (hashCode == OPER_HASH) {
                if (operDao == null) {
                    operDao = getOperationDAO();
                }
                ret.add(operDao.findId(((OperationValue) vals[i]).getId()));
            }
            else if (hashCode == SUBJ_HASH) {
                if (subjDao == null) {
                    subjDao = getSubjectDAO();
                }
                ret.add(subjDao.findById(((AuthzSubjectValue)vals[i]).getId()));
            }
            else {
                log.error("Invalid type.");
            }

        }
                
        return ret;
    }
    
    protected Object[] fromPojos(Collection pojos, Class c) {
        Object[] values = new Object[pojos.size()];
        
        int i = 0;
        for (Iterator it = pojos.iterator(); it.hasNext(); i++) {
            AuthzNamedEntity ent = (AuthzNamedEntity) it.next();
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

        if (c == org.hyperic.hq.authz.shared.OperationValue.class) {
            values = new OperationValue[locals.size()];
            isOperation = true;
        } else if (c == org.hyperic.hq.authz.shared.ResourceValue.class) {
            values = new ResourceValue[locals.size()];
            isResource = true;
        } else if (c == org.hyperic.hq.authz.shared.ResourceGroupValue.class) {
            values = new ResourceGroupValue[locals.size()];
            isResourceGroup = true;
        } else if (c == org.hyperic.hq.authz.shared.RoleValue.class) {
            values = new RoleValue[locals.size()];
            isRole = true;
        } else if (c == org.hyperic.hq.authz.shared.AuthzSubjectValue.class) {
            values = new AuthzSubjectValue[locals.size()];
            isAuthzSubject = true;
        } else {
            log.error("Invalid type.");
        }

        while (it.hasNext()) {
            if (isOperation) {
                values[counter] =
                    ((OperationLocal)it.next()).getOperationValue();
            } else if (isResource) {
                values[counter] =
                    ((ResourceLocal)it.next()).getResourceValue();
            } else if (isResourceGroup) {
                values[counter] =
                    ((ResourceGroupLocal)it.next()).getResourceGroupValue();
            } else if (isRole) {
                values[counter] =
                    ((RoleLocal)it.next()).getRoleValue();
            } else if (isAuthzSubject) {
                values[counter] =
                    ((AuthzSubjectLocal)it.next()).getAuthzSubjectValue();
            } else {
                log.error("Invalid type.");
            }
            counter++;
        }
        return values;
    }

    protected AuthzSubjectLocal lookupSubject(AuthzSubjectValue subject)
        throws FinderException {
        try {
            return getSubjectHome().findByPrimaryKey(subject.getPrimaryKey());
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    protected AuthzSubjectLocal lookupSubject(Integer id)
        throws FinderException {
        try {
            return getSubjectHome().findById(id);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    protected AuthzSubject lookupSubjectPojo(AuthzSubjectValue subject) {
        return lookupSubjectPojo(subject.getId());
    }

    protected AuthzSubject lookupSubjectPojo(Integer id) {
        return DAOFactory.getDAOFactory().getAuthzSubjectDAO().findById(id);
    }

    protected ResourceTypeLocal lookupType(ResourceTypeValue type)
        throws NamingException, FinderException {
        return getResourceTypeHome().findByPrimaryKey(type.getPrimaryKey());
    }

    protected ResourceGroupLocal lookupGroup(ResourceGroupValue group)
        throws NamingException, FinderException {
        return getGroupHome().findByPrimaryKey(group.getPrimaryKey());
    }

    protected ResourceGroupLocal lookupGroup(Integer id)
        throws NamingException, FinderException {
        ResourceGroupPK groupPk = new ResourceGroupPK(id);
        return getGroupHome().findByPrimaryKey(groupPk);
    }

    protected ResourceLocal lookupResource(ResourceValue resource)
        throws FinderException {
        if (resource.getId() == null) {
            String typeName = resource.getResourceTypeValue().getName();
            ResourceTypeLocal typeLocal;
            try {
                typeLocal = getResourceTypeHome()
                    .findByName(typeName);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
            return getResourceHome().findByInstanceId(typeLocal,
                                                      resource.getInstanceId());
        } 
        return getResourceHome().findByPrimaryKey(resource.getPrimaryKey());
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
     * Get a list of RolePK's a user has viewRole permission
     * for (or owns)
     */
    protected List getViewableRolePKs(AuthzSubjectValue who) 
        throws NamingException, PermissionException,
               FinderException {
        PermissionManager pm = PermissionManagerFactory.getInstance();
        List roleIds = 
            pm.findOperationScopeBySubject(who,
                                           AuthzConstants.roleOpViewRole,
                                           AuthzConstants.roleResourceTypeName,
                                           PageControl.PAGE_ALL);
        List pks = new ArrayList();
        // now make ResourceGroupPKs out of em
        for(int i = 0; i < roleIds.size(); i++) {
            Integer id = (Integer)roleIds.get(i);
            pks.add(new RolePK(id));
        }
        return pks;
    }

    /**
     * Get a list of ResourceGroupPK's a user has viewResourceGroup permission
     * for (or owns)
     */
    protected List getViewableGroupPKs(AuthzSubjectValue who) 
        throws NamingException, PermissionException,
               FinderException {
        
        PermissionManager pm = PermissionManagerFactory.getInstance();         
        List groupIds = 
            pm.findOperationScopeBySubject(who,
                                           AuthzConstants.groupOpViewResourceGroup,
                                           AuthzConstants.groupResourceTypeName,
                                           PageControl.PAGE_ALL);
        List pks = new ArrayList();
        // now make ResourceGroupPKs out of em
        for(int i = 0; i < groupIds.size(); i++) {
            Integer id = (Integer)groupIds.get(i);
            pks.add(new ResourceGroupPK(id));
        }
        return pks;
    }

    /** 
     * Filter a collection of groupLocal objects to only include those viewable
     * by the specified user
     * @throws FinderException 
     * @throws NamingException 
     * @throws PermissionException 
     */
    protected Collection filterViewableGroups(AuthzSubjectValue who,
                                              Collection groups)
        throws PermissionException, NamingException, FinderException {
        // finally scope down to only the ones the user can see
        List viewable = getViewableGroupPKs(who);
        for(Iterator i = groups.iterator(); i.hasNext();) {
            Object resGrp = i.next();
            
            ResourceGroupPK resGrpPk;
            if (resGrp instanceof ResourceGroup) {
                resGrpPk =
                    new ResourceGroupPK(((ResourceGroup) resGrp).getId());
            }
            else {
                resGrpPk = (ResourceGroupPK)
                    ((ResourceGroupLocal) resGrp).getPrimaryKey();
            }
            
            if (!viewable.contains(resGrpPk)) {
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
        Connection conn;
        try {
            conn = DBUtil.getConnByContext(this.getInitialContext(), DATASOURCE);
        } catch(NamingException exc){
            throw new SystemException("Unable to get database context: " +
                                         exc.getMessage(), exc);
        }
        return conn;
    }
    
    public void setSessionContext(SessionContext ctx) {
        this.ctx = ctx;
    }
    
    protected SessionContext getSessionContext() {
        return this.ctx;
    }
}
