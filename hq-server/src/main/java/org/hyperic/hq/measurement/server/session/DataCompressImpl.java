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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.shared.DataCompress;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The DataCompressImpl handles all compression and purging of measurement data
 * in the HQ system.
 */
@Service
@Transactional
public class DataCompressImpl implements DataCompress {

    private final Log log = LogFactory.getLog(DataCompressImpl.class);

    // Data tables
    private static final String TAB_DATA_1H = MeasurementConstants.TAB_DATA_1H;
    private static final String TAB_DATA_6H = MeasurementConstants.TAB_DATA_6H;
    private static final String TAB_DATA_1D = MeasurementConstants.TAB_DATA_1D;

    private DataCompressionDAO dataCompressionDAO;

    @Autowired
    public DataCompressImpl(DataCompressionDAO dataCompressionDAO) {
        this.dataCompressionDAO = dataCompressionDAO;
    }

    public void createMetricDataViews() {
        dataCompressionDAO.createMetricDataViews();
    }

    public void truncateMeasurementData(long truncateBefore) {
        log.info("Purging Raw Measurement Data older than " + TimeUtil.toString(truncateBefore));
        StopWatch watch = new StopWatch();
        dataCompressionDAO.truncateMeasurementData(truncateBefore);
        log.info("Done Purging Raw Measurement Data (" + ((watch.getElapsed()) / 1000) +
                 " seconds)");
    }

    public void compressData(long toInterval, long now, long startWindow, long endWindow) {
        if (toInterval == MeasurementConstants.HOUR) {
            dataCompressionDAO.compactData(dataCompressionDAO.getMeasurementUnionStatement(now),
                TAB_DATA_1H, startWindow, endWindow);
        } else if (toInterval == MeasurementConstants.SIX_HOUR) {
            dataCompressionDAO.compactData(TAB_DATA_1H, TAB_DATA_6H, startWindow, endWindow);
        } else if (toInterval == MeasurementConstants.DAY) {
            dataCompressionDAO.compactData(TAB_DATA_6H, TAB_DATA_1D, startWindow, endWindow);
        } else {
            throw new UnsupportedOperationException(
                "Cannot compress data for intervals other than 1 hour, 6 hours, or 1 day");
        }
    }

    public long getMinTimestamp(long dataInterval) {
        String tableName = getTableName(dataInterval);
        return dataCompressionDAO.getMinTimestamp(tableName);
    }

    public long getCompressionStartTime(long toInterval, long now) {
        // First determine the window to operate on. If no previous
        // compression information is found, the last value from the
        // table to compress from is used. (This will only occur on
        // the first compression run).
        long start = dataCompressionDAO.getMaxTimestamp(getTableName(toInterval));

        if (start == 0) {
            // No compressed data found, start from scratch.
            // Need to validate this behaviour with the oracle
            // JDBC driver. If no data exists the Postgres driver
            // returns 0 for MIN() or MAX().
            String fromTableName;
            if (toInterval == MeasurementConstants.HOUR) {
                fromTableName = dataCompressionDAO.getMeasurementUnionStatement(now);
            } else if (toInterval == MeasurementConstants.SIX_HOUR) {
                fromTableName = getTableName(MeasurementConstants.HOUR);
            } else if (toInterval == MeasurementConstants.DAY) {
                fromTableName = getTableName(MeasurementConstants.SIX_HOUR);
            } else {
                throw new UnsupportedOperationException(
                    "Cannot compress data to intervals other than 1 hour, 6 hours, or 1 day");
            }
            start = dataCompressionDAO.getMinTimestamp(fromTableName);

            // No measurement data found. (Probably a new installation)
            if (start == 0) {
                return 0;
            }
        } else {
            // Start at next interval
            start = start + toInterval;
        }
        // Rounding only necessary since if we are starting from scratch.
        return TimingVoodoo.roundDownTime(start, toInterval);
    }

    public long getMetricProblemMinTimestamp() {
        return dataCompressionDAO.getMinTimestamp(MeasurementConstants.TAB_PROB);
    }

    public void purgeMetricProblems(long startWindow, long endWindow) {
        dataCompressionDAO.purgeMeasurements(MeasurementConstants.TAB_PROB, startWindow, endWindow);
    }

    public void purgeMeasurements(long dataInterval, long startWindow, long endWindow) {
        String tableName = getTableName(dataInterval);
        log.info("Purging data older than " + TimeUtil.toString(endWindow) + " in " + tableName);
        StopWatch watch = new StopWatch();
        dataCompressionDAO.purgeMeasurements(tableName, startWindow, endWindow);
        log.info("Done (" + ((watch.getElapsed()) / 1000) + " seconds)");
    }

    private String getTableName(long dataInterval) {
        if (dataInterval == MeasurementConstants.HOUR) {
            return TAB_DATA_1H;
        } else if (dataInterval == MeasurementConstants.SIX_HOUR) {
            return TAB_DATA_6H;
        } else if (dataInterval == MeasurementConstants.DAY) {
            return TAB_DATA_1D;
        }
        throw new UnsupportedOperationException(
            "Intervals other than 1 hour, 6 hours, or 1 day are not supported");
    }

}
