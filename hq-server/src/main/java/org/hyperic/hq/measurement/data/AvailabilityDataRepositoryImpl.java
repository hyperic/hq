package org.hyperic.hq.measurement.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.hyperic.hibernate.DialectAccessor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.AvailabilityDataRLE;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.springframework.beans.factory.annotation.Autowired;

public class AvailabilityDataRepositoryImpl implements AvailabilityDataCustom {

    private static final String ALIAS_CLAUSE = " upper(t.alias) = '" +
                                               MeasurementConstants.CAT_AVAILABILITY.toUpperCase() +
                                               "' ";

    private static final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;
    private static final int BATCH_SIZE = 1000;

    private static final long MAX_TIMESTAMP = AvailabilityDataRLE.getLastTimestamp();
  
    // TOTAL_TIME and TOTAL_UPTIME are used to anchor the start and end values
    // to
    // the appropriate time range. They avoid the situation where a query
    // may result in Long.MAX_VALUE as the endtime and a startime which is <
    // the user specified value
    private static final String TOTAL_TIME = "least(rle.endtime,:endtime) "
                                             + "- greatest(rle.startime,:startime)";

    private static final String TOTAL_UPTIME = "(" + TOTAL_TIME + ") * rle.availVal";

    private DialectAccessor dialectAccessor;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public AvailabilityDataRepositoryImpl(DialectAccessor dialectAccessor) {
        this.dialectAccessor = dialectAccessor;
    }

