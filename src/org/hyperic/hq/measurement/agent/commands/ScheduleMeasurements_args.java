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
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.measurement.server.session.SRN;

public class ScheduleMeasurements_args 
    extends AgentRemoteValue 
{
    private static final String PARAM_DSN        = "dsn";
    private static final String PARAM_INTERVAL   = "interval";
    private static final String PARAM_DERIVED_ID = "derivedID";
    private static final String PARAM_SRN_ENT    = "srnEnt";
    private static final String PARAM_SRN_ID     = "srnID";
    private static final String PARAM_DSN_ID     = "dsnID";
    private static final String PARAM_NMEAS      = "nmeas";   // # measurements
    private static final String PARAM_CATEGORY   = "category";

    private void setup(){
        AppdefEntityID ent;

        this.setNumMeasurements(0);
        ent = new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_PLATFORM,
                                 0);
        this.setSRN(new SRN(ent, 0));
    }

    public ScheduleMeasurements_args(){
        super();
        this.setup();
    }

    public ScheduleMeasurements_args(AgentRemoteValue args) 
        throws AgentRemoteException 
    {
        AppdefEntityID ent;
        String sEnt;
        int nmeas, srnID;
        
        this.setup();
        nmeas = args.getValueAsInt(ScheduleMeasurements_args.PARAM_NMEAS);

        sEnt  = args.getValue(ScheduleMeasurements_args.PARAM_SRN_ENT);
        srnID = args.getValueAsInt(ScheduleMeasurements_args.PARAM_SRN_ID);
            
        try {
            ent = new AppdefEntityID(sEnt);
        } catch(InvalidAppdefTypeException exc){
            throw new AgentRemoteException("Invalid SRN entity: " + 
                                           exc.getMessage());
        }

        this.setSRN(new SRN(ent, srnID));

        // The reason we do all this parsing, only to be reparsed later is
        // to check validity of the arguments, since this is an entry point
        // into the API
        for(int i=0; i<nmeas; i++){
            String dsn, category;
            long ival;
            int derID, dsnID;

            dsn  = args.getValue(ScheduleMeasurements_args.PARAM_DSN + i);
            ival = args.getValueAsLong(ScheduleMeasurements_args.
                                       PARAM_INTERVAL + i);
            derID = args.getValueAsInt(ScheduleMeasurements_args.
                                       PARAM_DERIVED_ID + i);
            dsnID = args.getValueAsInt(ScheduleMeasurements_args.
                                       PARAM_DSN_ID + i);
            category = args.getValue(ScheduleMeasurements_args.
                                     PARAM_CATEGORY + i);

            // Add backwards compat for older servers (< 2.7.4 and < 2.6.29)
            // that don't send the category
            if (category == null) {
                category = "";
            }

            this.addMeasurement(dsn, ival, derID, dsnID, category);
        }
    }

    public int getNumMeasurements(){
        String val = this.getValue(ScheduleMeasurements_args.PARAM_NMEAS);

        return Integer.parseInt(val);
    }

    private void setNumMeasurements(int newMeas){
        super.setValue(ScheduleMeasurements_args.PARAM_NMEAS, 
                       Integer.toString(newMeas));
    }

    public void setSRN(SRN srn){
        super.setValue(ScheduleMeasurements_args.PARAM_SRN_ID,
                       Integer.toString(srn.getRevisionNumber()));
        super.setValue(ScheduleMeasurements_args.PARAM_SRN_ENT,
                       srn.getEntity().getAppdefKey());
    }

    public void addMeasurement(String dsn, long interval, int derivedID, 
                               int dsnID, String category)
    {
        int curMeas = this.getNumMeasurements();

        super.setValue(ScheduleMeasurements_args.PARAM_DSN + curMeas, dsn);
        super.setValue(ScheduleMeasurements_args.PARAM_INTERVAL + curMeas,
                       Long.toString(interval));
        super.setValue(ScheduleMeasurements_args.PARAM_DERIVED_ID + curMeas,
                       Integer.toString(derivedID));
        super.setValue(ScheduleMeasurements_args.PARAM_DSN_ID + curMeas,
                       Integer.toString(dsnID));
        super.setValue(ScheduleMeasurements_args.PARAM_CATEGORY + curMeas,
                       category);

        this.setNumMeasurements(curMeas + 1);
    }

    public void setValue(String key, String val){
        throw new AgentAssertionException("This should never be called");
    }

    public ScheduleMeasurements_metric getMeasurement(int measNum){
        return new ScheduleMeasurements_metric(this.getDSN(measNum),
                                               this.getInterval(measNum),
                                               this.getDerivedID(measNum),
                                               this.getDSNId(measNum),
                                               this.getCategory(measNum));
    }

    private String getDSN(int measNum){
        String res;

        String qval = ScheduleMeasurements_args.PARAM_DSN + measNum;

        res = this.getValue(qval);
        return res;
    }

    private long getInterval(int measNum){
        try {
            String qval = ScheduleMeasurements_args.PARAM_INTERVAL + measNum;

            return this.getValueAsLong(qval);
        } catch(AgentRemoteException exc){
            throw new AgentAssertionException("Object accessed even though " +
                                              "construction failed");
        }
    }

    private int getDerivedID(int measNum){
        try {
            String qval = ScheduleMeasurements_args.PARAM_DERIVED_ID + measNum;

            return this.getValueAsInt(qval);
        } catch(AgentRemoteException exc){
            throw new AgentAssertionException("Object accessed even though " +
                                              "construction failed");
        }
    }

    public SRN getSRN(){
        AppdefEntityID ent;
        try {
            String qval = ScheduleMeasurements_args.PARAM_SRN_ENT;
            String qval2 = ScheduleMeasurements_args.PARAM_SRN_ID;

            ent = new AppdefEntityID(this.getValue(qval));
            return new SRN(ent, this.getValueAsInt(qval2));
        } catch(InvalidAppdefTypeException exc){
            throw new AgentAssertionException("Object accessed even though " +
                                              "construction failed");
        } catch(AgentRemoteException exc){
            throw new AgentAssertionException("Object accessed even though " +
                                              "construction failed");
        }
    }

    private int getDSNId(int measNum){
        try {
            String qval = ScheduleMeasurements_args.PARAM_DSN_ID + measNum;

            return this.getValueAsInt(qval);
        } catch(AgentRemoteException exc){
            throw new AgentAssertionException("Object accessed even though " +
                                              "construction failed");
        }
    }

    private String getCategory(int measNum){
        String res;

        String qval = ScheduleMeasurements_args.PARAM_CATEGORY + measNum;

        res = this.getValue(qval);
   
        return res;
    }        
}
