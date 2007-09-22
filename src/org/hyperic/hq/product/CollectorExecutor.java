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

package org.hyperic.hq.product;

import java.util.Properties;

import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class CollectorExecutor extends ThreadPoolExecutor {
    private static final int THREAD_MAX = 30;
    private Properties _props;

    private static int getIntProperty(Properties props,
                                      String name,
                                      int defval) {
        String val = props.getProperty("collector." + name);
        if (val == null) {
            return defval;
        }
        else {
            return Integer.parseInt(val);
        }
    }

    public CollectorExecutor(Properties props) {
        super(getIntProperty(props, "corePoolSize", THREAD_MAX),
              getIntProperty(props, "maxPoolSize", THREAD_MAX),
              1, TimeUnit.MINUTES,
              new LinkedBlockingQueue());
        _props = props;
    }

    public void awaitTermination() throws InterruptedException {
        awaitTermination(getIntProperty(_props, "timeout", 1),
                         TimeUnit.MINUTES);
    }
}
