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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.UncategorizedSQLException;

public class TimingListenerWrapper<T extends Zevent>  implements ZeventListener<T> {
    private final Log log = LogFactory.getLog(TimingListenerWrapper.class);
    private ZeventListener _target;
    private long           _maxTime   = 0;
    private long           _totTime   = 0;
    private long           _numEvents = 0;

    public TimingListenerWrapper(ZeventListener target) {
        _target = target;
    }

    public void processEvents(List events) {
        long time, start = System.currentTimeMillis();
        try {
            _target.processEvents(events);
        } catch (UncategorizedSQLException ex) {
            log.debug("UncategorizedSQLException caught.", ex);
        } finally {
            time = System.currentTimeMillis() - start;
            if (time > _maxTime) {
                _maxTime = time;
            }
            _totTime   += time;
            _numEvents += events.size();
        }
    }

    public long getMaxTime() {
        return _maxTime;
    }

    public double getAverageTime() {
        if (_numEvents == 0) {
            return Double.NaN;
        }
        return (double)_totTime / (double)_numEvents;
    }

    public long getNumEvents() {
        return _numEvents;
    }

    @Override
    public boolean equals(Object obj) {
        return _target.equals(obj);
    }

    @Override
    public int hashCode() {
        return _target.hashCode();
    }

    @Override
    public String toString() {
        return _target.toString();
    }
    
    public ZeventListener getTarget() {
        return _target;
    }
}
