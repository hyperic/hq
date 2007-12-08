/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import org.apache.commons.lang.StringEscapeUtils;
import org.hyperic.hq.livedata.FormatType;
import org.hyperic.hq.livedata.LiveDataFormatter;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.hq.plugin.system.NetConnectionData;
import org.hyperic.hq.plugin.system.NetstatData;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.i18n.MessageBundle;

public class NetstatFormatter
    implements LiveDataFormatter
{
    private static final MessageBundle BUNDLE = 
        MessageBundle.getBundle("org.hyperic.hq.livedata.Resources");
    private static final ConfigSchema EMPTY_SCHEMA = new ConfigSchema();
    
    public boolean canFormat(LiveDataCommand cmd, FormatType type) {
        return cmd.getCommand().equals("netstat");
    }

    public String format(LiveDataCommand cmd, FormatType type, 
                         ConfigResponse formatCfg, Object val)
    {
        NetstatData d = (NetstatData)val;
        
        if (type.equals(FormatType.TEXT))
            return formatText(formatCfg, d);
        else if (type.equals(FormatType.HTML)) 
            return formatHtml(formatCfg, d);
        throw new IllegalStateException("Unhandled format type [" + type + "]");
    }

    private String formatText(ConfigResponse cfg, NetstatData d) {
        return "Text stuff "+ d.toString();
    }
    
    private String h(String s) {
        return StringEscapeUtils.escapeHtml(s);
    }

    private String formatHtml(ConfigResponse cfg, NetstatData d) {
        StringBuffer r = new StringBuffer();
        
        r.append("<div class='netstat_livedata'>")
         .append("<table cellpadding='0' cellspacing='0'><thead><tr>")
         .append("<td>")
         .append(BUNDLE.format("formatter.netstat.head.proto"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.netstat.head.local"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.netstat.head.foreign"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.netstat.head.state"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.netstat.head.process"))
         .append("</td></tr></thead><tbody>");

        for (Iterator i=d.getConnections().iterator(); i.hasNext(); ) {
            NetConnectionData c = (NetConnectionData)i.next();
            String formattedState;
            String formattedProcessName;

             if (h(c.getFormattedState()) == null || h(c.getFormattedState()).trim().length() == 0)
                formattedState = "-";
              else
                formattedState = h(c.getFormattedState());

             if (h(c.getFormattedProcessName()) == null || h(c.getFormattedProcessName()).trim().length() == 0)
                formattedProcessName = "-";
              else
                formattedProcessName = h(c.getFormattedProcessName());

            r.append("<tr><td>")
             .append(c.getProtocol())
             .append("</td><td>")
             .append(h(c.getFormattedLocalAddress()))
             .append("</td><td>")
             .append(h(c.getFormattedRemoteAddress()))
             .append("</td><td>")
             .append(formattedState)
             .append("</td><td>")
             .append(formattedProcessName)
             .append("</td></tr>");
        }
        r.append("</tbody></table></div>");
        
        return r.toString();
    }

    public ConfigSchema getConfig(LiveDataCommand cmd) {
        return EMPTY_SCHEMA;
    }

    public String getDescription() {
        return BUNDLE.format("formatter.netstat.desc");
    }

    public String getName() {
        return BUNDLE.format("formatter.netstat.name");
    }

    public String getId() {
        return "netstat";
    }
}
