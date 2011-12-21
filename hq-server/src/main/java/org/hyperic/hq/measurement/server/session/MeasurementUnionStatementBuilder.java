/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.measurement.server.session;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
    public static final String getUnionStatement(final long begin, long end, final HQDialect dialect) {
        final StringBuilder sql = new StringBuilder();
        final MeasRangeObj measRangeObj = MeasRangeObj.getInstance();
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
                                                 final Integer[] measIds, final HQDialect dialect) {
        final String measInStmt = (measIds.length == 0) ?
            "" : MeasTabManagerUtil.getMeasInStmt(measIds, true);
        return getUnionStatement(begin, end, measInStmt, dialect);
    }

    public static final String getUnionStatement(final long begin,
                                                 final long end,
                                                 Collection<Integer> measIds, final HQDialect dialect) {
        final String measInStmt = (measIds.isEmpty()) ?
            "" : MeasTabManagerUtil.getMeasInStmt(measIds, true);
        return getUnionStatement(begin, end, measInStmt, dialect);
    }
    
    private static final String getUnionStatement(final long begin,
                                                  final long end,
                                                  final String measInStmt, final HQDialect dialect) {
        final StringBuilder sql = new StringBuilder();
        final MeasRangeObj measRangeObj = MeasRangeObj.getInstance();
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
    public static String getUnionStatement(long begin, long end, int measId, final HQDialect dialect) {
        Integer[] measArray = new Integer[1];
        measArray[0] = new Integer(measId);
        return getUnionStatement(begin, end, measArray, dialect);
    }

    public static String getUnionStatement(long millisBack, final HQDialect dialect) {
        long timeNow = System.currentTimeMillis(),
             begin   = (timeNow - millisBack);
        return getUnionStatement(begin, timeNow, dialect);
    }

    public static String getUnionStatement(long millisBack, Collection<Integer> measIds, final HQDialect dialect) {
        long timeNow = System.currentTimeMillis(),
             begin   = (timeNow - millisBack);
        return getUnionStatement(begin, timeNow, measIds, dialect);
    }

    public static String getUnionStatement(long millisBack, Integer[] measIds, final HQDialect dialect) {
        long timeNow = System.currentTimeMillis(),
             begin   = (timeNow - millisBack);
        return getUnionStatement(begin, timeNow, measIds, dialect);
    }

    public static String getUnionStatement(long millisBack, int measId, final HQDialect dialect) {
        long timeNow = System.currentTimeMillis(),
             begin   = (timeNow - millisBack);
        return getUnionStatement(begin, timeNow, measId, dialect);
    }

}
