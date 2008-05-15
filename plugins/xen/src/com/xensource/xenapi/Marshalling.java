/*
 *============================================================================
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of version 2.1 of the GNU Lesser General Public
 * License as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *============================================================================
 * Copyright (C) 2007 XenSource Inc.
 *============================================================================
 */
package com.xensource.xenapi;

import java.util.*;

/**
 * Marshalls Java types onto the wire.
 * Does not cope with records.  Use individual record.toMap()
 */
public final class Marshalling {
    /**
     * Converts Integers to Strings
     * and Sets to Lists recursively.
     */
    public static Object toXMLRPC(Object o) {
        if (o instanceof String ||
            o instanceof Boolean ||
            o instanceof Float ||
            o instanceof Date) {
            return o;
	} else if (o instanceof Long) {
	    return o.toString();
        } else if (o instanceof Map) {
            Map<Object, Object> result = new HashMap<Object, Object>();
            Map m = (Map)o;
            for (Object k : m.keySet())
            {
                result.put(toXMLRPC(k), toXMLRPC(m.get(k)));
            }
            return result;
        } else if (o instanceof Set) {
            List<Object> result = new ArrayList<Object>();
            for (Object e : ((Set)o))
            {
                result.add(toXMLRPC(e));
            }
            return result;
	} else if (o instanceof XenAPIObject) {
	    return ((XenAPIObject) o).toWireString();
	}else if (o == null){
	    return "";
        } else {
		throw new RuntimeException ("=============don't know how to marshall:({[" + o + "]})");
        }
    }
}
