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

public class GetMeasurements_args extends AgentRemoteValue {
    private static final String PARAM_DSN   = "dsn";
    private static final String PARAM_NMEAS = "nmeas";

    private void setup(){
        this.setNumMeasurements(0);
    }

    public GetMeasurements_args(){
        super();
        this.setup();
    }

    public GetMeasurements_args(AgentRemoteValue args) 
        throws AgentRemoteException 
    {
        int nmeas;

        this.setup();
        nmeas = args.getValueAsInt(GetMeasurements_args.PARAM_NMEAS);

        for(int i=0; i<nmeas; i++){

            String metric =
                args.getValue(GetMeasurements_args.PARAM_DSN + i);

            this.addMeasurement(metric);
        }
    }

    public int getNumMeasurements(){
        try {
            return this.getValueAsInt(GetMeasurements_args.PARAM_NMEAS);
        } catch(AgentRemoteException exc){
            throw new AgentAssertionException("This should never occur");
        }
    }

    private void setNumMeasurements(int newMeas){
        super.setValue(GetMeasurements_args.PARAM_NMEAS, 
                       Integer.toString(newMeas));
    }

    public void addMeasurement(String metric){
        int curMeas = this.getNumMeasurements();

        super.setValue(GetMeasurements_args.PARAM_DSN + curMeas,
                       metric);
        this.setNumMeasurements(curMeas + 1);
    }


    public void setValue(String key, String val){
        throw new AgentAssertionException("This should never be called");
    }

    public String getMeasurement(int measNum){
        String qval;

        qval = GetMeasurements_args.PARAM_DSN + measNum;

        return this.getValue(qval);
    }
}
