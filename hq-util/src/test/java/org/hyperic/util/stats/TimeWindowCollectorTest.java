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

package org.hyperic.util.stats;

import junit.framework.TestCase;
import org.hyperic.util.stats.TimeWindowCollector;

public class TimeWindowCollectorTest extends TestCase {
    public TimeWindowCollectorTest(String name) {
        super(name);
    }

    /**
     * Setup a window of size 5, make sure that old entries are removed.
     */
    public void testWindow() throws Exception {
        TimeWindowCollector c = new TimeWindowCollector(5);
        assertTrue(c.getSum() == 0.0);
        assertFalse(c.hasRolled());
        c.addPoint(1, 10);
        assertTrue(c.getSum() == 10.0);
        c.addPoint(2, 11);
        assertTrue(c.getSum() == 21.0);
        c.addPoint(3, 12);
        assertTrue(c.getSum() == 33.0);
        c.addPoint(4, 13);
        assertTrue(c.getSum() == 46.0);
        c.addPoint(5, 14);
        assertTrue(c.getSum() == 60.0);
        c.addPoint(6, 15);
        assertTrue(c.getSum() == 75.0);
        c.addPoint(7, 16);
        assertFalse(c.hasRolled());
        assertTrue(c.getSum() == 91.0);
        assertFalse(c.hasRolled());
        c.removeOldPoints();
        assertTrue(c.getSum() == 81.0);
        assertTrue(c.hasRolled());
    }
}
