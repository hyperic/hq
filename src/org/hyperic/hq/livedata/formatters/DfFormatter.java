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
import java.util.Locale;

import org.apache.commons.lang.StringEscapeUtils;
import org.hyperic.hq.livedata.FormatType;
import org.hyperic.hq.livedata.LiveDataFormatter;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.hq.plugin.system.DfData;
import org.hyperic.hq.plugin.system.FileSystemData;
import org.hyperic.sigar.FileSystem;
import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.i18n.MessageBundle;
import org.hyperic.util.units.UnitNumber;
import org.hyperic.util.units.UnitsConstants;
import org.hyperic.util.units.UnitsFormat;

public class DfFormatter
    implements LiveDataFormatter
{
    private static final MessageBundle BUNDLE = 
        MessageBundle.getBundle("org.hyperic.hq.livedata.Resources");
    private static final ConfigSchema EMPTY_SCHEMA = new ConfigSchema();
    
    public boolean canFormat(LiveDataCommand cmd, FormatType type) {
        return cmd.getCommand().equals("df");
    }

    public String format(LiveDataCommand cmd, FormatType type, 
                         ConfigResponse formatCfg, Object val)
    {
        DfData df = (DfData)val;
        
        if (type.equals(FormatType.TEXT))
            return formatText(formatCfg, df);
        else if (type.equals(FormatType.HTML)) 
            return formatHtml(formatCfg, df);
        throw new IllegalStateException("Unhandled format type [" + type + "]");
    }

    private String formatText(ConfigResponse cfg, DfData df) {
        return "Text stuff "+ df;
    }
    
    private String h(String s) {
        return StringEscapeUtils.escapeHtml(s);
    }

    private String formatHtml(ConfigResponse cfg, DfData df) {
        StringBuffer r = new StringBuffer();
        
        r.append("<div class='df_livedata'><table cellpadding='0' cellspacing='0'><thead><tr>");
        r.append("<td>")
         .append(BUNDLE.format("formatter.df.head.name"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.df.head.size"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.df.head.used"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.df.head.avail"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.df.head.usePerc"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.df.head.mount"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.df.head.type"))
         .append("</td>")
         .append("</tr></thead><tbody>");
        
        
        for (Iterator i=df.getFileSystems().iterator(); i.hasNext(); ) {
            FileSystemData fd = (FileSystemData)i.next();
            FileSystemUsage stat = fd.getStat();
            FileSystem fs = fd.getConfig();
            long pct = (long)(stat.getUsePercent() * 100);
            String spct;
            
            if (pct == 0)
                spct = "-";
            else
                spct = pct + "%";
            
            r.append("<tr><td>")
             .append(h(fs.getDevName()))
             .append("</td><td>")
             .append(formatBytes(stat.getTotal()))
             .append("</td><td>")
             .append(formatBytes(stat.getUsed()))
             .append("</td><td>")
             .append(formatBytes(stat.getAvail()))
             .append("</td><td>")
             .append(spct)
             .append("</td><td>")
             .append(h(fs.getDirName()))
             .append("</td><td>")
             .append(h(fs.getSysTypeName() + "/" + fs.getTypeName()))
             .append("</td></tr>");
        }
        r.append("</tbody></table></div>");
        
        return r.toString();
    }

    private String formatBytes(long b) {
        return UnitsFormat.format(new UnitNumber(b, UnitsConstants.UNIT_BYTES,
                                                 UnitsConstants.SCALE_KILO),
                                  Locale.getDefault(), null).toString();
    }
    
    public ConfigSchema getConfig(LiveDataCommand cmd) {
        return EMPTY_SCHEMA;
    }

    public String getDescription() {
        return BUNDLE.format("formatter.df.desc");
    }

    public String getName() {
        return BUNDLE.format("formatter.df.name");
    }

    public String getId() {
        return "df";
    }
}
