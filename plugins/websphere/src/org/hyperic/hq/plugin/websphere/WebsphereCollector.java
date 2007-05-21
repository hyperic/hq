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

import java.util.Map;
import java.util.Set;

import org.hyperic.hq.product.Collector;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.j2ee.statistics.CountStatistic;
import javax.management.j2ee.statistics.RangeStatistic;
import javax.management.j2ee.statistics.Statistic;
import javax.management.j2ee.statistics.Stats;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.exception.ConnectorException;
import com.ibm.websphere.pmi.stat.WSStats;

public class WebsphereCollector extends Collector {

    private static final Log log =
        LogFactory.getLog(WebsphereCollector.class.getName());

    private ObjectName jvm;
    private ObjectName perf;
    private String domain;

    protected ObjectName newObjectNamePattern(String attrs)
        throws PluginException {

        try {
            return new ObjectName(this.domain + ":" + attrs + ",*");
        } catch (MalformedObjectNameException e) {
            throw new PluginException(e.getMessage());
        }
    }

    protected String getServerAttributes() {
        return
            "J2EEServer=" + getServerName() + "," +
            "node=" + getNodeName();        
    }

    protected String getProcessAttributes() {
        return
            "process=" + getServerName() + "," +
            "node=" + getNodeName();        
    }

    protected void init() throws PluginException {
        AdminClient mServer = getMBeanServer();

        try {
            this.domain = mServer.getDomainName();
        } catch (ConnectorException e) {
            throw new PluginException(e.getMessage(), e);
        }

        setSource(getNodeName() + "/" + getServerName());
        //resource specific stuff
        init(mServer);
    }

    protected void init(AdminClient mServer) throws PluginException {
        this.jvm =
            newObjectNamePattern("name=JVM," +
                                 "type=JVM," +
                                 "j2eeType=JVM," +
                                  getServerAttributes());

        this.jvm = resolve(mServer, this.jvm);

        this.perf =
            newObjectNamePattern("name=PerfMBean," +
                                 "type=Perf," +
                                 getProcessAttributes());
        
        this.perf = resolve(mServer, this.perf);
    }

    protected WSStats getStatsObject(AdminClient mServer, ObjectName name)
        throws PluginException {

        Object[] params = { name, Boolean.FALSE };
        String[] sig = {
            ObjectName.class.getName(),
            Boolean.class.getName()
        };

        try {
            return (WSStats)mServer.invoke(this.perf,
                                           "getStatsObject", params, sig);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    protected String getNodeName() {
        return getProperty(WebsphereProductPlugin.PROP_SERVER_NODE);
    }

    protected String getServerName() {
        return getProperty(WebsphereProductPlugin.PROP_SERVER_NAME);
    }
    
    protected AdminClient getMBeanServer() {
        try {
            return WebsphereUtil.getMBeanServer(getProperties());
        } catch (MetricUnreachableException e) {
            setAvailability(false);
            setErrorMessage(e.getMessage());
            log.error(e.getMessage(), e);
            return null;
        }
        
    }

    protected ObjectName resolve(AdminClient mServer, ObjectName name)
        throws PluginException {

        if (!name.isPattern()) {
            return name;
        }
        try {
            Set beans = mServer.queryNames(name, null);
            if (beans.size() != 1) {
                String msg =
                    name + " query returned " +
                    beans.size() + " results";
                throw new PluginException(msg);
            }

            ObjectName fullName =
                (ObjectName)beans.iterator().next();

            if (log.isDebugEnabled()) {
                log.debug(name + " resolved to: " + fullName);
            }
            
            return fullName;
        } catch (Exception e) {
            String msg =
                "resolve(" + name + "): " + e.getMessage();
            throw new PluginException(msg, e);
        }
    }
    
    protected Stats getStats(AdminClient mServer, ObjectName name) {
        try {
            return (Stats)mServer.getAttribute(name, "stats");
        } catch (Exception e) {
            log.error("getStats(" + name + "): " + e.getMessage(), e);
            return null;
        }
    }

    protected double getStatCount(Stats stats, String metric) {
        Statistic stat = stats.getStatistic(metric);

        if (stat instanceof CountStatistic) {
            return ((CountStatistic)stat).getCount();
        }
        else if (stat instanceof RangeStatistic) {
            return ((RangeStatistic)stat).getCurrent();
        }
        else {
            return Double.NaN;
        }
    }

    public MetricValue getValue(Metric metric, CollectorResult result) {
        return super.getValue(metric, result);
    }

    public void collect() {
        AdminClient mServer = getMBeanServer();
        if (mServer == null) {
            return;
        }

        setAvailability(true);

        Map values = getResult().getValues();

        Stats jvmStats =
            (Stats)getStats(mServer, this.jvm);

        if (jvmStats != null) {
            double total = getStatCount(jvmStats, "HeapSize");
            double used  = getStatCount(jvmStats, "UsedMemory");
            values.put("totalMemory", new Double(total));
            values.put("usedMemory", new Double(total));
            values.put("freeMemory", new Double(total-used));
        }
    }
}
