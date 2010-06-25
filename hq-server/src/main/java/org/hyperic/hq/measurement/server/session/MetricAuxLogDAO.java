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
import org.hibernate.criterion.Expression;
import org.hibernate.type.IntegerType;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.galerts.server.session.GalertAuxLog;
import org.hyperic.hq.galerts.server.session.GalertAuxLogProvider;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class MetricAuxLogDAO
    extends HibernateDAO<MetricAuxLogPojo> {
    private static Log _log = LogFactory.getLog(MetricAuxLogDAO.class);

    @Autowired
    public MetricAuxLogDAO(SessionFactory f) {
        super(MetricAuxLogPojo.class, f);
    }

    int deleteByMetricIds(Collection<Integer> ids) {
        final String hql = "delete from MetricAuxLogPojo where metric.id in (:ids)";

        Session session = getSession();
        HQDialect dialect = getHQDialect();
        int maxExprs;
        if (-1 == (maxExprs = dialect.getMaxExpressions())) {
            return session.createQuery(hql).setParameterList("ids", ids).executeUpdate();
        }

        int count = 0;
        ArrayList<Integer> subIds = new ArrayList<Integer>(maxExprs);
        for (Iterator<Integer> it = ids.iterator(); it.hasNext();) {
            subIds.clear();

            for (int i = 0; i < maxExprs && it.hasNext(); i++) {
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

    MetricAuxLogPojo find(GalertAuxLog log) {
        return (MetricAuxLogPojo) createCriteria().add(Expression.eq("auxLog", log)).uniqueResult();
    }

    Collection find(Collection mids) {
        List rtn = new ArrayList();
        HQDialect dialect = getHQDialect();
        String sql = "from MetricAuxLogPojo p where p.metric.id in (:metrics)";
        int maxExprs;
        if (-1 == (maxExprs = dialect.getMaxExpressions())) {
            return getSession().createQuery(sql).setParameterList("metrics", mids).list();
        }
        int i = 0;
        ArrayList metrics = new ArrayList(maxExprs);
        for (Iterator it = mids.iterator(); it.hasNext(); i++) {
            if (i != 0 && (i % maxExprs) == 0) {
                metrics.add(it.next());
                rtn.addAll(getSession().createQuery(sql).setParameterList("metrics", metrics)
                    .list());
                metrics.clear();
            } else {
                metrics.add(it.next());
                continue;
            }
        }
        return rtn;
    }

    void removeAll(GalertDef def) {
        String sql = "delete from MetricAuxLogPojo p where p.alertDef = :def";

        getSession().createQuery(sql).setParameter("def", def).executeUpdate();
    }

    /**
     * Resets the associated type between an aux log and other subsystems (such
     * as metrics, resource, etc.)
     */
    void resetAuxType(Collection<Integer> mids) {
        String hql = "update GalertAuxLog g set g.auxType = :type "
                     + "where exists (select p.id from MetricAuxLogPojo p "
                     + "where p.auxLog = g and " + "p.metric.id in (:metrics))";

        HQDialect dialect = getHQDialect();
        int maxExprs;
        if (-1 == (maxExprs = dialect.getMaxExpressions())) {
            getSession().createQuery(hql).setInteger("type",
                GalertAuxLogProvider.INSTANCE.getCode()).setParameterList("metrics", mids,
                new IntegerType()).executeUpdate();
            return;
        }
        int i = 0;
        ArrayList metrics = new ArrayList(maxExprs);
        for (Iterator it = mids.iterator(); it.hasNext(); i++) {
            if (i != 0 && (i % maxExprs) == 0) {
                metrics.add(it.next());
                getSession().createQuery(hql).setInteger("type",
                    GalertAuxLogProvider.INSTANCE.getCode()).setParameterList("metrics", metrics,
                    new IntegerType()).executeUpdate();
                metrics.clear();
            } else {
                metrics.add(it.next());
            }
        }
    }
}
