/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class BaselineDAO
    extends HibernateDAO<Baseline> {
    private Log _log = LogFactory.getLog(BaselineDAO.class);

    @Autowired
    public BaselineDAO(SessionFactory f) {
        super(Baseline.class, f);
    }

    public Baseline create(Measurement m, long computeTime, boolean userEntered, Double mean,
                           Double minExpectedValue, Double maxExpectedValue) {
        Baseline b = new Baseline(m, computeTime, userEntered, mean, minExpectedValue,
            maxExpectedValue);
        m.setBaseline(b);
        save(b);
        return b;
    }

    /*
     * @return List of Measurement
     */
    @SuppressWarnings("unchecked")
    public List<Measurement> findMeasurementsForBaselines(boolean enabled, long computeTime,
                                                          int maxResults) {
        String sql = new StringBuffer().append("SELECT {m.*}").append(" FROM EAM_MEASUREMENT m")
            .append(" LEFT JOIN EAM_MEASUREMENT_BL b on b.measurement_id = m.id").append(
                " JOIN EAM_MEASUREMENT_TEMPL t on m.template_id = t.id").append(
                " WHERE t.COLLECTION_TYPE=:collType").append(" AND m.ENABLED=:enabled").append(
                " AND m.COLL_INTERVAL is not null").append(
                " AND (b.id is null or b.compute_time < :computeTime) ").toString();
        int collType = MeasurementConstants.COLL_TYPE_DYNAMIC;
        return getSession().createSQLQuery(sql).addEntity("m", Measurement.class).setBoolean(
            "enabled", enabled).setInteger("collType", collType)
            .setLong("computeTime", computeTime).setMaxResults(maxResults).list();
    }

    @SuppressWarnings("unchecked")
    public List<Baseline> findByInstance(int appdefType, int appdefId) {
        String sql = "select b from Baseline b " + "where b.measurement.appdefType = ? and "
                     + "b.measurement.instanceId = ? and " + "b.measurement.interval is not null";

        return getSession().createQuery(sql).setInteger(0, appdefType).setInteger(1, appdefId)
            .list();
    }

    public Baseline findByTemplateForInstance(Integer mtId, Integer instanceId) {
        String sql = "select b from Baseline b " + "where b.measurement.template.id = ? and "
                     + "b.measurement.instanceId = ?";

        return (Baseline) getSession().createQuery(sql).setInteger(0, mtId.intValue()).setInteger(
            1, instanceId.intValue()).uniqueResult();
    }

    int deleteByIds(Collection<Integer> ids) {
        final String hql = "delete from Baseline where measurement.id in (:ids)";

        Session session = getSession();
        int count = 0;
        for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
            ArrayList<Integer> subIds = new ArrayList<Integer>();

            for (int i = 0; i < DBUtil.IN_CHUNK_SIZE && it.hasNext(); i++) {
                subIds.add(it.next());
            }

            count += session.createQuery(hql).setParameterList("ids", subIds).executeUpdate();

            if (_log.isDebugEnabled()) {
                _log.debug("deleteByMetricIds() " + subIds.size() + " of " + ids.size() +
                           " metric IDs");
            }
        }

        return count;
    }
}
