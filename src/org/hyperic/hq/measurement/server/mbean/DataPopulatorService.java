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

        DerivedMeasurementManagerLocal dmManager =
            DerivedMeasurementManagerEJBImpl.getOne();
        DataManagerLocal dataMan = DataManagerEJBImpl.getOne();

        long detailedPurgeInterval = getDetailedPurgeInterval();
        String cats[] = MeasurementConstants.VALID_CATEGORIES;
        for (int j = 0; j < cats.length; j++) {
            List meas = dmManager.findMeasurementsByCategory(cats[j]);
            _log.info("Found " + meas.size() + " enabled metrics for " +
                      cats[j]);

            for (Iterator i = meas.iterator(); i.hasNext(); ) {
                DerivedMeasurement dm = (DerivedMeasurement)i.next();
                DataPoint last = getLastDataPoint(dm.getId());

                if (last == null)
                    continue;

                _log.info("Last metric for dm=" + dm.getId() + "=" +
                          last.getMetricValue());

                List data = genData(dm, last, detailedPurgeInterval);

                _log.info("Generated " + data.size() + " data points");

                dataMan.addData(data, true);
            }
        }
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
