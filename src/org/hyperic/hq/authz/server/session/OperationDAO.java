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

import java.util.Collection;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;

public class OperationDAO extends HibernateDAO {
    public OperationDAO(DAOFactory f) {
        super(Operation.class, f);
    }


    Operation findById(Integer id) {
        return (Operation) super.findById(id);
    }

    void save(Operation entity) {
        super.save(entity);
    }

    void remove(Operation entity) {
        super.remove(entity);
    }

    public Operation getByName(String name) {
        String sql = "from Operation where name = :name";
        return (Operation) getSession()
            .createQuery(sql)
            .setParameter("name", name)
            .uniqueResult();
    }

    public Operation findByTypeAndName(ResourceType type, String name) {
        String sql = "from Operation where resourceType=? and name=?";
        return (Operation)getSession().createQuery(sql)
            .setParameter(0, type)
            .setString(1, name)
            .setCacheable(true)
            .setCacheRegion("Operation.findByTypeAndName")
            .uniqueResult();
    }
    
    public Collection findByRole(Integer roleId) {
        String sql = "select o from Role r join r.operations o where r.id = ?";
        return getSession().createQuery(sql)
            .setParameter(0, roleId)
            .list();
    }
    
    public boolean userHasOperation(AuthzSubject subj, Operation op) {
        String hql = new StringBuilder(128)
            .append("select 1 from Role r ")
            .append("join r.operations op ")
            .append("join r.subjects s ")
            .append("where s = :subject and op = :operation")
            .toString();
        return null != getSession()
            .createQuery(hql)
            .setParameter("subject", subj)
            .setParameter("operation", op)
            .uniqueResult();
    }

}
