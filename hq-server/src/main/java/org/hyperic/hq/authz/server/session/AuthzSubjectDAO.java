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
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.common.server.session.Crispo;
import org.hyperic.hq.common.server.session.CrispoDAO;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class AuthzSubjectDAO
    extends HibernateDAO<AuthzSubject> {
    private CrispoDAO crispoDao;
    private ResourceTypeDAO resourceTypeDAO;
    private RoleDAO roleDAO;
    private ResourceDAO resourceDAO;

    @Autowired
    public AuthzSubjectDAO(SessionFactory f, CrispoDAO crispoDAO, ResourceTypeDAO resourceTypeDAO,
                           ResourceDAO resourceDAO, RoleDAO roleDAO) {
        super(AuthzSubject.class, f);
        this.crispoDao = crispoDAO;
        this.resourceDAO = resourceDAO;
        this.resourceTypeDAO = resourceTypeDAO;
        this.roleDAO = roleDAO;
    }

    AuthzSubject create(AuthzSubject creator, String name, boolean active, String dsn, String dept,
                        String email, String first, String last, String phone, String sms,
                        boolean html) {
        AuthzSubject subject = new AuthzSubject(active, dsn, dept, email, html, first, last, name,
            phone, sms, false);
        save(subject);

        // XXX create resource for owner
      
        ResourceType rt = resourceTypeDAO.findByName(AuthzConstants.subjectResourceTypeName);
        if (rt == null) {
            throw new IllegalArgumentException("resource type not found " +
                                               AuthzConstants.subjectResourceTypeName);
        }

        
        Resource r = resourceDAO.create(rt, resourceDAO.findRootResource(), null, /* No Name? */
        creator, subject.getId(), false);

        subject.setResource(r);
        Role role = roleDAO.findByName(AuthzConstants.creatorRoleName);
        if (role == null) {
            throw new IllegalArgumentException("role not found " + AuthzConstants.creatorRoleName);
        }
        subject.addRole(role);

        // Insert an empty config response
        Crispo c = Crispo.create(new ConfigResponse(), false);
        crispoDao.save(c);

        subject.setPrefs(c);
        save(subject);
        return subject;
    }

   

    public void remove(AuthzSubject entity) {
        Crispo c = entity.getPrefs();
        entity.setPrefs(null);
        crispoDao.remove(c);
        super.remove(entity);
    }

    public AuthzSubject findByAuth(String name, String dsn) {
        String sql = "from AuthzSubject s where s.name=? and s.authDsn=?";
        return (AuthzSubject) getSession().createQuery(sql).setString(0, name).setString(1, dsn)
            .setCacheable(true).setCacheRegion("AuthzSubject.findByAuth").uniqueResult();
    }

    public AuthzSubject findByName(String name) {
        String sql = "from AuthzSubject where name=?";
        return (AuthzSubject) getSession().createQuery(sql).setString(0, name).setCacheable(true)
            .setCacheRegion("AuthzSubject.findByName").uniqueResult();
    }

    private Criteria findMatchingNameCriteria(String name) {
        name = '%' + name + '%';
        return createCriteria().add(
            Restrictions.or(Restrictions.ilike("name", name), Restrictions.or(Restrictions.ilike(
                "firstName", name), Restrictions.ilike("lastName", name)))).add(
            Restrictions.eq("system", Boolean.FALSE));
    }

    public PageList<AuthzSubject> findMatchingName(String name, PageControl pc) {
        Integer count = (Integer) findMatchingNameCriteria(name).setProjection(
            Projections.rowCount()).uniqueResult();

        Criteria crit = findMatchingNameCriteria(name).addOrder(Order.asc("sortName"));
        return getPagedResult(crit, count, pc);
    }

    /**
     * Create the criteria used by findById_orderName() because Criteria does
     * not yet support Cloneable
     */
    private Criteria findById_orderNameCriteria(Integer[] ids) {
        return createCriteria().add(Restrictions.in("id", ids));
    }

    public PageList<AuthzSubject> findById_orderName(Integer[] ids, PageControl pc) {
        Integer count = (Integer) findById_orderNameCriteria(ids).setProjection(
            Projections.rowCount()).uniqueResult();

        Criteria crit = findById_orderNameCriteria(ids).addOrder(
            pc.isAscending() ? Order.asc("sortName") : Order.desc("sortName"));

        return getPagedResult(crit, count, pc);
    }
    
    public Collection<AuthzSubject> findByIds(Integer[] ids) {
        final Criteria criteria = findById_orderNameCriteria(ids);
        final List<AuthzSubject> list = criteria.list();
        return list;
    }

    public Collection findAll_order(boolean isRoot, String col, boolean asc, Collection excludes) {
        Criteria criteria = createCriteria();

        if (isRoot) {
            Disjunction disjunctions = Restrictions.disjunction();
            disjunctions.add(Restrictions.eq("system", Boolean.FALSE));
            disjunctions.add(Restrictions.eq("id", AuthzConstants.rootSubjectId));
            criteria.add(disjunctions);
        } else {
            criteria.add(Restrictions.eq("system", Boolean.FALSE));
        }

        criteria.addOrder(asc ? Order.asc(col) : Order.desc(col));

        if (excludes != null && excludes.size() > 0) {
            criteria.add(Restrictions.not(Restrictions.in("id", excludes)));
        }

        return criteria.list();

    }

    public Collection<AuthzSubject> findAll_orderName(Collection excludes, boolean asc) {
        return findAll_order(false, "sortName", asc, excludes);
    }

    public Collection<AuthzSubject> findAll_orderFirstName(Collection excludes, boolean asc) {
        return findAll_order(false, "firstName", asc, excludes);
    }

    public Collection<AuthzSubject> findAll_orderLastName(Collection excludes, boolean asc) {
        return findAll_order(false, "lastName", asc, excludes);
    }

    public Collection<AuthzSubject> findAllRoot_orderName(Collection excludes, boolean asc) {
        return findAll_order(true, "sortName", asc, excludes);
    }

    public Collection<AuthzSubject> findAllRoot_orderFirstName(Collection excludes, boolean asc) {
        return findAll_order(true, "firstName", asc, excludes);
    }

    public Collection<AuthzSubject> findAllRoot_orderLastName(Collection excludes, boolean asc) {
        return findAll_order(true, "lastName", asc, excludes);
    }

    public Collection<AuthzSubject> findByRoleId_orderName(Integer roleId, boolean asc) {
        return getSession().createQuery(
            "select s from AuthzSubject s join fetch s.roles r " + "where r.id = ? and " +
                "(s.system = false or s.id = " + AuthzConstants.rootSubjectId +
                ") order by s.sortName " + (asc ? "asc" : "desc")).setInteger(0, roleId.intValue())
            .list();
    }

    public Collection<AuthzSubject> findByNotRoleId_orderName(Integer roleId, boolean asc) {
        return getSession().createQuery(
            "select distinct s from AuthzSubject s, Role r " + "where r.id = ? and s.id not in " +
                "(select id from r.subjects) and " + "s.system = false order by s.sortName " +
                (asc ? "asc" : "desc")).setInteger(0, roleId.intValue()).list();
    }
}
