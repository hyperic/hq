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

package org.hyperic.hq.product.servlet.mbean;

import org.hyperic.hq.product.servlet.filter.JMXSessionListener;

/**
 * Initialize Measurements for the current application.
 * This needs to be loaded on startup.
 *
 * @jmx:mbean
 */
public final class ContextInfo implements ContextInfoMBean {
    
    private String contextName;
    private String docBase;

    private JMXSessionListener sessionListener;
    
    private int requestCount;
    private int errorCount;
    
    private long totalTime;
    private int minTime = 0;
    private int maxTime;
    
    private int bytesSent;
    private int bytesReceived;

    // RT variables
    private String responseTimeLogDir = null;
    
    // If we are created, then the context has started
    private int available=1;

    public void setSessionListener(JMXSessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }
    
    /**
     * @jmx:managed-attribute
     */ 
    public int getAvailable() {
        // If we are started
        return available;
    }
    
    public void setAvailable(int available) {
        this.available=available;
    }
  
    /**
     * @jmx:managed-attribute
     */ 
    public long getTotalTime() {
        return totalTime;
    }

    /**
     * @jmx:managed-attribute
     */ 
    public int getMinTime() {
        return minTime;
    }

    /**
     * @jmx:managed-attribute
     */ 
    public int getMaxTime() {
        return maxTime;
    }

    
    /**
     * @jmx:managed-attribute
     */ 
    public int getAvgTime() {
        if(requestCount == 0) return 0;
        return (int) (totalTime/requestCount);
    }
    
    /**
     * @jmx:managed-attribute
     */ 
    public int getBytesSent() {
        return bytesSent;
    }

    /**
     * @jmx:managed-attribute
     */ 
    public int getBytesReceived() {
        return bytesReceived;
    }

    /**
     * @jmx:managed-attribute
     */ 
    public int getRequestCount() {
        return requestCount;
    }
    
    /**
     * @jmx:managed-attribute
     */ 
    public int getErrorCount() {
        return errorCount;
    }
    
    public synchronized void updateCounters(int bytesReceived, int bytesSent,
                                            int time, boolean iserror) {
        requestCount++;
        this.bytesReceived += bytesReceived;
        this.bytesSent += bytesSent;
        
        if(time > maxTime) maxTime = time;
        if(time < minTime || minTime == 0) minTime = time;
        totalTime += time;
        if(iserror) errorCount++;
    }

    /**
     * @jmx:managed-attribute
     */ 
    public int getSessionsCreated() {
        if(sessionListener == null) return 0;
        return sessionListener.getCreated();
    }
    
    /**
     * @jmx:managed-attribute
     */ 
    public int getSessionsDestroyed() {
        if(sessionListener == null) return 0;
        return sessionListener.getDestroyed();
    }
    
    /**
     * @jmx:managed-attribute
     */
    public String getDocBase() {
        return docBase;
    }

    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    /**
     * @jmx:managed-attribute
     */
    public String getContextName() {
        return contextName;
    }    

    /**
     * @jmx:managed-attribute
     */
    public String getResponseTimeLogDir() {
        return this.responseTimeLogDir;
    }

    /**
     * @jmx:managed-attribute
     */
    public void setResponseTimeLogDir(String logDir) {
        this.responseTimeLogDir = logDir;
    }
}
