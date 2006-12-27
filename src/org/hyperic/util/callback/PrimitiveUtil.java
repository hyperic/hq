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
