/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.common.server.session;

import java.util.List;

import org.hibernate.Query;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.dao.HibernateDAO;

public class AuditDAO extends HibernateDAO {
    public AuditDAO(DAOFactory f) {
        super(Audit.class, f);
    }

    Audit findById(Integer id) {
        return (Audit)super.findById(id);
    }

    void remove(Audit c) {
        super.remove(c);
    }
    
    void save(Audit c) { 
        super.save(c);
    }
    
    void handleResourceDelete(Resource r) {
        Integer ROOT = new Integer(0);
        String sql = "update Audit a set " +
                     "a.resource = :rootResource, " +
                     "a.original = false " +
                     "where resource = :resource"; 
        Resource root = 
            ResourceManagerEJBImpl.getOne().findResourcePojoById(ROOT); 
            
        getSession().createQuery(sql)
            .setParameter("rootResource", root)
            .setParameter("resource", r)
            .executeUpdate();
    }
    
    void handleSubjectDelete(AuthzSubject s) {
        getSession().createQuery("delete Audit where subject = :subject")
            .setParameter("subject", s)
            .executeUpdate();
    }
    
    List find(PageInfo pInfo, AuthzSubject me, long startTime, long endTime, 
              AuditImportance minImportance, AuditPurpose purpose, 
              AuthzSubject target, String klazz) 
    {
        AuditSortField sort = (AuditSortField)pInfo.getSort();
        String sql = "select a from Audit a " +
            "         join a.resource r " +
            "         join a.subject s " + 
            "where a.importanceEnum >= :minImportance and " + 
            "      a.startTime >= :startTime and " + 
            "      a.endTime   <  :endTime ";
        
        if (purpose != null)
            sql += " and a.purposeEnum = :purpose";
        
        if (klazz != null) 
            sql += " and a.klazz = :klazz";
        
        if (target != null)
            sql += " and a.subject = :subject";

        sql += " order by " + sort.getSortString("a", "r", "s") +
            (pInfo.isAscending() ? "" : " DESC");
        
        if (!sort.equals(AuditSortField.START_TIME)) {
            sql += ", " + AuditSortField.START_TIME.getSortString("a", "r", "s")
                   + " DESC";
        }
               
        Query q = getSession().createQuery(sql)
            .setInteger("minImportance", minImportance.getCode())
            .setLong("startTime", startTime)
            .setLong("endTime", endTime);

        if (purpose != null)
            q.setInteger("purposeEnum", purpose.getCode());
        
        if (klazz != null)
            q.setParameter("klazz", klazz);
        
        if (target != null)
            q.setParameter("subject", target);
        
        return pInfo.pageResults(q).list();
    }
}
