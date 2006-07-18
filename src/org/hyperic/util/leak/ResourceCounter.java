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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hyperic.util.StringUtil;

public class ResourceCounter {

    // A Map of type names->ResourceTracker.
    private Map counters = new HashMap();

    public ResourceCounter () {}

    public void openResource ( String resourceType, Object o ) {
        ResourceTracker rt;
        synchronized(counters) {
            rt = (ResourceTracker) counters.get(resourceType);
            if ( rt == null ) {
                rt = new ResourceTracker(this, resourceType);
                counters.put(resourceType, rt);
            }
        }
        rt.openResource(o);
    }

    public void closeResource ( String resourceType, Object o ) {
        ResourceTracker rt;
        synchronized(counters) {
            rt = (ResourceTracker) counters.get(resourceType);
        }
        if ( rt == null ) {
            throw new IllegalStateException("Cannot close resource: " 
                                            + o + " (type=" + resourceType + ")"
                                            + " because it no resource"
                                            + " of that type has ever "
                                            + "been opened!");
        }
        rt.closeResource(o);
    }

    public int getCount ( String resourceType ) {
        ResourceTracker rt;
        synchronized(counters) {
            rt = (ResourceTracker) counters.get(resourceType);
        }
        if ( rt == null ) { return 0; }

        return rt.getNumOpen();
    }

    private static String mapToString ( Map m, String intraDelim, String interDelim ) {

        if ( m == null ) return "*NULL-MAP*";

        Iterator i = m.keySet().iterator();
        Object key = null;
        Object value = null;
        String rstr = "";
        while ( i.hasNext() ) {
            if ( rstr.length() > 0 ) rstr += interDelim;
            key = i.next();
            value = m.get(key);
            if ( key == null ) key = "*NULL-KEY*";
            if ( value == null ) {
                rstr += key.toString() + intraDelim + " *NULL*";

            } else if ( value instanceof Map ) {
                rstr += key.toString() + intraDelim +
                        mapToString((Map) value, " => ", ", ");

            } else if ( value instanceof List ) {
                rstr += key.toString() + intraDelim +
                        StringUtil.listToString((List) value);

            } else {
                rstr += key.toString() + intraDelim + value.toString();
            }
        }
        return "{Map: " + rstr + "}";
    }

    public synchronized String dump () {
        return "Dumping ResourceCounter...\n"
            + mapToString(counters, "\t=>\t", "\n");
    }

    public synchronized String dumpHTML () {

        StringBuffer sb = new StringBuffer();
        Iterator resourceTypes = counters.keySet().iterator();
        String resourceType;
        ResourceTracker rt;
        sb.append("\nDumping ResourceCounter at ").append(new Date()).append("...");
        while ( resourceTypes.hasNext() ) {
            resourceType = (String) resourceTypes.next();
            rt = (ResourceTracker) counters.get(resourceType);
            sb.append("\n<h1>")
                .append(resourceType).append("</h1>")
                .append("\n").append(rt.dumpHTML())
                .append("\n</br><hr>\n");
        }
        return sb.toString();
    }

    public synchronized String dumpHTMLtoFile () throws IOException {

        FileWriter fw = null;
        File tempFile;
        try {
            tempFile = File.createTempFile("CAM-resource-counter-dump.", ".html");
            fw = new FileWriter(tempFile);
            fw.write("<html><body>");
            fw.write(dumpHTML());
            fw.write("</body></html>");
            return tempFile.getAbsolutePath();

        } finally {
            if ( fw != null ) try { fw.close(); } catch (Exception e) {}
        }
    }
}
