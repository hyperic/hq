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

package org.hyperic.hq.agent;

public class AgentMonitorValue {
    public static final int TYPE_STRING  = 0;   // Simple string
    public static final int TYPE_DOUBLE  = 1;   // Simple double
    public static final int TYPE_ASTRING = 2;   // Array of strings
    public static final int TYPE_AINT    = 3;   // Array of integers

    public static final int ERR_INCALCULABLE = 1; // Unable to calculate
    public static final int ERR_BADKEY       = 2; // Key to query value was bad
    public static final int ERR_BADMONITOR   = 3; // Monitor name was invalid
    public static final int ERR_INTERNAL     = 4; // Internal error occurred

    private boolean isErr;   // True of the value was an error value
    private int     errCode; // One of ERR_* if isErr is true
    private int     type;    // One of TYPE_*

    private String   sValue;
    private double   dValue;
    private String[] asValue;
    private int[]    aiValue;

    public AgentMonitorValue(){
        this.setErrCode(ERR_BADKEY);
        this.type = TYPE_STRING;
    }

    public AgentMonitorValue(String value){
        this.setValue(value);
    }

    public AgentMonitorValue(double value){
        this.setValue(value);
    }

    public AgentMonitorValue(String[] value){
        this.setValue(value);
    }

    public AgentMonitorValue(int[] value){
        this.setValue(value);
    }

    public boolean isErr(){
        return this.isErr;
    }

    public void setErrCode(int errCode){
        this.isErr   = true;
        this.errCode = errCode;
    }

    public int getErrCode(){
        return this.errCode;
    }

    public int getType(){
        return this.type;
    }

    public void setType(int type){
        AgentMonitorValue.checkValidType(type);
        this.type = type;
    }

    public String getStringValue(){
        if(this.isErr || this.type != TYPE_STRING)
            throw new IllegalStateException("Monitor value not a String");

        return this.sValue;
    }

    public void setValue(String sValue){
        this.isErr  = false;
        this.sValue = sValue;
        this.type   = TYPE_STRING;
    }

    public double getDoubleValue(){
        if(this.isErr || this.type != TYPE_DOUBLE)
            throw new IllegalStateException("Monitor value not a Double");

        return this.dValue;
    }

    public void setValue(double dValue){
        this.isErr  = false;
        this.dValue = dValue;
        this.type   = TYPE_DOUBLE;
    }

    public String[] getAStringValue(){
        if(this.isErr || this.type != TYPE_ASTRING)
            throw new IllegalStateException("Monitor value not an AString");

        return this.asValue;
    }

    public void setValue(String[] asValue){
        this.isErr   = false;
        this.asValue = asValue;
        this.type    = TYPE_ASTRING;
    }

    public int[] getAIntValue(){
        if(this.isErr || this.type != TYPE_AINT)
            throw new IllegalStateException("Monitor value not an AInt");

        return this.aiValue;
    }

    public void setValue(int[] aiValue){
        this.isErr   = false;
        this.aiValue = aiValue;
        this.type    = TYPE_AINT;
    }

    public static void checkValidType(int type){
        if(type < TYPE_STRING || type > TYPE_AINT)
            throw new IllegalArgumentException("Unknown type, '" + type + "'");
    }

    public String toString(){
        if(this.isErr){
            switch(this.errCode){
            case ERR_INCALCULABLE:
                return "**ERR INCALCULABLE**";
            case ERR_BADKEY:
                return "**ERR BADKEY**";
            case ERR_BADMONITOR:
                return "**ERR BADMONITOR**";
            case ERR_INTERNAL:
                return "**ERR INTERNAL**";
            default:
                return "**ERR UNKNOWN**";
            }
        } else {
            String res;

            switch(this.type){
            case TYPE_STRING:
                return this.sValue;
            case TYPE_DOUBLE:
                return Double.toString(this.dValue);
            case TYPE_ASTRING:
                res = "{";
                for(int i=0; i<this.asValue.length; i++){
                    res += "'" + this.asValue[i] + "'";
                    if(i != this.asValue.length - 1)
                        res += ", ";
                }
                return res + "}";
            case TYPE_AINT:
                res = "{";
                for(int i=0; i<this.aiValue.length; i++){
                    res += Integer.toString(this.aiValue[i]);
                    if(i != this.aiValue.length - 1)
                        res += ", ";
                }
                return res + "}";
            default:
                return "**ERR UNKNOWN**";
            }
        }
    }
}
