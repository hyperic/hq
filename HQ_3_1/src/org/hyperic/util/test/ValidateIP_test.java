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
import org.hyperic.util.HostIP;

public class ValidateIP_test extends TestCase {

    public ValidateIP_test(String name) {
        super(name);
    }

    public void testGoodIPs () throws Exception {
        String[] ips
            = { "127.0.0.1",
                "192.168.1.1",
                "10.0.0.254" };
        for ( int i=0; i<ips.length; i++ ) {
            assertTrue("IP #" + i + " was invalid.", HostIP.isValidIP(ips[i]));
        }
    }

    public void testBadIPs () throws Exception {
        String[] ips
            = { "127.0.0.0",
                "192.168.1.1.10",
                "0.168.1.1",
                "10.0.0.255",
                "10.255.2.2",
                "1.2.3.400",
                "1..2.3.4" };
        for ( int i=0; i<ips.length; i++ ) {
            assertTrue("IP #" + i + " was valid.", !HostIP.isValidIP(ips[i]));
        }
    }
}
