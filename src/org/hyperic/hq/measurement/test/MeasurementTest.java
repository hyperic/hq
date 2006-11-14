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
