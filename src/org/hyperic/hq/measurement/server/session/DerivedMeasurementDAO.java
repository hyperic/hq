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

package org.hyperic.hq.measurement.server.session;

import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.dao.HibernateDAO;

public class DerivedMeasurementDAO extends HibernateDAO {
    public DerivedMeasurementDAO(DAOFactory f) {
        super(DerivedMeasurement.class, f);
    }

    public DerivedMeasurement findById(Integer id) {
        return (DerivedMeasurement)super.findById(id);
    }

    public DerivedMeasurement get(Integer id) {
        return (DerivedMeasurement)super.get(id);
    }

    void remove(DerivedMeasurement entity) {
        if (entity.getBaseline() != null)
            super.remove(entity.getBaseline());
        super.remove(entity);
    }

    DerivedMeasurement create(Integer instanceId,
                              MeasurementTemplate mt,
                              long interval) {
        DerivedMeasurement dm = new DerivedMeasurement(instanceId, mt,
                                                       interval);

        dm.setEnabled(interval != 0);
        dm.setFormula(mt.getTemplate());
        save(dm);
        return dm;
    }

    List findByIds(Integer ids[]) {
        String sql = "from DerivedMeasurement where id IN (:ids)";

        return getSession().createQuery(sql)
            .setParameterList("ids", ids)
            .list();
    }

    List findAllCollected() {
        return createCriteria()
            .add(Restrictions.isNotNull("interval"))
            .list();
    }
    
    public DerivedMeasurement findByTemplateForInstance(Integer tid,
                                                        Integer iid) {
        String sql =
            "select distinct m from DerivedMeasurement m " +
            "join m.template t " +
            "where t.id=? and m.instanceId=?";

        return (DerivedMeasurement)getSession().createQuery(sql)
            .setInteger(0, tid.intValue())
            .setInteger(1, iid.intValue())
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findByTemplateForInstance")
            .uniqueResult();
    }
    
    /**
     * Look up a derived measurement, allowing for the query to return a stale 
     * copy of the derived measurement (for efficiency reasons).
     * 
     * @param tid
     * @param iid
     * @param allowStale <code>true</code> to allow stale copies of an alert 
     *                   definition in the query results; <code>false</code> to 
     *                   never allow stale copies, potentially always forcing a 
     *                   sync with the database.
     * @return The derived measurement or <code>null</code>.
     */
    public DerivedMeasurement findByTemplateForInstance(Integer tid, 
                                                        Integer iid, 
                                                        boolean allowStale) {
        Session session = this.getSession();
        FlushMode oldFlushMode = session.getFlushMode();
        
        try {
            if (allowStale) {
                session.setFlushMode(FlushMode.MANUAL);                
            }
            
            return this.findByTemplateForInstance(tid, iid); 
        } finally {
            session.setFlushMode(oldFlushMode);
        } 
    }


    public List findIdsByTemplateForInstances(Integer tid, Integer[] iids) {
        String sql = "select id from DerivedMeasurement " +
                     "where template.id = :tid and instanceId IN (:ids)";

        return getSession().createQuery(sql)
            .setInteger("tid", tid.intValue())
            .setParameterList("ids", iids)
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findIdsByTemplateForInstances")
            .list();
    }

    List findByTemplate(Integer id) {
        String sql =
            "select distinct m from DerivedMeasurement m " +
            "join m.template t " +
            "where t.id=?";

        return getSession().createQuery(sql)
            .setInteger(0, id.intValue()).list();
    }

    Map findByInstance(AppdefEntityID[] aeids)
    {
        Map rtn = new HashMap(aeids.length);
        String sql =
            "select distinct m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "where mt.appdefType=? and m.instanceId=? and " +
            "m.interval is not null";

        for (int i=0; i<aeids.length; i++)
        {
            int type = aeids[i].getType(),
                id   = aeids[i].getID();
            List derivedMeas = getSession().createQuery(sql)
                .setInteger(0, type)
                .setInteger(1, id)
                .setCacheable(true)
                .setCacheRegion("DerivedMeasurement.findByInstance_with_interval")
                .list();
            rtn.put(aeids[i], derivedMeas);
        }
        return rtn;
    }

    List findByInstance(int type, int id) {
        String sql =
            "select distinct m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "where mt.appdefType=? and m.instanceId=? and " +
            "m.interval is not null";
        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findByInstance_with_interval")
            .list();
    }

