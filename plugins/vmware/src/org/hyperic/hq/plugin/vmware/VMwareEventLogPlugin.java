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

package org.hyperic.hq.plugin.vmware;

import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.sigar.FileInfo;
import org.hyperic.util.config.ConfigResponse;

public class VMwareEventLogPlugin extends LogFileTailPlugin {

    private static final String[] LOG_LEVELS = {
        "error", //Error
        "warning", //Warning
        "info,question,answer", //Info
        "debug" //Debug
    };

    private VMwareEventLogParser parser;
    private String source;

    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);

        this.parser = new VMwareEventLogParser();

        //XXX name of the file is long and ugly
        this.source = "VM eventlog";
    }

    public TrackEvent processLine(FileInfo info, String line) {
        VMwareEventLogParser.Entry entry;

        try {
            entry = this.parser.parse(line);
            if (entry == null) {
                return null;
            }
        } catch (Exception e) {
            getLog().error("Error parsing line: '" + line + "'", e);
            return null;
        }

        return newTrackEvent(entry.time,
                             entry.type,
                             this.source,
                             entry.subject + " - " + entry.body);
    }

    public String getDefaultLogFile(String installPath) {
        return "event-${vm.name}.vmx.log"; //XXX lousy default
    }
}
