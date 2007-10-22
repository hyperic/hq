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

import org.apache.commons.lang.StringEscapeUtils;
import org.hyperic.hq.livedata.FormatType;
import org.hyperic.hq.livedata.LiveDataFormatter;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.sigar.FileSystem;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.i18n.MessageBundle;

public class FilesystemFormatter
    implements LiveDataFormatter
{
    private static final MessageBundle BUNDLE = 
        MessageBundle.getBundle("org.hyperic.hq.livedata.Resources");
    private static final ConfigSchema EMPTY_SCHEMA = new ConfigSchema();
    
    public boolean canFormat(LiveDataCommand cmd, FormatType type) {
        return cmd.getCommand().equals("filesystem");
    }

    public String format(LiveDataCommand cmd, FormatType type, 
                         ConfigResponse formatCfg, Object val)
    {
        FileSystem[] f = (FileSystem[])val;
        
        if (type.equals(FormatType.TEXT))
            return formatText(formatCfg, f);
        else if (type.equals(FormatType.HTML)) 
            return formatHtml(formatCfg, f);
        throw new IllegalStateException("Unhandled format type [" + type + "]");
    }

    private String formatText(ConfigResponse cfg, FileSystem[] f) {
        return "Text stuff "+ ArrayUtil.toString(f);
    }
    
    private String h(String s) {
        return StringEscapeUtils.escapeHtml(s);
    }

    private String formatHtml(ConfigResponse cfg, FileSystem[] f) {
        StringBuffer r = new StringBuffer();
        
        r.append("<div class='filesystem_livedata'><table><thead><tr>");
        r.append("<td>")
         .append(BUNDLE.format("formatter.filesystem.head.name"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.filesystem.head.mount"))
         .append("</td>")
         .append("<td>")
         .append(BUNDLE.format("formatter.filesystem.head.type"))
         .append("</td>")
         .append("</tr></thead><tbody>");

        for (int i=0; i<f.length; i++) {
            r.append("<tr><td>")
             .append(h(f[i].getDevName()))
             .append("</td><td>")
             .append(h(f[i].getDirName()))
             .append("</td><td>")
             .append(h(f[i].getSysTypeName()))
             .append("</td></tr>");
        }
        r.append("</tbody></table></div>");
        
        return r.toString();
    }

    public ConfigSchema getConfig(LiveDataCommand cmd) {
        return EMPTY_SCHEMA;
    }

    public String getDescription() {
        return BUNDLE.format("formatter.filesystem.desc");
    }

    public String getName() {
        return BUNDLE.format("formatter.filesystem.name");
    }

    public String getId() {
        return "fileSystem";
    }
}
