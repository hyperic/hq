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

package org.hyperic.util;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.util.json.JSON;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class for typesafe enums.  People implementing such enums should
 * subclass this class.  
 * 
 * Enums have a code and a description.  The code must be unique for enums
 * of a specific class.  This has great use in things like Web-UI where you
 * need a code representation as well as a string (rendering a listbox)
 * 
 * Each enumeration also provides a resource bundle and locale property to
 * look up the 'value' of the enumeration in that bundle.
 * 
 * This class can also be used as a dynamic enumeration as long as all the
 * enumerations use a unique code.  
 * 
 * XXX:  It would be good to implement the PersistentEnum stylee via 
 *       a UserType in Hibernate, so we don't have to do the conversion in
 *       every class that uses an enum.  Don't have the time now.. :-(
 *           http://www.hibernate.org/203.html
 */
public abstract class HypericEnum 
    implements JSON, Serializable
{
    private static final Log _log = LogFactory.getLog(HypericEnum.class);
    private static final boolean DEBUG_ENUMS = false;
    
    /**
     * Hash of classes onto sets of instances.
     * TODO:  Change the sets into hashmaps so we can do quicker lookups.
     */
    private static final Map _enumsByClass = new HashMap();
    
    private Class          _implClass;
    private int            _code;
    private transient String         _desc;
    private transient String         _localeProp;
    private transient ResourceBundle _bundle;
    
    protected HypericEnum(int code, String desc, String localeProp,
                          ResourceBundle bundle) 
    {
        init(getClass(), code, desc, localeProp, bundle);
    }
    
    protected HypericEnum(Class c, int code, String desc, String localeProp, 
                          ResourceBundle bundle) 
    {
        init(c, code, desc, localeProp, bundle);
    }
    
    private void init(Class c, int code, String desc, String localeProp, 
                      ResourceBundle bundle) 
    {
        _implClass  = c;
        _code       = code;
        _desc       = desc;
        _localeProp = localeProp;
        _bundle     = bundle;

        if (_bundle == null) {
            _log.warn("Unable to find bundle when creating enum for [" + 
                      _implClass + "]");
        }
        
        if (DEBUG_ENUMS) {
            if (_bundle != null && _bundle.getString(_localeProp) == null) {
                _log.warn("Unable to find prop [" + _localeProp + "] in " +
                          "bundle [" + _bundle + "]");
            }
            _log.info("[" + _bundle + "] (" + _localeProp + ") == " + 
                      getValue());
        }
        
        synchronized (_enumsByClass) {
            Set vals = (Set)_enumsByClass.get(_implClass);
            
            if (getByCode(_implClass, code) != null) {
                throw new IllegalStateException("2 enumerations of class [" + 
                                                _implClass + "] have the " + 
                                                "same code[" + code + "]");
            }
            if (vals == null) {
                vals = new HashSet();
                _enumsByClass.put(_implClass, vals);
            } 
            vals.add(this);
        }
    }
    
    public int getCode() {
        return _code;
    }
    
    /**
     * Returns the localized value of this enumeration. 
     */
    public String getValue() {
        String res;
        
        if (_bundle == null) {
            return "** No bundle for class " + _implClass + " **";
        }
        res = _bundle.getString(_localeProp);
        if (res == null) {
            return "** Property [" + _localeProp + "] not found in bundle [" +
                _bundle.toString() + "]";
        }
        return res;
    }
    
    public String getDescription() {
        return _desc;
    }
    
    public String toString() {
        return _desc;
    }
    
    public String getJsonName() {
        return _implClass.getName();
    }

    public JSONObject toJSON() {
        try {
            JSONObject res = new JSONObject()
                .put("code", getCode())
                .put("description", getDescription());

            return res;
        } catch(JSONException e) {
            throw new SystemException(e);
        }
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
     * Like {@link #findByCode(Class, int)} except returns null instead of
     * throwing an exception
     */
    public static HypericEnum getByCode(Class c, int code) {
        synchronized (_enumsByClass) {
            Set vals = (Set)_enumsByClass.get(c);
            
            if (vals != null) {
                for (Iterator i=vals.iterator(); i.hasNext(); ) {
                    HypericEnum e = (HypericEnum)i.next();
                
                    if (e.getCode() == code) 
                        return e;
                }
            }
        }
        return null;
    }
    
    /**
     * Find an enum of a specific class type by code.
     *
     * @param c     A subclass of {@link HypericEnum}
     * @param code  The integer code represented by the enum
     * @return the enum, else null
     */
    public static HypericEnum findByCode(Class c, int code) {
        HypericEnum res = getByCode(c, code);
        
        if (res != null)
            return res;

        throw new IllegalStateException("Unknown Enum Class [" + 
                                        c.getName() + "] code=" + code);
    }

    /**
     * Find an enum of a specific class type by description.
     *
     * @param c A subclass of {@link HypericEnum}
     * @param description The description represented by the enum.
     * @return The enum, else null.
     */
    public static HypericEnum findByDescription(Class c, String description) {
        synchronized (_enumsByClass) {
            Set vals = (Set)_enumsByClass.get(c);

            if (vals != null) {
                for (Iterator i = vals.iterator(); i.hasNext(); ) {
                    HypericEnum e = (HypericEnum)i.next();

                    if (e.getDescription().equals(description)) {
                        return e;
                    }
                }
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
    
    private Object readResolve() throws ObjectStreamException {
        return findByCode(_implClass, _code);
    }
}
