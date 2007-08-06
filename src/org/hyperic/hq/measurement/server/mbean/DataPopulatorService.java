package org.hyperic.hq.measurement.server.mbean;

import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DataManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.InitialContext;
import java.util.Properties;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MBean used for testing purposes.  When the populate() method is invoked,
 * we will look up all measurements that are currently scheduled, filling in
 * the detailed measurement data to simulate an environment that has been
 * running for as long as the 'keep detailed metric data' setting.
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=DataPopulator"
 */
public class DataPopulatorService implements DataPopulatorServiceMBean {

    private static Log _log = LogFactory.getLog(DataPopulatorService.class);

    public DataPopulatorService() {}

    /**
     * @jmx:managed-operation
     */
    public void stop() {}

    /**
     * @jmx:managed-operation
     */
    public void start() {}

    /**
     * @jmx:managed-operation
     */
    public void populate() throws Exception {
        populate(Long.MAX_VALUE);
    }

    /**
     * @jmx:managed-operation
     */
    public void populate(long max) throws Exception {

        DerivedMeasurementManagerLocal dmManager =
            DerivedMeasurementManagerEJBImpl.getOne();
        DataManagerLocal dataMan = DataManagerEJBImpl.getOne();

        long detailedPurgeInterval = getDetailedPurgeInterval();
        String cats[] = MeasurementConstants.VALID_CATEGORIES;

        long start = System.currentTimeMillis();
        long num = 0;

        _log.info("Starting data populatation at " +
                  TimeUtil.toString(start));

        List measurements = new ArrayList();
        for (int i = 0; i < cats.length; i++) {
            _log.info("Loading " + cats[i] + " measurements.");
            List meas = dmManager.findMeasurementsByCategory(cats[i]);
            measurements.addAll(meas);
        }

        _log.info("Loaded " + measurements.size() + " measurements");

        List dps = new ArrayList();
        max = (max < measurements.size()) ? max : measurements.size(); 
        for (int i = 0; i < max; i++ ) {
            DerivedMeasurement m = (DerivedMeasurement)measurements.get(i);
            _log.info("Loaded last data point for " + m.getId());
            dps.add(getLastDataPoint(m.getId()));
        }

        for (int i = 0; i < dps.size(); i++) {
            DerivedMeasurement m = (DerivedMeasurement)measurements.get(i);
            DataPoint dp = (DataPoint)dps.get(i);

            if (dp == null) {
                continue; // No data for this metric id.
            }

            List data = genData(m, dp, detailedPurgeInterval);
            _log.info("Inserting " + data.size() + " data points");
            dataMan.addData(data, true);
            num += data.size();
        }

        long duration = System.currentTimeMillis() - start;
        double rate =  num / (duration/1000);
        _log.info("Inserted " + num + " metrics in " +
                  StringUtil.formatDuration(duration) + " (" + rate +
                  " per second)");
    }

    private DataPoint getLastDataPoint(Integer mid) throws Exception {

        final String SQL =
            "SELECT timestamp, value FROM EAM_MEASUREMENT_DATA " +
            "WHERE measurement_id = ? AND timestamp = " +
            "(SELECT min(timestamp) FROM EAM_MEASUREMENT_DATA " +
            " WHERE measurement_id = ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnByContext(new InitialContext(),
                                           HQConstants.DATASOURCE);

            stmt = conn.prepareStatement(SQL);
            stmt.setInt(1, mid.intValue());
            stmt.setInt(2, mid.intValue());

            rs = stmt.executeQuery();

            if (!rs.next()) {
                _log.info("No metric data found for " + mid);
                return null;
            }

            MetricValue mv = new MetricValue();
            mv.setTimestamp(rs.getLong(1));
            mv.setValue(rs.getDouble(2));
            return new DataPoint(mid, mv);

        } catch (SQLException e) {
            _log.error("Error querying last data points", e);
            throw e;
        } finally {
            DBUtil.closeConnection(_log, conn);
        }
    }

    private long getDetailedPurgeInterval()
        throws Exception
    {
        Properties conf = ServerConfigManagerEJBImpl.getOne().getConfig();
        String purgeRawString = conf.getProperty(HQConstants.DataPurgeRaw);
        return Long.parseLong(purgeRawString);
    }

    private List genData(DerivedMeasurement dm, DataPoint dp, long range) {

        ArrayList data = new ArrayList();
        long last = dp.getMetricValue().getTimestamp();
        long end = System.currentTimeMillis() - range;
        double value = dp.getMetricValue().getValue();
        while (last > end) {
            last = last - dm.getInterval();
            MetricValue v = new MetricValue(value, last);
            DataPoint d = new DataPoint(dm.getId(), v);
            data.add(d);
        }

        return data;
    }
}