    List findByInstances(AppdefEntityID[] ids) {
        Map map = AppdefUtil.groupByAppdefType(ids);
        StringBuffer sql = new StringBuffer("from DerivedMeasurement where ");
        for (int i = 0; i < map.size(); i++) {
            if (i > 0) {
                sql.append(" or ");
            }
            sql.append(" id in " +
                       "(select m.id from DerivedMeasurement m " +
                       "join m.template t " +
                       "join t.monitorableType mt where " +
                       "mt.appdefType=")
                .append(":appdefType"+i)
                .append(" and ")
                .append("m.instanceId in (:list" + i + ")")
                .append(") ");
        }

        // delete derived measurements
        Query q = getSession().createQuery(sql.toString());
        int j = 0;
        for (Iterator i = map.keySet().iterator(); i.hasNext(); j++) {
            Integer appdefType = (Integer)i.next();
            List list = (List)map.get(appdefType);
            q.setInteger("appdefType"+j, appdefType.intValue())
                .setParameterList("list"+j, list);
        }
        
        return q.list();
    }

    int deleteByInstances(AppdefEntityID[] ids) {
        List v = findByInstances(ids);
        
        for (Iterator i=v.iterator(); i.hasNext(); ) {
            remove((DerivedMeasurement)i.next());
        }
        return v.size();
    }

    public List findByInstance(int type, int id, boolean enabled) {
        String sql =
            "select distinct m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "where mt.appdefType=? and m.instanceId=? and " +
            "m.enabled = ? and m.interval is not null";

        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .setBoolean(2, enabled)
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findByInstance")
            .list();
    }

    List findByInstanceForCategory(int type, int id, String cat) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "join t.category c " +
            "where mt.appdefType = ? and " +
            "m.instanceId = ? and " +
            "c.name = ? and " +
            "m.interval is not null";

        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .setString(2, cat).list();
    }

    List findByInstanceForCategory(int type, int id, boolean enabled,
                                   String cat) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "join t.category c " +
            "where mt.appdefType = ? and " +
            "m.instanceId = ? and " +
            "m.enabled = ? " +
            "c.name = ? and " +
            "m.interval is not null " +
            "order by t.name";

        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .setString(2, cat).list();
    }
    
    DerivedMeasurement findByAliasAndID(String alias,
                                        int appdefType, int appdefId) {

        String sql =
            "select distinct m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "where t.alias = ? and mt.appdefType = ? " +
            "and m.instanceId = ? and m.interval is not null";

        return (DerivedMeasurement)getSession().createQuery(sql)
            .setString(0, alias)
            .setInteger(1, appdefType)
            .setInteger(2, appdefId).uniqueResult();
    }

    List findDesignatedByInstanceForCategory(int appdefType, int iid,
                                             String cat) 
    {
        List res = findDesignatedByInstance(appdefType, iid);
        
        for (Iterator i=res.iterator(); i.hasNext(); ) {
            DerivedMeasurement dm = (DerivedMeasurement)i.next();
            
            if (!dm.getTemplate().getCategory().getName().equals(cat))
                i.remove();
        }
        
        return res;
    }

    List findDesignatedByInstance(int type, int id) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "where m.instanceId = ? and " +
            "mt.appdefType = ? and " +
            "t.designate = true " +
            "order by t.name";

        return getSession().createQuery(sql)
            .setInteger(0, id)
            .setInteger(1, type)
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findDesignatedByInstance")
            .list();
    }

    List findByRawExcludeIdentity(Integer rid) {
        String sql =
            "select distinct d from DerivedMeasurement d " +
            "join d.template t " +
            "join t.measurementArgsBag a, " +
            "RawMeasurement r " +
            "where d.interval is not null and " +
            "d.instanceId = r.instanceId and " +
            "a.template.id = r.template.id and " +
            "r.id = ? and " +
            "t.template <> ?";

        return getSession().createQuery(sql)
                .setInteger(0, rid.intValue())
                .setString(1, "ARG1").list();
    }

    List findByCategory(String cat) {
        String sql =
            "select distinct m from DerivedMeasurement m " +
            "join m.template t " +
            "join t.monitorableType mt " +
            "join t.category c " +
            "where m.enabled = true " +
            "and m.interval is not null and " +
            "c.name = ?";

        return getSession().createQuery(sql)
            .setString(0, cat)
            .setCacheable(true)
            .setCacheRegion("DerivedMeasurement.findByCategory")
            .list();
    }
}
