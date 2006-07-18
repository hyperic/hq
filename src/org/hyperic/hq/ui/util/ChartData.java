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

package org.hyperic.hq.ui.util;

import java.util.List;
import java.util.Map;

/**
 * A simple bean that stores data for rendering a chart.
 */
public class ChartData {
    
    //-------------------------------------instance variables
    
    private Boolean showAvg;
    private Boolean showLow;
    private Boolean showPeak;
    private Boolean showReq;
    private List summaries;
    private Map segments;

    //-------------------------------------constructors

    public ChartData() {
        super();
    }

    //-------------------------------------public methods

    public Map getSegments() {
        return segments;
    }

    public void setSegments(Map m) {
        segments = m;
    }

    public Boolean getShowAvg() {
        return showAvg;
    }

    public void setShowAvg(Boolean b) {
        showAvg = b;
    }

    public Boolean getShowLow() {
        return showLow;
    }

    public void setShowLow(Boolean b) {
        showLow = b;
    }

    public Boolean getShowPeak() {
        return showPeak;
    }

    public void setShowPeak(Boolean b) {
        showPeak = b;
    }

    public Boolean getShowReq() {
        return showReq;
    }

    public void setShowReq(Boolean b) {
        showReq = b;
    }

    public List getSummaries() {
        return summaries;
    }

    public void setSummaries(List l) {
        summaries = l;
    }
}
