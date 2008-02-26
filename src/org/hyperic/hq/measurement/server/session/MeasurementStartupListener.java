/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.measurement.server.session;

import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDeleteCallback;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.common.server.session.ServerConfigManagerEJBImpl;
import org.hyperic.hq.measurement.galerts.MetricAuxLogProvider;
import org.hyperic.hq.zevents.ZeventManager;

public class MeasurementStartupListener
    implements StartupListener
{
    private static final String PROP_REPSTATS_SIZE = "REPORT_STATS_SIZE";
    
    private final Log _log = 
        LogFactory.getLog(MeasurementStartupListener.class);
    
    private static final Object LOCK = new Object();
    private static DataInserter _dataInserter;
    private static DefaultMetricEnableCallback _defEnableCallback;
    private static MetricDeleteCallback _delCallback;
    
    public void hqStarted() {
        // Make sure we have the aux-log provider loaded
        MetricAuxLogProvider.class.toString();
        SRNManagerEJBImpl.getOne().initializeCache();
    
        /**
         * Add measurement enabler listener to enable metrics for newly
         * created resources or to reschedule when resources are updated.
         */
        Set listenEvents = new HashSet();
        listenEvents.add(ResourceCreatedZevent.class);
        listenEvents.add(ResourceUpdatedZevent.class);
        listenEvents.add(ResourceRefreshZevent.class);
        ZeventManager.getInstance()
                     .addBufferedListener(listenEvents,
                                          new MeasurementEnabler());

        listenEvents = new HashSet();
        listenEvents.add(DownMetricZevent.class);
        ZeventManager.getInstance().
            addBufferedListener(listenEvents, new DownMetricsCalculator());

        HQApp app = HQApp.getInstance();
        synchronized (LOCK) {
            _defEnableCallback = (DefaultMetricEnableCallback)
                app.registerCallbackCaller(DefaultMetricEnableCallback.class);
            _delCallback = (MetricDeleteCallback)
                app.registerCallbackCaller(MetricDeleteCallback.class);
            _dataInserter = new SynchronousDataInserter();
        }
        
        app.registerCallbackListener(MetricDeleteCallback.class, 
                                     new MetricDeleteCallback() {
            public void beforeMetricDelete(RawMeasurement m) {
            }

            public void beforeMetricsDelete(Collection mids) {
                MetricAuxLogManagerEJBImpl.getOne().metricsDeleted(mids);
            }
        });
        
        app.registerCallbackListener(ResourceDeleteCallback.class,
                                     new ResourceDeleteCallback() {

            public void preResourceDelete(Resource r)
                throws VetoException {
                DerivedMeasurementManagerEJBImpl.getOne()
                    .handleResourceCreateEvent(r);
            }
            
        });

        /*
        app.registerCallbackListener(PluginsDeployedCallback.class,
                                     new PluginsDeployedCallback() {
            public void pluginsDeployed(List plugins) {
                DerivedMeasurementManagerLocal dman =
                    DerivedMeasurementManagerEJBImpl.getOne();
                for (Iterator i = plugins.iterator(); i.hasNext();) {
                    String pluginName = (String)i.next();
                    dman.syncPluginMetrics(pluginName);
                }
            }
        });
        */

        initReportsStats();
        AgentScheduleSynchronizer.getInstance().initialize();
    }

    private void initReportsStats() {
        Properties cfg = new Properties();

        try {
            cfg = ServerConfigManagerEJBImpl.getOne().getConfig();
        } catch(Exception e) {
            _log.warn("Error getting server config", e);
        }
        
        int repSize = Integer.parseInt(cfg.getProperty(PROP_REPSTATS_SIZE, 
                                                       "1000"));
        ReportStatsCollector.getInstance().initialize(repSize);
    }
    
    public static void setDataInserter(DataInserter d) {
        synchronized (LOCK) {
            _dataInserter = d;
        }
    }
    
    static DataInserter getDataInserter() {
        synchronized (LOCK) {
            return _dataInserter;
        }
    }
    
    static DefaultMetricEnableCallback getDefaultEnableObj() {
        synchronized (LOCK) {
            return _defEnableCallback;
        }
    }
    
    static MetricDeleteCallback getMetricDeleteCallbackObj() {
        synchronized (LOCK) {
            return _delCallback;
        }
    }
}
