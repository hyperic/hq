package org.hyperic.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for typesafe enums.  People implementing such enums should
 * subclass this class.  
 * 
 * Enums have a code and a description.  The code must be unique for enums
 * of a specific class.  This has great use in things like Web-UI where you
 * need a code representation as well as a string (rendering a listbox)
 */
public abstract class HypericEnum {
    private static final Map _enumsByClass = new HashMap();
    
    private final int    _code;
    private final String _desc;
    
    public HypericEnum(int code, String desc) {
        _code = code;
        _desc = desc;
        
        synchronized (_enumsByClass) {
            Set vals = (Set)_enumsByClass.get(getClass());
            
            if (findByCode(getClass(), code) != null) {
                throw new IllegalStateException("2 enumerations of class [" + 
                                                getClass() + "] have the " + 
                                                "same code[" + code + "]");
            }
            if (vals == null) {
                vals = new HashSet();
                _enumsByClass.put(getClass(), vals);
            } 
            vals.add(this);
        }
    }
    
    public int getCode() {
        return _code;
    }
    
    public String getDescription() {
        return _desc;
    }
    
    private static Comparator CODE_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            HypericEnum e1, e2;
            
            e1 = (HypericEnum)o1;
            e2 = (HypericEnum)o2;
            if (e1.getCode() < e2.getCode())
                return -1;
            else if(e1.getCode() == e2.getCode()) 
                return 0;
            return 1;
        }
    };
    
    /**
     * Find an enum of a specific class type by code.
     *
     * @param c     A subclass of {@link HypericEnum}
     * @param code  The integer code represented by the enum
     * @return the enum, else null
     */
    public static HypericEnum findByCode(Class c, int code) {
        synchronized (_enumsByClass) {
            Set vals = (Set)_enumsByClass.get(c);
            
            if (vals == null)
                return null;
            
            for (Iterator i=vals.iterator(); i.hasNext(); ) {
                HypericEnum e = (HypericEnum)i.next();
                
                if (e.getCode() == code) 
                    return e;
            }
        }
        return null;
    }
    
    /**
     * Return a list of {@link HypericEnum} objects for a specific class, 
     * sorted by their code.
     * @param c Class to find enums for
     */
    public static List getAll(Class c) {
        List res;
        Set vals;
        
        synchronized (_enumsByClass) {
            vals = (Set)_enumsByClass.get(c);
        }

        if (vals == null)
            return Collections.EMPTY_LIST;
        
        res = new ArrayList(vals);
        Collections.sort(res, CODE_COMPARATOR);
        return Collections.unmodifiableList(res);
    }
}
