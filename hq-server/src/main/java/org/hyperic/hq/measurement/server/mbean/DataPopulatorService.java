package org.hyperic.hq.measurement.server.mbean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hyperic.hibernate.dialect.HQDialect;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.DataPoint;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementUnionStatementBuilder;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.jdbc.DBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

/**
 * MBean used for testing purposes.  When the populate() method is invoked,
 * we will look up all measurements that are currently scheduled, filling in
 * the detailed measurement data to simulate an environment that has been
 * running for as long as the 'keep detailed metric data' setting.
 *
 *
 */
@ManagedResource("hyperic.jmx:type=Service,name=DataPopulator")
@Service
public class DataPopulatorService implements DataPopulatorServiceMBean {

    private final Log log = LogFactory.getLog(DataPopulatorService.class);

    private DBUtil dbUtil;
    private MeasurementManager measurementManager;
    private DataManager dataManager;
    private ServerConfigManager serverConfigManager;
    private SessionFactory sessionFactory;
    
    
    
    @Autowired
    public DataPopulatorService(DBUtil dbUtil, MeasurementManager measurementManager, DataManager dataManager,
                                ServerConfigManager serverConfigManager, SessionFactory sessionFactory) {
        this.dbUtil = dbUtil;
        this.measurementManager = measurementManager;
        this.dataManager = dataManager;
        this.serverConfigManager = serverConfigManager;
        this.sessionFactory = sessionFactory;
    }

    /**
     * 
     */
    @ManagedOperation
    public void stop() {}

    /**
     * 
     */
    @ManagedOperation
    public void start() {}

    /**
     * 
     */
    @ManagedOperation
    public void populate() throws Exception {
        populate(Long.MAX_VALUE);
    }

    /**
     * 
     */
    @ManagedOperation
    public void populate(long max) throws Exception {

     

        long detailedPurgeInterval = getDetailedPurgeInterval();
        String cats[] = MeasurementConstants.VALID_CATEGORIES;

        long start = System.currentTimeMillis();
        long num = 0;

        log.info("Starting data populatation at " +
                  TimeUtil.toString(start));

        List<Measurement> measurements = new ArrayList<Measurement>();
        for (int i = 0; i < cats.length; i++) {
            log.info("Loading " + cats[i] + " measurements.");
            List<Measurement> meas = measurementManager.findMeasurementsByCategory(cats[i]);
            measurements.addAll(meas);
        }

        log.info("Loaded " + measurements.size() + " measurements");

        List<DataPoint> dps = new ArrayList<DataPoint>();
        max = (max < measurements.size()) ? max : measurements.size();
        for (int i = 0; i < max; i++ ) {
            Measurement m = measurements.get(i);
            log.info("Loaded last data point for " + m.getId());
            dps.add(getLastDataPoint(m.getId()));
        }

        for (int i = 0; i < dps.size(); i++) {
            Measurement m = measurements.get(i);
            DataPoint dp = dps.get(i);

            if (dp == null) {
                continue; // No data for this metric id.
            }

            List<DataPoint> data = genData(m, dp, detailedPurgeInterval);
            log.info("Inserting " + data.size() + " data points");
            dataManager.addData(data);
            num += data.size();
        }

        long duration = System.currentTimeMillis() - start;
        double rate =  num / (duration/1000);
        log.info("Inserted " + num + " metrics in " +
                  StringUtil.formatDuration(duration) + " (" + rate +
                  " per second)");
    }

    private DataPoint getLastDataPoint(Integer mid) throws Exception {

        String table = MeasurementUnionStatementBuilder.getUnionStatement(
            getDetailedPurgeInterval(), mid.intValue(), (HQDialect) ((SessionFactoryImplementor) sessionFactory)
            .getDialect());
        final String SQL =
            "SELECT timestamp, value FROM " + table +
            " WHERE measurement_id = ? AND timestamp = " +
            "(SELECT min(timestamp) FROM " + table +
            " WHERE measurement_id = ?)";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbUtil.getConnection();

            stmt = conn.prepareStatement(SQL);
            stmt.setInt(1, mid.intValue());
            stmt.setInt(2, mid.intValue());

            rs = stmt.executeQuery();

            if (!rs.next()) {
                log.info("No metric data found for " + mid);
                return null;
            }

            MetricValue mv = new MetricValue();
            mv.setTimestamp(rs.getLong(1));
            mv.setValue(rs.getDouble(2));
            return new DataPoint(mid, mv);

        } catch (SQLException e) {
            log.error("Error querying last data points", e);
            throw e;
        } finally {
            DBUtil.closeConnection(log, conn);
        }
    }

    private long getDetailedPurgeInterval()
        throws Exception
    {
        Properties conf = serverConfigManager.getConfig();
        String purgeRawString = conf.getProperty(HQConstants.DataPurgeRaw);
        return Long.parseLong(purgeRawString);
    }

    private List<DataPoint> genData(Measurement dm, DataPoint dp, long range) {

        ArrayList<DataPoint> data = new ArrayList<DataPoint>();
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
