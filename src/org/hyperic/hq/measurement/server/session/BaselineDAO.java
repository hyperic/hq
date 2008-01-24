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

import java.util.Collection;
import java.util.List;

import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.dao.HibernateDAO;

public class BaselineDAO extends HibernateDAO {
    public BaselineDAO(DAOFactory f) {
        super(Baseline.class, f);
    }

    public Baseline findById(Integer id) {
        return (Baseline)super.findById(id);
    }

    public void save(Baseline entity) {
        super.save(entity);
    }

    public void remove(Baseline b) {
        b.getDerivedMeasurement().clearBaseline();
        super.remove(b);
    }

    public Baseline create(DerivedMeasurement dm, long computeTime,
                           boolean userEntered, Double mean,
                           Double minExpectedValue, Double maxExpectedValue) {
        Baseline b = new Baseline(dm, computeTime, userEntered, mean,
                                  minExpectedValue, maxExpectedValue);
        dm.setBaseline(b);
        save(b);
        return b;
    }

    public Baseline findByMeasurementId(Integer mid) {
        String sql = "from Baseline b where b.derivedMeasurement.id = ?";

        return (Baseline)getSession().createQuery(sql)
            .setInteger(0, mid.intValue())
            .setCacheable(true)
            .setCacheRegion("Baseline.findByMeasurementId")
            .uniqueResult();
    }

    public List findByInstance(int appdefType, int appdefId) {
        String sql =
            "select b from Baseline b " +
            "where b.derivedMeasurement.appdefType = ? and " +
            "b.derivedMeasurement.instanceId = ? and " +
            "b.derivedMeasurement.interval is not null";

        return getSession().createQuery(sql)
            .setInteger(0, appdefType)
            .setInteger(1, appdefId).list();
    }

    public Baseline findByTemplateForInstance(Integer mtId,
                                              Integer instanceId) {
        String sql =
            "select b from Baseline b " +
            "where b.derivedMeasurement.template.id = ? and " +
            "b.derivedMeasurement.instanceId = ?";

        return (Baseline)getSession().createQuery(sql)
            .setInteger(0, mtId.intValue())
            .setInteger(1, instanceId.intValue()).uniqueResult();
    }

    int deleteByIds(Collection ids) {
        return getSession()
            .createQuery("delete from Baseline where derivedMeasurement.id in (:ids)")
            .setParameterList("ids", ids)
            .executeUpdate();
    }
}
