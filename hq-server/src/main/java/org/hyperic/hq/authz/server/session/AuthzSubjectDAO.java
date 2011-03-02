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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.common.server.session.CrispoDAO;
import org.hyperic.hq.config.domain.Crispo;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

@Repository
public class AuthzSubjectDAO {
    

    private CrispoDAO crispoDao;
    private RoleDAO roleDAO;
    private ResourceTypeDao resourceTypeDao;
    @PersistenceContext
    private EntityManager entityManager;
   
    protected AuthzSubjectDAO() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Autowired
    public AuthzSubjectDAO( CrispoDAO crispoDAO, 
                           RoleDAO roleDAO, ResourceTypeDao resourceTypeDao) {
        this.crispoDao = crispoDAO;
        this.roleDAO = roleDAO;
        this.resourceTypeDao = resourceTypeDao;
    }
    
    public AuthzSubject findById(Serializable id) {
        if (id == null) return null;
        AuthzSubject result = entityManager.find(AuthzSubject.class, id);
        if(result != null) {
            result.attach();
        }    
        return result;
    }

    AuthzSubject create(AuthzSubject creator, String name, boolean active, String dsn, String dept,
                        String email, String first, String last, String phone, String sms,
                        boolean html) {
        AuthzSubject subject = new AuthzSubject(active, dsn, dept, email, html, first, last, name,
            phone, sms, false);
        entityManager.persist(subject);
        subject.attach();
        // XXX create resource for owner
        //TODO need this?
        //ResourceType rt = resourceTypeDao.findByName(AuthzConstants.subjectResourceTypeName);
        //if (rt == null) {
          //  throw new IllegalArgumentException("resource type not found " +
                                               //AuthzConstants.subjectResourceTypeName);
        //}

        //TODO
        //Resource r = resourceDAO.create(rt, resourceDAO.findRootResource(), null, /* No Name? */
        //creator, subject.getId(), false);

        //TODO put back Roles?  Current NPE issue initializing Roles set in AuthzSubject...
        //subject.setResource(r);
        //Role role = roleDAO.findByName(AuthzConstants.creatorRoleName);
        //if (role == null) {
          //  throw new IllegalArgumentException("role not found " + AuthzConstants.creatorRoleName);
        //}
        //subject.addRole(role);

        // Insert an empty config response
        Crispo c = Crispo.create(new ConfigResponse());
        crispoDao.save(c);

        subject.setPrefs(c);
        return subject;
    }

    public AuthzSubject findById(Integer id) {
        if (id == null) return null;
        //We aren't allowing lazy fetching of Node-Backed objects, so while you may have gotten a proxy here before, now you don't
        //You also may have been expecting an ObjectNotFoundException.  Now you get back null.
        AuthzSubject result = entityManager.find(AuthzSubject.class, id);
        if(result != null) {
            result.attach();
        }    
        return result;
    }
    
    public AuthzSubject get(Integer id) {
        //You are getting exactly what you expected from Hibernate
        return findById(id);
    }

    public void remove(AuthzSubject entity) {
        Crispo c = entity.getPrefs();
        entity.setPrefs(null);
        crispoDao.remove(c);
        entityManager.remove(entity);
    }
    
    public int size() {
        return ((Number)entityManager.createQuery("select count(a) from AuthzSubject a").getSingleResult()).intValue();
    }

    public AuthzSubject findByAuth(String name, String dsn) {
        String sql = "select s from AuthzSubject s where s.name=:name and s.dsn=:dsn";
        try {
            AuthzSubject subject =  entityManager.createQuery(sql,AuthzSubject.class).setHint("org.hibernate.cacheable", true).
            setHint("org.hibernate.cacheRegion", "AuthzSubject.findByAuth").setParameter("name", name).setParameter("dsn", dsn).getSingleResult();
            subject.attach();
            return subject;
        }catch(EmptyResultDataAccessException e) {
            //Hibernate UniqueResult would return null if nothing, but throw Exception if more than one.  getSingleResult does not do this
            return null;
        }
    }

