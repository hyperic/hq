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

package org.hyperic.hq.bizapp.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hyperic.hq.measurement.server.session.AvailabilityManagerEJBImpl;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;

public class MeasurementSeparator {
    private final AvailabilityManagerLocal _availMan =
        AvailabilityManagerEJBImpl.getOne();
    private List _orderedAvailIds;
    private Object[] _mids;
    private Object[] _avIds;
    public MeasurementSeparator() {
        _orderedAvailIds = _availMan.getAllAvailIds();
    }
    public Integer[] getAvIds() {
        Integer[] rtn = new Integer[_avIds.length];
        System.arraycopy(_avIds, 0, rtn, 0, _avIds.length);
        return rtn;
    }
    public Integer[] getMids() {
        Integer[] rtn = new Integer[_mids.length];
        System.arraycopy(_mids, 0, rtn, 0, _mids.length);
        return rtn;
    }
    public void set(Integer[] mids) {
        List midList = new ArrayList(mids.length);
        List aidList = new ArrayList(mids.length);
        for (int i=0; i<mids.length; i++) {
            if (mids[i] == null) {
                continue;
            } else if (isAvailMeas(mids[i])) {
                aidList.add(mids[i]);
            } else {
                midList.add(mids[i]);
            }
        }
        _mids = midList.toArray();
        _avIds = aidList.toArray();
    }
    public boolean isAvailMeas(Integer id) {
        int res =
            Collections.binarySearch(_orderedAvailIds, id);
        if (res >= 0) {
            return true;
        }
        return false;
    }
}