    /**
     * @return List of Object[]. [0] = Measurement Obj [1] = min(availVal), [2]
     *         = max(availVal), [3] = avg(availVal) [4] = mid count, [5] = total
     *         uptime, [6] = = total time
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> findAggregateAvailability(List<Integer> measIds, long start, long end) {
        if (measIds.isEmpty()) {
            // Nothing to do
            return new ArrayList<Object[]>(0);
        }
        String sql = new StringBuilder().append("SELECT m, min(rle.availVal),")
            .append(" max(rle.availVal),").append(" avg(rle.availVal),")
            .append(" (:endtime  - :startime) / m.interval, ").append(" sum(")
            .append(TOTAL_UPTIME)
            .append("), ")
            .append(" sum(")
            .append(TOTAL_TIME)
            .append(") ")
            .append(" FROM Measurement m")
            .append(" JOIN m.availabilityData rle")
            .append(" WHERE m.id in (:mids)")
            .append(" AND (rle.startime > :startime")
            .append("   OR rle.endtime > :startime)")
            .append(" AND (rle.startime < :endtime")
            .append("   OR rle.endtime < :endtime)")
            // must group by all columns in query for postgres to work
            // there is an open bug on this for hibernate to
            // automatically expand group by's
            // http://opensource.atlassian.com/projects/hibernate/browse/HHH-2407
            .append(" GROUP BY m.id, m.version, m.resource,")
            .append(" m.template, m.mtime,m.enabled,").append(" m.interval, m.dsn,")
            .append(" rle.endtime").append(" ORDER BY rle.endtime").toString();

        final int size = measIds.size();
        final HQDialect dialect = dialectAccessor.getHQDialect();
        final int batchSize = dialect.getMaxExpressions() < 0 ? Integer.MAX_VALUE : dialect
            .getMaxExpressions();
        final List<Object[]> rtn = new ArrayList<Object[]>(size);
        for (int i = 0; i < size; i += batchSize) {
            final int last = Math.min(i + batchSize, size);
            final List<Integer> sublist = measIds.subList(i, last);
            rtn.addAll(entityManager.createQuery(sql)
                .setParameter("startime", start).setParameter("endtime", end)
                .setParameter("mids", sublist).getResultList());
        }
        return rtn;
    }

    public List<AvailabilityDataRLE> findLastByMeasurements(List<Integer> mids) {
        // sort so that the cache has the best opportunity use the query
        // multiple times
        mids = new ArrayList<Integer>(mids);
        Collections.sort(mids);
        List<AvailabilityDataRLE> rtn = new ArrayList<AvailabilityDataRLE>(mids.size());
        if (mids.isEmpty()) {
            return rtn;
        }
        String ql = new StringBuilder().append("select a from AvailabilityDataRLE a")
            .append(" WHERE a.endtime = :endtime").append(" AND a.measurement.id in (:ids)")
            .toString();
        TypedQuery<AvailabilityDataRLE> query = entityManager.createQuery(ql,
            AvailabilityDataRLE.class).setParameter("endtime", MAX_TIMESTAMP);

        for (int i = 0; i < mids.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, mids.size());
            query.setParameter("ids", mids.subList(i, end));
            rtn.addAll(query.getResultList());
        }
        return rtn;
    }

    public List<AvailabilityDataRLE> getDownMeasurements(List<Integer> includes) {
        StringBuilder sql = new StringBuilder().append("SELECT rle FROM AvailabilityDataRLE rle")
            .append(" JOIN rle.measurement m").append(" JOIN m.template t")
            .append(" WHERE rle.endtime = ").append(MAX_TIMESTAMP)
            .append(" AND m.resource is not null ").append(" AND rle.availVal = ")
            .append(AVAIL_DOWN).append(" AND ").append(ALIAS_CLAUSE);
        final boolean hasIncludes = (includes != null && includes.size() > 0) ? true : false;
        if (hasIncludes) {
            sql.append(" and rle.measurement.id in (:mids)");
        }
        TypedQuery<AvailabilityDataRLE> query = entityManager.createQuery(sql.toString(),
            AvailabilityDataRLE.class);
        if (!hasIncludes) {
            return query.getResultList();
        }
        List<AvailabilityDataRLE> rtn = new ArrayList<AvailabilityDataRLE>(includes.size());
        for (int i = 0; i < includes.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, includes.size());
            query.setParameter("mids", includes.subList(i, end));
            rtn.addAll(query.getResultList());
        }
        return rtn;
    }

    @SuppressWarnings("unchecked")
    public Map<Integer, TreeSet<AvailabilityDataRLE>> getHistoricalAvailMap(List<Integer> mids,
                                                                            final long after,
                                                                            final boolean descending) {
        if (mids.isEmpty()) {
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
        StringBuilder sql = new StringBuilder().append("select rle FROM AvailabilityDataRLE rle").append(
            " WHERE rle.measurement.id in (:mids)");
        if (after > 0) {
            sql.append(" AND rle.endtime >= :endtime");
        }
        TypedQuery<AvailabilityDataRLE> query = entityManager.createQuery(sql.toString(),
            AvailabilityDataRLE.class).setParameter("mids", mids);
        if (after > 0) {
            query.setParameter("endtime", after);
        }
        List<AvailabilityDataRLE> list = query.getResultList();
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
        for (Integer mid : mids) {
            if (!rtn.containsKey(mid)) {
                rtn.put(mid, new TreeSet<AvailabilityDataRLE>(comparator));
            }
        }
        return rtn;
    }

    public List<AvailabilityDataRLE> getHistoricalAvails(List<Integer> mids, long start, long end,
                                                         boolean descending) {
        final List<AvailabilityDataRLE> rtn = new ArrayList<AvailabilityDataRLE>(mids.size());
        final String sql = new StringBuilder().append("FROM AvailabilityDataRLE rle")
            .append(" WHERE rle.measurement.id in (:mids)").append(" AND rle.endtime > :startime")
            .append(" AND rle.startime < :endtime").append(" ORDER BY rle.measurement,")
            .append(" rle.startime").append(((descending) ? " DESC" : " ASC")).toString();
        for (int i = 0; i < mids.size(); i += BATCH_SIZE) {
            final int last = Math.min(i + BATCH_SIZE, mids.size());
            rtn.addAll(entityManager.createQuery(sql, AvailabilityDataRLE.class)
                .setParameter("startime", start).setParameter("endtime", end)
                .setParameter("mids", mids.subList(i, last)).getResultList());
        }
        return rtn;
    }

    public List<AvailabilityDataRLE> getHistoricalAvails(Measurement m, long start, long end,
                                                         boolean descending) {
        String sql = new StringBuilder().append("FROM AvailabilityDataRLE rle ")
            .append("WHERE rle.measurement = :m AND").append(" (rle.startime > :startime")
            .append("   OR rle.endtime > :startime)").append(" AND (rle.startime < :endtime")
            .append("   OR rle.endtime < :endtime)").append(" ORDER BY rle.measurement,")
            .append(" rle.startime").append(((descending) ? " DESC" : " ASC")).toString();
        return entityManager.createQuery(sql, AvailabilityDataRLE.class)
            .setParameter("startime", start).setParameter("endtime", end).setParameter("m", m)
            .getResultList();
    }

    public List<AvailabilityDataRLE> getHistoricalAvails(Integer resource, long start, long end) {
        String sql = new StringBuilder().append("SELECT rle")
            .append(" FROM AvailabilityDataRLE rle").append(" JOIN rle.measurement m")
            .append(" WHERE m.resource = :resource").append(" AND rle.endtime > :startime")
            .append(" AND rle.startime < :endtime").append(" ORDER BY rle.startime").toString();
        return entityManager.createQuery(sql, AvailabilityDataRLE.class)
            .setParameter("resource", resource).setParameter("startime", start)
            .setParameter("endtime", end).getResultList();
    }

}
