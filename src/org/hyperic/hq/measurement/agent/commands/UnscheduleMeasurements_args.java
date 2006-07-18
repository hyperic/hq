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
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;

public class UnscheduleMeasurements_args extends AgentRemoteValue {
    private static final String PARAM_ENT     = "ent";
    private static final String PARAM_NENTS   = "nmeas";

    private void setup(){
        this.setNumEntities(0);
    }

    public UnscheduleMeasurements_args(){
        super();
        this.setup();
    }

    public UnscheduleMeasurements_args(AgentRemoteValue args) 
        throws AgentRemoteException 
    {
        super();

        int nmeas;

        this.setup();
        nmeas = args.getValueAsInt(UnscheduleMeasurements_args.PARAM_NENTS);

        for(int i=0; i<nmeas; i++){
            AppdefEntityID ent;
            String sEnt;
            
            sEnt = args.getValue(UnscheduleMeasurements_args.PARAM_ENT + i);

            try {
                ent = new AppdefEntityID(sEnt);
            } catch(InvalidAppdefTypeException exc){
                throw new AgentRemoteException("Invalid entity: " +
                                               exc.getMessage());
            }

            this.addEntity(ent);
        }
    }

    public void setValue(String key, String val){
        throw new AgentAssertionException("This should never be called");
    }

    public AppdefEntityID getEntity(int measNo){
        try {
            String sEnt = this.getValue(PARAM_ENT + measNo);

            return new AppdefEntityID(sEnt);
        } catch(InvalidAppdefTypeException exc){
            throw new AgentAssertionException("Object accessed even though " +
                                              "construction failed");
        }
    }

    public void addEntity(AppdefEntityID ent){
        int numEnts = this.getNumEntities();

        super.setValue(UnscheduleMeasurements_args.PARAM_ENT + numEnts,
                       ent.getAppdefKey());

        this.setNumEntities(numEnts + 1);
    }

    public int getNumEntities(){
        String val = this.getValue(UnscheduleMeasurements_args.PARAM_NENTS);

        return Integer.parseInt(val);
    }

    private void setNumEntities(int newMeas){
        super.setValue(UnscheduleMeasurements_args.PARAM_NENTS,
                       Integer.toString(newMeas));
    }
}
