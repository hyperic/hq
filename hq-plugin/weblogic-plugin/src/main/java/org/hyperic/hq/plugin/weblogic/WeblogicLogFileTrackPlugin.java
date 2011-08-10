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

package org.hyperic.hq.plugin.weblogic;

import java.text.SimpleDateFormat;
import java.util.Locale;

import org.hyperic.hq.product.LogFileTailPlugin;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.sigar.FileInfo;

public class WeblogicLogFileTrackPlugin extends LogFileTailPlugin {

    private static final String[] LOG_LEVELS = {
        "Error,Critical,Alert,Emergency", //Error
        "Warning", //Warning
        "Info,Notice", //Info
        "Debug,Trace" //Debug
    };

    private WeblogicLogParser parser = null;
    
    private WeblogicLogParser getParser() {
        if (this.parser == null) {
            this.parser = new WeblogicLogParser();
            String dateTimeFormatString = getManager().getProperty(
                WeblogicLogParser.DATE_TIME_FORMAT_PROPERTY);
            if (dateTimeFormatString != null) {
                try {
                    this.parser.setDateTimeFormat(new SimpleDateFormat(dateTimeFormatString, Locale
                        .getDefault()));
                } catch (IllegalArgumentException e) {
                    getLog().error(
                        "Unable to create date format from: " + dateTimeFormatString +
                            ". Using default.");
                }
            }
        }
        return this.parser;
    }

    public String[] getLogLevelAliases() {
        return LOG_LEVELS;
    }

    public TrackEvent processLine(FileInfo info, String line) {
        WeblogicLogParser.Entry entry;

        try {
            entry = getParser().parse(line);
            if (entry == null) {
                return null;
            }
        } catch (Exception e) {
            getLog().error("Error parsing line: '" + line + "'", e);
            return null;
        }

        return newTrackEvent(entry.time,
                             entry.level,
                             entry.subsystem,
                             entry.message);
    }
}
