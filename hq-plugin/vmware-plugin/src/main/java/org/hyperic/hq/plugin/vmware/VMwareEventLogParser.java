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

package org.hyperic.hq.plugin.vmware;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.HashMap;

import org.hyperic.util.StringUtil;
import org.hyperic.util.xmlparser.XmlParseException;
import org.hyperic.util.xmlparser.XmlParser;
import org.hyperic.util.xmlparser.XmlTagEntryHandler;
import org.hyperic.util.xmlparser.XmlTagHandler;
import org.hyperic.util.xmlparser.XmlTagInfo;
import org.hyperic.util.xmlparser.XmlTextHandler;
import org.hyperic.util.xmlparser.XmlUnAttrHandler;

public class VMwareEventLogParser {

    private static final XmlTagInfo[] EMPTY_TAG_INFO =
        new XmlTagInfo[0];

    private EventTag tag;
    private Entry entry;

    public VMwareEventLogParser() {
        this.tag = new EventTag();
    }

    class Entry {
        long time;
        String subject;
        String body;
        String type;
        
        public String toString() {
            return
                "[" + this.type + "] " +
                "[" + new Date(this.time) + "] " +
                this.subject + " - " + this.body;
        }
    }

    public Entry parse(String line) throws XmlParseException {
        //else parser chokes:
        //"Next character must be ";" terminating reference to entity"
        line = StringUtil.replace(line, "&", "&amp;");
        ByteArrayInputStream bytes = 
            new ByteArrayInputStream(line.getBytes());
        XmlParser.parse(bytes, this.tag);

        Entry entry = new Entry();
        entry.type = this.tag.getEventType();
        entry.time = Long.parseLong(this.tag.time.text);
        entry.time *= 1000; //to millis
        entry.subject = this.tag.subject.text;
        entry.body = this.tag.body.p.text;

        return entry;
    }

    private static class EventTag extends BaseTag {
        TimeTag time = new TimeTag();
        SubjectTag subject = new SubjectTag();
        BodyTag body = new BodyTag();

        public String getName() {
            return "event";
        }
        
        public String getEventType() {
            return (String)this.attrs.get("type");
        }

        public XmlTagInfo[] getSubTags() {
            return new XmlTagInfo[] {
                new XmlTagInfo(time, 
                               XmlTagInfo.ONE_OR_MORE),
                new XmlTagInfo(subject,
                               XmlTagInfo.ONE_OR_MORE),
                new XmlTagInfo(body, 
                               XmlTagInfo.ONE_OR_MORE),
                new XmlTagInfo(new ChoiceTag(), 
                               XmlTagInfo.ZERO_OR_MORE),
                new XmlTagInfo(new UserTag(), 
                               XmlTagInfo.ZERO_OR_MORE),

            };
        }
    }

    private static class TimeTag extends BaseTag {
        public String getName() {
            return "time";
        }
    }

    private static class SubjectTag extends BaseTag {
        public String getName() {
            return "subject";
        }
    }
    
    private static class BodyTag extends BaseTag {
        ParaTag p = new ParaTag();

        public String getName() {
            return "body";
        }
        
        public XmlTagInfo[] getSubTags() {
            return new XmlTagInfo[] {
                new XmlTagInfo(this.p,
                               XmlTagInfo.ZERO_OR_MORE),
            };
        }
    }

    private static class ParaTag extends BaseTag {
        public String getName() {
            return "p";
        }
    }
    
    private static class UserTag extends BaseTag {
        public String getName() {
            return "user";
        }
    }
    
    private static class ChoiceTag extends BaseTag {
        public String getName() {
            return "choice";
        }
    }

    private static abstract class BaseTag
        implements XmlTagHandler,
                   XmlUnAttrHandler,
                   XmlTagEntryHandler,
                   XmlTextHandler {

        String text;
        HashMap attrs = null;

        public abstract String getName();
    
        public XmlTagInfo[] getSubTags() {
            return EMPTY_TAG_INFO;
        }
        
        public void handleUnknownAttribute(String name, String value) {
            if (this.attrs == null) {
                this.attrs = new HashMap();
            }
            this.attrs.put(name, value);
        }
        
        public void handleText(String text) {
            this.text = text.trim();
        }
        
        public void enter() {
            if (this.attrs != null) {
                this.attrs.clear();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        VMwareEventLogParser parser = new VMwareEventLogParser();
        for (int i=0; i<args.length; i++) {
            File file = new File(args[i]);
            System.out.println(file);
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("<event")) {
                        continue;
                    }
                    try {
                        System.out.println(parser.parse(line));
                    } catch (Exception e) {
                        System.out.println("ERROR-->" + e.getMessage());
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
