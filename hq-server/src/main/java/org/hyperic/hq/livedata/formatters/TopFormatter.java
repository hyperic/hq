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
import java.util.Arrays;
import java.util.Date;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.hyperic.hq.livedata.FormatType;
import org.hyperic.hq.livedata.LiveDataFormatter;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.hq.plugin.system.ProcessData;
import org.hyperic.hq.plugin.system.ProcessReport;
import org.hyperic.hq.plugin.system.TopData;
import org.hyperic.hq.plugin.system.TopReport;
import org.hyperic.hq.plugin.system.TopReport.TOPN_SORT_TYPE;
import org.hyperic.hq.plugin.system.UptimeData;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.ProcStat;
import org.hyperic.sigar.Swap;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.i18n.MessageBundle;

public class TopFormatter
 implements LiveDataFormatter
{
    private static final MessageBundle BUNDLE = MessageBundle.getBundle("org.hyperic.hq.livedata.Resources");
    private static final ConfigSchema EMPTY_SCHEMA = new ConfigSchema();

    public boolean canFormat(LiveDataCommand cmd, FormatType type) {
        return cmd.getCommand().equals("top");
    }

    public String format(LiveDataCommand cmd, FormatType type, ConfigResponse formatCfg, Object val) {
        TopData td = (TopData) val;

        if (type.equals(FormatType.TEXT)) {
            return formatText(formatCfg, td);
        } else if (type.equals(FormatType.HTML)) {
            return formatHtml(formatCfg, td);
        }
        throw new IllegalStateException("Unhandled format type [" + type + "]");
    }

    private String formatText(ConfigResponse cfg, TopData td) {
        return "Text stuff " + td;
    }

    private static String h(String s) {
        return StringEscapeUtils.escapeHtml(s);
    }

    public static String formatHtml(TopReport t, TOPN_SORT_TYPE sortType, int numberOfProcessesToShow) {
        StringBuilder buf = new StringBuilder();
        buf.append("<div id='topn_result_cont'>\n");
        buf.append("<div id='result' style='border: 1px solid #7BAFFF;'>")
                .append("<div class='fivepad' style='background:#efefef;'>").append("<b>Time</b>: ")
                .append(h(t.getUpTime())).append("<br/>");

        buf.append("<b>CPU States</b>: ").append(h(t.getCpu().split(":")[1]).replace("{", "").replace("}", ""))
                .append("<br/>");

        buf.append("<b>Mem</b>: ").append(h(t.getMem().split(":")[1]).replace("{", "").replace("}", ""))
                .append("<br/>");

        buf.append("<b>Swap</b>: ").append(h(t.getSwap().split(":")[1]).replace("{", "").replace("}", ""))
                .append("<br/>");

        buf.append("<b>Processes</b>: ").append(h(t.getProcStat().replace("{", "").replace("}", "")))
                .append("<br/><br/>");

        buf.append("</div>\n<table style='table-layout:auto' cellpadding='0' cellspacing='0' width='100%'><thead><tr><td>")
                .append(BUNDLE.format("formatter.top.proc.name")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.pid")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.user")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.stime")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.size")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.rss")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.cpu")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.mem")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.disk.total")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.args")).append("</td></tr></thead><tbody>");

        int i = 0;
        for (ProcessReport pr : t.getProcessesSorted(sortType)) {
            if (i++ >= numberOfProcessesToShow) {
                break;
            }
            char[] st = new char[1];
            st[0] = pr.getState();
            String str = new String(buf);
            char stateStr = ((str.trim().length() == 0) ? '-' : pr.getState());
            String cmd = h(pr.getBaseName());
            
            StringBuilder argsBuilder = new StringBuilder();
            for (String arg : pr.getArgs()) {
                argsBuilder.append(arg).append(",");
            }
            //remove the last comma
            if (argsBuilder.length() > 0) {
                argsBuilder.setLength(argsBuilder.length()-1);          
            }
            String args = h(argsBuilder.toString());

            buf.append("<tr>")
                    .append("<td title='").append(cmd).append("' style='word-wrap:break-word;' >")
                    .append(StringUtils.substring(cmd, 0, 25))
                    .append("</td>")
                    .append("<td>").append(pr.getPid()).append("</td>")
                    .append("<td>").append(pr.getOwner()).append("</td>")
                    .append("<td>").append(pr.getStartTime()).append("</td>")
                    .append("<td>").append(pr.getSize()).append("</td>")
                    .append("<td>").append(pr.getResident()).append("</td>")
                    .append("<td>").append(pr.getCpuPerc()).append("</td>")
                    .append("<td>").append(pr.getMemPerc()).append("</td>")
                    .append("<td>").append(pr.getFormatedTotalDiskBytes()).append("</td>")
                    .append("<td title='").append(args).append("' style='word-wrap:break-word;' >")
                    .append(StringUtils.substring(args, 0, 60))
                    .append("</td></tr>");
        }

        buf.append("</tbody></table></div></div>");
        return buf.toString();
    }

    private String formatHtml(ConfigResponse cfg, TopData t) {
        StringBuffer r = new StringBuffer();
        DateFormat dateFmt = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        UptimeData utd = t.getUptime();

        r.append("<div class='top_livedata'><div class='fivepad' style='background:#efefef;'><b>Time</b>: ")
                .append(h(dateFmt.format(new Date(utd.getTime())))).append(" up ").append(h(utd.getFormattedUptime()))
                .append("<br/>");

        r.append("<b>Load Avg</b>: ").append(h(utd.getFormattedLoadavg())).append("<br/>");

        CpuPerc cpu = t.getCpu();
        r.append("<b>CPU States</b>: ")
                .append(h(BUNDLE.format("formatter.top.cpuStates", CpuPerc.format(cpu.getUser()),
                        CpuPerc.format(cpu.getSys()), CpuPerc.format(cpu.getNice()), CpuPerc.format(cpu.getWait()),
                        CpuPerc.format(cpu.getIdle())))).append("<br/>");

        Mem mem = t.getMem();
        r.append("<b>Mem</b>: ")
                .append(h(BUNDLE.format("formatter.top.memUse", (mem.getTotal() / 1024) + "k", (mem.getUsed() / 1024)
                        + "k", (mem.getFree() / 1024) + "k"))).append("<br/>");

        Swap swap = t.getSwap();
        r.append("<b>Swap</b>: ")
                .append(h(BUNDLE.format("formatter.top.memUse", (swap.getTotal() / 1024) + "k", (swap.getUsed() / 1024)
                        + "k", (swap.getFree() / 1024) + "k"))).append("<br/>");

        ProcStat ps = t.getProcStat();
        r.append("<b>Processes</b>: ")
                .append(h(BUNDLE.format("formatter.top.procSummary", "" + ps.getTotal(), "" + ps.getRunning(),
                        "" + ps.getSleeping(), "" + ps.getStopped(), "" + ps.getZombie()))).append("<br/><br/>");

        r.append("</div><table cellpadding='0' cellspacing='0' width='100%'><thead><tr><td>")
                .append(BUNDLE.format("formatter.top.proc.pid")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.user")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.stime")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.size")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.rss")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.share")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.state")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.time")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.cpu")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.mem")).append("</td><td>")
                .append(BUNDLE.format("formatter.top.proc.name")).append("</td></tr></thead><tbody>");
        
        for (Object element : t.getProcesses()) {
            ProcessData d = (ProcessData) element;
            char[] buf = new char[1];
            buf[0] = d.getState();
            String str = new String(buf);
            char stateStr = ((str.trim().length() == 0) ? '-' : d.getState());
            r.append("<tr><td>").append(d.getPid()).append("</td>").append("<td>").append(d.getOwner()).append("</td>")
                    .append("<td>").append(d.getFormattedStartTime()).append("</td>").append("<td>")
                    .append(d.getFormattedSize()).append("</td>").append("<td>").append(d.getFormattedResident())
                    .append("</td>").append("<td>").append(d.getFormattedShare()).append("</td>").append("<td>")
                    .append(stateStr).append("</td>").append("<td>").append(d.getFormattedCpuTotal()).append("</td>")
                    .append("<td>").append(d.getFormattedCpuPerc()).append("</td>").append("<td>")
                    .append(d.getFormattedMemPerc()).append("</td>").append("<td>").append(h(d.getBaseName()))
                    .append("</td></tr>");
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
