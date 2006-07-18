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

package org.hyperic.util.leak;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.util.StringUtil;

public class ResourceTracker {

    private Map itsResourceMap = new HashMap();

    // How often to see if we're leaked.
    private static final long LEAKCHECK_INTERVAL = 60000;

    // If anything has been open for more than 2 minutes, we're leaked.
    private static final long LEAKTIME = 120000;

    private long lastCheckTime = System.currentTimeMillis();

    private Log log;
    private ResourceCounter rc;
    private String desc;
    private volatile int numOpen;

    public ResourceTracker (String desc) {
        this(null, desc);
    }
    public ResourceTracker (ResourceCounter parent, String desc) {
        this.rc   = parent;
        this.desc = desc;
        log = LogFactory.getLog(ResourceTracker.class.getName());
        numOpen = 0;
    }

    public void openResource ( Object res ) {
        // log.warn("openResource("+res+")");
        ResourceRec oldRec;
        synchronized (itsResourceMap) {
            checkLeaked();
            oldRec = (ResourceRec) itsResourceMap.get(res);
            if (oldRec == null) {
                itsResourceMap.put(res, new ResourceRec(res));
            } else if ( oldRec.open ) {
                Exception e = new Exception();
                throw new IllegalStateException("Tried to re-open a " + desc 
                                                + " that was already open: " 
                                                + oldRec + " (current stack="
                                                + StringUtil.getStackTrace(e)
                                                + ")");
            } else {
                oldRec.open();
            }
        }
        numOpen++;
    }
    public void closeResource ( Object res ) {
        // log.warn("closeResource("+res+")");
        ResourceRec oldRec;
        synchronized (itsResourceMap) {
            oldRec = (ResourceRec) itsResourceMap.get(res);
            if (oldRec == null) { // || (oldRec != null && (!oldRec.open))) {
                Exception e = new Exception();
                throw new IllegalStateException("Cannot close a " + desc 
                                                + " that was not open "
                                                + "(OLDREC="+oldRec+"=OLDREC): "
                                                + res + " (current stack=" 
                                                + StringUtil.getStackTrace(e)
                                                + ")");
            }
            oldRec.open = false;
        }
        numOpen--;
    }

    public int getNumOpen () {
        return numOpen;
    }

    private synchronized void checkLeaked () {
        if ( itsResourceMap.size() == 0 ) return;

        // If we have checked for leakage in the past 5 minutes, don't
        // check again
        long now = System.currentTimeMillis();
        if ( now - lastCheckTime < LEAKCHECK_INTERVAL ) return;
        try {
            Iterator i = itsResourceMap.keySet().iterator();
            ResourceRec rr, oldest = null;
            long age, oldestAge = now;
            while (i.hasNext()) {
                rr = (ResourceRec) itsResourceMap.get(i.next());
                if ( !rr.open ) continue;
                age = now - rr.timestamp;
                if ( age > LEAKTIME ) {
                    if ( rc != null ) {
                        try {
                            String filename = rc.dumpHTMLtoFile();
                            log.error("LEAKAGE DETECTED (DCP DUMP)---> dumped debug info to: " + filename);
                        } catch (IOException ioe) {
                            log.error("LEAKAGE DETECTED (DCP DUMP)---> ERROR dumping debug info: " + ioe);
                        }
                    } else {
                        log.error("LEAKAGE DETECTED: " + rr);
                    }
                    try { Thread.sleep(1000); } catch (InterruptedException e) {}
                    return;
                }
                if ( oldest == null || age > (now - oldest.timestamp) ) {
                    oldest = rr;
                    oldestAge = now - oldest.timestamp;
                }
            }
            if (oldest != null ) {
                log.warn("ResourceTracker: No leakage detected "
                         + "(total=" + itsResourceMap.size() + ", oldest "
                         + desc + " has been open for " 
                         + String.valueOf(oldestAge/1000) + " seconds)");
            } else {
                log.warn("ResourceTracker: No leakage detected (no " 
                         + desc + " currently opened)");
            }
        } finally {
            lastCheckTime = System.currentTimeMillis();
        }
    }

    public String printThreadStats (Thread t) {
        StringBuffer sb = new StringBuffer();
        sb.append("name=").append(t.getName())
            .append(", isAlive=").append(t.isAlive())
            .append(", group=").append(t.getThreadGroup())
            .append(", toString=").append(t);
        return sb.toString();
    }

