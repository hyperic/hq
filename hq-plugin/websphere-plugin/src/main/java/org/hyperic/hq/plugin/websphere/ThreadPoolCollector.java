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
package org.hyperic.hq.plugin.websphere;

import com.ibm.websphere.management.AdminClient;
import javax.management.ObjectName;
import org.hyperic.hq.product.PluginException;

public class ThreadPoolCollector extends StatsDefaultCollector {

    private static final String[][] ATTRS = {
        {"PoolSize", "poolSize"},
        {"CreateCount", "threadCreates"},
        {"DestroyCount", "threadDestroys"},
        {"ActiveCount", "activeThreads"},
        {"PercentMaxed", "PercentMaxed"},
        {"DeclaredThreadHungCount"},
        {"ClearedThreadHangCount"},
        {"ConcurrentHungThreadCount"},
        {"ActiveTime"},
        {"PercentUsed"}};

    protected void init(AdminClient mServer) throws PluginException {
        ObjectName name = newObjectNamePattern("type=ThreadPool,"
                + "name=" + getModuleName() + ","
                + getProcessAttributes());
        setObjectName(resolve(mServer, name));
    }

    protected String[][] getAttributes() {
        return ATTRS;
    }
}
