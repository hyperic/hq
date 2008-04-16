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

package org.hyperic.util.test;

import junit.framework.TestCase;
import org.hyperic.util.config.ConfigResponse;

public class ConfigResponse_test extends TestCase {
    public ConfigResponse_test(String name) {
        super(name);
    }

    public void testMergeSimple() throws Exception {
        ConfigResponse one, two;

        one = new ConfigResponse();
        two = new ConfigResponse();
        one.setValue("one", "bar");
        two.setValue("two", "baz");

        assertEquals(one.size(), two.size());
        one.merge(two, false);
        assertEquals(one.size(), two.size() + 1);
        assertEquals(one.getValue("two"), "baz");
    }

    public void testMergeOverwrite() throws Exception {
        ConfigResponse one, two;

        one = new ConfigResponse();
        two = new ConfigResponse();
        one.setValue("one",   "bar");
        one.setValue("two",   "baz");
        one.setValue("three", "bong");
        two.setValue("two",   "flub");

        assertEquals(one.size(), two.size() + 2);
        one.merge(two, true);
        assertEquals(one.size(), two.size() + 2);
        assertEquals(one.getValue("two"), "flub");
    }
}

