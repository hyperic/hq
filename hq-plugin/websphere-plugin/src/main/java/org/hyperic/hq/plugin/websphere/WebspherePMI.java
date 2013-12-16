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
import com.ibm.websphere.pmi.PmiConstants;
import com.ibm.websphere.pmi.PmiException;
import com.ibm.websphere.pmi.client.CpdCollection;
import com.ibm.websphere.pmi.client.CpdData;
import com.ibm.websphere.pmi.client.CpdLoad;
import com.ibm.websphere.pmi.client.CpdStat;
import com.ibm.websphere.pmi.client.CpdValue;
import com.ibm.websphere.pmi.client.PerfDescriptor;
import com.ibm.websphere.pmi.client.PmiClient;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Properties;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;

/**
 * map a Metric to the WebSphere PMI api.
 */
public class WebspherePMI {

    private static HashMap clientCache = new HashMap();
    private static HashMap descriptorCache = new HashMap();

    static class CpdCache {

        int expire = 1000 * 60 * 10;
        long timestamp = 0;
        CpdCollection value = null;
    }

    static class PmiClientWrapper extends PmiClient {

        HashMap cache = new HashMap();

        public PmiClientWrapper() throws RemoteException {
            super();
        }

        public PmiClientWrapper(String arg0, String arg1, String arg2)
                throws RemoteException {
            super(arg0, arg1, arg2);
        }

        public PmiClientWrapper(Properties arg0, String arg1)
                throws RemoteException {
            super(arg0, arg1);
        }

        public CpdCollection get(Metric metric, PerfDescriptor desc)
                throws PmiException {

            long timeNow = System.currentTimeMillis();
            String key = metric.getObjectName();

            CpdCache cacheValue = (CpdCache) cache.get(key);

            if (cacheValue == null) {
                cacheValue = new CpdCache();
                cache.put(key, cacheValue);
            } else if ((timeNow - cacheValue.timestamp) > cacheValue.expire) {
                cacheValue.value = null;
            }

            if (cacheValue.value == null) {
                cacheValue.timestamp = timeNow;
                cacheValue.value = super.get(desc, false);
            }

            return cacheValue.value;
        }
    }

    public static PmiClient getPmiClient(Properties cfg)
            throws RemoteException {

        Properties props = getAdminProperties(cfg);

        return new PmiClientWrapper(props, "WAS50");
    }

    private static PmiClient getPmiClient(Metric metric)
            throws RemoteException {

        PmiClient pmiclient;

        String key = metric.getPropString();

        synchronized (clientCache) {
            pmiclient = (PmiClient) clientCache.get(key);
        }

        if (pmiclient == null) {
            pmiclient = getPmiClient(metric.getProperties());

            synchronized (clientCache) {
                clientCache.put(key, pmiclient);
            }
        }

        return pmiclient;
    }

    public static PerfDescriptor getModuleDescriptor(PmiClient pmiclient,
            Metric metric) {
        String key = metric.getObjectName();

        PerfDescriptor moduleDescriptor;

        synchronized (descriptorCache) {
            moduleDescriptor = (PerfDescriptor) descriptorCache.get(key);
        }

        if (moduleDescriptor == null) {
            Properties props = metric.getObjectProperties();

            PerfDescriptor serverDescriptor =
                    PmiClient.createPerfDescriptor(metric.getDomainName());

            moduleDescriptor =
                    PmiClient.createPerfDescriptor(serverDescriptor,
                    props.getProperty("Module"));

            synchronized (descriptorCache) {
                descriptorCache.put(key, moduleDescriptor);
            }
        }

        return moduleDescriptor;
    }

