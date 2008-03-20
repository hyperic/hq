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

package org.hyperic.hq.galerts.server.session;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hibernate.SortField;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;

class GalertLogDAO
    extends HibernateDAO
{
    GalertLogDAO(DAOFactory f) {
        super(GalertLog.class, f);
    }

    GalertLog findById(Integer id) {
        return (GalertLog)super.findById(id);
    }

    void save(GalertLog log) {
        super.save(log);
    }
    
    void save(GalertAuxLog log) {
        super.save(log);
    }

    void remove(GalertLog log) {
        super.remove(log);
    }
    
    List findAll(ResourceGroup g) {
        String sql = "from GalertLog l where l.alertDef.group = :group " + 
                     "order by l.timestamp";
        
        return getSession().createQuery(sql)
            .setParameter("group", g)
            .list();
    }
    
    public GalertLog findLastByDefinition(GalertDef def, boolean fixed) {
        return (GalertLog) createCriteria()
            .add(Expression.eq("alertDef", def))
            .add(Expression.eq("fixed", new Boolean(fixed)))
            .addOrder(Order.desc("timestamp"))
            .setMaxResults(1)
            .uniqueResult();
    }

    PageList findByTimeWindow(ResourceGroup g, long begin, long end,
                              PageControl pc) {
        final String tsProp = "timestamp";
        Integer count = (Integer) createCriteria()
            .createAlias("alertDef", "d")
            .add(Restrictions.eq("d.group", g))
            .add(Restrictions.ge(tsProp, new Long(begin)))
            .add(Restrictions.le(tsProp, new Long(end)))
            .setProjection(Projections.rowCount())
            .uniqueResult();

        if (count.intValue() > 0) {
            Criteria crit = createCriteria()
                .createAlias("alertDef", "d")
                .add(Restrictions.eq("d.group", g))
                .add(Restrictions.ge(tsProp, new Long(begin)))
                .add(Restrictions.le(tsProp, new Long(end)))
                .addOrder(pc.isDescending() ? Order.desc(tsProp) :
                                              Order.asc(tsProp));
            
            return getPagedResult(crit, count, pc);
        }

        
        return new PageList();
    }

    List findByCreateTime(long startTime, long endTime, int count) {
        return createCriteria()
            .add(Expression.between("timestamp", new Long(startTime), 
                                    new Long(endTime)))
            .addOrder(Order.desc("timestamp"))
            .setMaxResults(count)
            .list();
    }
    
    List findByCreateTimeAndPriority(Integer subjectId, long begin, long end, 
                                     AlertSeverity severity, PageInfo pageInfo) 
    {
        GalertLogSortField sort = (GalertLogSortField)pageInfo.getSort();
        String op = AuthzConstants.groupOpManageAlerts;
        String sql = 
            PermissionManagerFactory.getInstance().getGroupAlertsHQL() +
            " order by " + sort.getSortString("a", "d", "g") + 
            (pageInfo.isAscending() ? "" : " DESC");

               
        if (!sort.equals(GalertLogSortField.DATE)) {
            sql += ", " + GalertLogSortField.DATE.getSortString("a", "d", "g") +
                   " DESC";
        }
               
        Query q = getSession().createQuery(sql)
                              .setLong("begin", begin)
                              .setLong("end", end)
                              .setInteger("priority", severity.getCode());
                              
        if (sql.indexOf("subj") > 0) {
            q.setInteger("subj", subjectId.intValue())
             .setParameter("op", op);
        }

        return pageInfo.pageResults(q).list();
    }

    void removeAll(ResourceGroup g) {
        String sql = "delete from GalertLog l where l.alertDef.group = :group";
        
        getSession().createQuery(sql)
            .setParameter("group", g)
            .executeUpdate();
    }
    
    void removeAll(GalertDef d) {
        String sql = "delete from GalertLog l where l.alertDef = :def";
        
        getSession().createQuery(sql)
                    .setParameter("def", d)
                    .executeUpdate();
    }

    public Integer countAlerts(ResourceGroup g) {
        return (Integer) createCriteria()
            .createAlias("alertDef", "d")
            .add(Restrictions.eq("d.group", g))
            .setProjection(Projections.rowCount())
            .uniqueResult();
    }
}
