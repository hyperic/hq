package org.hyperic.hq.measurement.server.session;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.hyperic.hibernate.Util;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasRange;
import org.hyperic.hq.measurement.shared.MeasRangeObj;
import org.hyperic.hq.measurement.shared.MeasTabManagerUtil;

public class MeasurementUnionStatementBuilder {

    private static final String TAB_DATA = MeasurementConstants.TAB_DATA;
    
    private static String getTimeInStmt(long begin, long end) {
        return new StringBuilder()
            .append(" WHERE timestamp between ")
            .append(begin).append(" and ").append(end)
            .toString();
    }
    /**
     * Get the UNION statement from the detailed measurement tables based on
     * the beginning of the time range.
     * @param begin The beginning of the time range.
     * @param end The end of the time range
     * @return The UNION SQL statement.
     */
    public static final String getUnionStatement(final long begin, long end) {
        final StringBuilder sql = new StringBuilder();
        final MeasRangeObj measRangeObj = MeasRangeObj.getInstance();
        final HQDialect dialect = Util.getHQDialect();
        final List ranges = (dialect.useMetricUnion()) ?
            Collections.singletonList(new MeasRange(TAB_DATA, begin, end)) :
            measRangeObj.getRanges();
        sql.append("(");
        for (final Iterator i=ranges.iterator(); i.hasNext(); ) {
            final MeasRange range = (MeasRange)i.next();
            final String table = measRangeObj.getTable(ranges, end);
            sql.append("SELECT * FROM ").
                append(table).
                append(getTimeInStmt(begin, end));
            end = range.getMinTimestamp()-1l;
            if (end >= begin) {
                sql.append(" UNION ALL ");
                continue;
            } else {
                break;
            }
        }
        sql.append(") ").append(TAB_DATA);
        return sql.toString();
    }
    
    /**
     * Get the UNION statement from the detailed measurement tables based on
     * the beginning of the time range.
     * @param begin The beginning of the time range.
     * @param end The end of the time range
     * @param measId The array of measurement ids to set the where clause against
     * @return The UNION SQL statement.
     */
    public static final String getUnionStatement(final long begin,
                                                 final long end,
                                                 final Integer[] measIds) {
        final String measInStmt = (measIds.length == 0) ?
            "" : MeasTabManagerUtil.getMeasInStmt(measIds, true);
        final StringBuilder sql = new StringBuilder();
        final MeasRangeObj measRangeObj = MeasRangeObj.getInstance();
        final HQDialect dialect = Util.getHQDialect();
        final List ranges = (dialect.useMetricUnion()) ?
            Collections.singletonList(new MeasRange(TAB_DATA, begin, end)) :
            measRangeObj.getRanges();
        int joins = 0;
        sql.append("(");
        for (final Iterator i=ranges.iterator(); i.hasNext(); ) {
            final MeasRange range = (MeasRange)i.next();
            final long rBegin = range.getMinTimestamp(),
                       rEnd   = range.getMaxTimestamp();
            boolean outOfRange = (begin > rEnd || end < rBegin);
            if (outOfRange && joins == 0) {
                continue;
            } else if (outOfRange && joins > 0) {
                break;
            }
            String table = range.getTable();
            if (joins > 0) {
                sql.append(" UNION ALL ");
            }
            joins++;
            sql.append("SELECT * FROM ")
               .append(table)
               .append( getTimeInStmt(((rBegin > begin) ? rBegin : begin),
                                      ((rEnd < end) ? rEnd : end)) )
               .append(measInStmt);
        }
        sql.append(") ").append(TAB_DATA);
        return sql.toString();
    }
    
    /**
     * Get the UNION statement from the detailed measurement tables based on
     * the beginning of the time range.
     * @param begin The beginning of the time range.
     * @param end The end of the time range
     * @param measId The measurement id to set the where clause against
     * @return The UNION SQL statement.
     */
    public static String getUnionStatement(long begin, long end, int measId) {
        Integer[] measArray = new Integer[1];
        measArray[0] = new Integer(measId);
        return getUnionStatement(begin, end, measArray);
    }

    public static String getUnionStatement(long millisBack) {
        long timeNow = System.currentTimeMillis(),
             begin   = (timeNow - millisBack);
        return getUnionStatement(begin, timeNow);
    }

    public static String getUnionStatement(long millisBack, Integer[] measIds) {
        long timeNow = System.currentTimeMillis(),
             begin   = (timeNow - millisBack);
        return getUnionStatement(begin, timeNow, measIds);
    }

    public static String getUnionStatement(long millisBack, int measId) {
        long timeNow = System.currentTimeMillis(),
             begin   = (timeNow - millisBack);
        return getUnionStatement(begin, timeNow, measId);
    }

}
