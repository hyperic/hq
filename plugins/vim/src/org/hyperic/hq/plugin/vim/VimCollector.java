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

import com.vmware.vim.ManagedObjectReference;
import com.vmware.vim.PerfCounterInfo;
import com.vmware.vim.PerfMetricId;

public abstract class VimCollector extends Collector {

    private static final Log _log =
        LogFactory.getLog(VimCollector.class.getName());

    public static final String PROP_URL = "url";
    protected Properties _props;

    protected abstract void collect(VimUtil vim)
        throws Exception;

    protected abstract ManagedObjectReference getRoot();

    protected abstract String getType();

    protected abstract String getName();

    protected void init() throws PluginException {
        super.init();
        _props = getProperties();
        setSource(VimUtil.getURL(_props));
    }

    protected String getHostname() {
        return _props.getProperty(PROP_HOSTNAME);
    }

    protected String getCounterKey(PerfCounterInfo info) {
        String group = info.getGroupInfo().getKey();
        String name = info.getNameInfo().getKey();
        String rollup = info.getRollupType().getValue();
        return group + "." + name + "." + rollup;
    }

    //http://pubs.vmware.com/vi3/sdk/ReferenceGuide/vim.PerformanceManager.html
    //interval that summarizes statistics for five minute intervals, the ID is 300
    private static final Integer PERF_INTERVAL_ID = new Integer(300);

    protected PerfMetricId[] getPerfMetricIds(VimUtil vim,
                                              ManagedObjectReference perfManager,
                                              ManagedObjectReference entity)
        throws Exception {

        PerfMetricId[] ids =
            vim.getConn().getService().queryAvailablePerfMetric(perfManager, 
                                                                entity, 
                                                                null, 
                                                                null, 
                                                                PERF_INTERVAL_ID);
        return ids;
    }    

    protected ManagedObjectReference getManagedObjectReference(VimUtil vim) 
        throws Exception {

        ManagedObjectReference obj =
            vim.getUtil().getDecendentMoRef(getRoot(), getType(), getName());
        if (obj == null) {
            throw new PluginException(getType() + "/" + getName() + ": not found");
        }
        return obj;
    }

    protected IntHashMap getCounterInfo(VimUtil vim,
                                        ManagedObjectReference perfManager)
        throws Exception {

        PerfCounterInfo[] counters =
            (PerfCounterInfo[])vim.getUtil().getDynamicProperty(perfManager,
                                                                "perfCounter");
        IntHashMap info = new IntHashMap(counters.length);
        for (int i=0; i<counters.length; i++) {
            info.put(counters[i].getKey(), counters[i]);
        }
        return info;
    }

    public void collect() {
        VimUtil vim = new VimUtil();

        try {
            vim.init(getProperties());
            setAvailability(vim.getConn().isConnected());
            collect(vim);
        } catch (Exception e) {
            setAvailability(false);
            setErrorMessage(e.getMessage(), e);
            _log.error(e.getMessage(), e);
        } finally {
            vim.dispose();
        }
    }
}
