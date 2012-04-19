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

import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.Scheduler;
import org.hyperic.hq.common.ProductProperties;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.galerts.MetricAuxLogProvider;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MeasurementSystemInitializer  {

    private static final Log _log = LogFactory.getLog(MeasurementSystemInitializer.class);

    private static ScheduledFuture _dataPurgeFuture;
    private final MeasurementManager measurementManager;

    private static Scheduler scheduler;

    @Autowired
    public MeasurementSystemInitializer(MeasurementManager measurementManager,
                                        ReportStatsCollector reportStatsCollector,
                                        Scheduler scheduler) {
        this.measurementManager = measurementManager;
        MeasurementSystemInitializer.scheduler = scheduler;

    }

    @PostConstruct
    public void init() {
        // Make sure we have the aux-log provider loaded
        MetricAuxLogProvider.class.toString();
        prefetchEnabledMeasurementsAndTemplates();
        startDataPurgeWorker();
    }

    public void stopDataPurgeWorker() {
        if (_dataPurgeFuture != null) {
            _log.info("Stopping Data Purge Worker");
            _dataPurgeFuture.cancel(true);
            scheduler.purgeTasks();
            _dataPurgeFuture = null;
        }
    }

    /**
     * Starts either the com.hyperic.hq.measurement.DataPurgeJob or
     * org.hyperic.hq.measurement.DataPurgeJob after stopping an existing worker
     * if one is already scheduled. The worker is scheduled to run at 10 past
     * every hour.
     */
    public void startDataPurgeWorker() {
        stopDataPurgeWorker();
        // want to schedule at 10 past the hour for legacy, nice to know
        // when all of our DB maintenance occurs.
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now());
        if (cal.get(Calendar.MINUTE) >= 10) {
            cal.add(Calendar.HOUR_OF_DAY, 1);
        }
        cal.set(Calendar.MINUTE, 10);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        final long initialDelay = cal.getTimeInMillis() - now();
        _log.info("Starting Data Purge Worker");
        Runnable dataPurgeJob = (Runnable) ProductProperties
            .getPropertyInstance("hyperic.hq.dataPurge");
        if (dataPurgeJob == null) {
            _log.fatal("Could not start DataPurgeWorker");
            return;
        }
        _dataPurgeFuture = scheduler.scheduleAtFixedRate(dataPurgeJob, initialDelay,
            MeasurementConstants.HOUR);
    }

    private final long now() {
        return System.currentTimeMillis();
    }

    private void prefetchEnabledMeasurementsAndTemplates() {
        measurementManager.findAllEnabledMeasurementsAndTemplates();
    }
    
    @PreDestroy
    private void destroy() { 
        scheduler.shutdown() ;
        scheduler = null ;
       _dataPurgeFuture = null ;
    }//EOM
    

}
