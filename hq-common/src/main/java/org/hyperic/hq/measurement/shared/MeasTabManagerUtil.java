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

package org.hyperic.hq.measurement.shared;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.util.jdbc.DBUtil;

public class MeasTabManagerUtil {
    private static Calendar _baseCal = Calendar.getInstance();
    private static final String logCtx = MeasTabManagerUtil.class.getName();
    private static final Log _log = LogFactory.getLog(logCtx);

    public static final int NUMBER_OF_TABLES = 18,
                            NUMBER_OF_TABLES_PER_DAY = 2;
    public static final String MEAS_TABLE = "HQ_METRIC_DATA";
    public static final String MEAS_VIEW = MEAS_TABLE;
    public static final String OLD_MEAS_TABLE = MEAS_TABLE + "_COMPAT";
   
    private static final String TAB_MEAS = MeasurementConstants.TAB_MEAS;

    static {
        _baseCal.set(2006, 0, 1, 0, 0);
    }

    public static long getBaseTime() {
        return _baseCal.getTimeInMillis();
    }

    public static MeasRange[] getMetricRanges(long begin, long end) {
        List<MeasRange> ranges = MeasRangeObj.getInstance().getRanges();
        List<MeasRange> rtn = new ArrayList<MeasRange>(ranges.size());
        for (MeasRange range : ranges) {
            long rBegin = range.getMinTimestamp();
            long rEnd   = range.getMaxTimestamp();
            if (begin > rEnd || end < rBegin) {
                continue;
            }
            rtn.add(range);
        }
        return (MeasRange[]) rtn.toArray(new MeasRange[0]);
    }

    /**
     * Get the array of tables that fall in the time range
     */
    public static String[] getMetricTables(long begin, long end) {
        List<MeasRange> ranges = MeasRangeObj.getInstance().getRanges();
        String[] tables = new String[ranges.size()];
        int i = 0;
        for (MeasRange range : ranges) {
            long rBegin = range.getMinTimestamp();
            long rEnd   = range.getMaxTimestamp();
            if (begin > rEnd || end < rBegin) {
                continue;
            }
            tables[i++] = range.getTable();
        }
        // Now we want to trim the empties
        String[] retTables = new String[i];
        for (i = 0; i < retTables.length; i++) {
            retTables[i] = tables[i];
        }
        return retTables;
    }

    public static String getMeasInStmt(Collection<Integer> measIds, boolean prependAnd) {
        if (measIds.isEmpty()) {
            return "";
        }
        StringBuilder rtn = new StringBuilder();
        rtn.append(" "+((prependAnd) ? "AND" : "")+" measurement_id");
        // mysql gets a perf boost from using "=" as apposed to "in"
        if (measIds.size() == 1) {
            rtn.append(" = " + measIds.iterator().next());
            return rtn.toString();
        }
        rtn.append(" in (");
        for (Integer measId : measIds) {
            if (measId == null) {
                continue;
            }
            rtn.append(measId).append(",");
        }
        rtn.deleteCharAt(rtn.length()-1);
        rtn.append(")");
        return rtn.toString();
    }

    public static String getMeasInStmt(Integer[] measIds, boolean prependAnd) {
        if (measIds.length == 0) {
            return "";
        }
        StringBuilder rtn = new StringBuilder();
        rtn.append(" "+((prependAnd) ? "AND" : "")+" measurement_id");
        // mysql gets a perf boost from using "=" as apposed to "in"
        if (measIds.length == 1) {
            rtn.append(" = "+measIds[0]);
            return rtn.toString();
        }
        rtn.append(" in (");
        for (int i=0; i<measIds.length; i++) {
            if (measIds[i] == null) {
                continue;
            }
            rtn.append(measIds[i]).append(",");
        }
        rtn.deleteCharAt(rtn.length()-1);
        rtn.append(")");
        return rtn.toString();
    }

