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

package org.hyperic.hq.bizapp.server.session;

import java.util.Iterator;
import java.util.List;

import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.measurement.server.session.CollectionSummary;
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.ReportStatsCollector;

public class HQInternalService implements HQInternalServiceMBean {
    public double getMetricInsertsPerMinute() {
        double val = ReportStatsCollector.getInstance()
                        .getCollector().valPerTimestamp(); 

        return val * 1000.0 * 60.0;
    }

    public int getAgentCount() {
        return AgentManagerEJBImpl.getOne().getAgentCountUsed();
    }

    public double getMetricsCollectedPerMinute() { 
        List vals = DerivedMeasurementManagerEJBImpl.getOne()
                        .findMetricCountSummaries();
        double total = 0.0;
        
        for (Iterator i = vals.iterator(); i.hasNext(); ) {
            CollectionSummary s = (CollectionSummary)i.next();
            
            total += (float)s.getTotal() / (float)s.getInterval();
        }
        
        return total;
    }

    public int getPlatformCount() {
        return PlatformManagerEJBImpl.getOne().getPlatformCount().intValue();
    }

    public long getTransactionCount() {
        return HQApp.getInstance().getTransactions();
    }

    public long getTransactionFailureCount() {
        return HQApp.getInstance().getTransactionsFailed();
    }

    public long getAgentRequests() {
        return AgentManagerEJBImpl.getOne().getTotalConnectedAgents();
    }
    
    public int getAgentConnections() {
        return AgentManagerEJBImpl.getOne().getNumConnectedAgents();
    }
}
