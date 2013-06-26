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

package org.hyperic.lather;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The basic value object for passing data to/from a Lather server.  
 *
 * LatherValue objects have a variety of ways to interact with them.
 * The most simplistic way is to use simple key/value pairs.  
 *
 * The object also supports keys which map onto lists of data.
 *
 * Values contained within the object can have associated primitive
 * types, such as int, double, byte[] and String.
 *
 * Access to LatherValue is unsynchronized, so callers must do their
 * own.
 */
public abstract class LatherValue {
    private Map stringVals;  // Strings -> Strings
    private Map intVals;     // Strings -> Integers
    private Map doubleVals;  // Strings -> Doubles
    private Map longVals;    // Strings -> Longs
    private Map byteaVals;   // Strings -> byte[]s
    private Map objectVals;  // Strings -> LatherValues
    private Map javaObjectVals; // Strings -> Object

    private Map stringLists; // Strings -> List of Strings
    private Map intLists;    // Strings -> List of Integers
    private Map doubleLists; // Strings -> List of Doubles
    private Map byteaLists;  // Strings -> List of byte[]s
    private Map objectLists; // Strings -> List of LatherValues
    private final Map<String, Serializable> serializableMap = new HashMap<String, Serializable>();

    private boolean ensureOrder;

    /**
     * Get a map used for storing the different types of values (i.e.
     * setStringVals, etc.)
     */
    private Map createStorageMap(){
        if (this.ensureOrder) {
            return new TreeMap();
        }
        return new HashMap();
    }

    private void setup(boolean ensureOrder){
        this.ensureOrder = ensureOrder;

        this.stringVals  = this.createStorageMap();
        this.intVals     = this.createStorageMap();
        this.doubleVals  = this.createStorageMap();
        this.longVals    = this.createStorageMap();
        this.byteaVals   = this.createStorageMap();
        this.objectVals  = this.createStorageMap();

        this.stringLists = this.createStorageMap();
        this.intLists    = this.createStorageMap();
        this.doubleLists = this.createStorageMap();
        this.byteaLists  = this.createStorageMap();
        this.objectLists = this.createStorageMap();
    }

    public LatherValue(){
        this.setup(false);
    }

    /**
     * Using this constructor guarantees that encoding the exact
     * same LatherValue 2 different times generates the same byte
     * order, as the maps are traversed in the same order.
     */
    public LatherValue(boolean ensureOrder){
        this.setup(ensureOrder);
    }

    private void checkArg(Object arg){
        if(arg == null){
            throw new IllegalArgumentException("Argument cannot be null");
        }
    }

    protected String getStringValue(String key){
        String res;

        if((res = (String)this.stringVals.get(key)) == null){
            throw new LatherKeyNotFoundException(key);
        }

        return res;
    }

    protected void setStringValue(String key, String value){
        this.checkArg(value);
        this.stringVals.put(key, value);
    }

    protected int getIntValue(String key){
        Integer res;

        if((res = (Integer)this.intVals.get(key)) == null){
            throw new LatherKeyNotFoundException(key);
        }

        return res.intValue();
    }

    protected void setIntValue(String key, int value){
        this.intVals.put(key, new Integer(value));
    }

    protected double getDoubleValue(String key){
        Double res;

        if((res = (Double)this.doubleVals.get(key)) == null){
            throw new LatherKeyNotFoundException(key);
        }

        return res.doubleValue();
    }

    protected void setDoubleValue(String key, double value){
        this.doubleVals.put(key, new Double(value));
    }

    protected long getLongValue(String key){
        Long res;

        if ((res = (Long)this.longVals.get(key)) == null){
            throw new LatherKeyNotFoundException(key);
        }
        
        return res.longValue();
    }

    protected void setLongValue(String key, long value){
        this.longVals.put(key, new Long(value));
    }

    protected byte[] getByteAValue(String key){
        byte[] res;

        if((res = (byte[])this.byteaVals.get(key)) == null){
            throw new LatherKeyNotFoundException(key);
        }

        return res;
    }

    protected void setObjectValue(String key, LatherValue value){
        this.checkArg(value);
        this.objectVals.put(key, value);
    }

