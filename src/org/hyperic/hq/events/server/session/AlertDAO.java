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
package org.hyperic.hq.events.server.session;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.dao.HibernateDAO;

public class AlertDAO extends HibernateDAO {
    public AlertDAO(DAOFactory f) {
        super(Alert.class, f);
    }

    public Alert findById(Integer id) {
        return (Alert)super.findById(id);
    }

    int deleteByIds(Integer[] ids) {
        String sql = "delete Alert where id in (:ids)";

        return getSession().createQuery(sql)
            .setParameterList("ids", ids)
            .executeUpdate();
    }

    public Alert get(Integer id)
    {
        return (Alert)super.get(id);
    }

    int deleteByCreateTime(long begin, long end) {
        String sql = "delete Alert where ctime between :timeStart and :timeEnd";

        return getSession().createQuery(sql)
            .setLong("timeStart", begin)
            .setLong("timeEnd", end)
            .executeUpdate();
    }
    
    public List findByEntity(AppdefEntityID id) {
        return findByEntity(id, "a.ctime DESC");
    }
    
    List findEscalatables() {
        String sql = "from Alert a"; 
    
        return getSession().createQuery(sql)
            .list();
    }

    List findByCreateTimeAndPriority(Integer subj, long begin, long end,
                                     int priority, PageInfo pageInfo)   
    {
        String[] ops =
            new String[] { AuthzConstants.platformOpManageAlerts,
                           AuthzConstants.serverOpManageAlerts,
                           AuthzConstants.serviceOpManageAlerts };
        AlertSortField sort = (AlertSortField)pageInfo.getSort();
        Query q;
        
        String sql = PermissionManagerFactory.getInstance().getAlertsHQL() +
                     " order by " + sort.getSortString("a", "d", "r") + 
                     (pageInfo.isAscending() ? "" : " DESC");
        
        // If sorting by something other than date, do a secondary sort by
        // date, descending
        if (!sort.equals(AlertSortField.DATE)) {
            sql += ", " + AlertSortField.DATE.getSortString("a", "d", "r") +
                   " DESC";
        }
            
        q = getSession().createQuery(sql)
            .setLong("begin", begin)
            .setLong("end", end)
            .setInteger("priority", priority)
            .setCacheable(true)
            .setCacheRegion("Alert.findByCreateTime");

        if (sql.indexOf("subj") > 0) {
            q.setInteger("subj", subj.intValue())
             .setParameterList("ops", ops);
        }

        return pageInfo.pageResults(q).list();
    }
    
    public List findByAppdefEntityInRange(AppdefEntityID id, long begin,
                                          long end, boolean nameSort,
                                          boolean asc)
    {
        String sql = "from Alert a where a.alertDefinition.appdefType = :aType " +
            "and a.alertDefinition.appdefId = :aId and a.ctime between :begin " +
            "and :end order by " +
            (nameSort ? "a.alertDefinition.name" : "a.ctime") + 
            (asc ? " asc" : " desc");
        
        return getSession().createQuery(sql)
            .setInteger("aType", id.getType())
            .setInteger("aId", id.getID())
            .setLong("begin", begin)
            .setLong("end", end)
            .list();
    }
    
    private List findByEntity(AppdefEntityID id, String orderBy) {
        String sql = "from Alert a WHERE a.alertDefinition.appdefType = :aType " +
            "and a.alertDefinition.appdefId = :aId ORDER BY " + orderBy;
        
        return getSession().createQuery(sql)
            .setInteger("aType", id.getType())
            .setInteger("aId", id.getId().intValue())
            .setCacheable(true)
            .setCacheRegion("Alert.findByEntity")
            .list();
    }

    int deleteByEntity(AppdefEntityID id) {
        String sql = "delete Alert WHERE alertDefinition in " +
            "(SELECT d FROM AlertDefinition d WHERE d.appdefType = :aType " +
               "and d.appdefId = :aId)";

        return getSession().createQuery(sql)
            .setInteger("aType", id.getType())
            .setInteger("aId", id.getId().intValue())
            .executeUpdate();
    }

    public List findByAppdefEntitySortByAlertDef(AppdefEntityID id) {
        return findByEntity(id, "a.alertDefinition.name DESC");
    }
    
    public Alert findByAlertDefinitionAndCtime(AlertDefinition def, long ctime){
        String sql = "from Alert a WHERE a.alertDefinition = :alertDef " +
            "and a.ctime = :ctime";
        
        return (Alert)getSession().createQuery(sql)
            .setParameter("alertDef", def)
            .setLong("ctime", ctime)
            .uniqueResult();
    }

    public List findByAlertDefinition(AlertDefinition def) {
        String sql = "from Alert a WHERE a.alertDefinition = :alertDef";
        
        return getSession().createQuery(sql)
            .setParameter("alertDef", def)
            .list();
    }

    public Alert findLastByDefinition(AlertDefinition def, boolean fixed) {
        try {
            return (Alert) createCriteria()
                .add(Restrictions.eq("alertDefinition", def))
                .add(Restrictions.eq("fixed", new Boolean(fixed)))
                .addOrder(Order.desc("ctime"))
                .setMaxResults(1)
                .uniqueResult();
        } catch (Exception e) {
            return null;
        }
    }

    int deleteByAlertDefinition(AlertDefinition def) {
        String sql = "DELETE Alert WHERE alertDefinition = :alertDef";

        return getSession().createQuery(sql)
            .setParameter("alertDef", def)
            .executeUpdate();
    }
    
    public Integer countAlerts(AlertDefinition def) {
        return (Integer) createCriteria()
            .add(Restrictions.eq("alertDefinition", def))
            .setProjection(Projections.rowCount())
            .uniqueResult(); 
    }
    
    public Integer countAlerts(AppdefEntityID aeid) {
        return (Integer) createCriteria()
            .createAlias("alertDefinition", "d")
            .add(Restrictions.eq("d.appdefType", new Integer(aeid.getType())))
            .add(Restrictions.eq("d.appdefId", aeid.getId()))
            .setProjection(Projections.rowCount())
            .uniqueResult(); 
    }
    
    void remove(Alert alert) {
        super.remove(alert);
    }
    
    void save(Alert alert) { 
        super.save(alert);
    }
}
