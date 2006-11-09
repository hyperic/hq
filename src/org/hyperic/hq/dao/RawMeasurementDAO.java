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
import org.hyperic.hq.measurement.MeasurementTemplate;
import org.hyperic.hq.measurement.RawMeasurement;
import org.hyperic.hq.appdef.shared.AppdefEntityID;

import java.util.List;

/**
 * CRUD methods, finders, etc. for RawMeasurement
 */
public class RawMeasurementDAO extends HibernateDAO
{
    public RawMeasurementDAO(Session session) {
        super(RawMeasurement.class, session);
    }

    public RawMeasurement findById(Integer id) {
        return (RawMeasurement)super.findById(id);
    }

    public void remove(Integer id) {
        RawMeasurement m = findById(id);
        remove(m);
    }
    
    public void remove(RawMeasurement entity) {
        super.remove(entity);
    }

    public RawMeasurement update(RawMeasurement m, String dsn) {
        m.setDsn(dsn);
        save(m);
        return m;
    }

    public RawMeasurement create(Integer instanceId, MeasurementTemplate mt,
                                 String dsn) {
        RawMeasurement rm = new RawMeasurement(instanceId, mt, dsn);
        save(rm);
        return rm;
    }

    public List findByInstance(int appdefType, int appdefId) {
        String sql =
            "from RawMeasurement m " +
            "join fetch m.template as t " +
            "join fetch t.monitorableType as mt " +
            "where mt.appdefType = ? and m.instanceId = ? " +
            "and t.measurementArgs is empty";

        return getSession().createQuery(sql)
            .setInteger(0, appdefType)
            .setInteger(1, appdefId).list();
    }

    public int deleteByInstances(AppdefEntityID[] ids) {
        StringBuffer sql = new StringBuffer()
            .append("delete RawMeasurement r where r.id in " +
                    "(select m.id from RawMeasurement m " +
                    "join m.template as t " +
                    "join t.monitorableType as mt where " +
                    "mt.appdefType in (");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append(ids[i].getType());
        }
        
        sql.append(") and m.instanceId in (");
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append(ids[i].getID());
        }
        sql.append(") )");

        return getSession().createQuery(sql.toString()).
            executeUpdate();
    }

    public RawMeasurement findByDsnForInstance(String dsn, Integer id) {
        String sql =
            "from RawMeasurement m " +
            "where m.dsn = ? and m.instanceId = ?";
        
        return (RawMeasurement)getSession().createQuery(sql)
            .setString(0, dsn)
            .setInteger(1, id.intValue()).uniqueResult();
    }

    public RawMeasurement findByTemplateForInstance(Integer tid,
                                                    Integer instanceId) {
        String sql =
            "from RawMeasurement m " +
            "join fetch m.template as t " +
            "where t.id = ? and m.instanceId = ?";

        return (RawMeasurement)getSession().createQuery(sql)
            .setInteger(0, tid.intValue())
            .setInteger(1, instanceId.intValue()).uniqueResult();
    }

    public List findByTemplate(Integer id) {
        String sql =
            "from RawMeasurement m " +
            "join fetch m.template as t " +
            "where t.id = ?";

        return getSession().createQuery(sql)
            .setInteger(0, id.intValue()).list();
    }

    public List findByDerivedMeasurement(Integer did) {
        String sql =
            "select distinct r from RawMeasurement r " +
            "join fetch r.template t " +
            "join fetch t.measurementArgs a, " +
            "DerivedMeasurement m " +
            "where r.instanceId = m.instanceId and " +
            "t.id = a.templateArg.id and " +
            "m.id = ? ";

        return getSession().createQuery(sql)
                .setInteger(0, did.intValue()).list();
    }
}
