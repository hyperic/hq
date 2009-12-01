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

import java.io.IOException;
import javax.servlet.ServletException;

/**
 * Needs to be configured in each web application.
 *
 * @jmx:mbean
 */
public final class ServletInfo
    implements ServletInfoMBean
{
    private int requestCount;
    private int errorCount;
    private int minTime = 0;
    private int maxTime;
    
    private long totalTime;
    
    private String servletPath;
    
    // Defaults to 1, when the servlet is created.
    // There is no notification that a servlet went down, but
    // we can set it to 0 if a request fails.
    private int available = 1;

    /**
     * @jmx:managed-attribute
     */ 
    public int getAvailable() {
        // if we are started
        return available;
    }
    
    public void setAvailable(int available) {
        this.available = available;
    }
    
    /**
     * @jmx:managed-attribute
     */ 
    public String getServletPath() {
        return servletPath;
    }

    /**
     * @jmx:managed-attribute
     */ 
    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
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
        return (int)(totalTime/requestCount);
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
    
    public synchronized void updateTimes(int time, boolean isError, int clIn,
                                         int clOut)
        throws IOException, ServletException
    {
        if(isError)  
            errorCount++;
        else 
            available = 1;
        
        requestCount++;

        if(time > maxTime ) maxTime = time;
        if(time < minTime || minTime == 0) minTime = time;
        totalTime += time;
    }
}
