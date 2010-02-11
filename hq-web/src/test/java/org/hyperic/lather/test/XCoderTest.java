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

package org.hyperic.lather.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Random;

import junit.framework.TestCase;

import org.hyperic.lather.LatherValue;
import org.hyperic.lather.xcode.LatherXCoder;

public class XCoderTest
    extends TestCase
{
    private int DO_STRING  = 1 << 0;
    private int DO_INT     = 1 << 1;
    private int DO_DOUBLE  = 1 << 2;
    private int DO_BYTEA   = 1 << 3;
    private int DO_STRINGL = 1 << 4;
    private int DO_INTL    = 1 << 5;
    private int DO_DOUBLEL = 1 << 6;
    private int DO_BYTEAL  = 1 << 7;
    private int DO_OBJECT  = 1 << 8;
    private int DO_OBJECTL = 1 << 9;
    private int DO_LONG    = 1 << 10;
    private int MAX_VAL    = 1 << 11;

    public XCoderTest(String name){
        super(name);
    }

    static String getRandomString(Random r){
        return "Blah: " + r.nextInt();
    }


    private boolean equals(byte[] one, byte[] two){
        if(one == null && two == null)
            return true;

        if(one == null && two != null ||
           one != null && two == null)
        {
            return false;
        }

        if(one.length != two.length)
            return false;

        for(int i=0; i<one.length; i++)
            if(one[i] != two[i])
                return false;

        return true;
    }

    public void testXCode() 
        throws Exception 
    {
        final LatherXCoder xCoder = new LatherXCoder();
        final long seed = 0xdeadbeef;
        Random r, v;
        int nVals;

        r = new Random(seed);
        v = new Random(seed);

        for(int i=0; i<MAX_VAL; i++){
            PassThroughLatherValue decoded;
            ByteArrayOutputStream bos;
            ByteArrayInputStream bis;
            DataOutputStream dos;
            DataInputStream dis;
            byte[] data, newData;

            bos = new ByteArrayOutputStream();
            dos = new DataOutputStream(bos);
            
            xCoder.encode(this.generateValue(i, r, null), dos);
            data = bos.toByteArray();
            bis  = new ByteArrayInputStream(data);
            dis  = new DataInputStream(bis);

            decoded = (PassThroughLatherValue)xCoder.decode(dis, 
                                     PassThroughLatherValue.class);
            this.generateValue(i, v, decoded);

            bos = new ByteArrayOutputStream();
            dos = new DataOutputStream(bos);
            xCoder.encode(decoded, dos);

            // Compare the byte streams
            newData = bos.toByteArray();
            if(this.equals(data, newData) == false){
                if(data.length == newData.length){
                    for(int j=0; j<data.length; j++){
                        if(data[j] != newData[j]){
                            System.out.println("Data " + j + ": " + data[j] + 
                                               " vs " + newData[j]);
                        }
                    }
                }
                this.fail("Byte streams unequal at doVal = " + i +
                          " data.length = " + data.length + 
                          " newData.length = " + newData.length);
            }
        }
    }

    private PassThroughLatherValue generateValue(int doVal, Random r, 
                                              PassThroughLatherValue validate)
    {
        PassThroughLatherValue res = new PassThroughLatherValue();
        int nVals;

        if((doVal & DO_STRING) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                String key = this.getRandomString(r);
                String val = this.getRandomString(r);
                
                res.setStringValue(key, val);
                if(validate != null){
                    if(validate.getStringValue(key).equals(val) == false){
                        this.fail("Strings mismatch at doVal = " + doVal);
                    }
                }
            }
        }

        if((doVal & DO_INT) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                String key = this.getRandomString(r);
                int val = r.nextInt();

                res.setIntValue(key, val);
                if(validate != null){
                    if(validate.getIntValue(key) != val){
                        this.fail("Int mismatch at doVal = " + doVal);
                    }
                }
            }
        }

        if((doVal & DO_DOUBLE) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                String key = this.getRandomString(r);
                double val = r.nextDouble();

                res.setDoubleValue(key, val);
                if(validate != null){
                    if(validate.getDoubleValue(key) != val){
                        this.fail("Double mismatch at doVal = " + doVal);
                    }
                }
            }
        }

        if((doVal & DO_BYTEA) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                String key = this.getRandomString(r);
                byte[] val;
                
                val = new byte[r.nextInt(100)];
                r.nextBytes(val);

                res.setByteAValue(key, val);
                if(validate != null){
                    if(this.equals(validate.getByteAValue(key), val) == false){
                        this.fail("ByteA mismatch at doVal = " + doVal);
                    }
                }
            }
        }

        if((doVal & DO_STRINGL) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                String listName = this.getRandomString(r);
                String[] valList = null;
                int listSize = r.nextInt(5) + 1;
                
                if(validate != null)
                    valList = validate.getStringList(listName);

                for(int j=0; j<listSize; j++){
                    String val = this.getRandomString(r);

                    res.addStringToList(listName, val);
                    if(valList != null && valList[j].equals(val) == false){
                        this.fail("StringL mismatch at doVal = " + doVal);
                    }
                }
            }
        }

        if((doVal & DO_INTL) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                String listName = this.getRandomString(r);
                int[] valList = null;
                int listSize = r.nextInt(5) + 1;
                
                if(validate != null)
                    valList = validate.getIntList(listName);

                for(int j=0; j<listSize; j++){
                    int val = r.nextInt();

                    res.addIntToList(listName, val);
                    if(valList != null && valList[j] != val){
                        this.fail("IntL mismatch at doVal = " + doVal);
                    }
                }
            }
        }

        if((doVal & DO_DOUBLEL) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                String listName = this.getRandomString(r);
                double[] valList = null;
                int listSize = r.nextInt(5) + 1;
                
                if(validate != null)
                    valList = validate.getDoubleList(listName);

                for(int j=0; j<listSize; j++){
                    double val = r.nextDouble();

                    res.addDoubleToList(listName, val);
                    if(valList != null && valList[j] != val){
                        this.fail("DoubleL mismatch at doVal = " + doVal);
                    }
                }
            }
        }

        if((doVal & DO_BYTEAL) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                String listName = this.getRandomString(r);
                byte[][] valList = null;
                int listSize = r.nextInt(5) + 1;
                
                if(validate != null)
                    valList = validate.getByteAList(listName);

                for(int j=0; j<listSize; j++){
                    byte[] val = new byte[r.nextInt(100)];

                    r.nextBytes(val);

                    res.addByteAToList(listName, val);
                    if(valList != null && !this.equals(valList[j], val)){
                        this.fail("ByteAL mismatch at doVal = " + doVal);
                    }
                }
            }
        }

        if((doVal & DO_OBJECT) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                SubValue val;
                String key = this.getRandomString(r),
                    subKey = this.getRandomString(r),
                    subVal = this.getRandomString(r);
                
                val = new SubValue();
                val.setStringValue(subKey, subVal);

                res.setObjectValue(key, val);
                if(validate != null){
                    SubValue otherVal;

                    otherVal = (SubValue)validate.getObjectValue(key);
                    if(!otherVal.getStringValue(subKey).equals(subVal)){
                        this.fail("Object mismatch at doVal = " + doVal);
                    }
                }
            }
        }

        if((doVal & DO_OBJECTL) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                String listName = this.getRandomString(r);
                LatherValue[] valList = null;
                int listSize = r.nextInt(5) + 1;
                
                if(validate != null)
                    valList = validate.getObjectList(listName);

                for(int j=0; j<listSize; j++){
                    SubValue val = new SubValue();
                    String subKey = this.getRandomString(r),
                        subVal = this.getRandomString(r);
                    
                    val.setStringValue(subKey, subVal);

                    res.addObjectToList(listName, val);
                    if(valList != null){
                        SubValue otherVal = (SubValue)valList[j];

                        if(!otherVal.getStringValue(subKey).equals(subVal)){
                            this.fail("ObjectL mismatch at doVal = " + doVal);
                        }
                    }
                }
            }
        }

        if((doVal & DO_LONG) != 0){
            nVals = r.nextInt(5) + 1;
            for(int i=0; i<nVals; i++){
                String key = this.getRandomString(r);
                long val = r.nextLong();

                res.setLongValue(key, val);
                if(validate != null){
                    if(validate.getLongValue(key) != val){
                        this.fail("Long mismatch at doVal = " + doVal);
                    }
                }
            }
        }

        return res;
    }
}
