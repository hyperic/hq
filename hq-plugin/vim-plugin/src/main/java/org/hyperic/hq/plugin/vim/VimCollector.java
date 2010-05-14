/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.plugin.vim;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.collection.IntHashMap;

import com.vmware.vim25.PerfCounterInfo;
import com.vmware.vim25.PerfMetricId;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.PerformanceManager;

public abstract class VimCollector extends Collector {

    private static final Log _log =
        LogFactory.getLog(VimCollector.class.getName());

    public static final String PROP_URL = "url";
    protected Properties _props;

    protected abstract void collect(VimUtil vim)
        throws Exception;

    protected abstract String getType();

    protected abstract String getName();

    protected void init() throws PluginException {
        _props = getProperties();
        setSource(VimUtil.getURL(_props));
    }

    protected String getHostname() {
        return _props.getProperty(PROP_HOSTNAME);
    }

    protected String getCounterKey(PerfCounterInfo info) {
        String group = info.getGroupInfo().getKey();
        String name = info.getNameInfo().getKey();
        String rollup = info.getRollupType().toString();
        return group + "." + name + "." + rollup;
    }

    //http://pubs.vmware.com/vi3/sdk/ReferenceGuide/vim.PerformanceManager.html
    //interval that summarizes statistics for five minute intervals, the ID is 300
    private static final Integer PERF_INTERVAL_ID = new Integer(300);

    protected PerfMetricId[] getPerfMetricIds(PerformanceManager perfManager,
                                              ManagedEntity entity)
        throws Exception {

        PerfMetricId[] ids =
            perfManager.queryAvailablePerfMetric(entity,
                                                 null,
                                                 null,
                                                 PERF_INTERVAL_ID);
        return ids;
    }    

    protected ManagedEntity getManagedEntity(VimUtil mo) 
        throws Exception {

        return mo.find(getType(), getName());
    }

    protected IntHashMap getCounterInfo(PerformanceManager perfManager)
        throws Exception {

        PerfCounterInfo[] counters =
            perfManager.getPerfCounter();

        IntHashMap info = new IntHashMap(counters.length);
        for (int i=0; i<counters.length; i++) {
            info.put(counters[i].getKey(), counters[i]);
        }
        return info;
    }

    //collect() only allows 1 connection per-host
    //support isPoolable for concurrent connections to multiple hosts 
    public boolean isPoolable() {
        return true;
    }

    public void collect() {
        VimConnection conn;

        try {
            setAvailability(false);
            conn = VimConnection.getInstance(getProperties());
            synchronized (conn.LOCK) {
                collect(conn.vim);
            }
        } catch (Exception e) {
            setErrorMessage(e.getMessage(), e);
            _log.error(e.getMessage(), e);
        }
    }
}
