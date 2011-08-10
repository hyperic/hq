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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.hyperic.hq.agent.AgentRemoteValue;

public class AgentRemoveFileData_args extends AgentRemoteValue {

    public AgentRemoveFileData_args(Collection<String> filenames) {
        int i=0;
        for (final String filename : filenames) {
            super.setValue("file" + i++, filename);
        }
    }

    public AgentRemoveFileData_args(AgentRemoteValue args) {
        final Set<String> keys = args.getKeys();
        for (final String key : keys) {
            super.setValue(key, args.getValue(key));
        }
    }

    public Collection<String> getFiles() {
        final Collection<String> rtn = new ArrayList<String>();
        final Set<String> keys = getKeys();
        for (final String key : keys) {
            rtn.add(getValue(key));
        }
        return rtn;
    }

}
