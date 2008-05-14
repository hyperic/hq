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

package org.hyperic.hq.agent.commands;

import org.hyperic.hq.agent.AgentRemoteValue;

public class AgentUpgrade_args extends AgentRemoteValue {
 
    private static final String BUNDLE_FILE_ARG = "bundleFile";
    private static final String DESTINATION_DIR_ARG = "destinationDir";

    public AgentUpgrade_args(String bundleFile, String destinationDir) {
        super();
        setValue(BUNDLE_FILE_ARG, bundleFile);
        setValue(DESTINATION_DIR_ARG, destinationDir);
    }
    
    public AgentUpgrade_args(AgentRemoteValue args) {
        super();
        // copy the values from the AgentRemoteValue object
        setValue(BUNDLE_FILE_ARG, getValue(BUNDLE_FILE_ARG));
        setValue(DESTINATION_DIR_ARG, getValue(DESTINATION_DIR_ARG));       
    }

    public String getBundleFile() {
        return getValue(BUNDLE_FILE_ARG);
    }
    
    public String getDestination() {
        return getValue(DESTINATION_DIR_ARG);
    }
}
