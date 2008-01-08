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

import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoManagerEJBImpl;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

class AuthzSubjectDAO 
    extends HibernateDAO
{
    AuthzSubjectDAO(DAOFactory f) {
        super(AuthzSubject.class, f);
    }

    AuthzSubject create(AuthzSubject creator, String name, boolean active, 
                        String dsn, String dept, String email, String first, 
                        String last, String phone,  
                        String sms, boolean html)
    {
        AuthzSubject subject = new AuthzSubject(active, dsn, dept, email,
                                                html, first, last, name,
                                                phone, sms, false);
        save(subject);
        DAOFactory daoFactory = DAOFactory.getDAOFactory();

        // XXX create resource for owner
        ResourceTypeDAO rtdao = daoFactory.getResourceTypeDAO();
        ResourceType rt =
            rtdao.findByName(AuthzConstants.subjectResourceTypeName);
        if (rt == null) {
            throw new IllegalArgumentException("resource type not found " +
                                               AuthzConstants.subjectResourceTypeName);
        }

        ResourceDAO rDao = daoFactory.getResourceDAO();
        Resource r = rDao.create(rt, rDao.findRootResource(),  
                                 null, /* No Name? */
                                 creator, subject.getId(), false); 

        subject.setResource(r);
        Role role = daoFactory.getRoleDAO().findByName(
            AuthzConstants.creatorRoleName);
        if (role == null) {
            throw new IllegalArgumentException("role not found " +
                                               AuthzConstants.creatorRoleName);
        }
        subject.addRole(role);
        
        // Insert an empty config response
        Crispo c = CrispoManagerEJBImpl.getOne().create(new ConfigResponse());
        subject.setPrefs(c);
        save(subject);
        return subject;
    }

    public AuthzSubject findById(Integer id) {
        return (AuthzSubject) super.findById(id);
    }

    public AuthzSubject getById(Integer id) {
        return (AuthzSubject) super.get(id);
    }

    void remove(AuthzSubject entity) {
        Crispo c = entity.getPrefs();
        entity.setPrefs(null);
        CrispoManagerEJBImpl.getOne().deleteCrispo(c);
        super.remove(entity);
    }

    public AuthzSubject findByAuth(String name, String dsn) {
        String sql = "from AuthzSubject s where s.name=? and s.authDsn=?";
        return (AuthzSubject)getSession().createQuery(sql)
            .setString(0, name)
            .setString(1, dsn)
            .setCacheable(true)
            .setCacheRegion("AuthzSubject.findByAuth")
            .uniqueResult();
    }

    public AuthzSubject findByName(String name) {
        String sql = "from AuthzSubject where name=?";
        return (AuthzSubject)getSession().createQuery(sql)
            .setString(0, name)
            .setCacheable(true)
            .setCacheRegion("AuthzSubject.findByName")
            .uniqueResult();
    }

    /**
     * Create the criteria used by findById_orderName() because Criteria does
     * not yet support Cloneable
     */
    private Criteria findById_orderNameCriteria(Integer[] ids) {
        return createCriteria().add(Restrictions.in("id", ids));
    }
    
    public PageList findById_orderName(Integer[] ids, PageControl pc) {
        Integer count = (Integer) findById_orderNameCriteria(ids)
            .setProjection(Projections.rowCount())
            .uniqueResult(); 
        
        Criteria crit = findById_orderNameCriteria(ids)
            .addOrder( pc.isAscending() ? Order.asc("sortName") :
                                          Order.desc("sortName"));
        
        return getPagedResult(crit, count, pc);
    }

    public Collection findAll_order(boolean isRoot, String col,
                                    boolean asc, Collection excludes) 
    {
        Criteria criteria = createCriteria();
        
        if (isRoot) {
            Disjunction disjunctions = Restrictions.disjunction();
            disjunctions.add(Restrictions.eq("system", Boolean.FALSE));
            disjunctions.add(Restrictions.eq("id",
                                             AuthzConstants.rootSubjectId));
            criteria .add(disjunctions);
        } else {
            criteria.add(Restrictions.eq("system", Boolean.FALSE));
        }
        
        criteria.addOrder( asc ? Order.asc(col) : Order.desc(col));
        
        if (excludes != null && excludes.size() > 0) {
            criteria.add( Restrictions.not( Restrictions.in("id", excludes)));
        }
        
        return criteria.list();
    
    }

    public Collection findAll_orderName(Collection excludes, boolean asc) {
        return findAll_order(false, "sortName", asc, excludes);
    }

    public Collection findAll_orderFirstName(Collection excludes, boolean asc) {
        return findAll_order(false, "firstName", asc, excludes);
    }

    public Collection findAll_orderLastName(Collection excludes, boolean asc) {
        return findAll_order(false, "lastName", asc, excludes);
    }

    public Collection findAllRoot_orderName(Collection excludes, boolean asc) {
        return findAll_order(true, "sortName", asc, excludes);
    }

    public Collection findAllRoot_orderFirstName(Collection excludes,
                                                 boolean asc)
    {
        return findAll_order(true, "firstName", asc, excludes);
    }

    public Collection findAllRoot_orderLastName(Collection excludes,
                                                boolean asc)
    {
        return findAll_order(true, "lastName", asc, excludes);
    }

    public Collection findByRoleId_orderName(Integer roleId, boolean asc) {
        return getSession()
            .createQuery("select s from AuthzSubject s join fetch s.roles r " +
                         "where r.id = ? and " +
                         "(s.system = false or s.id = " +
                         AuthzConstants.rootSubjectId +
                         ") order by s.sortName " + (asc ? "asc" : "desc"))
            .setInteger(0, roleId.intValue())
            .list();
    }

    public Collection findByNotRoleId_orderName(Integer roleId, boolean asc) {
        return getSession()
            .createQuery("select distinct s from AuthzSubject s, Role r " +
                         "where r.id = ? and s.id not in " +
                         "(select id from r.subjects) and " +
                         "s.system = false order by s.sortName " +
                         (asc ? "asc" : "desc"))
            .setInteger(0, roleId.intValue())
            .list();
    }
}
