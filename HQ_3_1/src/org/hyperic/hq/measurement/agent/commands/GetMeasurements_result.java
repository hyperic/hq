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

package org.hyperic.hq.measurement.agent.commands;

import org.hyperic.hq.agent.AgentAssertionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.agent.AgentRemoteValue;

import org.hyperic.hq.product.MetricValue;

/**
 * The result object from the GetMeasurements command.  This is a tricky
 * object, since it can potentially represent 2 different types of 
 * values: MeasurementValues and Exceptions.  Since GetMeasurements can 
 * retrieve multiple measurements at once, any one may fail, and therefore
 * the caller should know if an exception occurred while retrieving it.  
 * The caller should invoke obj.getException() if obj.getMeasurement() returns
 * null, to get the exception message.
 */

public class GetMeasurements_result extends AgentRemoteValue {
    private static final String PARAM_NMEAS  = "nmeas";
    private static final String PARAM_TYPE   = "type";
    private static final String PARAM_VALUE  = "value";
    private static final String PARAM_RTIME  = "rtime";

    private static final String PARAM_TYPE_EXC    = "e";
    private static final String PARAM_TYPE_DOUBLE = "d";

    private void setup(){
        this.setNumMeasurements(0);
    }

    public GetMeasurements_result(){
        super();
        this.setup();
    }

    public GetMeasurements_result(AgentRemoteValue args) 
        throws AgentRemoteException 
    {
        int nmeas;

        this.setup();
        nmeas = args.getValueAsInt(GetMeasurements_result.PARAM_NMEAS);

        for(int i=0; i<nmeas; i++){
            String type, val;

            type = args.getValue(GetMeasurements_result.PARAM_TYPE + i);
            val  = args.getValue(GetMeasurements_result.PARAM_VALUE + i);
            if(type.equals(GetMeasurements_result.PARAM_TYPE_DOUBLE)){
                MetricValue objVal;
                Double dval;
                long rTime;

                try {
                    dval = Double.valueOf(val);
                } catch(NumberFormatException exc){
                    throw new AgentRemoteException("Invalid double value: " + 
                                                   val);
                }

                rTime = args.getValueAsLong(GetMeasurements_result.PARAM_RTIME+
                                            i);
                objVal = new MetricValue(dval, rTime);
                this.addMeasurement(objVal);
            } else if(type.equals(GetMeasurements_result.PARAM_TYPE_EXC)){
                this.addException(val);
                continue;
            } else {
                throw new AgentRemoteException("Unknown measurement type: " +
                                               type);
            }
        }
    }

    public int getNumMeasurements(){
        String val = this.getValue(GetMeasurements_result.PARAM_NMEAS);

        return Integer.parseInt(val);
    }

    private void setNumMeasurements(int newMeas){
        super.setValue(GetMeasurements_result.PARAM_NMEAS, 
                       Integer.toString(newMeas));
    }

    public void addException(String excMsg){
        int curMeas = this.getNumMeasurements();

        super.setValue(GetMeasurements_result.PARAM_TYPE + curMeas, 
                       GetMeasurements_result.PARAM_TYPE_EXC);
        super.setValue(GetMeasurements_result.PARAM_VALUE + curMeas, excMsg);
        super.setValue(GetMeasurements_result.PARAM_RTIME + curMeas, "0");
        this.setNumMeasurements(curMeas + 1);
    }

    public void addMeasurement(MetricValue val){
        int curMeas = this.getNumMeasurements();

        super.setValue(GetMeasurements_result.PARAM_TYPE + curMeas, 
                       GetMeasurements_result.PARAM_TYPE_DOUBLE);
        super.setValue(GetMeasurements_result.PARAM_VALUE + curMeas, 
                       Double.toString(val.getValue()));
        super.setValue(GetMeasurements_result.PARAM_RTIME + curMeas, 
                       Long.toString(val.getTimestamp()));
        this.setNumMeasurements(curMeas + 1);
    }


    public void setValue(String key, String val){
        throw new AgentAssertionException("This should never be called");
    }

    public String getException(int measNum){
        return this.getValue(GetMeasurements_result.PARAM_VALUE + measNum);
    }

    public MetricValue getMeasurement(int measNum){
        String type = this.getType(measNum);

        if(type.equals(GetMeasurements_result.PARAM_TYPE_EXC))
            return null;

        return new MetricValue(this.getValue(measNum, type),
                                    this.getTime(measNum));
    }

    private String getType(int measNum){
        return this.getValue(GetMeasurements_result.PARAM_TYPE + measNum);
    }

    private Double getValue(int measNum, String type){
        try {
            String qval = GetMeasurements_result.PARAM_VALUE + measNum, val;

            val  = this.getValue(qval);
            if(type.equals(GetMeasurements_result.PARAM_TYPE_DOUBLE)){
                return Double.valueOf(val);
            } else {
                throw new AgentRemoteException("Unknown measurement type");
            }
        } catch(AgentRemoteException exc){
            throw new AgentAssertionException("Object accessed even though " +
                                              "construction failed: " + exc);
        }
    }

    private long getTime(int measNum){
        try {
            String qval = GetMeasurements_result.PARAM_RTIME + measNum;

            return this.getValueAsLong(qval);
        } catch(AgentRemoteException exc){
            throw new AgentAssertionException("Object accessed even though " +
                                              "construction failed");
        }
    }
}
