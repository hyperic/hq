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

package org.hyperic.hq.events;

import java.util.List;
import java.util.ResourceBundle;

import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.util.HypericEnum;

public class EventLogStatus
    extends HypericEnum
{
    private static final String BUNDLE = "org.hyperic.hq.events.Resources";
    
    public static final EventLogStatus ERROR = 
        new EventLogStatus(LogTrackPlugin.LOGLEVEL_ERROR,
                           "error", "eventLog.status.error");
    public static final EventLogStatus WARN = 
        new EventLogStatus(LogTrackPlugin.LOGLEVEL_WARN,
                           "warn", "eventLog.status.warn");
    public static final EventLogStatus INFO = 
        new EventLogStatus(LogTrackPlugin.LOGLEVEL_INFO,
                           "info", "eventLog.status.info");
    public static final EventLogStatus DEBUG = 
        new EventLogStatus(LogTrackPlugin.LOGLEVEL_DEBUG,
                           "debug", "eventLog.status.debug");
    // Note:  We use a code other than -1 for ANY, since
    //        we want it to sort AFTER debug.  The values in the DB are
    //        stored as 'ANY', but this object represents a filter.
    public static final EventLogStatus ANY = 
        new EventLogStatus(10, "any", "eventLog.status.any");

    private EventLogStatus(int code, String desc, String localeProp) {
        super(code, desc, localeProp, 
              ResourceBundle.getBundle(BUNDLE)); 
    }
    
    public static List getAll() {
        return getAll(EventLogStatus.class);
    }
    
    public static EventLogStatus findByCode(int code) {
        return (EventLogStatus)findByCode(EventLogStatus.class, code);
    }
}
