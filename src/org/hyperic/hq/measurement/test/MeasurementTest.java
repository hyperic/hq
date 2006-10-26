package org.hyperic.hq.measurement.test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Unit tests for Raw and Derived Measurements.  These tests use seed data
 * from the example plugins found in plugins/examples.
 */
public class MeasurementTest extends TemplateTest
{
    private Log _log = LogFactory.getLog(MeasurementTest.class.getName());

    public MeasurementTest(String string) {
        super(string);
    }

    /**
     * Load the TypeInfo's and MeasurementInfo's for our tests.
     */
    public void setUp() throws Exception {
        super.setUp();
    }

    protected void cleanup() throws Exception {
        super.cleanup();
    }

    public void testSimple() throws Exception {

        runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    cleanup();
                }
            });
    }
}
