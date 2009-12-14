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

package org.hyperic.hq.events.server.session;

import java.util.Date;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.Scheduler;
import org.hyperic.hq.application.StartupListener;
import org.hyperic.hq.events.shared.HeartBeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The startup listener that schedules the Heart Beat Service to dispatch heart
 * beats at a fixed rate.
 */
@Service
public class HeartBeatServiceStartupListener implements StartupListener {

    /**
     * The period (in msec) at which heart beats are dispatched by the Heart
     * Beat Service.
     */
    public static final int HEART_BEAT_PERIOD_MILLIS = 30 * 1000;

    private final Log log = LogFactory.getLog(HeartBeatServiceStartupListener.class);

    private HQApp hqApp;
    private HeartBeatService heartBeatService;

    @Autowired
    public HeartBeatServiceStartupListener(HQApp hqApp, HeartBeatService heartBeatService) {
        this.hqApp = hqApp;
        this.heartBeatService = heartBeatService;
    }

    /**
     * @see org.hyperic.hq.application.StartupListener#hqStarted()
     */
    @PostConstruct
    public void hqStarted() {
        // We want to start dispatching heart beats only after all plugins
        // have been deployed since this is when the server starts accepting
        // metrics from agents.
        log.info("Scheduling Heart Beat Service to dispatch heart beats every " + (HEART_BEAT_PERIOD_MILLIS / 1000) +
                 " sec");

        Scheduler scheduler = hqApp.getScheduler();

        scheduler.scheduleAtFixedRate(new HeartBeatServiceTask(), Scheduler.NO_INITIAL_DELAY, HEART_BEAT_PERIOD_MILLIS);
    }

    private class HeartBeatServiceTask implements Runnable {

        private final Log log = LogFactory.getLog(HeartBeatServiceTask.class);

        public void run() {
            try {
                heartBeatService.dispatchHeartBeat(new Date());
            } catch (Throwable t) {
                // we want to catch everything so that the scheduled task
                // continues executing.
                log.error("Error while dispatching a heart beat", t);
            }

        }

    }

}
