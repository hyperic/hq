/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2011], VMWare, Inc.
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

package org.hyperic.hq.agent.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hyperic.hq.agent.AgentRemoteValue;

public class FileRemoval_result extends AgentRemoteValue {
    
    public FileRemoval_result(Map<String, Boolean> fileRemovalMap) {
        for (final Entry<String, Boolean> entry : fileRemovalMap.entrySet()) {
            super.setValue(entry.getKey(), entry.getValue().toString());
        }
    }
    
    public FileRemoval_result(AgentRemoteValue res) {
        final Set<String> keys = res.getKeys();
        for (final String key : keys) {
            super.setValue(key, res.getValue(key));
        }
    }

    public Map<String, Boolean> getResult() {
        final HashMap<String, Boolean> rtn = new HashMap<String, Boolean>();
        final Set<String> keys = super.getKeys();
        for (final String key : keys) {
            rtn.put(key, new Boolean(super.getValue(key)));
        }
        return rtn;
    }

}
