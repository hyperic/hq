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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WeblogicLogParser {

   private Pattern pattern = null;
    private static final Log LOG = LogFactory.getLog(WeblogicLogParser.class.getName());
    public static final String DATE_TIME_FORMAT_PROPERTY = "weblogic.parser.datetime.format";
    private static final String DEFAULT_DATE_TIME_FORMAT = "MMMM d, yyyy h:mm:ss a z";
    private DateFormat dateTimeFormat;
    
    public WeblogicLogParser(){
        this.dateTimeFormat = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT,Locale.getDefault());
    }
    
    public DateFormat getDateTimeFormat() {
        return dateTimeFormat;
    }

    public void setDateTimeFormat(DateFormat dateTimeFormat) {
        this.dateTimeFormat = dateTimeFormat;
    }

    class Entry {
        long time;
        String level;
        String subsystem;
        String machine;
        String server;
        String thread;
        String user;
        String transaction;
        String id;
        String message;
        
        public String toString() {
            return
            "[" + this.level + "] " +
            "[" + new Date(this.time) + "] " +
            this.message;
        }
    }
    
    private Pattern getPattern() {
        if (this.pattern == null) {
            String linePattern = "<[^>]*(<[^>]*>)*[^<]*> ";
            this.pattern = Pattern.compile(linePattern);
        }
        return this.pattern;
    }
    
    private String nextField(Iterator it) {
        String field = (String)it.next();
        int ix = field.lastIndexOf("> ");
        if (ix == -1) {
            return field.substring(1);
        }
        else {
            return field.substring(1, ix);
        }
    }
    
    Entry parse(String line) {
        if (!line.startsWith("####")) {
            return null;
        }
        line = line.substring(4) + " ";

        List list = new ArrayList();
        Matcher matcher = getPattern().matcher(line);
        int i = 0;
        while (matcher.find()) {
            list.add(matcher.group());
            if (++i >= 9) {
                String field =
                    line.substring(matcher.end(), line.length());
                list.add(field);
                break;
            }
        }

        Entry entry = new Entry();
        Iterator it = list.iterator();
        String timestamp = nextField(it);
        try {
            entry.time = this.dateTimeFormat.parse(timestamp).getTime();
        } catch (ParseException e) {
            LOG.warn("Unable to match log timestamp (" + timestamp +
                      ") with the current date format. Adjust the formatter property (" + DATE_TIME_FORMAT_PROPERTY +
                      ") to get accurate timestamp results. Using current time for event until format is fixed.");
            entry.time = System.currentTimeMillis();
        }

        entry.level = nextField(it);
        entry.subsystem = nextField(it);
        entry.machine = nextField(it);
        entry.server = nextField(it);
        entry.thread = nextField(it);
        entry.user = nextField(it);
        entry.transaction = nextField(it);
        entry.id = nextField(it);
        entry.message = nextField(it);

        return entry;
    }

    public static void main(String[] args) throws Exception {
        WeblogicLogParser parser = new WeblogicLogParser();

        for (int i=0; i<args.length; i++) {
            File file = new File(args[i]);
            System.out.println(file);
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        Entry entry = parser.parse(line);
                        if (entry == null){
                            continue;
                        }
                        System.out.println(entry);
                    } catch (Exception e) {
                        System.out.println("ERROR-->" + e.getMessage());
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
