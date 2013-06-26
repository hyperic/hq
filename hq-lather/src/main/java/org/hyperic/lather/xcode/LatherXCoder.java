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

package org.hyperic.lather.xcode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hyperic.lather.LatherRemoteException;
import org.hyperic.lather.LatherValue;

/**
 * A class which encodes/decodes LatherValue objects to/from data streams.
 *
 * Developer note:  This class is tightly intertwined with LatherValue
 *                  for speed purposes.
 */
public class LatherXCoder {
    private final int HAS_STRINGS = 1 << 0;
    private final int HAS_INTS = 1 << 1;
    private final int HAS_DOUBLES = 1 << 2;
    private final int HAS_BYTEAS = 1 << 3;
    private final int HAS_STRINGLS = 1 << 4;
    private final int HAS_INTLS = 1 << 5;
    private final int HAS_DOUBLELS = 1 << 6;
    private final int HAS_BYTEALS = 1 << 7;
    private final int HAS_OBJECTS = 1 << 8;
    private final int HAS_OBJECTLS = 1 << 9;
    private final int HAS_LONGS = 1 << 10;
    private final int HAS_SERIALAIZABLES = 1 << 11;

    /**
     * Encode the given value object into a stream.
     * 
     * @param value
     *            Value to encode
     * @param out
     *            Stream to write the encoded representation of 'value' to
     */
    public void encode(LatherValue value, DataOutputStream out)
        throws IOException
    {
        Map stringVals, intVals, doubleVals, longVals, byteaVals, objectVals,
 stringLists, intLists, doubleLists, byteaLists, objectLists;
            
        int contents = 0;

        stringVals  = value.getStringVals();
        intVals     = value.getIntVals();
        longVals    = value.getLongVals();
        doubleVals  = value.getDoubleVals();
        byteaVals   = value.getByteAVals();
        objectVals  = value.getObjectVals();
        stringLists = value.getStringLists();
        intLists    = value.getIntLists();
        doubleLists = value.getDoubleLists();
        byteaLists  = value.getByteALists();
        objectLists = value.getObjectLists();

        if (stringVals.size() > 0) {
            contents |= HAS_STRINGS;
        }
        if (intVals.size() > 0) {
            contents |= HAS_INTS;
        }
        if (longVals.size() > 0) {
            contents |= HAS_LONGS;
        }
        if (doubleVals.size() > 0) {
            contents |= HAS_DOUBLES;
        }
        if (byteaVals.size() > 0) {
            contents |= HAS_BYTEAS;
        }
        if (stringLists.size() > 0) {
            contents |= HAS_STRINGLS;
        }
        if (intLists.size() > 0) {
            contents |= HAS_INTLS;
        }
        if (doubleLists.size() > 0) {
            contents |= HAS_DOUBLELS;
        }
        if (byteaLists.size() > 0) {
            contents |= HAS_BYTEALS;
        }
        if (objectVals.size() > 0) {
            contents |= HAS_OBJECTS;
        }
        if (objectLists.size() > 0) {
            contents |= HAS_OBJECTLS;
        }
        if (value.getSerializableMap().size() > 0) {
            contents |= HAS_SERIALAIZABLES;
        }

        // The first thing we write is the type of data that will be
        // sent -- this makes the minimum packet size == 4 bytes
        out.writeInt(contents);
        
        if((contents & HAS_STRINGS) != 0){
            out.writeInt(stringVals.size());

            for(Iterator i=stringVals.entrySet().iterator(); 
                i.hasNext(); 
                )
            {
                Map.Entry ent = (Map.Entry)i.next();

                out.writeUTF((String)ent.getKey());
                out.writeUTF((String)ent.getValue());
            }
        }
        
        if((contents & HAS_INTS) != 0){
            out.writeInt(intVals.size());

            for(Iterator i=intVals.entrySet().iterator(); 
                i.hasNext(); 
                )
            {
                Map.Entry ent = (Map.Entry)i.next();

                out.writeUTF((String)ent.getKey());
                out.writeInt(((Integer)ent.getValue()).intValue());
            }
        }

        if((contents & HAS_DOUBLES) != 0){
            out.writeInt(doubleVals.size());

            for(Iterator i=doubleVals.entrySet().iterator(); 
                i.hasNext(); 
                )
            {
                Map.Entry ent = (Map.Entry)i.next();

                out.writeUTF((String)ent.getKey());
                out.writeDouble(((Double)ent.getValue()).doubleValue());
            }
        }

        if((contents & HAS_BYTEAS) != 0){
            out.writeInt(byteaVals.size());

            for(Iterator i=byteaVals.entrySet().iterator(); 
                i.hasNext(); 
                )
            {
                Map.Entry ent = (Map.Entry)i.next();
                byte[] val;

                out.writeUTF((String)ent.getKey());
                val = (byte[])ent.getValue();
                out.writeInt(val.length);
                out.write(val, 0, val.length);
            }
        }

        if((contents & HAS_STRINGLS) != 0){
            out.writeInt(stringLists.size());

            for(Iterator i=stringLists.entrySet().iterator();
                i.hasNext();
                )
            {
                Map.Entry ent = (Map.Entry)i.next();
                List vals = (List)ent.getValue();

                out.writeUTF((String)ent.getKey());
                out.writeInt(vals.size());
                for(Iterator j=vals.iterator(); j.hasNext(); ){
                    out.writeUTF((String)j.next());
                }
            }
        }

        if((contents & HAS_INTLS) != 0){
            out.writeInt(intLists.size());

            for(Iterator i=intLists.entrySet().iterator();
                i.hasNext();
                )
            {
                Map.Entry ent = (Map.Entry)i.next();
                List vals = (List)ent.getValue();

                out.writeUTF((String)ent.getKey());
                out.writeInt(vals.size());
                for(Iterator j=vals.iterator(); j.hasNext(); ){
                    out.writeInt(((Integer)j.next()).intValue());
                }
            }
        }

        if((contents & HAS_DOUBLELS) != 0){
            out.writeInt(doubleLists.size());

            for(Iterator i=doubleLists.entrySet().iterator();
                i.hasNext();
                )
            {
                Map.Entry ent = (Map.Entry)i.next();
                List vals = (List)ent.getValue();

                out.writeUTF((String)ent.getKey());
                out.writeInt(vals.size());
                for(Iterator j=vals.iterator(); j.hasNext(); ){
                    out.writeDouble(((Double)j.next()).doubleValue());
                }
            }
        }

        if((contents & HAS_BYTEALS) != 0){
            out.writeInt(byteaLists.size());

            for(Iterator i=byteaLists.entrySet().iterator();
                i.hasNext();
                )
            {
                Map.Entry ent = (Map.Entry)i.next();
                List vals = (List) ent.getValue();

                out.writeUTF((String)ent.getKey());
                out.writeInt(vals.size());
                for(Iterator j=vals.iterator(); j.hasNext(); ){
                    byte[] val = (byte[])j.next();

                    out.writeInt(val.length);
                    out.write(val, 0, val.length);
                }
            }
        }

        if((contents & HAS_OBJECTS) != 0){
            out.writeInt(objectVals.size());
            
            for(Iterator i=objectVals.entrySet().iterator();
                i.hasNext();
                )
            {
                ByteArrayOutputStream bOs;
                DataOutputStream subDoS;
                Map.Entry ent = (Map.Entry)i.next();
                byte[] data;

                bOs    = new ByteArrayOutputStream();
                subDoS = new DataOutputStream(bOs);
                this.encode((LatherValue)ent.getValue(), subDoS);

                out.writeUTF((String)ent.getKey());
                out.writeUTF(ent.getValue().getClass().getName());
                data = bOs.toByteArray();
                out.writeInt(data.length);
                out.write(data, 0, data.length);
            }
        }

        if((contents & HAS_OBJECTLS) != 0){
            out.writeInt(objectLists.size());
            
            for(Iterator i=objectLists.entrySet().iterator();
                i.hasNext();
                )
            {
                Map.Entry ent = (Map.Entry)i.next();
                List vals = (List)ent.getValue();

                out.writeUTF((String)ent.getKey());
                out.writeInt(vals.size());
                for(Iterator j=vals.iterator(); j.hasNext(); ){
                    ByteArrayOutputStream bOs;
                    DataOutputStream subDoS;
                    LatherValue val = (LatherValue)j.next();
                    byte[] data;

                    bOs    = new ByteArrayOutputStream();
                    subDoS = new DataOutputStream(bOs);
                    this.encode(val, subDoS);

                    out.writeUTF(val.getClass().getName());
                    data = bOs.toByteArray();
                    out.writeInt(data.length);
                    out.write(data, 0, data.length);
                }
            }
        }

        if((contents & HAS_LONGS) != 0){
            out.writeInt(longVals.size());

            for(Iterator i=longVals.entrySet().iterator(); 
                i.hasNext(); 
                )
            {
                Map.Entry ent = (Map.Entry)i.next();

                out.writeUTF((String)ent.getKey());
                out.writeLong(((Long)ent.getValue()).longValue());
            }
        }
        if ((contents & HAS_SERIALAIZABLES) != 0) {
            out.writeInt(value.getSerializableMap().size());
            byte[] data;
            ByteArrayOutputStream b;
            ObjectOutputStream oos;
            for (Entry<String, Serializable> entry : value.getSerializableMap().entrySet()) {
                // Write Object name
                out.writeUTF(entry.getKey());

                // Turn the Object to byte array
                b = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(b);
                oos.writeObject(entry.getValue());
                data = b.toByteArray();

                // Write Object size
                out.writeInt(data.length);

                // Write the actual Object as byte array
                out.write(data);

            }

        }
    }

