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

import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.type.IntegerType;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.dao.HibernateDAO;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;

@Repository
public class AvailabilityDataDAO
    extends HibernateDAO<AvailabilityDataRLE> {

    private static final String logCtx = AvailabilityDataDAO.class.getName();
    private final Log _log = LogFactory.getLog(logCtx);

    private static final long MAX_TIMESTAMP = AvailabilityDataRLE.getLastTimestamp();
    private static final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;
    private static final String ALIAS_CLAUSE = " upper(t.alias) = '" +
                                               MeasurementConstants.CAT_AVAILABILITY.toUpperCase() +
                                               "' ";
    // TOTAL_TIME and TOTAL_UPTIME are used to anchor the start and end values
    // to
    // the appropriate time range. They avoid the situation where a query
    // TOTAL_TIME and TOTAL_UPTIME are used to anchor the start and end values
    // to
    // the appropriate time range. They avoid the situation where a query
    // may result in Long.MAX_VALUE as the endtime and a startime which is <
    // the user specified value
    private static final String TOTAL_TIME = "least(rle.endtime,:endtime) "
                                             + "- greatest(rle.availabilityDataId.startime,:startime)";
    private static final String TOTAL_UPTIME = "(" + TOTAL_TIME + ") * rle.availVal";

    private final DBUtil dbUtil;

    @Autowired
    public AvailabilityDataDAO(SessionFactory f, DBUtil dbUtil) {
        super(AvailabilityDataRLE.class, f);
        this.dbUtil = dbUtil;
    }

    @SuppressWarnings("unchecked")
    List<AvailabilityDataRLE> findLastAvail(List<Integer> mids, long after) {
        // sort so that the cache has the best opportunity use the query
        // multiple times
        mids = new ArrayList<Integer>(mids);
        Collections.sort(mids);
        List<AvailabilityDataRLE> rtn = new ArrayList<AvailabilityDataRLE>(mids.size());
        if (mids.isEmpty()) {
            return rtn;
            // check if sa
        }
        String hql = new StringBuilder().append("from AvailabilityDataRLE").append(
            " WHERE endtime > :endtime").append(" AND availabilityDataId.measurement in (:ids)")
            .append(" ORDER BY endtime desc").toString();
        Query query = getSession().createQuery(hql).setLong("endtime", after);
        for (int i = 0; i < mids.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, mids.size());
            query.setParameterList("ids", mids.subList(i, end), new IntegerType());
            rtn.addAll(query.list());
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    List<AvailabilityDataRLE> findLastAvail(List<Integer> mids) {
        // sort so that the cache has the best opportunity use the query
        // multiple times
        mids = new ArrayList<Integer>(mids);
        Collections.sort(mids);
        List<AvailabilityDataRLE> rtn = new ArrayList<AvailabilityDataRLE>(mids.size());
        if (mids.isEmpty()) {
            return rtn;
        }
        String hql = new StringBuilder().append("from AvailabilityDataRLE").append(
            " WHERE endtime = :endtime").append(" AND availabilityDataId.measurement in (:ids)")
            .toString();
        // need to do this because of hibernate bug
        // http://opensource.atlassian.com/projects/hibernate/browse/HHH-1985
        Query query = getSession().createQuery(hql).setLong("endtime", MAX_TIMESTAMP);
        for (int i = 0; i < mids.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, mids.size());
            query.setParameterList("ids", mids.subList(i, end), new IntegerType());
            rtn.addAll(query.list());
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    AvailabilityDataRLE findAvail(DataPoint state) {
        String sql = new StringBuilder().append("FROM AvailabilityDataRLE").append(
            " WHERE availabilityDataId.measurement = :meas").append(
            " AND availabilityDataId.startime = :startime").toString();
        List<AvailabilityDataRLE> list = getSession().createQuery(sql).setLong("startime",
            state.getTimestamp()).setInteger("meas", state.getMeasurementId().intValue()).list();
        if (list.isEmpty()) {
            return null;
        }
        return (AvailabilityDataRLE) list.get(0);
    }

    @SuppressWarnings("unchecked")
    List<AvailabilityDataRLE> findAllAvailsAfter(DataPoint state) {
        String sql = new StringBuilder().append("FROM AvailabilityDataRLE").append(
            " WHERE availabilityDataId.measurement = :meas").append(
            " AND availabilityDataId.startime > :startime").append(" ORDER BY startime asc")
            .toString();
        return getSession().createQuery(sql).setLong("startime", state.getTimestamp()).setInteger(
            "meas", state.getMeasurementId().intValue()).list();
    }

    @SuppressWarnings("unchecked")
    AvailabilityDataRLE findAvailAfter(DataPoint state) {
        String sql = new StringBuilder().append("FROM AvailabilityDataRLE").append(
            " WHERE availabilityDataId.measurement = :meas").append(
            " AND availabilityDataId.startime > :startime").append(" ORDER BY startime asc")
            .toString();
        List<AvailabilityDataRLE> list = getSession().createQuery(sql).setLong("startime",
            state.getTimestamp()).setInteger("meas", state.getMeasurementId().intValue()).setMaxResults(
            1).list();
        if (list.isEmpty()) {
            return null;
        }
        return (AvailabilityDataRLE) list.get(0);
    }

    void updateVal(AvailabilityDataRLE avail, double newVal) {
        avail.setAvailVal(newVal);
        save(avail);
    }

    @SuppressWarnings("unchecked")
    AvailabilityDataRLE findAvailBefore(DataPoint state) {
        String sql = new StringBuilder().append("FROM AvailabilityDataRLE").append(
            " WHERE availabilityDataId.measurement = :meas").append(
            " AND availabilityDataId.startime < :startime").append(" ORDER BY startime desc")
            .toString();
        List<AvailabilityDataRLE> list = getSession().createQuery(sql).setLong("startime",
            state.getTimestamp()).setInteger("meas", state.getMeasurementId().intValue()).setMaxResults(
            1).list();
        if (list.isEmpty()) {
            return null;
        }
        return (AvailabilityDataRLE) list.get(0);
    }

    /**
     * @return List of AvailabilityDataRLE objs
     */
    @SuppressWarnings("unchecked")
    List<AvailabilityDataRLE> getHistoricalAvails(Measurement m, long start, long end,
                                                  boolean descending) {
        String sql = new StringBuilder().append("FROM AvailabilityDataRLE rle ").append(
            "WHERE rle.availabilityDataId.measurement = :m AND").append(
            " (rle.availabilityDataId.startime > :startime").append(
            "   OR rle.endtime > :startime)").append(
            " AND (rle.availabilityDataId.startime < :endtime").append(
            "   OR rle.endtime < :endtime)")
            .append(" ORDER BY rle.availabilityDataId.measurement,").append(
                " rle.availabilityDataId.startime").append(((descending) ? " DESC" : " ASC"))
            .toString();
        return getSession().createQuery(sql).setLong("startime", start).setLong("endtime", end)
            .setParameter("m", m).list();
    }

    /**
     * @return List of AvailabilityDataRLE objs
     */
    @SuppressWarnings("unchecked")
    List<AvailabilityDataRLE> getHistoricalAvails(Integer[] mids, long start, long end,
                                                  boolean descending) {
        final List<AvailabilityDataRLE> rtn = new ArrayList<AvailabilityDataRLE>(mids.length);
        final List<Integer> list = Arrays.asList(mids);
        final String sql = new StringBuilder().append("FROM AvailabilityDataRLE rle").append(
            " WHERE rle.availabilityDataId.measurement in (:mids)").append(
            " AND rle.endtime > :startime").append(
            " AND rle.availabilityDataId.startime < :endtime").append(
            " ORDER BY rle.availabilityDataId.measurement,").append(
            " rle.availabilityDataId.startime").append(((descending) ? " DESC" : " ASC"))
            .toString();
        for (int i=0; i<list.size(); i+=BATCH_SIZE) {
            final int last = Math.min(i+BATCH_SIZE, list.size());
            rtn.addAll(getSession().
                createQuery(sql)
                .setLong("startime", start)
                .setLong("endtime", end)
                .setParameterList("mids", list.subList(i, last), new IntegerType())
                .list());
        }
        return rtn;
    }

    /**
     * @return {@link Map} of {@link Integer} to ({@link TreeSet} of
     *         {@link AvailabilityDataRLE}).
     *         <p>
     *         The {@link Map} key of {@link Integer} == {@link Measurement}
     *         .getId().
     *         <p>
     *         The {@link TreeSet}'s comparator sorts by
     *         {@link AvailabilityDataRLE}.getStartime().
     */
    @SuppressWarnings("unchecked")
    Map<Integer, TreeSet<AvailabilityDataRLE>> getHistoricalAvailMap(Integer[] mids,
                                                                     final long after,
                                                                     final boolean descending) {
        if (mids.length <= 0) {
            return Collections.EMPTY_MAP;
        }
        final Comparator<AvailabilityDataRLE> comparator = new Comparator<AvailabilityDataRLE>() {
            public int compare(AvailabilityDataRLE lhs, AvailabilityDataRLE rhs) {
                Long lhsStart = new Long(lhs.getStartime());
                Long rhsStart = new Long(rhs.getStartime());
                if (descending) {
                    return rhsStart.compareTo(lhsStart);
                }
                return lhsStart.compareTo(rhsStart);
            }
        };
        StringBuilder sql = new StringBuilder().append("FROM AvailabilityDataRLE rle").append(
            " WHERE rle.availabilityDataId.measurement in (:mids)");
        if (after > 0) {
            sql.append(" AND rle.endtime >= :endtime");
        }
        Query query = getSession().createQuery(sql.toString()).setParameterList("mids", mids,
            new IntegerType());
        if (after > 0) {
            query.setLong("endtime", after);
        }
        List<AvailabilityDataRLE> list = query.list();
        Map<Integer, TreeSet<AvailabilityDataRLE>> rtn = new HashMap<Integer, TreeSet<AvailabilityDataRLE>>(
            list.size());
        TreeSet<AvailabilityDataRLE> tmp;
        for (AvailabilityDataRLE rle : list) {
            Integer mId = rle.getMeasurement().getId();
            if (null == (tmp = rtn.get(mId))) {
                tmp = new TreeSet<AvailabilityDataRLE>(comparator);
                rtn.put(rle.getMeasurement().getId(), tmp);
            }
            tmp.add(rle);
        }
        for (int i = 0; i < mids.length; i++) {
            if (!rtn.containsKey(mids[i])) {
                rtn.put(mids[i], new TreeSet<AvailabilityDataRLE>(comparator));
            }
        }
        return rtn;
    }

    /**
     * @return List of AvailabilityDataRLE objs
     */
    @SuppressWarnings("unchecked")
    List<AvailabilityDataRLE> getHistoricalAvails(Resource res, long start, long end) {
        String sql = new StringBuilder().append("SELECT rle").append(
            " FROM AvailabilityDataRLE rle").append(" JOIN rle.availabilityDataId.measurement m")
            .append(" WHERE m.resource = :resource").append(" AND rle.endtime > :startime").append(
                " AND rle.availabilityDataId.startime < :endtime").append(
                " ORDER BY rle.availabilityDataId.startime").toString();
        return getSession().createQuery(sql).setParameter("resource", res).setLong("startime",
            start).setLong("endtime", end).list();
    }

 
    
    /**
     * @return List of Object[]. [0] = Measurement Obj [1] = min(availVal), [2]
     *         = max(availVal), [3] = avg(availVal) [4] = mid count, [5] = total
     *         uptime, [6] = = total time
     */
    @SuppressWarnings("unchecked")
    List<Object[]> findAggregateAvailability(Integer[] mids, long start, long end) {
        if (mids.length == 0) {
            // Nothing to do
            return new ArrayList<Object[]>(0);
        }
        String sql = new StringBuilder().append("SELECT m, min(rle.availVal),").append(
            " max(rle.availVal),").append(" avg(rle.availVal),").append(
            " (:endtime - :startime) / m.interval, ").append(" sum(").append(TOTAL_UPTIME).append(
            "), ").append(" sum(").append(TOTAL_TIME).append(") ").append(" FROM Measurement m")
            .append(" JOIN m.availabilityData rle").append(" WHERE m in (:mids)").append(
                " AND (rle.availabilityDataId.startime > :startime").append(
                "   OR rle.endtime > :startime)").append(
                " AND (rle.availabilityDataId.startime < :endtime").append(
                "   OR rle.endtime < :endtime)")
            // must group by all columns in query for postgres to work
            // there is an open bug on this for hibernate to
            // automatically expand group by's
            // http://opensource.atlassian.com/projects/hibernate/browse/HHH-2407
            .append(" GROUP BY m.id, m._version_, m.instanceId,").append(
                " m.template, m.mtime,m.enabled,").append(" m.interval, m.formula,m.resource,").append(
                " rle.endtime").append(" ORDER BY rle.endtime").toString();
        final List<Integer> measIds = Arrays.asList(mids);
        final int size = measIds.size();
        final HQDialect dialect = getHQDialect();
        final int batchSize = dialect.getMaxExpressions() < 0 ? Integer.MAX_VALUE : dialect.getMaxExpressions();
        final List<Object[]> rtn = new ArrayList<Object[]>(size);
        for (int i=0; i<size; i+=batchSize) {
            final int last = Math.min(i+batchSize, size);
            final List sublist = measIds.subList(i, last);
            rtn.addAll(getSession()
                        .createQuery(sql)
                         .setLong("startime", start)
                       .setLong("endtime", end)
                         .setParameterList("mids", sublist, new IntegerType())
                       .list());
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    Map<Integer,Double> findAggregateAvailabilityUp(final List<Integer> mids, final long start, final long end) throws SQLException {
        if (mids==null || mids.size() == 0) {
            return null;
        }
        StringBuilder midsSublistStrBuilder = new StringBuilder();
        Iterator<Integer> midsItr = mids.iterator();
        while (midsItr.hasNext()) {
            midsSublistStrBuilder.append(',').append(String.valueOf(midsItr.next().intValue()));
        }

        String relevantMidsCondStr = "rle.MEASUREMENT_ID in (" + midsSublistStrBuilder.substring(1) + ")";
        String sqlBaseAvailInWin = new StringBuilder()
        .append("SELECT rle.MEASUREMENT_ID, SUM(rle.endtime - rle.startime)")
        .append(" FROM HQ_AVAIL_DATA_RLE rle")
        .append(" WHERE ").append(relevantMidsCondStr)
        .append(" AND rle.startime >= ").append(start)
        .append(" AND rle.endtime <= ").append(end).toString();
        String sqlAllAvailInWin = new StringBuilder()
        .append(sqlBaseAvailInWin)
        .append(" GROUP BY rle.MEASUREMENT_ID")
        .toString();
        String sqlAllAvailAtWinEdges = new StringBuilder()
        .append("SELECT rle.MEASUREMENT_ID, rle.startime, rle.endtime")
        .append(" FROM HQ_AVAIL_DATA_RLE rle")
        .append(" WHERE ").append(relevantMidsCondStr)
        .append(" AND ((rle.startime < ").append(start).append(" AND rle.endtime > ").append(start).append(")")
        .append(" OR (rle.startime < ").append(end).append(" AND rle.endtime > ").append(end).append("))")
        .toString();
        String sqlAvailUpInWin = new StringBuilder()
        .append(sqlBaseAvailInWin)
        .append(" AND rle.availVal = " + MeasurementConstants.AVAIL_UP)
        .append(" GROUP BY rle.MEASUREMENT_ID")
        .toString();
        String sqlAvailUpAtWinEdges = new StringBuilder()
        .append(sqlAllAvailAtWinEdges)
        .append(" AND rle.availVal = " + MeasurementConstants.AVAIL_UP)
        .toString();
        
        final HQDialect dialect = getHQDialect();
        final int batchSize = dialect.getMaxExpressions() < 0 ? Integer.MAX_VALUE : dialect.getMaxExpressions();
        Connection conn = null;
        try {
            conn = dbUtil.getConnection();
            IAvailExtractionStrategy midWinStrtg = new MidWinAvailExtractionStrategy();
            IAvailExtractionStrategy winEdgeStrtg = new WinEdgeAvailExtractionStrategy(start, end);
            Map<Integer,Long> msmtToAllAvailSumTimeInWin = executeAvailQuery(conn,sqlAllAvailInWin,batchSize,midWinStrtg);
            Map<Integer,Long> msmtToAllAvailInWinEdge = executeAvailQuery(conn,sqlAllAvailAtWinEdges,batchSize,winEdgeStrtg);
            Map<Integer,Long> msmtToAllAvailSumTime = merge(msmtToAllAvailSumTimeInWin,msmtToAllAvailInWinEdge);
            
            Map<Integer,Long> msmtToAvailUpSumTimeInWin = executeAvailQuery(conn,sqlAvailUpInWin,batchSize,midWinStrtg);
            Map<Integer,Long> msmtToAvailUpAvailInWinEdge = executeAvailQuery(conn,sqlAvailUpAtWinEdges,batchSize,winEdgeStrtg);
            Map<Integer,Long> msmtToAvailUpSumTime = merge(msmtToAvailUpSumTimeInWin,msmtToAvailUpAvailInWinEdge);
           
            Map<Integer,Double> msmtToAvailAvg = calcAvg(msmtToAllAvailSumTime,msmtToAvailUpSumTime);
            return msmtToAvailAvg;
        } finally {
            DBUtil.closeConnection(logCtx,conn);
        }
    }
    
    protected static interface IAvailExtractionStrategy {
        public long extract(ResultSet rs) throws SQLException;
    }
    protected static class MidWinAvailExtractionStrategy implements IAvailExtractionStrategy {
        public long extract(ResultSet rs) throws SQLException {
            return rs.getLong(2);
        }
    }
    protected static class WinEdgeAvailExtractionStrategy implements IAvailExtractionStrategy {
        protected final long timeFrameStart;
        protected final long timeFrameEnd;
        public WinEdgeAvailExtractionStrategy(long timeFrameStart, long timeFrameEnd) {
            this.timeFrameStart= timeFrameStart;
            this.timeFrameEnd = timeFrameEnd;
        }
        public long extract(ResultSet rs) throws SQLException {
            long availSectionStart = rs.getLong(2);
            long availSectionEnd = rs.getLong(3);
            return Math.min(availSectionEnd, this.timeFrameEnd) - Math.max(availSectionStart, this.timeFrameStart);
        }
    }
    protected Map<Integer,Long> executeAvailQuery(Connection conn, final String sql, final int batchSize, IAvailExtractionStrategy extractStrtg) throws SQLException {
        Statement stmt = null;
        ResultSet rs = null;
        Map<Integer,Long> rtn = new HashMap<Integer, Long>();
        try {
            stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);
            rs.setFetchSize(batchSize);
            Long accumulatedTime;
            int availId;
            while (rs.next()) {
                availId = rs.getInt(1);
                accumulatedTime = rtn.get(Integer.valueOf(availId));
                rtn.put(availId, Long.valueOf(extractStrtg.extract(rs)+(accumulatedTime!=null?accumulatedTime.longValue():0)));
            }            
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, stmt);
        }
        return rtn;
    }
    protected Map<Integer,Long> merge(Map<Integer,Long> map1, Map<Integer,Long> map2) {
        Map<Integer,Long> rtn = new HashMap<Integer,Long>();
        Set<Integer> globalKeys = new HashSet<Integer>();
        globalKeys.addAll(map1.keySet());
        globalKeys.addAll(map2.keySet());
        for (Integer key : globalKeys) {
            Long map1Val = map1.get(key); 
            Long map2Val = map2.get(key); 
            rtn.put(key,(map1Val!=null?map1Val:0)+(map2Val!=null?map2Val:0));
        }
        return rtn;
    }
    protected Map<Integer, Double> calcAvg(Map<Integer,Long> allAvail, Map<Integer,Long> availUp) {
        Map<Integer,Double> rtn = new HashMap<Integer,Double>();
        Long availUpTime = null;
        for (Integer availId : allAvail.keySet()) {
            availUpTime = availUp.get(availId);
            rtn.put(availId, availUpTime!=null?((double)availUpTime/allAvail.get(availId)):0);
        }
        return rtn;
    }

    
    /**
     * @return List of Object[]. [0] = measurement template id, [1] =
     *         min(availVal), [2] = max(availVal), [3] = avg(availVal) [4] = mid
     *         count, [5] = total uptime, [6] = = total time
     */
    @SuppressWarnings("unchecked")
    List<Object[]> findAggregateAvailability(Integer[] tids, Integer[] iids, long start, long end) {
        if (tids.length == 0) {
            // Nothing to do
            return new ArrayList<Object[]>(0);
        }
        String sql = new StringBuilder().append("SELECT m.template.id, min(rle.availVal),").append(
            " max(rle.availVal),").append(" avg(rle.availVal),").append(" count(distinct m.id), ")
            .append(" sum(").append(TOTAL_UPTIME).append("), ").append(" sum(").append(TOTAL_TIME)
            .append(") ").append(" FROM Measurement m").append(" JOIN m.availabilityData rle")
            .append(" WHERE m.template in (:tids)").append(" AND m.instanceId in (:iids)").append(
                " AND (rle.availabilityDataId.startime > :startime").append(
                "   OR rle.endtime > :startime)").append(
                " AND (rle.availabilityDataId.startime < :endtime").append(
                "   OR rle.endtime < :endtime)").append(" GROUP BY m.template.id, rle.endtime")
            .append(" ORDER BY rle.endtime").toString();
        return getSession().createQuery(sql).setLong("startime", start).setLong("endtime", end)
            .setParameterList("tids", tids, new IntegerType()).setParameterList("iids", iids,
                new IntegerType()).list();
    }

    AvailabilityDataRLE create(Measurement meas, long startime, long endtime, double availVal) {
        AvailabilityDataRLE availObj = new AvailabilityDataRLE(meas, startime, endtime, availVal);
        getSession().save(availObj);
        return availObj;
    }

    /**
     * @return List of down Measurements
     */
    @SuppressWarnings("unchecked")
    List<AvailabilityDataRLE> getDownMeasurements(List<Integer> includes) {
        StringBuilder sql = new StringBuilder().append("SELECT rle FROM AvailabilityDataRLE rle")
            .append(" JOIN rle.availabilityDataId.measurement m").append(" JOIN m.template t")
            .append(" WHERE rle.endtime = ").append(MAX_TIMESTAMP).append(
                " AND m.resource is not null ").append(" AND rle.availVal = ").append(AVAIL_DOWN)
            .append(" AND ").append(ALIAS_CLAUSE);
        final boolean hasIncludes = (includes != null && includes.size() > 0) ? true : false;
        if (hasIncludes) {
            sql.append(" AND rle.availabilityDataId.measurement in (:mids)");
        }
        Query query = getSession().createQuery(sql.toString());
        if (!hasIncludes) {
            return query.list();
        }
        List<AvailabilityDataRLE> rtn = new ArrayList<AvailabilityDataRLE>(includes.size());
        for (int i = 0; i < includes.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, includes.size());
            query.setParameterList("mids", includes.subList(i, end), new IntegerType());
            rtn.addAll(query.list());
        }
        return rtn;
    }

    AvailabilityDataRLE create(Measurement meas, long startime, double availVal) {
        AvailabilityDataRLE availObj = new AvailabilityDataRLE(meas, startime, availVal);
        if (_log.isDebugEnabled()) {
            _log.debug("creating Avail: " + availObj);
        }
        save(availObj);
        return availObj;
    }
}
