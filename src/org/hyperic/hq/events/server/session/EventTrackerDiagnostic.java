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

import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.MethodInvocationMetricsGroup;
import org.hyperic.util.PrintfFormat;

/**
 * A diagnostic object for event tracker operations.
 */
class EventTrackerDiagnostic implements DiagnosticObject {
    
    private static final ThreadLocal ADD_REFERENCE = new ThreadLocal();
    
    private static final ThreadLocal UPDATE_REFERENCE = new ThreadLocal();
    
    private static final ThreadLocal DELETE_REFERENCE = new ThreadLocal();
    
    private static final ThreadLocal GET_REFERENCE = new ThreadLocal();
    
    private static final EventTrackerDiagnostic INSTANCE = 
        new EventTrackerDiagnostic();
    
    private final MethodInvocationMetricsGroup _addMetrics;
    private final MethodInvocationMetricsGroup _updateMetrics;
    private final MethodInvocationMetricsGroup _deleteMetrics;
    private final MethodInvocationMetricsGroup _getMetrics;
    
    /**
     * @return The singleton instance.
     */
    public static EventTrackerDiagnostic getInstance() {
        return INSTANCE;
    }
    
    /**
     * Private constructor for a singleton.
     */
    private EventTrackerDiagnostic() {
        _addMetrics = new MethodInvocationMetricsGroup("    Add Events:");
        _updateMetrics = new MethodInvocationMetricsGroup("    Update Events:");
        _deleteMetrics = new MethodInvocationMetricsGroup("    Delete Events:");
        _getMetrics = new MethodInvocationMetricsGroup("    Get Events:");
    }

    /**
     * @see org.hyperic.hq.common.DiagnosticObject#getName()
     */
    public String getName() {
        return "Event Tracker Stats";
    }

    /**
     * @see org.hyperic.hq.common.DiagnosticObject#getShortName()
     */
    public String getShortName() {
        return "EventTrackerStats";
    }

    /**
     * @see org.hyperic.hq.common.DiagnosticObject#getStatus()
     */
    public String getStatus() {        
        MethodInvocationMetricsGroup[] allMetrics = 
            {_addMetrics, _getMetrics, _updateMetrics, _deleteMetrics};

        StringBuffer res = new StringBuffer();
        res.append("Event Tracker Queries:\n");
        
        PrintfFormat timingFmt = 
            new PrintfFormat("%-20s max=%-10d min=%-7d avg=%-10.2f num=%-20d\n");
        
        for (int i = 0; i < allMetrics.length; i++) {
            MethodInvocationMetricsGroup metricGroup = allMetrics[i];
            
            metricGroup.flush();
            
            Object[] args = new Object[] { metricGroup.getMetricGroupName(), 
                                           new Long(metricGroup.getMaxInvocationTime()), 
                                           new Long(metricGroup.getMinInvocationTime()), 
                                           new Double(metricGroup.getAverageInvocationTime()), 
                                           new Long(metricGroup.getNumberInvocations())};
            
            res.append(timingFmt.sprintf(args));
        }
        
        return res.toString();
    }
    
    /**
     * Start timing the event tracker add reference method.
     */
    public void startAddReference() {
        ADD_REFERENCE.set(new Long(System.currentTimeMillis()));
    }
    
    /**
     * Stop timing the event tracker add reference method and collect 
     * the invocation time metrics.
     */
    public void endAddReference() {
        long endTime = System.currentTimeMillis();
        
        Long startTime = (Long)ADD_REFERENCE.get();
        
        if (startTime != null) {
            long invocationTime = endTime-startTime.longValue();
            _addMetrics.addInvocationTimeSynch(invocationTime);
        }
    }
    
    /**
     * Start timing the event tracker update reference method.
     */
    public void startUpdateReference() {
        UPDATE_REFERENCE.set(new Long(System.currentTimeMillis()));
    }
    
    /**
     * Stop timing the event tracker update reference method and collect 
     * the invocation time metrics.
     */
    public void endUpdateReference() {
        long endTime = System.currentTimeMillis();
        
        Long startTime = (Long)UPDATE_REFERENCE.get();
        
        if (startTime != null) {
            long invocationTime = endTime-startTime.longValue();
            _updateMetrics.addInvocationTimeSynch(invocationTime);
        }
    }
    
    /**
     * Start timing the event tracker delete reference method.
     */
    public void startDeleteReference() {
        DELETE_REFERENCE.set(new Long(System.currentTimeMillis()));        
    }
    
    /**
     * Stop timing the event tracker delete reference method and collect 
     * the invocation time metrics.
     */
    public void endDeleteReference() {
        long endTime = System.currentTimeMillis();
        
        Long startTime = (Long)DELETE_REFERENCE.get();
        
        if (startTime != null) {
            long invocationTime = endTime-startTime.longValue();
            _deleteMetrics.addInvocationTimeSynch(invocationTime);
        }        
    }
    
    /**
     * Start timing the event tracker get referenced event streams method.
     */
    public void startGetReferencedEventStreams() {
        GET_REFERENCE.set(new Long(System.currentTimeMillis()));              
    }
    
    /**
     * Stop timing the event tracker get referenced event streams method 
     * and collect the invocation time metrics.
     */
    public void endGetReferencedEventStreams() {
        long endTime = System.currentTimeMillis();
        
        Long startTime = (Long)GET_REFERENCE.get();
        
        if (startTime != null) {
            long invocationTime = endTime-startTime.longValue();
            _getMetrics.addInvocationTimeSynch(invocationTime);
        }          
    }

}
