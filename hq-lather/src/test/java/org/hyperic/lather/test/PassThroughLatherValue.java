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

import org.hyperic.lather.LatherValue;

public class PassThroughLatherValue
    extends LatherValue
{
    public PassThroughLatherValue(){
        super(true);
    }

    public String getStringValue(String key){
        return super.getStringValue(key);
    }
        
    public void setStringValue(String key, String value){
        super.setStringValue(key, value);
    }

    public int getIntValue(String key){
        return super.getIntValue(key);
    }
        
    public void setIntValue(String key, int value){
        super.setIntValue(key, value);
    }

    public long getLongValue(String key){
        return super.getLongValue(key);
    }

    public void setLongValue(String key, long value){
        super.setLongValue(key, value);
    }

    public double getDoubleValue(String key){
        return super.getDoubleValue(key);
    }
        
    public void setDoubleValue(String key, double value){
        super.setDoubleValue(key, value);
    }

    public byte[] getByteAValue(String key){
        return super.getByteAValue(key);
    }
        
    public void setByteAValue(String key, byte[] value){
        super.setByteAValue(key, value);
    }
    
    public void addStringToList(String listName, String value){
        super.addStringToList(listName, value);
    }

    public String[] getStringList(String listName){
        return super.getStringList(listName);
    }

    public void addIntToList(String listName, int value){
        super.addIntToList(listName, value);
    }

    public int[] getIntList(String listName){
        return super.getIntList(listName);
    }

    public void addDoubleToList(String listName, double value){
        super.addDoubleToList(listName, value);
    }

    public double[] getDoubleList(String listName){
        return super.getDoubleList(listName);
    }

    public void addByteAToList(String listName, byte[] value){
        super.addByteAToList(listName, value);
    }

    public byte[][] getByteAList(String listName){
        return super.getByteAList(listName);
    }

    public void setObjectValue(String key, LatherValue value){
        super.setObjectValue(key, value);
    }

    public LatherValue getObjectValue(String key){
        return super.getObjectValue(key);
    }

    public void addObjectToList(String listName, LatherValue value){
        super.addObjectToList(listName, value);
    }

    public LatherValue[] getObjectList(String listName){
        return  (LatherValue[])super.getObjectList(listName);
    }

    public void validate(){
    }
}
