/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.measurement;

import static org.junit.Assert.assertEquals;

import org.hyperic.hq.measurement.server.session.MetricProblemDAO;
import org.hyperic.hq.measurement.shared.MeasRangeObj;
import org.hyperic.hq.test.BaseInfrastructureTest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Integration test of{@link DataPurgeJob}. For now we are just testing the
 * parts that moved over from DataCompressImpl.
 * @author jhickey
 * 
 */
@DirtiesContext
public class DataPurgeJobTest
    extends BaseInfrastructureTest {

    @Autowired
    private DataPurgeJob dataPurgeJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MetricProblemDAO metricProblemDAO;

    /**
     * Sanity test that no exceptions are thrown on truncating measurement data
     * (not sure how to verify that tables have been truncated)
     */
    @Test
    public void testTruncateMeasurementData() {
        dataPurgeJob.truncateMeasurementData(System.currentTimeMillis());
    }

    @Test
    public void testPurgeMeasurementsOneHourTable() {
        long measurementTimestamps = System.currentTimeMillis();
        jdbcTemplate.update("INSERT INTO " + MeasurementConstants.TAB_DATA_1H +
                            "(measurement_id, timestamp, value, minvalue, maxvalue) values(12345," +
                            measurementTimestamps + ",1,1,1)");
        // Purge all measurements older than 100 ms in the future (so..
        // everything)
        dataPurgeJob.purgeMeasurements(MeasurementConstants.HOUR, measurementTimestamps + 100l);
        assertEquals(0, jdbcTemplate.queryForInt("SELECT COUNT(*) from " +
                                                 MeasurementConstants.TAB_DATA_1H));
    }

    @Test
    public void testPurgeMeasurementsSixHourTable() {
        long measurementTimestamps = System.currentTimeMillis();
        long purgeOlderThan = measurementTimestamps + 100l;

        jdbcTemplate.update("INSERT INTO " + MeasurementConstants.TAB_DATA_6H +
                            "(measurement_id, timestamp, value, minvalue, maxvalue) values(12345," +
                            measurementTimestamps + ",1,1,1)");

        // This is more recent than purge cutoff - should remain
        jdbcTemplate.update("INSERT INTO " + MeasurementConstants.TAB_DATA_6H +
                            "(measurement_id, timestamp, value, minvalue, maxvalue) values(12345," +
                            measurementTimestamps + 200l + ",1,1,1)");

        dataPurgeJob.purgeMeasurements(MeasurementConstants.SIX_HOUR, purgeOlderThan);
        assertEquals(1, jdbcTemplate.queryForInt("SELECT COUNT(*) from " +
                                                 MeasurementConstants.TAB_DATA_6H));
    }

    @Test
    public void testPurgeMeasurementsOneDayTable() {
        long measurementTimestamps = System.currentTimeMillis();
        jdbcTemplate.update("INSERT INTO " + MeasurementConstants.TAB_DATA_1D +
                            "(measurement_id, timestamp, value, minvalue, maxvalue) values(12345," +
                            measurementTimestamps + ",1,1,1)");
        // Purge all measurements older than 100 ms in the future (so..
        // everything)
        dataPurgeJob.purgeMeasurements(MeasurementConstants.DAY, measurementTimestamps + 100l);
        assertEquals(0, jdbcTemplate.queryForInt("SELECT COUNT(*) from " +
                                                 MeasurementConstants.TAB_DATA_1D));
    }

    @Test
    public void testPurgeMetricProblems() {
        long timestamp = System.currentTimeMillis();
        metricProblemDAO.create(12345, timestamp, MeasurementConstants.PROBLEM_TYPE_ALERT, 5678);
        flushSession();
        metricProblemDAO.create(12345, timestamp + 2001, MeasurementConstants.PROBLEM_TYPE_ALERT,
            5678);
        flushSession();
        dataPurgeJob.purgeMetricProblems(timestamp + 100l);
        clearSession();
        assertEquals(1, metricProblemDAO.findAll().size());
    }

    @Test
    public void testCompressDataToOneHour() {
        long now = System.currentTimeMillis();
        long twoHoursAgo = now - MeasurementConstants.HOUR * 2;
        long oneHourAgo = now - MeasurementConstants.HOUR;

        String metricTable = MeasRangeObj.getInstance().getTable(now);

        jdbcTemplate.update("INSERT INTO " + metricTable +
                            "(measurement_id, timestamp, value) values(12345," + twoHoursAgo +
                            ",1)");
        jdbcTemplate
            .update("INSERT INTO " + metricTable +
                    "(measurement_id, timestamp, value) values(12345," + oneHourAgo + ",1)");
        // compress starts at oldest timestamp in metricTable (within an hour
        // from now) and compresses each hour until now
        dataPurgeJob.compressData(MeasurementConstants.HOUR, now);
        // One entry should be moved to the 1 hour table
        assertEquals(1, jdbcTemplate.queryForInt("SELECT COUNT(*) from " +
                                                 MeasurementConstants.TAB_DATA_1H));
    }

    @Test
    @Ignore
    public void testCompressDataToSixHours() {
        long now = System.currentTimeMillis();
        long fiveHoursAgo = now - MeasurementConstants.HOUR * 5;
        long thirteenHoursAgo = now - MeasurementConstants.HOUR * 13;
    
        jdbcTemplate.update("INSERT INTO " + MeasurementConstants.TAB_DATA_1H +
                            "(measurement_id, timestamp, value, minvalue, maxvalue) values(12345," +
                           fiveHoursAgo + ",1,1,1)");
        jdbcTemplate.update("INSERT INTO " + MeasurementConstants.TAB_DATA_6H +
                            "(measurement_id, timestamp, value, minvalue, maxvalue) values(12345," +
                            thirteenHoursAgo + ",1,1,1)");
        // compression starts at 13 hours ago + 6 hours (since last entry in 6H
        // table is 13 hours old)
        dataPurgeJob.compressData(MeasurementConstants.SIX_HOUR, now);
        assertEquals(1, jdbcTemplate.queryForInt("SELECT COUNT(*) from " +
                                                 MeasurementConstants.TAB_DATA_6H));
    }
    
    @Test
    public void testCompressDataNoData() {
        assertEquals(0,dataPurgeJob.compressData(MeasurementConstants.SIX_HOUR, System.currentTimeMillis()));
    }

    @Test
    public void testCompressDataToOneDay() {
        long now = System.currentTimeMillis();
        long thirteenHoursAgo = now - MeasurementConstants.HOUR * 26;
        jdbcTemplate.update("INSERT INTO " + MeasurementConstants.TAB_DATA_6H +
                            "(measurement_id, timestamp, value, minvalue, maxvalue) values(12345," +
                            thirteenHoursAgo + ",1,1,1)");

        // compression starts at oldest timestamp in 6 hour table
        dataPurgeJob.compressData(MeasurementConstants.DAY, now);
        assertEquals(1, jdbcTemplate.queryForInt("SELECT COUNT(*) from " +
                                                 MeasurementConstants.TAB_DATA_1D));
    }
}
