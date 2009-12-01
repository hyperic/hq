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

package org.hyperic.hq.plugin.db2;

import java.util.Properties;

import org.hyperic.db2monitor.DB2Monitor;
import org.hyperic.db2monitor.DB2MonitorException;
import org.hyperic.db2monitor.SqlmDB2;
import org.hyperic.db2monitor.SqlmDbase;
import org.hyperic.db2monitor.SqlmTable;
import org.hyperic.db2monitor.SqlmTablespace;

import org.hyperic.hq.product.MeasurementPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;


public class DB2MeasurementPlugin
    extends MeasurementPlugin
{
    private static final String AVAIL_ATTR = "availability";
    private static long CACHE_TIMEOUT      = 30000; // 30 seconds

    private DB2Monitor db2Monitor = null;

    // Cached metric information
    private SqlmDB2 sqlmDB2          = null; 
    private SqlmDbase[] sqlmDbase    = null;
    private long lastUpdate          = 0;
    private static boolean loadedLibraries = false;
    
    public DB2MeasurementPlugin() {
        this.setName(DB2ProductPlugin.NAME);
    }

    public void init(PluginManager manager)
        throws PluginException
    {
        super.init(manager);

        if (loadedLibraries) {
            return;
        }
        loadedLibraries = true; //only try once.
        // On windows try to load dependent DB2 libraries first to avoid
        // annoying popups from java.exe.
        if (isWin32()) {
            try {
                System.loadLibrary("DB2APP");
            } catch (UnsatisfiedLinkError le) {
                getLog().debug("DB2 client libraries not installed.  " +
                               "Skipping DB2 initialization");
                return;
            }
        }

        try {
            DB2Monitor.load();
        } catch (UnsatisfiedLinkError le) {
            // Not everyone has the db2 shared libraries available.  getValue()
            // will fail gracefully if this is the case.
            getLog().debug("Unable to load db2Monitor: " + le.getMessage());
            return;
        } catch (DB2MonitorException e) {
            //platform not supported
        }
    }

    public MetricValue getValue(Metric metric)
        throws PluginException, MetricNotFoundException,
               MetricUnreachableException 
    {
        // XXX: if specific instance information is given, we must reattach
        //      to that specific instance.  would be good to do this in init(),
        //      but the measurement config is not passed in.

        String attr = metric.getAttributeName();
        Properties objProps = metric.getObjectProperties();
        Properties props = metric.getProperties();

        if (this.db2Monitor == null) {
            // Bah, late binding since we cannot determine this stuff at init()
            String nodename = 
                props.getProperty(DB2ProductPlugin.PROP_NODENAME);
            String user = 
                props.getProperty(DB2ProductPlugin.PROP_USER, "");
            String password =
                props.getProperty(DB2ProductPlugin.PROP_PASSWORD, "");
            
            try {
                if (nodename != null) {
                    // Attach to the instance specified
                    getLog().debug("Attaching to instance: " + nodename);
                    this.db2Monitor = new DB2Monitor(nodename, user, 
                                                     password);
                } else {
                    // Attach to the default local instance
                    getLog().debug("Attaching to default local instance");
                    this.db2Monitor = new DB2Monitor();
                }
            } catch (DB2MonitorException e) {
                getLog().error("Error attaching to DB2 instance: " +
                               e.getMessage());
            }

            if (this.db2Monitor == null) {
                throw new MetricNotFoundException("DB2 Monitor not loaded");
            }
        }

        if ((this.sqlmDB2 == null) || (this.sqlmDbase == null) ||
            (System.currentTimeMillis() > (this.lastUpdate + CACHE_TIMEOUT))) {
            try {
                this.sqlmDB2 = db2Monitor.getSqlmDB2();
                this.sqlmDbase = db2Monitor.getSqlmDbase();
                this.lastUpdate = System.currentTimeMillis();
            } catch (UnsatisfiedLinkError e) {
                throw new MetricNotFoundException(e.getMessage());
            } catch (DB2MonitorException e) {
                throw new MetricUnreachableException(e.getMessage());
            }
        }

        String type = objProps.getProperty("type");
        Double val = null;

        if (type.equals(DB2ProductPlugin.DATABASE)) {
            // Database type
            String dbase = objProps.getProperty(DB2ProductPlugin.PROP_DATABASE);
            if (dbase == null) {
                // Backwards compat
                dbase = props.getProperty(DB2ProductPlugin.PROP_DATABASE, "");
            }
            for (int i = 0; i < this.sqlmDbase.length; i++) {
                if (dbase.equalsIgnoreCase(this.sqlmDbase[i].getName())) {
                    if (attr.equals(AVAIL_ATTR)) {
                        return new 
                            MetricValue(Metric.AVAIL_UP,
                                             System.currentTimeMillis());
                    }
                    val = (Double)this.sqlmDbase[i].get(attr);
                }
            }
        } else if (type.equals(DB2ProductPlugin.TABLE)) {
            // XXX: Cache this
            String dbase = objProps.getProperty(DB2ProductPlugin.PROP_DATABASE);
            if (dbase == null) {
                // Backwards compat
                dbase = props.getProperty(DB2ProductPlugin.PROP_DATABASE, "");
            }

            String table = objProps.getProperty(DB2ProductPlugin.PROP_TABLE);
            if (table == null) {
                // Backwards compat
                table = props.getProperty(DB2ProductPlugin.PROP_TABLE, "");
            }

            SqlmTable[] sqlmTable;
            try {
                sqlmTable = this.db2Monitor.getSqlmTable(dbase);
            } catch (UnsatisfiedLinkError e) {
                throw new MetricNotFoundException(e.getMessage());
            } catch (DB2MonitorException e) {
                throw new MetricUnreachableException(e.getMessage());
            }

            for (int i = 0; i < sqlmTable.length; i++) {
                if (table.equalsIgnoreCase(sqlmTable[i].getName())) {
                    if (attr.equals(AVAIL_ATTR)) {
                        return new
                            MetricValue(Metric.AVAIL_UP,
                                             System.currentTimeMillis());
                    }
                    val = (Double)sqlmTable[i].get(attr);
                }
            }
        } else if (type.equals(DB2ProductPlugin.TABLESPACE)) {
            // XXX: Cache this
            String dbase = objProps.getProperty(DB2ProductPlugin.PROP_DATABASE);
            if (dbase == null) {
                // Backwards compat
                dbase = props.getProperty(DB2ProductPlugin.PROP_DATABASE, "");
            }

            String table = objProps.getProperty(DB2ProductPlugin.PROP_TABLESPACE);
            if (table == null) { 
                // Backwards compat
                table = props.getProperty(DB2ProductPlugin.PROP_TABLESPACE, "");
            }

            SqlmTablespace[] sqlmTablespace;
            try {
                sqlmTablespace = this.db2Monitor.getSqlmTablespace(dbase);
            } catch (UnsatisfiedLinkError e) {
                throw new MetricNotFoundException(e.getMessage());
            } catch (DB2MonitorException e) {
                throw new MetricUnreachableException(e.getMessage());
            }

            for (int i = 0; i < sqlmTablespace.length; i++) {
                if (table.equalsIgnoreCase(sqlmTablespace[i].getName())) {
                    if (attr.equals(AVAIL_ATTR)) {
                        return new MetricValue(Metric.AVAIL_UP,
                                               System.currentTimeMillis());
                    }
                    val = (Double)sqlmTablespace[i].get(attr);
                }
            }
        } else {
            // Server type
            if (attr.equals(AVAIL_ATTR)) {
                return new MetricValue(Metric.AVAIL_UP,
                                       System.currentTimeMillis());
            }

            val = (Double)this.sqlmDB2.get(attr);
        }

        if (val == null)
            throw new MetricNotFoundException(attr);

        return new MetricValue(val, System.currentTimeMillis());
    }
}
