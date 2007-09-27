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

package org.hyperic.util.callback;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PrimitiveUtil {
    private static final Map BASIC_VALUE;
    
    static {
        Map v = new HashMap();
        
        v.put(Boolean.TYPE, Boolean.FALSE);
        v.put(Character.TYPE, new Character('x'));
        v.put(Byte.TYPE, new Byte((byte)0));
        v.put(Short.TYPE, new Short((short)0));
        v.put(Integer.TYPE, new Integer(0));
        v.put(Long.TYPE, new Long(0));
        v.put(Float.TYPE, new Float(0));
        v.put(Double.TYPE, new Double(0));
        v.put(Void.TYPE, null);
        BASIC_VALUE = Collections.unmodifiableMap(v);
    }
    
    private PrimitiveUtil() {}

    /**
     * Get a basic value for a class, given the class.  If the class is not
     * a primitive type, null will be returned (as null is a valid object for
     * any non-primitive class).  If the class represents a primitive, a 
     * non-null value will be returned (unless the class is void)
     * 
     * @param c Class to get the basic value for
     */
    public static Object getBasicValue(Class c) {
        if (!c.isPrimitive())
            return null;
        
        return BASIC_VALUE.get(c);
    }
}
