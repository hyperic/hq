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

public abstract class WebsphereCollector extends Collector {

    private static final Log log =
        LogFactory.getLog(WebsphereCollector.class.getName());

    protected ObjectName name;
    private ObjectName perf;
    private String domain;

    protected ObjectName getObjectName() {
        return this.name;
    }

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

        //resource specific stuff
        init(mServer);
        if (this.name != null) {
            setSource(this.name.toString());
        }
    }

    protected void init(AdminClient mServer)
        throws PluginException {

    }

    protected ObjectName getPerfObjectName(AdminClient mServer)
        throws PluginException {

        if (this.perf == null) {
            this.perf =
                newObjectNamePattern("name=PerfMBean," +
                                     "type=Perf," +
                                     getProcessAttributes());
        
            this.perf = resolve(mServer, this.perf);
        }

        return this.perf;
    }

    protected WSStats getStatsObject(AdminClient mServer, ObjectName name) {
        Object[] params = { name, Boolean.FALSE };
        String[] sig = {
            ObjectName.class.getName(),
            Boolean.class.getName()
        };

        try {
            return (WSStats)mServer.invoke(getPerfObjectName(mServer),
                                           "getStatsObject", params, sig);
        } catch (Exception e) {
            log.error("getStatsObject(" + name + "): " + e.getMessage(), e);
            return null;
        }
    }

    protected String getNodeName() {
        return getProperties().getProperty(WebsphereProductPlugin.PROP_SERVER_NODE);
    }

    protected String getServerName() {
        return getProperties().getProperty(WebsphereProductPlugin.PROP_SERVER_NAME);
    }

    protected String getModuleName() {
        return getProperties().getProperty("Module");
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

        return WebsphereUtil.resolve(mServer, name);
    }

    protected Object getAttribute(AdminClient mServer,
                                  ObjectName name,
                                  String attr) {

        try {
            WebsphereStopWatch timer = new WebsphereStopWatch();
            Object o = mServer.getAttribute(name, attr);
            if (log.isDebugEnabled()) {
                String call = "getAttribute(" + name + ", " + attr + ")";
                if (o == null) {
                    log.debug(call + "==null");
                }
                if (timer.isTooLong()) {
                    log.debug(call + " took: " +
                              timer.getElapsedSeconds() + " seconds");
                }
            }
            return o;
        } catch (Exception e) {
            log.error("getAttribute(" + name + ", " + attr +
                      "): " + e.getMessage(), e);
            return null;
        }
    }

    protected Stats getStats(AdminClient mServer, ObjectName name) {
        return (Stats)getAttribute(mServer, name, "stats");
    }

    protected double getStatCount(Statistic stat) {
        if (stat instanceof CountStatistic) {
            return ((CountStatistic)stat).getCount();
        }
        else if (stat instanceof RangeStatistic) {
            return ((RangeStatistic)stat).getCurrent();
        }
        else {
            log.error("Unsupported stat type: " +
                      stat.getName() + "/" + stat.getClass().getName());
            return Double.NaN;
        }        
    }

    protected double getStatCount(Stats stats, String metric) {
        return getStatCount(stats.getStatistic(metric));
    }

    public MetricValue getValue(Metric metric, CollectorResult result) {
        return super.getValue(metric, result);
    }

    protected boolean collectStats(ObjectName name) {
        AdminClient mServer = getMBeanServer();
        if (mServer == null) {
            return false;
        }
        return collectStats(mServer, name);
    }

    protected boolean collectStats(AdminClient mServer, ObjectName oname) {
        Stats stats = getStats(mServer, oname);
        if (stats == null) {
            setAvailability(false);
            return false;
        }
        setAvailability(true);

        String[] names = stats.getStatisticNames();
        Map values = getResult().getValues();
        for (int i=0; i<names.length; i++) {
            double val = getStatCount(stats, names[i]);

            //pmi names have lowercase 1st char
            String name =
                Character.toLowerCase(names[i].charAt(0)) +
                names[i].substring(1);
            values.put(name, new Double(val));
        }

        return true;
    }
}