    public String toString () {
        StringBuffer sb = new StringBuffer();
        sb.append("ResourceTracker(").append(desc).append(") : ");
        Iterator iter;
        ResourceRec rr;
        List recs = getSortedList();
        for ( int i=0; i<recs.size(); i++ ) {
            rr = (ResourceRec) recs.get(i);
            sb.append(rr.toString());
        }
        return sb.toString();
    }

    private List getSortedList () {
        List recs = new ArrayList();
        Iterator iter;
        synchronized (itsResourceMap) {
            iter = itsResourceMap.keySet().iterator();
            while (iter.hasNext()) recs.add(itsResourceMap.get(iter.next()));
        }
        Collections.sort(recs);
        return recs;
    }

    public String dumpHTML () {
        StringBuffer sb = new StringBuffer();
        Iterator iter;
        ResourceRec rr;
        List recs = getSortedList();
        int openCount = 0, closedCount = 0;
        sb.append("<table border=\"1\"><tr><th>resource</th><th>opened</th><th>thread</th><th>creation stack</th></tr>");
        for ( int i=0; i<recs.size(); i++ ) {
            rr = (ResourceRec) recs.get(i);
            sb.append("<tr><td valign=\"top\">").append(rr.resource.toString());
            sb.append("</td><td nowrap valign=\"top\">").append(rr.getTimestampString())
                .append(" open=").append(rr.open)
                .append("</td><td valign=\"top\">").append(printThreadStats(rr.thread))
                .append("</td><td valign=\"top\"><font size=\"-2\">").append(rr.getHTMLStackString())
                .append("</font></td></tr>");
            if ( rr.open ) openCount++;
            else closedCount++;
        }
        sb.append("</table>TOTAL OPEN=").append(openCount)
            .append(", CLOSED=").append(closedCount);
        return sb.toString();
    }

    class ResourceRec implements Comparable {
        public final Object resource;
        public volatile Exception stack;
        public volatile Thread thread;
        public volatile long timestamp;
        public volatile boolean open;
        private String timestampString = null;
        private String stackString = null;
        private String htmlStack = null;
        private String stringRepresentation = null;
        public ResourceRec ( Object resource ) {
            this.resource = resource;
            open = false;
            init();
        }
        public void init () {
            timestamp = System.currentTimeMillis();
            stack = new Exception();
            thread = Thread.currentThread();
        }
        public void open () {
            init();
            open = true;
        }
        public String toString () {
            if (stringRepresentation == null) {
                StringBuffer sb = new StringBuffer();
                sb.append("[Resource=").append(resource)
                    .append(" time=").append(getTimestampString())
                    .append(" stack=").append(getStackString())
                    .append("]");
                stringRepresentation = sb.toString();
            }
            return stringRepresentation;
        }
        public int hashCode () { return resource.hashCode(); }
        public boolean equals (Object o) {
            if ( o instanceof ResourceRec ) {
                return ((ResourceRec) o).resource.equals(this.resource);
            }
            return false;
        }
        public int compareTo ( Object o ) {
            if ( o instanceof ResourceRec ) {
                ResourceRec other = (ResourceRec) o;
                if (this.open == other.open) { 
                    long diff = other.timestamp - timestamp;
                    if ( diff > Integer.MAX_VALUE ) return Integer.MAX_VALUE;
                    if ( diff < Integer.MIN_VALUE ) return Integer.MIN_VALUE;
                    return (int) diff;

                } else if (this.open) {
                    return Integer.MIN_VALUE;
                } else {
                    return Integer.MAX_VALUE;
                }
            }
            return 0;
        }
        public String getTimestampString () {
            if ( timestampString == null ) {
                timestampString = (new Date(timestamp)).toString();
            }
            return timestampString;
        }
        public String getStackString () {
            if ( stackString == null ) {
                stackString = StringUtil.getStackTrace(stack);
            }
            return stackString;
        }
        public String getHTMLStackString () {
            if ( htmlStack == null ) {
                htmlStack = StringUtil.replace(getStackString(), 
                                               "\n", "\n<br>");
            }
            return htmlStack;
        }
    }
}
