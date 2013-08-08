/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2013], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.measurement.agent.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.MeasurementCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.bizapp.shared.lather.TopNSendReport_args;
import org.hyperic.hq.measurement.TopNSchedule;
import org.hyperic.hq.measurement.agent.commands.ScheduleTopn_args;
import org.hyperic.hq.plugin.system.ProcessData;
import org.hyperic.hq.plugin.system.ProcessReport;
import org.hyperic.hq.plugin.system.TopData;
import org.hyperic.hq.plugin.system.TopReport;
import org.hyperic.hq.util.properties.PropertiesUtil;
import org.hyperic.sigar.Humidor;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;


class TopNScheduler {

    private static final int SEND_INTERVAL = 1;
    private static final int MAX_BATCHSIZE = 5;
    private static final int MAX_ELEMENT_TO_STORE = 24 * 60;
    private static final String DATA_FOLDERNAME = "topn_records";
    private static final String SCHEDULE_FILE = "topn_schedule";

    private final AgentStorageProvider storage;
    private final Log                  log;
    private Sigar _sigarImpl;
    private Humidor _humidor;
    private ScheduledExecutorService scheduler;
    private ScheduledExecutorService sender;
    private final MeasurementCallbackClient client;
    private String agentToken;
    private boolean deductServerTimeDiff = true;


    TopNScheduler(AgentStorageProvider storage, Properties config) throws AgentStartException {
        this.log             = LogFactory.getLog(TopNScheduler.class);
        this.storage         = storage;
        this.client = setupClient();

        // by default we the deduction feature is on
        Boolean deductServerOffset = PropertiesUtil.getBooleanValue(
                config.getProperty(ServerTimeDiff.PROP_DEDUCT_SERVER_TIME_DIFF), true);

        deductServerTimeDiff = deductServerOffset;

        createSender();
        loadScheduleData();
    }


    private void loadScheduleData() {
        TopNSchedule schedule = null;
        if (null != (schedule = storage.<TopNSchedule> getObject(SCHEDULE_FILE))) {
            scheduleTopN(schedule);
        }
    }


    private void createSender() {
        sender = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private final AtomicLong i = new AtomicLong(0);
            public Thread newThread(Runnable r) {
                return new Thread(r, "TopNSender" + i.getAndIncrement());
            }
        });

        sender.scheduleAtFixedRate(new Runnable() {
            public void run() {
                boolean success;
                List<TopReport> reports = new ArrayList<TopReport>();
                for (TopReport report : storage.<TopReport> getObjectsFromFolder(DATA_FOLDERNAME, MAX_BATCHSIZE)) {
                    reports.add(report);

                }
                // If we don't have anything to send -- move along
                if (reports.isEmpty()) {
                    log.debug("No TopN records were found in the storage");
                    return;
                }
                log.debug("Sending " + reports.size() + " TopN entries " + "to server");
                success = false;
                try {
                    TopNSendReport_args report = new TopNSendReport_args();
                    if (agentToken == null) {
                        agentToken = storage.getValue(CommandsAPIInfo.PROP_AGENT_TOKEN);
                    }
                    report.setAgentToken(agentToken);
                    report.setTopReports(reports);
                    client.topNSendReport(report);
                    success = true;
                } catch (AgentCallbackClientException exc) {
                    log.error("Error sending TOPN data to server: " + exc.getMessage());
                }

                // delete the records we sent from the storage
                if (success) {
                    List<String> filesToDelete = new ArrayList<String>();
                    for (TopReport report : reports) {
                        filesToDelete.add(String.valueOf(report.getCreateTime()));
                    }
                    storage.deleteObjectsFromFolder(DATA_FOLDERNAME,
                            filesToDelete.toArray(new String[filesToDelete.size()]));
                }
            }
            // TimeUnit.MINUTE does not work on java5
        }, SEND_INTERVAL * 60, SEND_INTERVAL * 60, TimeUnit.SECONDS);

    }


    private void createScheduler() {
        scheduler = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            private final AtomicLong i = new AtomicLong(0);

            public Thread newThread(Runnable r) {
                return new Thread(r, "TopNScheduler" + i.getAndIncrement());
            }
        });
    }

    public void unscheduleTopN() {
        scheduler.shutdown();
        storage.deleteObject(SCHEDULE_FILE);
    }

    public void scheduleTopN(final ScheduleTopn_args args) {
        TopNSchedule schedule = new TopNSchedule();
        schedule.setQueryFilter(args.getQueryFilter());
        schedule.setInterval(args.getInterval());
        schedule.setNumberOfProcesses(args.getNumberOfProcesses());
        schedule.setLastUpdateTime(System.currentTimeMillis());

        // Store the schedule data in the local storage
        storage.saveObject(schedule, SCHEDULE_FILE);

        scheduleTopN(schedule);
    }

    private void scheduleTopN(final TopNSchedule schedule) {
        log.info("Scheduling TopN gathering task at interval of " + schedule.getInterval() + " minutes");
        // If the scheduler is alive we need to kill it and create a new one
        // with the new scheduling data
        if ((null != scheduler) && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        createScheduler();
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                TopData data = null;
                try {
                    data = TopData.gather(getSigar(), schedule.getQueryFilter());
                } catch (SigarException e) {
                    log.error("Unable to gather Top data", e);
                }
                if (null != data) {
                    TopReport report = generateTopReport(data);
                    report.filterTopProcesses(schedule.getNumberOfProcesses());
                    try {
                        storage.addObjectToFolder(DATA_FOLDERNAME, report, report.getCreateTime(), MAX_ELEMENT_TO_STORE);
                    } catch (Exception ex) {
                        log.error("Unable to store TopN data", ex);
                    }
                }

            }
            // TimeUnit.MINUTE does not work on java5
        }, 10, schedule.getInterval() * 60, TimeUnit.SECONDS);

    }

    /**
     * Shut down the schedule thread.
     */

    void die() {
        if (sender != null) {
            sender.shutdown();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (_sigarImpl != null) {
            _sigarImpl.close();
            _sigarImpl = null;
        }
    }


    private TopReport generateTopReport(TopData data) {
        TopReport report = new TopReport();
        if (deductServerTimeDiff
                && (Math.abs(ServerTimeDiff.getInstance().getServerTimeDiff()) > ServerTimeDiff.MIN_OFFSET_FOR_DEDUCTION)) {
            // deduct the server time offset from the metric
            // value time stamp
            report.setCreateTime(System.currentTimeMillis() + ServerTimeDiff.getInstance().getServerTimeDiff());
        } else {
            report.setCreateTime(System.currentTimeMillis());
        }
        report.setUpTime(data.getUptime().toString());
        report.setCpu(data.getCpu().toString());
        report.setMem(data.getMem().toString());
        report.setSwap(data.getSwap().toString());
        report.setProcStat(data.getProcStat().toString());
        for (ProcessData process : data.getProcesses()) {
            ProcessReport processReport = new ProcessReport(process);
            report.addProcess(processReport);
        }
        return report;
    }

    private MeasurementCallbackClient setupClient() throws AgentStartException {
        StorageProviderFetcher fetcher;
        fetcher = new StorageProviderFetcher(storage);
        return new MeasurementCallbackClient(fetcher);
    }

    private synchronized SigarProxy getSigar() {
        if (_humidor == null) {
            _sigarImpl = new Sigar();
            _humidor = new Humidor(_sigarImpl);
        }
        return _humidor.getSigar();
    }

}
