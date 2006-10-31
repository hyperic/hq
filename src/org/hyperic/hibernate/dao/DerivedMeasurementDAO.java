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

package org.hyperic.hibernate.dao;

import org.hibernate.Session;
import org.hyperic.hq.measurement.DerivedMeasurement;
import org.hyperic.hq.measurement.MeasurementTemplate;

import java.util.List;

/**
 * CRUD methods, finders, etc. for DerivedMeasurement
 */
public class DerivedMeasurementDAO extends HibernateDAO
{
    public DerivedMeasurementDAO(Session session) {
        super(DerivedMeasurement.class, session);
    }

    public DerivedMeasurement findById(Integer id) {
        return (DerivedMeasurement)super.findById(id);
    }

    public void evict(DerivedMeasurement entity) {
        super.evict(entity);
    }

    public DerivedMeasurement merge(DerivedMeasurement entity) {
        return (DerivedMeasurement)super.merge(entity);
    }

    public void save(DerivedMeasurement entity) {
        super.save(entity);
    }

    public void remove(DerivedMeasurement entity) {
        super.remove(entity);
    }

    /**
     * Update the interval for a derived measurement
     */
    public DerivedMeasurement update(DerivedMeasurement dm, long interval) {
        dm.setEnabled(interval != 0);
        dm.setInterval(interval);
        save(dm);
        return dm;
    }

    /**
     * Enable a derived measurement
     */
    public DerivedMeasurement update(DerivedMeasurement dm, boolean enabled) {
        dm.setEnabled(enabled);
        save(dm);
        return dm;
    }

    public DerivedMeasurement create(Integer instanceId,
                                     MeasurementTemplate mt,
                                     long interval) {
        DerivedMeasurement dm = new DerivedMeasurement(instanceId, mt,
                                                       interval);

        dm.setEnabled(interval != 0);
        dm.setFormula(mt.getTemplate());
        save(dm);
        return dm;
    }

    public List findByIds(Integer ids[]) {
        StringBuffer buf = 
            new StringBuffer("from DerivedMeasurement where id IN (");
        int len = ids.length;
        for (int i = 0; i < len - 1; i++) {
            buf.append(ids[i] + ", ");
        }
        buf.append(ids[len - 1] + ")");

        return getSession().createQuery(buf.toString()).list();
    }

    public DerivedMeasurement findByTemplateForInstance(Integer tid, 
                                                        Integer iid) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join fetch m.template t " +
            "where t.id=? and m.instanceId=?";

        return (DerivedMeasurement)getSession().createQuery(sql)
            .setInteger(0, tid.intValue())
            .setInteger(1, iid.intValue()).uniqueResult();
    }

    public List findByTemplate(Integer id) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join fetch m.template t " +
            "where t.id=?";

        return getSession().createQuery(sql)
            .setInteger(0, id.intValue()).list();
    }

    public List findByInstance(int type, int id) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join fetch m.template t " +
            "join fetch t.monitorableType mt " +
            "where mt.appdefType=? and m.instanceId=? and " +
            "m.interval is not null";

        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id).list();
    }

    public List findByInstance(int type, int id, boolean enabled) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join fetch m.template t " +
            "join fetch t.monitorableType mt " +
            "where mt.appdefType=? and m.instanceId=? and " +
            "m.enabled = ? and m.interval is not null";

        return getSession().createQuery(sql)
            .setInteger(0, type)
            .setInteger(1, id)
            .setBoolean(2, enabled).list();
    }

    public DerivedMeasurement findByAliasAndID(String alias,
                                               int appdefType, int appdefId) {

        String sql =
            "select m from DerivedMeasurement m " +
            "join fetch m.tempalte t " +
            "join fetch t.monitorableType mt " +
            "where t.alias = ? and mt.appdefType = ? " +
            "and m.instanceId = ? and m.interval is not null";

        return (DerivedMeasurement)getSession().createQuery(sql)
            .setString(0, alias)
            .setInteger(1, appdefType)
            .setInteger(2, appdefId).uniqueResult();
    }

    public List findDesignatedByInstanceForCategory(int appdefType, int iid,
                                                    String cat) {
        String sql =
            "select m from DerivedMeasurement m " +
            "join fetch m.template t " +
            "join fetch t.monitorableType mt " +
            "where m.instanceId = ? " +
            "and t.designate = true " +
            "and mt.appdefType = ? " +
            "and t.category.name = ?";

        return getSession().createQuery(sql)
            .setInteger(0, iid)
            .setInteger(1, appdefType)
            .setString(2, cat).list();
    }

    public List findByRawExcludeIdentity(Integer rid) {
        String sql =
            "select distinct d from DerivedMeasurement d " +
            "join fetch d.template t " +
            "join fetch t.measurementArgs a, " +
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
}