    //XXX plenty of optimizations can be made here.
    public static Double getValue(Metric metric)
            throws PluginException,
            MetricNotFoundException,
            MetricUnreachableException {

        //XXX for the moment, if there is a module descriptor for the
        //server/service it is considered available.  this will remain
        //the case for certain services such as EJB, JDBC and Thread Pools
        //others such as server and webapp should have more robust checks.
        boolean isAvail = metric.isAvail();

        PmiClientWrapper pmiclient;

        try {
            pmiclient = (PmiClientWrapper) getPmiClient(metric);
        } catch (RemoteException e) {
            if (isAvail) {
                return new Double(Metric.AVAIL_DOWN);
            }

            Properties props = metric.getProperties();

            String msg = "Cannot connect to server "
                    + props.getProperty(WebsphereProductPlugin.PROP_ADMIN_HOST)
                    + ":"
                    + props.getProperty(WebsphereProductPlugin.PROP_ADMIN_PORT);

            String rmsg = e.getMessage();
            if (rmsg.indexOf("\n") < 0) {
                msg += " (" + rmsg + ")";
            } //else is a bigass stacktrace

            throw new MetricUnreachableException(msg, e);
        }

        CpdCollection stats;

        synchronized (pmiclient) {
            PerfDescriptor moduleDescriptor =
                    getModuleDescriptor(pmiclient, metric);

            try {
                stats = pmiclient.get(metric, moduleDescriptor);
            } catch (PmiException pe) {
                String msg = "Error getting " + metric.getObjectName();
                throw new PluginException(msg, pe);
            }
        }

        boolean haveStats =
                ((stats != null)
                && (stats.dataMembers() != null)
                && (stats.dataMembers().length > 0));

        if (isAvail) {
            if (haveStats) {
                //if the server can collect stats, then the service
                //is considered available.
                return new Double(Metric.AVAIL_UP);
            } else {
                return new Double(Metric.AVAIL_DOWN);
            }
        }

        CpdData data = null;

        if (haveStats) {
            data = findCpdData(stats.dataMembers(),
                    metric.getAttributeName());
        }

        if (data == null) {
            String obj = metric.getObjectPropString();
            if (!obj.equals("Module=jvmRuntimeModule") && //jvm must be present
                    obj.startsWith("Module=") && obj.endsWith("Module")) //aggregates
            {
                //can happen if no services are deployed for
                //the given type:
                //Module={connectionPool,servletSessions,transaction,etc}Module
                return new Double(Double.NaN);
            }

            throw new MetricNotFoundException(metric.getObjectName());
        }

        CpdValue value = data.getValue();

        switch (value.getType()) {
            case PmiConstants.TYPE_STAT:
                CpdStat cpdstat = (CpdStat) value;
                return new Double(cpdstat.getValue());
            case PmiConstants.TYPE_LOAD:
                CpdLoad cpdload = (CpdLoad) value;
                return new Double(cpdload.getValue());
            case PmiConstants.TYPE_LONG:
            case PmiConstants.TYPE_DOUBLE:
            case PmiConstants.TYPE_INT:
                return new Double(value.getValue());
            default:
                throw new MetricInvalidException(metric.toString());
        }
    }

    private static CpdData findCpdData(CpdData[] data, String name) {
        String match = "." + name;

        for (int i = 0; i < data.length; i++) {
            if (data[i].getDescriptor().getName().endsWith(match)) {
                return data[i];
            }
        }

        return null;
    }

    public static Properties getAdminProperties(Properties cfg) {
        String host =
                cfg.getProperty(WebsphereProductPlugin.PROP_ADMIN_HOST,
                "localhost");
        String port =
                cfg.getProperty(WebsphereProductPlugin.PROP_ADMIN_PORT,
                "8880");

        Properties props = new Properties();

        props.setProperty(AdminClient.CONNECTOR_TYPE,
                AdminClient.CONNECTOR_TYPE_SOAP);

        props.setProperty(AdminClient.CONNECTOR_HOST, host);

        props.setProperty(AdminClient.CONNECTOR_PORT, port);

        String user = cfg.getProperty(AdminClient.USERNAME, "");
        String pass = cfg.getProperty(AdminClient.PASSWORD, "");

        if ((!"".equals(user)) && (!"".equals(pass))) {
            props.setProperty(AdminClient.USERNAME, user);
            props.setProperty(AdminClient.PASSWORD, pass);
            props.setProperty(AdminClient.CONNECTOR_SECURITY_ENABLED, "true");
        }

        return props;
    }
}
