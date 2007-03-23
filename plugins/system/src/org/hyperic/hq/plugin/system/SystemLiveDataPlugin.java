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

package org.hyperic.hq.plugin.system;

import org.hyperic.hq.product.LiveDataPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigResponse;
import com.thoughtworks.xstream.XStream;

public class SystemLiveDataPlugin extends LiveDataPlugin {

    private static final String CMD_CPUINFO    = "cpuinfo";
    private static final String CMD_CPU        = "cpu";
    private static final String CMD_CPUPERC    = "cpuperc";
    private static final String CMD_FILESYSTEM = "filesystem";

    private static final String _COMMANDS[] = {
        CMD_CPUINFO,
        CMD_CPU,
        CMD_CPUPERC,
        CMD_FILESYSTEM
    };

    public Object getData(String command, ConfigResponse config)
        throws PluginException
    {
        Sigar sigar = new Sigar();

        try {
            if (command.equals(CMD_CPUINFO)) {
                return sigar.getCpuInfoList();
            } else if (command.equals(CMD_CPU)) {
                return sigar.getCpuList();
            } else if (command.equals(CMD_CPUPERC)) {
                return sigar.getCpuPercList();
            } else if (command.equals(CMD_FILESYSTEM)) {
                return sigar.getFileSystemList();
            } else {
                throw new PluginException("Unknown command '" + command + "'");
            }
        } catch (SigarException e) {
            throw new PluginException("Error getting system data", e);
        }
    }

    public String[] getCommands() {
        return _COMMANDS;
    }

    public static void main(String[] args) throws Exception {
        SystemLiveDataPlugin p = new SystemLiveDataPlugin();
        ConfigResponse emtpy = new ConfigResponse();
        for (int i = 0; i < _COMMANDS.length; i++) {
            System.out.println("Running command " + _COMMANDS[i]);
            Object o = p.getData(_COMMANDS[i], emtpy);

            XStream xstream = new XStream();
            System.out.println(xstream.toXML(o));
        }
    }
}