    private static int getDayOfPeriod(Calendar cal, long timems) {
        int rtn = 0;
        cal.clear();
        cal.setTime(new java.util.Date(timems));
        Calendar currCal = Calendar.getInstance();
        currCal.setTime(new java.util.Date(timems));
        while (cal.get(Calendar.YEAR) >= _baseCal.get(Calendar.YEAR)) {
            if (cal.get(Calendar.YEAR) == currCal.get(Calendar.YEAR)) {
                rtn += currCal.get(Calendar.DAY_OF_YEAR);
            } else {
                rtn += cal.get(Calendar.DAY_OF_YEAR);
            }
            
            cal.add(Calendar.YEAR, -1);
            cal.set(Calendar.MONTH, 11);
            cal.set(Calendar.DAY_OF_MONTH, 31);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 0);
        }
        return rtn;
    }

    public static String getMeasTabname(long timems) {
        Calendar cal = Calendar.getInstance();
        return getMeasTabname(cal, timems);
    }

    public static String getMeasTabname(Calendar cal, long timems) {
        int dayofperiod = getDayOfPeriod(cal, timems);
        cal.clear();
        cal.setTime(new java.util.Date(timems));
        _log.debug("dayofperiod -> " + dayofperiod);
        int hourofday = cal.get(Calendar.HOUR_OF_DAY);
        int numdaytables = NUMBER_OF_TABLES / NUMBER_OF_TABLES_PER_DAY;
        int daytable = dayofperiod % numdaytables;
        int dayincr = 24 / NUMBER_OF_TABLES_PER_DAY;
        int dayslice = 0;
        for (int i = dayincr; i < 24; i += dayincr) {
            if (hourofday < i) {
                break;
            }
            dayslice++;
        }
        return MEAS_TABLE + "_" + daytable + "D_" + dayslice + "S";
    }
    
    public static long getMeasTabEndTime(Calendar cal, long timems) {
        cal.clear();
        cal.setTime(new java.util.Date(timems));
        int incr = 24/NUMBER_OF_TABLES_PER_DAY;
        for (int i=incr; i<=24; i+=incr)
        {
            if (cal.get(Calendar.HOUR_OF_DAY) < i) {
                cal.set(Calendar.HOUR_OF_DAY, i-1);
                break;
            }
        }
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    public static long getMeasTabEndTime(long timems) {
        Calendar cal = Calendar.getInstance();
        return getMeasTabEndTime(cal, timems);
    }

    public static long getMeasTabStartTime(Calendar cal, long timems) {
        cal.clear();
        cal.setTime(new java.util.Date(timems));
        int incr = 24/NUMBER_OF_TABLES_PER_DAY;
        for (int i=incr; i<=24; i+=incr)
        {
            if (cal.get(Calendar.HOUR_OF_DAY) < i) {
                cal.set(Calendar.HOUR_OF_DAY, i-incr);
                break;
            }
        }
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long getMeasTabStartTime(long timems) {
        Calendar cal = Calendar.getInstance();
        return getMeasTabStartTime(cal, timems);
    }

    public static long getPrevMeasTabTime(long timems) {
        Calendar cal = Calendar.getInstance();
        return getPrevMeasTabTime(cal, timems);
    }

    public static long getPrevMeasTabTime(Calendar cal, long timems) {
        cal.clear();
        cal.setTime(new java.util.Date(timems));
        long rtn = -1;

        // need to do this because of DST
        // add() and roll() don't work right
        String currTable = getMeasTabname(timems);
        _log.debug("(getPrevMeasTabTime) before -> " + getDateStr(timems) +
                   ", " + currTable);
        String newTable;
        int incr = 24/NUMBER_OF_TABLES_PER_DAY/3;
        incr = ((incr < 1) ? 1 : incr)*-1;
        do {
            cal.add(Calendar.HOUR_OF_DAY, incr);
            rtn = cal.getTimeInMillis();
            _log.debug("subtracting 1 hour: " + getDateStr(rtn));
            newTable = getMeasTabname(rtn);
        } while (currTable.equals(newTable));

        _log.debug("(getPrevMeasTabTime) after -> " + getDateStr(rtn) + ", " +
                   newTable);
        return rtn;
    }

   

    public static List<Integer> getMeasIdsFromTemplateIds(Connection conn, Integer[] tids)
    throws SQLException {
        List<Integer> rtn = new ArrayList<Integer>();
        StringBuffer tidsConj =
            new StringBuffer(DBUtil.composeConjunctions("template_id", tids.length));
        DBUtil.replacePlaceHolders(tidsConj, tids);
        final String sql = new StringBuilder(tidsConj.length()+64)
            .append("SELECT distinct id FROM ").append(TAB_MEAS)
            .append(" WHERE ").append(tidsConj)
            .toString();
        Statement stmt = null;
        ResultSet rs   = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                rtn.add(new Integer(rs.getInt(1)));
            }
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, stmt);
        }
        return rtn;
    }

    public static List<Integer> getMeasIds(Connection conn, Integer[] tids, Integer[] iids)
    throws SQLException {
        List<Integer> rtn = new ArrayList<Integer>();
        StringBuffer iidsConj = new StringBuffer(
                DBUtil.composeConjunctions("instance_id", iids.length));
        DBUtil.replacePlaceHolders(iidsConj, iids);
        StringBuffer tidsConj = new StringBuffer(
                DBUtil.composeConjunctions("template_id", tids.length));
        DBUtil.replacePlaceHolders(tidsConj, tids);
        final String sql = new StringBuilder(iidsConj.length()+tidsConj.length()+64)
            .append("SELECT distinct id FROM ").append(TAB_MEAS)
            .append(" WHERE ").append(iidsConj).append(" AND ").append(tidsConj)
            .toString();
        Statement stmt = null;
        ResultSet rs   = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                rtn.add(new Integer(rs.getInt(1)));
            }
        } finally {
            DBUtil.closeResultSet(logCtx, rs);
            DBUtil.closeStatement(logCtx, stmt);
        }
        return rtn;
    }

    /**
     * This is meant to be a regression test for the measurement rollover
     * scheme
     */
    public static void main(String[] args) {
        long regressTime = System.currentTimeMillis() + 50 * 3600000;
        for (int i = 0; i < 2000; i++) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new java.util.Date(regressTime));
            cal.add(Calendar.DAY_OF_YEAR, (NUMBER_OF_TABLES / NUMBER_OF_TABLES_PER_DAY));
            regressTime = cal.getTimeInMillis();
            String firstTable = getMeasTabname(regressTime);
            String[] a = firstTable.split("_");
            int prevslice = Integer.parseInt(a[4].substring(0, 1));
            int prevday = Integer.parseInt(a[3].substring(0, 1));
            int prevyear = getYear(regressTime);
            long currTime = getPrevMeasTabTime(regressTime);
            String currTable = getMeasTabname(currTime);
            int curryear;
            long prevTime = 0l;
            String prevTable = null;

            System.out.println("Number: " + i);
            System.out.println("Date: " + getDateStr(regressTime));
            System.out.println("First Table: " + firstTable);

            do {
                System.out.println(currTable);
                a = currTable.split("_");
                boolean rollslice = (prevslice == 0);
                int currslice = Integer.parseInt(a[4].substring(0, 1));
                int currday = Integer.parseInt(a[3].substring(0, 1));
                curryear = getYear(currTime);

                if (prevTable != null && prevTable.equals(currTable)) {
                    System.out.println("ERROR: " + prevTable + " == " + currTable);
                    System.exit(1);
                } else
                if ((currslice == (NUMBER_OF_TABLES_PER_DAY - 1) && prevslice != 0) ||
                    currslice == prevslice) {
                    System.out.println("slice ERROR:" +
                        "\nFirst -> " + getDateStr(regressTime) + ", " + firstTable +
                        "\nPrevious -> " + getDateStr(prevTime) + ", " + prevTable +
                        "\nCurrent -> " + getDateStr(currTime) + ", " + currTable);
                    System.out.println("Current slice -> " + currslice);
                    System.out.println("Previous slice -> " + prevslice);
                    System.exit(1);
                } else
                if (rollslice && (currday != ((NUMBER_OF_TABLES / NUMBER_OF_TABLES_PER_DAY) - 1) && prevday == 0) ||
                    rollslice && (currday != (prevday - 1)) && prevday != 0) {
                    System.out.println("day ERROR:" +
                        "\nFirst -> " + getDateStr(regressTime) + ", " + firstTable +
                        "\nPrevious -> " + getDateStr(prevTime) + ", " + prevTable +
                        "\nCurrent -> " + getDateStr(currTime) + ", " + currTable);
                    System.out.println("Current Day -> " + currday);
                    System.out.println("Previous Day -> " + prevday);
                    System.out.println("Current Year -> " + curryear);
                    System.out.println("Previous Year -> " + prevyear);
                    System.exit(1);
                }
                prevslice = currslice;
                prevday = currday;
                prevTable = currTable;
                prevTime = currTime;
                currTime = getPrevMeasTabTime(currTime);
                currTable = getMeasTabname(currTime);
                prevyear = curryear;
            }
            while (!firstTable.equals(currTable));
            System.out.println("End -> " + currTable);
        }
        System.out.println(getDateStr(regressTime));
    }

    private static int getYear(long timems) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new java.util.Date(timems));
        return cal.get(Calendar.YEAR);
    }

    private static String getDateStr(long timems) {
        return DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                              DateFormat.SHORT).
            format(new java.util.Date(timems));
    }
}
