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
package org.hyperic.hq.livedata.formatters;

import java.util.Iterator;

import org.hyperic.hq.livedata.FormatType;
import org.hyperic.hq.livedata.LiveDataFormatter;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.hq.plugin.system.IfconfigData;
import org.hyperic.hq.plugin.system.NetInterfaceData;
import org.hyperic.sigar.CpuInfo;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.NetInterfaceStat;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.i18n.MessageBundle;

public class IfconfigFormatter
    implements LiveDataFormatter
{
    private static final MessageBundle BUNDLE = 
        MessageBundle.getBundle("org.hyperic.hq.livedata.Resources");
    private static final ConfigSchema EMPTY_SCHEMA = new ConfigSchema();
    
    public boolean canFormat(LiveDataCommand cmd, FormatType type) {
        return cmd.getCommand().equals("ifconfig");
    }

    public String format(LiveDataCommand cmd, FormatType type, 
                         ConfigResponse formatCfg, Object val)
    {
        IfconfigData d = (IfconfigData)val;
        
        if (type.equals(FormatType.TEXT))
            return formatText(formatCfg, d);
        else if (type.equals(FormatType.HTML)) 
            return formatHtml(formatCfg, d);
        throw new IllegalStateException("Unhandled format type [" + type + "]");
    }

    private String formatText(ConfigResponse cfg, IfconfigData d) {
        return "Text stuff "+ d;
    }

    private String formatHtml(ConfigResponse cfg, IfconfigData d) {
        StringBuffer r = new StringBuffer();
        
        r.append("<div><table><thead><tr>");
        r.append("<td>")
         .append(BUNDLE.format("formatter.ifconfig.head.name"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.ifconfig.head.type"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.ifconfig.head.hwaddr"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.ifconfig.head.addr"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.ifconfig.head.broadcast"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.ifconfig.head.netmask"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.ifconfig.head.mtu"))
         .append("</td>")
         .append("</tr></thead><tbody>");

        for (Iterator i=d.getInterfaces().iterator(); i.hasNext(); ) {
            NetInterfaceData nd = (NetInterfaceData)i.next();
            NetInterfaceConfig nc = nd.getConfig();
            
            r.append("<tr><td>")
             .append(nc.getName())
             .append("</td><td>")
             .append(nc.getType())
             .append("</td><td>")
             .append(nc.getHwaddr())
             .append("</td><td>")
             .append(nc.getAddress())
             .append("</td><td>")
             .append(nc.getBroadcast())
             .append("</td><td>")
             .append(nc.getNetmask())
             .append("</td><td>")
             .append(nc.getMtu())
             .append("</td></tr>");
        }

        r.append("<table><thead><tr>");
        r.append("<td>")
         .append(BUNDLE.format("formatter.ifconfig.head.name"))
         .append("</td>")
         .append("<td colspan='6'>")
         .append(BUNDLE.format("formatter.ifconfig.head.tx"))
         .append("</td>")
         .append("<td colspan='7'>")
         .append(BUNDLE.format("formatter.ifconfig.head.rx"))
         .append("</td></tr><tr><td></td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.bytes"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.dropped"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.errors"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.frame"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.overruns"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.packets"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.bytes"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.carrier"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.collisions"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.dropped"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.errors"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.overruns"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.ifconfig.head.packets"))
         .append("</td></tr></thead><tbody>");

        for (Iterator i=d.getInterfaces().iterator(); i.hasNext(); ) {
            NetInterfaceData nd = (NetInterfaceData)i.next();
            NetInterfaceStat ns = (NetInterfaceStat)nd.getStat(); 
            
            r.append("<tr><td>")
             .append(nd.getConfig().getName())
             .append("</td><td>")
             .append(ns.getRxBytes())
             .append("</td><td>")
             .append(ns.getRxDropped())
             .append("</td><td>")
             .append(ns.getRxErrors())
             .append("</td><td>")
             .append(ns.getRxFrame())
             .append("</td><td>")
             .append(ns.getRxOverruns())
             .append("</td><td>")
             .append(ns.getRxPackets())
             .append("</td><td>")
             .append(ns.getTxBytes())
             .append("</td><td>")
             .append(ns.getTxCarrier())
             .append("</td><td>")
             .append(ns.getTxCollisions())
             .append("</td><td>")
             .append(ns.getTxDropped())
             .append("</td><td>")
             .append(ns.getTxErrors())
             .append("</td><td>")
             .append(ns.getTxOverruns())
             .append("</td><td>")
             .append(ns.getTxPackets())
             .append("</td></tr>");
        }
        
        r.append("</tbody></table></div>");
        return r.toString();
    }

    public ConfigSchema getConfig(LiveDataCommand cmd) {
        return EMPTY_SCHEMA;
    }

    public String getDescription() {
        return BUNDLE.format("formatter.ifconfig.desc");
    }

    public String getName() {
        return BUNDLE.format("formatter.ifconfig.name");
    }

    public String getId() {
        return "ifconfig";
    }
}
