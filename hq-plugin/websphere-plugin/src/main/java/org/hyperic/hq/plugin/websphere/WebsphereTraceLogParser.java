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
package org.hyperic.hq.plugin.websphere;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

//parser for trace.log
public class WebsphereTraceLogParser {

    private DateFormat dateFormat =
            new SimpleDateFormat("M/d/yy hh:mm:ss:SSS z");

    class Entry {

        long time;
        String thread;
        String subsystem;
        String level;
        String message;

        @Override
        public String toString() {
            return "[" + this.level + "] "
                    + "[" + new Date(this.time) + "] "
                    + this.message;
        }
    }

    Entry parse(String line) {
        if ((line.length() == 0)
                || (line.charAt(0) != '[')) {
            return null;
        }

        Entry entry = new Entry();

        line = line.substring(1);
        int ix = line.indexOf(']');
        if (ix == -1) {
            return null;
        }
        String timestamp = line.substring(0, ix);
        entry.time = System.currentTimeMillis();

        line = line.substring(ix + 1).trim();

        ix = line.indexOf(' ');
        if (ix == -1) {
            return null;
        }
        entry.thread = line.substring(0, ix);

        line = line.substring(ix + 1).trim();
        ix = line.indexOf(' ');
        if (ix == -1) {
            return null;
        }
        entry.subsystem = line.substring(0, ix);

        line = line.substring(ix + 1).trim();
        ix = line.indexOf(' ');
        if (ix == -1) {
            return null;
        }
        entry.level = line.substring(0, ix);

        line = line.substring(ix + 1).trim();

        entry.message = line;

        try {
            entry.time =
                    this.dateFormat.parse(timestamp).getTime();
        } catch (ParseException e) {
            entry.time = System.currentTimeMillis();
        }

        return entry;
    }

    public static void main(String[] args) throws Exception {
        WebsphereTraceLogParser parser = new WebsphereTraceLogParser();

        for (int i = 0; i < args.length; i++) {
            File file = new File(args[i]);
            System.out.println(file);
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        Entry entry = parser.parse(line);
                        if (entry == null) {
                            continue;
                        }
                        System.out.println(entry);
                    } catch (Exception e) {
                        System.out.println("ERROR-->" + e.getMessage());
                        System.out.println("LINE='" + line + "'");
                        e.printStackTrace();
                        break;
                    }
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
    }
}