    public AuthzSubject findByName(String name) {
        String sql = "select s from AuthzSubject s where s.name=?";
        try {
            AuthzSubject subject = entityManager.createQuery(sql,AuthzSubject.class).setHint("org.hibernate.cacheable", true).
            setHint("org.hibernate.cacheRegion", "AuthzSubject.findByName").setParameter(1, name).getSingleResult();
            subject.attach();
            return subject;
        }catch(EmptyResultDataAccessException e) {
            //Hibernate UniqueResult would return null if nothing, but throw Exception if more than one.  getSingleResult does not do this
            return null;
        }
    }

//    private Criteria findMatchingNameCriteria(String name) {
//        name = '%' + name + '%';
//        return createCriteria().add(
//            Restrictions.or(Restrictions.ilike("name", name), Restrictions.or(Restrictions.ilike(
//                "firstName", name), Restrictions.ilike("lastName", name)))).add(
//            Restrictions.eq("system", Boolean.FALSE));
//    }

    public PageList<AuthzSubject> findMatchingName(String name, PageControl pc) {
        //TODO implement
        //Integer count = (Integer) findMatchingNameCriteria(name).setProjection(
          //  Projections.rowCount()).uniqueResult();

        //Criteria crit = findMatchingNameCriteria(name).addOrder(Order.asc("sortName"));
        //return getPagedResult(crit, count, pc);
        return new PageList<AuthzSubject>();
    }

    /**
//     * Create the criteria used by findById_orderName() because Criteria does
//     * not yet support Cloneable
//     */
//    private Criteria findById_orderNameCriteria(Integer[] ids) {
//        return createCriteria().add(Restrictions.in("id", ids));
//    }

    public PageList<AuthzSubject> findById_orderName(Integer[] ids, PageControl pc) {
        //TODO
//        Integer count = (Integer) findById_orderNameCriteria(ids).setProjection(
//            Projections.rowCount()).uniqueResult();
//
//        Criteria crit = findById_orderNameCriteria(ids).addOrder(
//            pc.isAscending() ? Order.asc("sortName") : Order.desc("sortName"));
//
//        return getPagedResult(crit, count, pc);
        return new PageList<AuthzSubject>();
    }

    public Collection findAll_order(boolean isRoot, String col, boolean asc, Collection excludes) {
        //TODO
//        Criteria criteria = createCriteria();
//
//        if (isRoot) {
//            Disjunction disjunctions = Restrictions.disjunction();
//            disjunctions.add(Restrictions.eq("system", Boolean.FALSE));
//            disjunctions.add(Restrictions.eq("id", AuthzConstants.rootSubjectId));
//            criteria.add(disjunctions);
//        } else {
//            criteria.add(Restrictions.eq("system", Boolean.FALSE));
//        }
//
//        criteria.addOrder(asc ? Order.asc(col) : Order.desc(col));
//
//        if (excludes != null && excludes.size() > 0) {
//            criteria.add(Restrictions.not(Restrictions.in("id", excludes)));
//        }
//
//        return criteria.list();
        return new ArrayList();

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
        List<AuthzSubject> subjects = entityManager.createQuery(
            "select s from AuthzSubject s join fetch s.roles r " + "where r.id = ? and " +
                "(s.system = false or s.id = " + AuthzConstants.rootSubjectId +
                ") order by s.sortName " + (asc ? "asc" : "desc"),AuthzSubject.class).setParameter(1, roleId.intValue())
            .getResultList();
        for(AuthzSubject subject: subjects) {
            subject.attach();
        }
        return subjects;
    }

    public Collection<AuthzSubject> findByNotRoleId_orderName(Integer roleId, boolean asc) {
        List<AuthzSubject> subjects = entityManager.createQuery(
            "select distinct s from AuthzSubject s, Role r " + "where r.id = ? and s.id not in " +
                "(select id from r.subjects) and " + "s.system = false order by s.sortName " +
                (asc ? "asc" : "desc"),AuthzSubject.class).setParameter(1, roleId.intValue()).getResultList();
        for(AuthzSubject subject: subjects) {
            subject.attach();
        }
        return subjects;
    }
    
    //TODO was taken from ResourceDAO.  Implement properly
    public int reassignResources(int oldOwner, int newOwner) {
        //return getSession().createQuery(
          //         "UPDATE org.hyperic.hq.authz.server.session.Resource " + "SET owner.id = :newOwner " + "WHERE owner.id = :oldOwner")
            //        .setInteger("oldOwner", oldOwner).setInteger("newOwner", newOwner).executeUpdate();
        return 1;
    }
}
