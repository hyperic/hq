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

package org.hyperic.hq.zevents;

import java.io.ObjectStreamException;
import java.util.Date;

public class HeartBeatZevent extends Zevent {

    static {
        ZeventManager.getInstance().
            registerEventClass(HeartBeatZevent.class);
    }

    /**
     * There is only ever one instance of this source id so we can use object 
     * identity for checking equality.
     */
    public static class HeartBeatZeventSource
        implements ZeventSourceId
    {
        private static final long serialVersionUID = -398257689585772094L;
        
        private static final HeartBeatZeventSource INSTANCE = new HeartBeatZeventSource();
        
        private HeartBeatZeventSource() {
        }
        
        public static HeartBeatZeventSource getInstance() {
            return INSTANCE;
        }
                
        public String toString() {
            return "Heart Beat";
        }
        
        // necessary to maintain the singleton across JVMs
        private Object readResolve() throws ObjectStreamException {
            return HeartBeatZeventSource.getInstance();
        }
        
    }

    private static class HeartBeatZeventPayload
        implements ZeventPayload
    {
        private final long _timestamp;

        public HeartBeatZeventPayload(Date beat) {
            _timestamp = beat.getTime();
        }

        public long getTimestamp() {
            return _timestamp;
        }
        
        public String toString() {
            return String.valueOf(_timestamp);
        }
    }

    public long getTimestamp() {
        return ((HeartBeatZeventPayload)getPayload()).getTimestamp();
    }
    
    public HeartBeatZevent(Date beat) {
        super(HeartBeatZeventSource.getInstance(),
              new HeartBeatZeventPayload(beat));
    }
}
