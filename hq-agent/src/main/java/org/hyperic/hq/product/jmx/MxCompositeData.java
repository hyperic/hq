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

package org.hyperic.hq.product.jmx;

import java.util.HashMap;
import java.util.Map;

import javax.management.openmbean.CompositeData;

public class MxCompositeData {

    private static final Map handlers = new HashMap();

    static {
        //Add "free" metric to java.lang:type=Memory:HeapMemoryUsage
        setHandler("java.lang.management.MemoryUsage", new Handler() {
           public Object getObject(CompositeData data, String key) {
               if (key.equals("free")) {
                   Long committed = (Long)data.get("committed");
                   Long used = (Long)data.get("used");
                   return new Long(committed.longValue() - used.longValue());
               }
               else return null;
           }
        });
    }
    
    public interface Handler {
        public Object getObject(CompositeData data, String key);    
    }

    public static void setHandler(String type, Handler handler) {
        handlers.put(type, handler);
    }

    public static Object getValue(CompositeData data, String key) {
        Handler handler =
            (Handler)handlers.get(data.getCompositeType().getTypeName());
        if (handler != null) {
            Object value = handler.getObject(data, key);
            if (value != null) {
                return value;
            }
        }

        return data.get(key);
    }
}
