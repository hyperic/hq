package org.hyperic.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapUtil {
    private MapUtil() {
    }
    
    /**
     * Get a value from a map, creating a new instance of a class and 
     * inserting it as the default value if it's not found.  This method
     * synchronizes on the map argument and relies on toCreate to be a
     * class with a default constructor.
     *
     * @param m        Map to get value from
     * @param key      Key into map
     * @param toCreate Class to instantiate and insert into 'm' if the key
     *                 doesn't exist in the map.  
     *                 
     * @return The value of the map in the key, or an instance of toCreate
     *         if not.
     */
    public static Object getOrCreate(Map m, Object key, Class toCreate) { 
        Object res;
     
        synchronized (m) {
            res = m.get(key);
            if (res != null)
                return res;
        
            try {
                res = toCreate.newInstance();
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        
            m.put(key, res);
            return res;
        }
    }
    
    public static void main(String[] args) {
        Map m = new HashMap();
        
        Object o = getOrCreate(m, "f", ArrayList.class);
        System.out.println("o is " + o);
        Object o2 = getOrCreate(m, "f", ArrayList.class);
        System.out.println("o2 is " + o2);
        System.out.println("o == o2 ? " + (o == o2));
    }
}
