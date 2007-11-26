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
import org.hyperic.hq.common.DiagnosticThread;
import org.hyperic.util.stats.StatsCollector;

public class ReportStatsCollector {
    private static final ReportStatsCollector INSTANCE = 
        new ReportStatsCollector();
    private static final Object LOCK = new Object();
    private StatsCollector _stats;
    
    private ReportStatsCollector() {}
    
    public void initialize(int numEnts) {
        synchronized (LOCK) {
            _stats = new StatsCollector(numEnts);
        }
        
        DiagnosticThread.addDiagnosticObject(new DiagnosticObject() {
            public String getName() {
                return "Metric Reports Stats";
            }

            public String getShortName() {
                return "metricReportsStats";
            }

            public String getStatus() {
                DateFormat fmt = 
                    DateFormat.getDateTimeInstance(DateFormat.LONG,  
                                                   DateFormat.LONG);
                long start = getCollector().getOldestTime() * 1000 * 60;
                long end   = getCollector().getNewestTime() * 1000 * 60;
                
                return "Metric Report Data\n" + 
                     "    Start:     " + fmt.format(new Date(start)) + "\n" +
                     "    End:       " + fmt.format(new Date(end)) + "\n" +
                     "    # points:  " + getCollector().getSize() + "\n" +
                     "    Rate:      " + getCollector().valPerTimestamp() +
                     " / min";
            }
        });
    }
    
    public StatsCollector getCollector() {
        synchronized (LOCK) {
            return _stats;
        }
    }
    
    public static ReportStatsCollector getInstance() {
        return INSTANCE;
    }
}
