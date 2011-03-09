/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2010], VMWare, Inc.
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
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.ext.ProblemMetricInfo;
import org.hyperic.hq.measurement.ext.ProblemResourceInfo;
import org.hyperic.hq.measurement.shared.ProblemMetricManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.pager.PageControl;
import org.springframework.stereotype.Service;

@Service
public class ProblemMetricManagerImpl implements ProblemMetricManager {

    
    public void createProblem(Integer mid, long time, int type, Integer additional) {
    }

    
    public MetricProblem getByIdAndTimestamp(Measurement meas, long timestamp) {
        return null;
    }

    
    public ProblemMetricInfo[] getProblemMetrics(AppdefEntityID aid, long begin, long end) {
        return new ProblemMetricInfo[0];
    }

    
    public ProblemResourceInfo[] getProblemResources(long begin, long end,
                                                     Set<AppdefEntityID> permitted,
                                                     PageControl pc) {
        return new ProblemResourceInfo[0];
    }

    
    public ProblemResourceInfo[] getProblemResourcesByTypeAndInstances(
        int appdefType, Integer[] instanceIds, long begin, long end, PageControl pc) {
        return new ProblemResourceInfo[0];
    }

    @SuppressWarnings("unchecked")
    
    public Map<Integer, ProblemMetricInfo> getProblemsByTemplate(
        int appdefType, Integer[] eids, long begin, long end) {
        return Collections.emptyMap();
    }

    
    public void processMetricValue(Integer mid, MetricValue mv, int type) {
    }

    
    public void removeProblems(Collection<Integer> mids) {
    }

    
    public void removeProblems(AppdefEntityID entityId) {
    }

}
