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

package org.hyperic.hq.dao;


import org.hibernate.Session;
import org.hyperic.hq.authz.Operation;
import org.hyperic.hq.authz.ResourceType;
import org.hyperic.hq.authz.Role;
import org.hyperic.hq.authz.shared.OperationValue;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.dao.DAOFactory;

/**
 * CRUD methods, finders, etc. for Operation
 */
public class OperationDAO extends HibernateDAO
{
    public OperationDAO(Session session) {
        super(Operation.class, session);
    }

    public Operation create(OperationValue createInfo) {
        Operation res = new Operation(createInfo);
        DAOFactory factory = DAOFactory.getDAOFactory();
        Role rootRole=factory.getRoleDAO().findById(AuthzConstants.rootRoleId);
        if (rootRole == null) {
            throw new IllegalArgumentException("role id not found "+
                                               AuthzConstants.rootRoleId);
        }
        rootRole.getOperations().add(res);
        save(res);
        return res;
    }

    public Operation findById(Integer id) {
        return (Operation) super.findById(id);
    }

    public void save(Operation entity) {
        super.save(entity);
    }

    public Operation merge(Operation entity) {
        return (Operation) super.merge(entity);
    }

    public void remove(Operation entity) {
        super.remove(entity);
    }

    public void evict(Operation entity) {
        super.evict(entity);
    }

    public Operation findByTypeAndName(ResourceType type, String name)
    {            
        String sql = "select o from Operation o " +
                     "join fetch o.resourceType rt " +
                     "where rt.id=? and o.name=?";
        return (Operation)getSession().createQuery(sql)
            .setInteger(0, type.getId().intValue())
            .setString(1, name)
            .uniqueResult();
    }
}
