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

import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.hyperic.hq.livedata.FormatType;
import org.hyperic.hq.livedata.LiveDataFormatter;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.hq.plugin.system.ProcessData;
import org.hyperic.hq.plugin.system.TopData;
import org.hyperic.hq.plugin.system.UptimeData;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.CurrentProcessSummary;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Swap;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.i18n.MessageBundle;

public class TopFormatter
    implements LiveDataFormatter
{
    private static final MessageBundle BUNDLE = 
        MessageBundle.getBundle("org.hyperic.hq.livedata.Resources");
    private static final ConfigSchema EMPTY_SCHEMA = new ConfigSchema();
    
    public boolean canFormat(LiveDataCommand cmd, FormatType type) {
        return cmd.getCommand().equals("top");
    }

    public String format(LiveDataCommand cmd, FormatType type, 
                         ConfigResponse formatCfg, Object val)
    {
        TopData td = (TopData)val;
        
        if (type.equals(FormatType.TEXT))
            return formatText(formatCfg, td);
        else if (type.equals(FormatType.HTML)) 
            return formatHtml(formatCfg, td);
        throw new IllegalStateException("Unhandled format type [" + type + "]");
    }

    private String formatText(ConfigResponse cfg, TopData td) {
        return "Text stuff " + td;
    }

    private String h(String s) {
        return StringEscapeUtils.escapeHtml(s);
    }
    
    private String formatHtml(ConfigResponse cfg, TopData t) {
        StringBuffer r = new StringBuffer();
        DateFormat dateFmt = 
            DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        UptimeData utd = t.getUptime();
        
        r.append("<div class='top_livedata'><b>Time</b>: ")
         .append(h(dateFmt.format(new Date(utd.getTime()))))
         .append(" up ")
         .append(h(utd.getFormattedUptime()))
         .append("<br/>");
        
        r.append("<b>Load Avg</b>: ")
         .append(h(utd.getFormattedLoadavg()))
         .append("<br/>");

        CpuPerc cpu = t.getCpu();
        r.append("<b>CPU States</b>: ")
         .append(h(BUNDLE.format("formatter.top.cpuStates",
                                 CpuPerc.format(cpu.getUser()),
                                 CpuPerc.format(cpu.getSys()),
                                 CpuPerc.format(cpu.getNice()),
                                 CpuPerc.format(cpu.getWait()),
                                 CpuPerc.format(cpu.getIdle()))))
         .append("<br/>");

        Mem mem = t.getMem();
        r.append("<b>Mem</b>: ")
         .append(h(BUNDLE.format("formatter.top.memUse",
                                 (mem.getTotal() / 1024) + "k",
                                 (mem.getUsed() / 1024) + "k",
                                 (mem.getFree() / 1024) + "k")))
         .append("<br/>");
        
        Swap swap = t.getSwap();
        r.append("<b>Swap</b>: ")
         .append(h(BUNDLE.format("formatter.top.memUse",
                                 (swap.getTotal() / 1024) + "k",
                                 (swap.getUsed() / 1024) + "k",
                                 (swap.getFree() / 1024) + "k")))
         .append("<br/>");
        
        
        CurrentProcessSummary ps = t.getCurrentProcessSummary();
        r.append("<b>Processes</b>: ")
         .append(h(BUNDLE.format("formatter.top.procSummary", 
                                 "" + ps.getTotal(), "" + ps.getRunning(), 
                                 "" + ps.getSleeping(), "" + ps.getStopped(), 
                                 "" + ps.getZombie())))
         .append("<br/>");
        
        
        r.append("<table><thead><tr><td>")
         .append(BUNDLE.format("formatter.top.proc.pid"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.top.proc.user"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.top.proc.stime"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.top.proc.size"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.top.proc.rss"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.top.proc.share"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.top.proc.state"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.top.proc.time"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.top.proc.cpu"))
         .append("</td><td>")
         .append(BUNDLE.format("formatter.top.proc.name"))
         .append("</td></tr></thead><tbody>");
         
        for (Iterator i=t.getProcesses().iterator(); i.hasNext(); ) {
            ProcessData d = (ProcessData)i.next();
            r.append("<tr><td>").append(d.getPid()).append("</td>")
             .append("<td>").append(d.getOwner()).append("</td>")    
             .append("<td>").append(d.getFormattedStartTime()).append("</td>") 
             .append("<td>").append(d.getFormattedSize()).append("</td>")
             .append("<td>").append(d.getFormattedResident()).append("</td>")  
             .append("<td>").append(d.getFormattedShare()).append("</td>")      
             .append("<td>").append(d.getState()).append("</td>")                           
             .append("<td>").append(d.getFormattedCpuTotal()).append("</td>")                           
             .append("<td>").append(d.getFormattedCpuPerc()).append("</td>")                           
             .append("<td>").append(h(d.getBaseName())).append("</td></tr>");                           
        }
        
        r.append("</tbody></table></div>");
        return r.toString();
    }

    public ConfigSchema getConfig(LiveDataCommand cmd) {
        return EMPTY_SCHEMA;
    }

    public String getDescription() {
        return BUNDLE.format("formatter.top.desc");
    }

    public String getName() {
        return BUNDLE.format("formatter.top.name");
    }

    public String getId() {
        return "top";
    }
}