    /**
     * Decode a LatherValue object from a stream.  
     *
     * @param in Stream to read from, which has an encoded LatherValue object
     * @param cl Class to instantiate and store values to.  The object
     *           must be a subclass of the LatherValue object
     * 
     * @return an instantiation of the 'cl' argument, containing the
     *         decoded data.
     */
    public LatherValue decode(DataInputStream in, Class cl)
        throws IOException, LatherRemoteException
    {
        LatherValue res;
        String listName;
        List newList;
        Map map;
        int contents, nVals, nListVals;

        if(LatherValue.class.isAssignableFrom(cl) == false){
            throw new IllegalArgumentException("Passed class (" + cl + 
                                               ") is not a subclass of " +
                                               "LatherValue");
        }

        try {
            res = (LatherValue)cl.newInstance();
        } catch(Exception exc){
            throw new IllegalArgumentException("Passed class is not " +
                                               "accessable: " + exc); 
        }

        contents = in.readInt();

        if((contents & HAS_STRINGS) != 0){
            nVals = in.readInt();
            map   = res.getStringVals();
            for(int i=0; i<nVals; i++){
                map.put(in.readUTF(), in.readUTF());
            }
        }

        if((contents & HAS_INTS) != 0){
            nVals = in.readInt();
            map   = res.getIntVals();
            for(int i=0; i<nVals; i++){
                map.put(in.readUTF(), new Integer(in.readInt()));
            }
        }

        if((contents & HAS_DOUBLES) != 0){
            nVals = in.readInt();
            map   = res.getDoubleVals();
            for(int i=0; i<nVals; i++){
                map.put(in.readUTF(), new Double(in.readDouble()));
            }
        }

        if((contents & HAS_BYTEAS) != 0){
            nVals = in.readInt();
            map   = res.getByteAVals();
            for(int i=0; i<nVals; i++){
                byte[] bytes;
                
                listName = in.readUTF();
                bytes    = new byte[in.readInt()];
                in.readFully(bytes);
                map.put(listName, bytes);
            }
        }

        if((contents & HAS_STRINGLS) != 0){
            nVals = in.readInt();
            map   = res.getStringLists();
            for(int i=0; i<nVals; i++){
                listName  = in.readUTF();
                nListVals = in.readInt();
                newList   = new ArrayList(nListVals);

                for(int j=0; j<nListVals; j++){
                    newList.add(in.readUTF());
                }
                map.put(listName, newList);
            }
        }

        if((contents & HAS_INTLS) != 0){
            nVals = in.readInt();
            map   = res.getIntLists();
            for(int i=0; i<nVals; i++){
                listName  = in.readUTF();
                nListVals = in.readInt();
                newList   = new ArrayList(nListVals);

                for(int j=0; j<nListVals; j++){
                    newList.add(new Integer(in.readInt()));
                }
                map.put(listName, newList);
            }
        }

        if((contents & HAS_DOUBLELS) != 0){
            nVals = in.readInt();
            map   = res.getDoubleLists();
            for(int i=0; i<nVals; i++){
                listName  = in.readUTF();
                nListVals = in.readInt();
                newList   = new ArrayList(nListVals);

                for(int j=0; j<nListVals; j++){
                    newList.add(new Double(in.readDouble()));
                }
                map.put(listName, newList);
            }
        }

        if((contents & HAS_BYTEALS) != 0){
            nVals = in.readInt();
            map   = res.getByteALists();
            for(int i=0; i<nVals; i++){
                listName  = in.readUTF();
                nListVals = in.readInt();
                newList   = new ArrayList(nListVals);

                for(int j=0; j<nListVals; j++){
                    byte[] bytes;

                    bytes = new byte[in.readInt()];
                    in.readFully(bytes);
                    newList.add(bytes);
                }
                map.put(listName, newList);
            }
        }

        if((contents & HAS_OBJECTS) != 0){
            nVals = in.readInt();
            
            map = res.getObjectVals();
            for(int i=0; i<nVals; i++){
                ByteArrayInputStream bIs;
                DataInputStream subDiS;
                String className;
                byte[] bytes;
                Class valClass;

                listName  = in.readUTF();
                className = in.readUTF();
                bytes     = new byte[in.readInt()];
                in.readFully(bytes);
                bIs       = new ByteArrayInputStream(bytes);
                subDiS    = new DataInputStream(bIs);

                try {
                    valClass  = Class.forName(className);
                } catch(ClassNotFoundException exc){
                    throw new LatherRemoteException("Unable to locate '" +
                                                    className + "' to decode "+
                                                    "LatherValue");
                }

                map.put(listName, this.decode(subDiS, valClass));
            }
        }

        if((contents & HAS_OBJECTLS) != 0){
            nVals = in.readInt();

            map = res.getObjectLists();
            for(int i=0; i<nVals; i++){
                ByteArrayInputStream bIs;
                DataInputStream subDiS;
                String className;
                byte[] bytes;
                Class valClass;

                listName  = in.readUTF();
                nListVals = in.readInt();
                newList   = new ArrayList(nListVals);

                for(int j=0; j<nListVals; j++){
                    className = in.readUTF();
                    bytes     = new byte[in.readInt()];
                    in.readFully(bytes);
                    bIs       = new ByteArrayInputStream(bytes);
                    subDiS    = new DataInputStream(bIs);

                    try {
                        valClass  = Class.forName(className);
                    } catch(ClassNotFoundException exc){
                        throw new LatherRemoteException("Unable to locate '" +
                                                   className + "' to decode "+
                                                   "LatherValue");
                    }

                    newList.add(this.decode(subDiS, valClass));
                }

                map.put(listName, newList);
            }
        }

        if((contents & HAS_LONGS) != 0){
            nVals = in.readInt();
            map   = res.getLongVals();
            for(int i=0; i<nVals; i++){
                String key = in.readUTF();
                Long test = new Long(in.readLong());
                map.put(key, test);
            }
        }

        if ((contents & HAS_SERIALAIZABLES) != 0) {
            ObjectInputStream ois;
            ByteArrayInputStream b;
            nVals = in.readInt();
            byte[] bytes;
            map = res.getSerializableMap();
            for (int i = 0; i < nVals; i++) {
                String key = in.readUTF();
                Serializable obj;
                try {
                    bytes = new byte[in.readInt()];
                    b = new ByteArrayInputStream(bytes);
                    in.readFully(bytes);
                    ois = new ObjectInputStream(b);
                    obj = (Serializable) ois.readObject();
                    map.put(key, obj);
                } catch (ClassNotFoundException e) {
                    // Should not ever get here..
                }

            }
        }

        res.validate();
        return res;
    }
}
