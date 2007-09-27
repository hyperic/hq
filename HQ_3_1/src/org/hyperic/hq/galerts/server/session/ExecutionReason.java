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

package org.hyperic.hq.galerts.server.session;

import java.util.Collections;
import java.util.List;

import org.hyperic.hq.events.AlertAuxLog;

public class ExecutionReason {
    private final String             _shortReason;
    private final String             _longReason;
    private final List               _auxLogs;
    private final GalertDefPartition _partition;
    
    public ExecutionReason(String shortReason, String longReason, List auxLogs,
                           GalertDefPartition partition) 
    {
        _shortReason = shortReason;
        _longReason  = longReason;
        _auxLogs     = auxLogs;
        _partition   = partition;
    }
    
    public String getShortReason() {
        return _shortReason;
    }
    
    public String getLongReason() {
        return _longReason;
    }
    
    /**
     * Returns a list of {@link AlertAuxLog}s
     */
    public List getAuxLogs() {
        return Collections.unmodifiableList(_auxLogs);
    }
    
    public GalertDefPartition getPartition() {
        return _partition;
    }
    
    public String toString() {
        return "Execution Reason (" + _partition + "):\n" + 
            "Short = [" + _shortReason + "]\n" + 
            "Long = \n" + 
            "[" + _longReason + "]" + "\n";
    }
}
