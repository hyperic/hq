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

import org.hyperic.sigar.win32.Pdh;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.ServiceConfig;
import org.hyperic.sigar.win32.Win32Exception;

public class Win32MeasurementPlugin extends MeasurementPlugin {

    public static final String DOMAIN = "win32";

    private static final String PROP_OBJECT   = "Object";
    private static final String PROP_INSTANCE = "Instance";
    private static final String PROP_TYPE     = "Type";
    
    public MetricValue getValue(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException {

        if (metric.getDomainName().equals(DOMAIN)) {
            return getValueProxy(metric);
        }
        else {
            return getValueCompat(metric);
        }
    }
    
    private MetricValue getValueProxy(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException {

        Properties props = metric.getObjectProperties();

        String object   = props.getProperty(PROP_OBJECT);
        String instance = props.getProperty(PROP_INSTANCE);
        String type     = props.getProperty(PROP_TYPE);
        String counter  = metric.getAttributeName();

        boolean isAvail=false, isFormatted=false;
        
        if (type != null) {
            isAvail = Metric.ATTR_AVAIL.equals(type);
            if (!isAvail) {
                isFormatted = "Formatted".equals(type);
            }
        }

        if (object == null) {
            if ((object = props.getProperty("Service")) != null) {
                if (counter.equals("StartType")) {
                    return new MetricValue(getServiceStartType(object));
                }
                else {
                    return getServiceValue(object);
                }
            }
        }
        
        if (object == null) {
            throw new MetricInvalidException(metric.toString());
        }
        
        StringBuffer name = new StringBuffer();

        name.append('\\').append(object);
        if (instance != null) {
            name.append('(').append(instance).append(')');
        }
        name.append('\\').append(counter);
        
        return getPdhValue(metric, name.toString(),
                           isAvail, isFormatted);
    }

    private MetricValue getValueCompat(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException {
        
        String domain = getDomainName(metric);
        String attr   = getAttributeName(metric);

        if (domain.equals("ServiceAvail")) {
            //check w/ service manager if service is running
            return getServiceValue(attr);
        }

        StringBuffer name = new StringBuffer();

        if (domain.charAt(0) != '\\') {
            name.append("\\");
        }
        name.append(domain);
        if (domain.charAt(domain.length()-1) != '\\') {
            name.append("\\");
        }
        name.append(attr);

        String propString = metric.getObjectPropString();
        boolean isFormatted=false, isAvail=false;
        
        if (propString.equals("Type=Formatted")) {
            isFormatted = true;
        }
        else if (propString.equals("Type=Availability")) {
            isAvail = true;
        }
        
        return getPdhValue(metric, name.toString(),
                           isAvail, isFormatted);
    }
    
    protected String getAttributeName(Metric metric) {
        return metric.getAttributeName();
    }
    
    protected String getDomainName(Metric metric) {
        return metric.getDomainName();
    }

    protected double adjustValue(Metric metric, double value) {
        return value;
    }

    private static int getServiceStatus(String name) {
        Service svc = null;
        try {
            svc = new Service(name);
            return svc.getStatus();
        } catch (Win32Exception e) {
            return Service.SERVICE_STOPPED;
        } finally {
            if (svc != null) {
                svc.close();
            }
        }
    }

    private static int getServiceStartType(String name) {
        Service svc = null;
        try {
            svc = new Service(name);
            return svc.getConfig().getStartType();
        } catch (Win32Exception e) {
            return ServiceConfig.START_DISABLED;
        } finally {
            if (svc != null) {
                svc.close();
            }
        }
    }

    static boolean isServiceRunning(String name) {
        return getServiceStatus(name) == Service.SERVICE_RUNNING;
    }

    private MetricValue getPdhValue(Metric metric,
                                    String counter,
                                    boolean isAvail,
                                    boolean isFormatted)
        throws MetricNotFoundException {
        
        Pdh pdh = null;

        try {
            pdh = new Pdh();
            double value;
            //default is raw
            if (isFormatted) {
                value = pdh.getFormattedValue(counter);    
            }
            else {
                value = pdh.getRawValue(counter);    
            }

            if (isAvail) {
                return new MetricValue(Metric.AVAIL_UP);
            }
            return new MetricValue(adjustValue(metric, value),
                                   System.currentTimeMillis());
        } catch (Win32Exception e) {
            throw new MetricNotFoundException(counter);
        } finally {
            if (pdh != null) {
                try { pdh.close(); } catch (Win32Exception e) {}
            }
        }
    }
    
    private MetricValue getServiceValue(String name) {
        double avail;

        switch (getServiceStatus(name)) {
            case Service.SERVICE_RUNNING:
                avail = Metric.AVAIL_UP;
                break;
            case Service.SERVICE_PAUSED:
                avail = Metric.AVAIL_PAUSED;
                break;
            case Service.SERVICE_STOPPED:
            default:
                avail = Metric.AVAIL_DOWN;
                break;
        }

        return new MetricValue(avail);
    }
}
