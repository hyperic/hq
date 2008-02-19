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

import java.util.HashMap;
import java.util.Map;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarProxyCache;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarNotImplementedException;
import org.hyperic.sigar.jmx.SigarInvokerJMX;

import org.hyperic.sigar.ptql.ProcessQuery;
import org.hyperic.sigar.ptql.ProcessQueryFactory;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.hyperic.sigar.ptql.MalformedQueryException;
import org.hyperic.sigar.ptql.QueryLoadException;

import org.hyperic.util.StringUtil;

import org.hyperic.util.config.ConfigResponse;

public class SigarMeasurementPlugin extends MeasurementPlugin {

    public static final String DOMAIN      = "sigar";
    public static final String PTQL_DOMAIN = "sigar.ptql";
    public static final String PTQL_CONFIG = "process.query";

    private Sigar      sigar        = null;
    private SigarProxy sigarProxy   = null;
    private ProcessFinder processFinder = null;
    private static final Map AVAIL_ATTRS = new HashMap();

    //Availaiblity helpers. Assume resource is available if
    //we can collect the given attribute.
    static {
        AVAIL_ATTRS.put("DirUsage", "Total");
        AVAIL_ATTRS.put("FileInfo", "Type");
        AVAIL_ATTRS.put("MultiProcCpu", "Processes");
        AVAIL_ATTRS.put("MultiProcMem", "Size");
    }

    protected Sigar getSigar() throws PluginException {
        if (this.sigar != null) {
            return this.sigar;
        }
        try {
            this.sigar = new Sigar();
            this.sigarProxy = SigarProxyCache.newInstance(sigar);
        } catch (UnsatisfiedLinkError le) {
            //XXX ok for now; sigar is not loaded in the server
            //getValue will fail in the agent in sigar was not properly
            //installed.
            getLog().warn("unable to load sigar: " + le.getMessage());
            return null;
        }
        
        return this.sigar;
    }
    
    public void shutdown() throws PluginException {
        if (this.sigar != null) {
            this.sigar.close();
        }
        super.shutdown();
    }

    public String translate(String template, ConfigResponse config) {
        if (template.indexOf(PTQL_DOMAIN + ":") > 0) {
            //do not encode the query input.
            String newTemplate =
                StringUtil.replace(template,
                                   "%" + PTQL_CONFIG + "%",
                                   config.getValue(PTQL_CONFIG));
            //yes, this is '!=', not '.equals'
            //StringUtil.replace returns the original if unchanged.
            if (newTemplate != template) {
                return newTemplate;
            }
            //fallthru e.g. Win32MeasurementPlugin uses sigar.ptql
            //domain but not %process.query%
        }
        
        return super.translate(template, config);
    }

    private String translatePTQL(Metric jdsn)
        throws MetricNotFoundException {

        String qs = jdsn.getPropString();
        if (this.processFinder == null) {
            this.processFinder =
                new ProcessFinder(this.sigarProxy);
        }

        ProcessQuery query;

        try {
            query = ProcessQueryFactory.getInstance(qs);
        } catch (MalformedQueryException e) {
            throw new MetricInvalidException(e.getMessage(), e);
        } catch (QueryLoadException e) {
            throw new MetricInvalidException(e.getMessage(), e);
        }

        long pid;
        try {
            pid = processFinder.findSingleProcess(query);
        } catch (SigarNotImplementedException e) {
            throw new MetricInvalidException(e.getMessage(), e);
        } catch (MalformedQueryException e) {
            throw new MetricNotFoundException(e.getMessage(), e);
        } catch (SigarException e) {
            throw new MetricNotFoundException(e.getMessage(), e);
        }

        final String type = SigarInvokerJMX.PROP_TYPE;
        final String arg  = SigarInvokerJMX.PROP_ARG;

        //rewrite template with found pid.
        return new StringBuffer().
            append(type).append('=').
            append(jdsn.getObjectProperty(type)).append(',').
            append(arg).append('=').append(pid).
            toString();
    }

    private boolean isProcessState(String attr) {
        return attr.equals("State");
    }

    public MetricValue getValue(Metric metric) 
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException
    {
        Object systemValue;
        Double useVal;

        String domain = metric.getDomainName();
        String name = metric.getObjectName();
        String attr = metric.getAttributeName();

        if (this.sigar == null) {
            getSigar();
        }

        if (domain.equals(PTQL_DOMAIN)) {
            name = translatePTQL(metric);
        }
        else {
            //backcompat for metrics still scheduled w/ old template
            name = StringUtil.replace(name,
                                      "Type=FileSystemUsage",
                                      "Type=MountedFileSystemUsage");
        }

        boolean isAvail = false;
        boolean isProcessState = false;
        //check for Availability attribute aliases
        if (attr.equals(Metric.ATTR_AVAIL)) {
            String type =
                metric.getObjectProperty(SigarInvokerJMX.PROP_TYPE);
            String alias = (String)AVAIL_ATTRS.get(type);
            if (alias != null) {
                attr = alias;
                isAvail = true;
            }
        }
        else {
            isAvail = isProcessState = isProcessState(attr);
        }
        
        SigarInvokerJMX invoker =
            SigarInvokerJMX.getInstance(sigarProxy, name);

        try {
            synchronized (sigar) {
                systemValue = invoker.invoke(attr);
            }
        } catch (SigarNotImplementedException e) {
            return MetricValue.NONE;
        } catch (SigarException e) {
            if (isAvail) {
                return new MetricValue(Metric.AVAIL_DOWN);
            }
            else {
                throw new MetricNotFoundException(e.getMessage(), e);
            }
        }

        if (isAvail && !isProcessState) {
            useVal = new Double(Metric.AVAIL_UP);
        }
        else if (systemValue instanceof Double) {
            useVal = (Double)systemValue;
        }
        else if (systemValue instanceof Long) {
            useVal = new Double(((Long)systemValue).longValue());
        }
        else if (systemValue instanceof Integer) {
            useVal = new Double(((Integer)systemValue).intValue());
        }
        else if (systemValue instanceof Character) {
            char c = ((Character)systemValue).charValue();
            //process state
            if (isProcessState(attr)) {
                double avail;
                switch (c) {
                  case 'Z':
                    avail = Metric.AVAIL_DOWN;
                    break;
                  case 'T':
                    avail = Metric.AVAIL_PAUSED;
                    break;
                  default:
                    avail = Metric.AVAIL_UP;
                    break;
                }
                useVal = new Double(avail);
            }
            else {
                useVal = new Double((int)c);
            }
        }
        else {
            //wont happen.
            throw new MetricNotFoundException("System plugin returned a " +
                                                systemValue.getClass() + 
                                                " object, which could not " +
                                                " be handled");
        }
        if (useVal.doubleValue() == Sigar.FIELD_NOTIMPL) {
            return MetricValue.NONE;
        }
        return new MetricValue(useVal, System.currentTimeMillis());
    }
}
