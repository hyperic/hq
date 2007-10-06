package org.hyperic.hq.plugin.netservices;
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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;

import org.hyperic.hq.product.PluginException;

public class InetAddressChecker extends NetServicesCollector {

    private Method isReachable;
    private Object[] timeout;

    private InetAddress getAddress() throws IOException {
        return InetAddress.getByName(getHostname());
    }

    protected void init() throws PluginException {
        super.init();
        try {
            this.isReachable =
                InetAddress.class.getMethod("isReachable",
                                            new Class[] { int.class });
        } catch (Exception e) {
            throw new PluginException("This service requires Java 1.5 or higher");
        }

        this.timeout = new Object[] { new Integer(getTimeoutMillis()) };

        try {
            setSource(getAddress().toString());
        } catch (IOException e) {
            setSource(getHostname());
        }
    }

    public void collect() {
        
        try {
            startTime();
            InetAddress address = getAddress();
            Boolean avail =
                (Boolean)this.isReachable.invoke(address, this.timeout);
            setAvailability(avail.booleanValue());
            endTime();
        } catch (Exception e) {
            setAvailability(false);
            setErrorMessage(e.getMessage());
        }
    }
}
