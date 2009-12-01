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

package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.data.TrackEventReport;
import org.hyperic.hq.product.TrackEvent;

import org.hyperic.lather.LatherRemoteException;

/**
 * Arguments for sending log file messages
 */
public class TrackSend_args extends SecureAgentLatherValue {
    private static final String PROP_TIMESTAMP = "timestamps";
    private static final String PROP_LEVEL     = "levels";
    private static final String PROP_MESSAGE   = "messages";
    private static final String PROP_ID        = "ids";
    private static final String PROP_TYPE      = "types";
    private static final String PROP_SOURCE    = "sources";
    
    public static final int TYPE_LOG    = 1;
    public static final int TYPE_CONFIG = 2;

    public TrackSend_args() {
        super();
    }

    public int getType() {
        return super.getIntValue(PROP_TYPE);
    }

    public void setType(int type) {
        super.setIntValue(PROP_TYPE, type);
    }

    public TrackEventReport getEvents() throws LatherRemoteException {
        double[] tstamps = this.getDoubleList(PROP_TIMESTAMP);
        int[]    levels  = this.getIntList(PROP_LEVEL);
        String[] msgs    = this.getStringList(PROP_MESSAGE);
        String[] srcs    = this.getStringList(PROP_SOURCE);
        int[]    ids     = this.getIntList(PROP_ID);
        int[]    types   = this.getIntList(PROP_TYPE);
        
        TrackEventReport report = new TrackEventReport();  
        
        if (tstamps.length != levels.length || levels.length != msgs.length ||
            msgs.length    != srcs.length   || srcs.length   != ids.length  ||
            ids.length     != types.length) {
            throw new LatherRemoteException("TrackEvent report mismatch");
        }
        
        for (int i = 0; i < tstamps.length; i++) {      
            TrackEvent event = 
                new TrackEvent(new AppdefEntityID(types[i], ids[i]),
                               (long) tstamps[i],
                               levels[i],
                               srcs[i],
                               msgs[i]);
            report.addEvent(event);
        }

        return report;
    }

    public void setEvents(TrackEventReport eventsReport) {
        TrackEvent[] events = eventsReport.getEvents();
        
        for (int i = 0; i < events.length; i++) {
            TrackEvent event = events[i];
            if (event.getAppdefId() == null)
                throw new IllegalArgumentException(
                    "AppdefEntityID for event cannot be NULL");

            super.addDoubleToList(PROP_TIMESTAMP, event.getTime());
            super.addIntToList(PROP_LEVEL, event.getLevel());
            super.addStringToList(PROP_MESSAGE, event.getMessage());
            super.addStringToList(PROP_SOURCE, event.getSource());
        
            super.addIntToList(PROP_ID, event.getAppdefId().getID());
            super.addIntToList(PROP_TYPE, event.getAppdefId().getType());
        }
    }

    public void validate() throws LatherRemoteException {
    }
}
