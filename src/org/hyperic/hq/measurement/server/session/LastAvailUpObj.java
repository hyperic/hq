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

package org.hyperic.hq.measurement.server.session;

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.measurement.shared.AvailState;

public class LastAvailUpObj {
    private static final LastAvailUpObj _only = new LastAvailUpObj();
    private Map _availState = new HashMap();
    
    private LastAvailUpObj() {
    }
    
    public static LastAvailUpObj getInst() {
        return _only;
    }

    public AvailState get(Integer id, AvailState defaultState) {
        synchronized (_availState) {
            AvailState rtn;
            if (null == (rtn = (AvailState)_availState.get(id))) {
                _availState.put(id, defaultState);
                return defaultState;
            }
            return rtn;
        }
    }

    public AvailState get(Integer id) {
        synchronized (_availState) {
            AvailState rtn;
            if (null == (rtn = (AvailState)_availState.get(id)))
                return null;
            return rtn;
        }
    }
    
    public void clear() {
        synchronized (_availState) {
            _availState.clear();
        }
    }
    
    public void put(Integer id, AvailState state) {
        synchronized (_availState) {
            _availState.put(id, state);
        }
    }
}
