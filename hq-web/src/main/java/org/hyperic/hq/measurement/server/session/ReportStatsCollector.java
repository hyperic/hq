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
package org.hyperic.hq.measurement.server.session;

import java.text.DateFormat;
import java.util.Date;

import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticsLogger;
import org.hyperic.util.PrintfFormat;
import org.hyperic.util.stats.StatsCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ReportStatsCollector {

    private final Object lock = new Object();
    private StatsCollector stats;

    @Autowired
    public ReportStatsCollector(DiagnosticsLogger diagnosticsLogger) {
        diagnosticsLogger.addDiagnosticObject(new DiagnosticObject() {
            public String getName() {
                return "Metric Reports Stats";
            }

            public String getShortName() {
                return "metricReportsStats";
            }

            public String toString() {
                return "ReportStatsCollector";
            }

            public String getStatus() {
                DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
                long start = getCollector().getOldestTime();
                long end = getCollector().getNewestTime();
                long now = System.currentTimeMillis();
                double nMetrics = getCollector().valPerTimestamp(now) * 60;
                PrintfFormat pfmt = new PrintfFormat("%.3f");
                return "Metric Report Data\n" + "    Start:     " + fmt.format(new Date(start)) + "\n" +
                       "    End:       " + fmt.format(new Date(end)) + "\n" + "    Samples:   " +
                       getCollector().getSize() + "\n" + "    Rate:      " + pfmt.sprintf(nMetrics) + " kMetrics / min";
            }
        });
        initialize(2);
    }

    public void initialize(int numEnts) {
        synchronized (lock) {
            stats = new StatsCollector(numEnts);
        }
    }

    public StatsCollector getCollector() {
        synchronized (lock) {
            return stats;
        }
    }

    public String toString() {
        return "ReportStatsCollector";
    }
}
