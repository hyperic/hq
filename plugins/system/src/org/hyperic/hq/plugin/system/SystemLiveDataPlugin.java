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
import org.hyperic.hq.product.ProcessControlPlugin;
import org.hyperic.hq.product.SigarMeasurementPlugin;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.util.config.ConfigResponse;
import com.thoughtworks.xstream.XStream;

public class SystemLiveDataPlugin extends LiveDataPlugin {

    public static final String PROP_PID        = "process.pid";
    public static final String PROP_SIGNAL     = "process.signal";
    public static final String PROP_FILE       = "read.file";
    public static final String PROP_OFFSET     = "read.offset";
    public static final String PROP_NUMBYTES   = "read.numBytes";

    private static final String CMD_READ       = "read";
    private static final String CMD_CPUINFO    = "cpuinfo";
    private static final String CMD_CPU        = "cpu";
    private static final String CMD_CPUPERC    = "cpuperc";
    private static final String CMD_DF         = "df";
    private static final String CMD_TOP        = "top";
    private static final String CMD_PROCESS    = "process";
    private static final String CMD_KILL       = "kill";
    private static final String CMD_NETSTAT    = "netstat";
    private static final String CMD_IFCONFIG   = "ifconfig";
    private static final String CMD_WHO        = "who";

    private static final String _COMMANDS[] = {
        CMD_READ,
        CMD_CPUINFO,
        CMD_CPU,
        CMD_CPUPERC,
        CMD_DF,
        CMD_TOP,
        CMD_PROCESS,
        CMD_KILL,
        CMD_NETSTAT,
        CMD_IFCONFIG,
        CMD_WHO
    };

    private ReadData getReadData(ConfigResponse config) 
        throws PluginException
    {
        String file      = config.getValue(PROP_FILE);
        String sOffset   = config.getValue(PROP_OFFSET);
        String sNumBytes = config.getValue(PROP_NUMBYTES);
        long offset;
        int numBytes;
        
        if (file == null || sOffset == null || sNumBytes == null) {
            throw new PluginException("Must specify " + PROP_FILE + ", " +
                                      PROP_OFFSET + ", " + PROP_NUMBYTES);
        }
        
        try {
            offset = Long.parseLong(sOffset);
        } catch(NumberFormatException e) {
            throw new PluginException("Invalid offset: " + sOffset);
        }
        
        try {
            numBytes = Integer.parseInt(sNumBytes);
        } catch(NumberFormatException e) {
            throw new PluginException("Invalid numBytes: " + sNumBytes);
        }
        return ReadData.gather(file, offset, numBytes);
    }
    
    private long getPid(ConfigResponse config)
        throws PluginException {

        String pid = config.getValue(PROP_PID);
        if (pid == null) {
            throw new PluginException("Missing " + PROP_PID);
        }        
        try {
            return Long.parseLong(pid);
        } catch (NumberFormatException e) {
            throw new PluginException("Invalid pid: " + pid);
        }
    }

    public Object getData(String command, ConfigResponse config)
        throws PluginException
    {
        Sigar sigar = new Sigar();

        try {
            if (command.equals(CMD_READ)) {
                return getReadData(config);
            } else if (command.equals(CMD_CPUINFO)) {
                return sigar.getCpuInfoList();
            } else if (command.equals(CMD_CPU)) {
                return sigar.getCpuList();
            } else if (command.equals(CMD_CPUPERC)) {
                return sigar.getCpuPercList();
            } else if (command.equals(CMD_DF)) {
                return DfData.gather(sigar);
            } else if (command.equals(CMD_TOP)) {
                String filter =
                    config.getValue(SigarMeasurementPlugin.PTQL_CONFIG);
                return TopData.gather(sigar, filter);
            } else if (command.equals(CMD_PROCESS)) {
                return ProcessDetailData.gather(sigar, getPid(config));
            } else if (command.equals(CMD_KILL)) {
                String signame = config.getValue(PROP_SIGNAL);
                if (signame == null) {
                    signame = ProcessControlPlugin.SIGKILL;
                }

                long pid = getPid(config);
                int signal = ProcessControlPlugin.getSignal(signame);

                sigar.kill(pid, signal);
                return null;
            } else if (command.equals(CMD_NETSTAT)) {
                NetstatData data = new NetstatData();
                String flags = config.getValue("netstat.flags");
                if (flags != null) {
                    data.setFlags(flags);
                }
                data.populate(sigar);
                return data;
            } else if (command.equals(CMD_IFCONFIG)) {
                return IfconfigData.gather(sigar);
            } else if (command.equals(CMD_WHO)) {
                return sigar.getWhoList();
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
