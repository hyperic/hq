/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

package org.hyperic.hq.measurement.shared;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.LoggableInterface;
import org.hyperic.hq.events.ResourceEventInterface;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.TrackEvent;

/**
 * This event type represents an entry in the resource log
 */
public class ResourceLogEvent extends AbstractEvent
    implements java.io.Serializable, ResourceEventInterface, LoggableInterface {
    
    private static final long serialVersionUID = 2981297729726736244L;

    private static Map levelStrs = new HashMap();
    
    static {
        levelStrs.put(new Integer(-1),                            "ANY");
        levelStrs.put(new Integer(LogTrackPlugin.LOGLEVEL_ERROR), "ERR");
        levelStrs.put(new Integer(LogTrackPlugin.LOGLEVEL_WARN),  "WRN");
        levelStrs.put(new Integer(LogTrackPlugin.LOGLEVEL_INFO),  "INF");
        levelStrs.put(new Integer(LogTrackPlugin.LOGLEVEL_DEBUG), "DBG");
    }

    private AppdefEntityID resource;
    private String logSrc;
    private String logMsg;
    private int logLevel;

    public ResourceLogEvent(TrackEvent te) {
        super.setTimestamp(te.getTime());
        super.setInstanceId(te.getAppdefId().getId());
        this.setResource(te.getAppdefId());
        this.setSource(te.getSource());
        this.setMessage(te.getMessage());
        this.setLevel(te.getLevel());
    }

    public AppdefEntityID getResource() {
        return this.resource;
    }
    
    public void setResource(AppdefEntityID aid) {
        this.resource = aid;
    }
    
    public String getMessage() {
        return this.logMsg;
    }
    
    public void setMessage(String msg) {
        this.logMsg = msg;
    }
    
    public int getLevel() {
        return this.logLevel;
    }

    private void setLevel(int level) {
        // Verify that we are setting a valid level
        if (levelStrs.containsKey(new Integer(level)))
            this.logLevel = level;
        else
            throw new IllegalArgumentException(
                "Level " + level + " is not a valid log level");
    }

    public String getLevelString() {
        return getLevelString(this.logLevel);
    }

    public String getSource() {
        return this.logSrc;
    }
    
    public void setSource(String src) {
        this.logSrc = src;
    }
    
    public String toString() {
        return this.logSrc + ": " + this.getMessage();
    }
    
    public static final String getLevelString(int level) {
        Integer key = new Integer(level);
        return (String) levelStrs.get(key);
    }

    public String getSubject() {
        return getSource();
    }
}