    protected LatherValue getObjectValue(String key){
        LatherValue res;

        if((res = (LatherValue)this.objectVals.get(key)) == null){
            throw new LatherKeyNotFoundException(key);
        }

        return res;
    }

    protected void setByteAValue(String key, byte[] value){
        this.checkArg(value);
        this.byteaVals.put(key, value);
    }

    private List getListValueForAdd(Map map, String listName){
        List res = (List)map.get(listName);

        if(res == null){
            res = new ArrayList();
            map.put(listName, res);
        }
        
        return res;
    }

    private List getListValueForGet(Map map, String listName){
        List res = (List)map.get(listName);

        if(res == null){
            throw new LatherKeyNotFoundException(listName);
        }

        return res;
    }

    protected void addStringToList(String listName, String value){
        List list = this.getListValueForAdd(this.stringLists, listName);

        this.checkArg(value);
        list.add(value);
    }
    
    protected String[] getStringList(String listName){
        List list = this.getListValueForGet(this.stringLists, listName);

        return (String[])list.toArray(new String[0]);
    }

    protected void addIntToList(String listName, int value){
        List list = this.getListValueForAdd(this.intLists, listName);

        list.add(new Integer(value));
    }
    
    protected int[] getIntList(String listName){
        List list = this.getListValueForGet(this.intLists, listName);
        int[] res;
        int idx;

        res = new int[list.size()];
        idx = 0;
        for(Iterator i=list.iterator(); i.hasNext(); ){
            Integer val = (Integer)i.next();

            res[idx++] = val.intValue();
        }

        return res;
    }

    protected void addDoubleToList(String listName, double value){
        List list = this.getListValueForAdd(this.doubleLists, listName);

        list.add(new Double(value));
    }
    
    protected double[] getDoubleList(String listName){
        List list = this.getListValueForGet(this.doubleLists, listName);
        double[] res;
        int idx;

        res = new double[list.size()];
        idx = 0;
        for(Iterator i=list.iterator(); i.hasNext(); ){
            Double val = (Double)i.next();

            res[idx++] = val.doubleValue();
        }

        return res;
    }

    protected void addByteAToList(String listName, byte[] value){
        List list = this.getListValueForAdd(this.byteaLists, listName);

        this.checkArg(value);
        list.add(value);
    }
    
    protected byte[][] getByteAList(String listName){
        List list = this.getListValueForGet(this.byteaLists, listName);

        return (byte[][])list.toArray(new byte[0][]);
    }

    protected void addObjectToList(String listName, Object value){
        List list = this.getListValueForAdd(this.objectLists, listName);

        this.checkArg(value);
        list.add(value);
    }

    protected Object[] getObjectList(String listName){
        List list = this.getListValueForGet(this.objectLists, listName);
        
        return list.toArray(new LatherValue[0]);
    }

    protected Serializable getObject(String objectName) {
        return serializableMap.get(objectName);
    }

    protected void addObject(String objectName, Serializable object) {
        serializableMap.put(objectName, object);
    }

    public Map getStringVals(){
        return this.stringVals;
    }

    public Map getIntVals(){
        return this.intVals;
    }

    public Map getDoubleVals(){
        return this.doubleVals;
    }

    public Map getLongVals(){
        return this.longVals;
    }

    public Map getByteAVals(){
        return this.byteaVals;
    }

    public Map getObjectVals(){
        return this.objectVals;
    }

    public Map getStringLists(){
        return this.stringLists;
    }

    public Map getIntLists(){
        return this.intLists;
    }

    public Map getDoubleLists(){
        return this.doubleLists;
    }

    public Map getByteALists(){
        return this.byteaLists;
    }

    public Map getObjectLists(){
        return this.objectLists;
    }

    public Map<String, Serializable> getSerializableMap() {
        return this.serializableMap;
    }

    /**
     * This method is called to verify that a LatherValue object
     * has all the appropriate contents.  It is invoked after the
     * LatherXCoder decodes a stream and formulates a LatherValue.
     */
    public abstract void validate()
        throws LatherRemoteException;
}
