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

package org.hyperic.hq.measurement.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hyperic.hq.product.MetricValue;


public class MeasurementReportConstructor {
    /**
     *  clientIDs is a hash onto a hash of dsn IDs onto lists of 
     *  ValueData objects
     */
    private HashMap clientIDs;  
    
    public MeasurementReportConstructor(){
        this.clientIDs = new HashMap();
    }

    public void addDataPoint(int clientID, int dsnID, 
                             MetricValue data)
    {
        HashMap   dsnMap;
        ArrayList valData;
        Integer   iClientID, iDsnID;

        iClientID = new Integer(clientID);
        iDsnID    = new Integer(dsnID);

        dsnMap    = (HashMap)this.clientIDs.get(iClientID);
        if(dsnMap == null){
            dsnMap = new HashMap();
            this.clientIDs.put(iClientID, dsnMap);
        }

        valData = (ArrayList)dsnMap.get(iDsnID);
        if(valData == null){
            valData = new ArrayList();
            dsnMap.put(iDsnID, valData);
        }

        valData.add(data);
    }

    public DSNList[] constructDSNList(){
        DSNList[] cids;
        int cidIdx;

        cids   = new DSNList[this.clientIDs.size()];
        cidIdx = 0;

        for(Iterator i=this.clientIDs.entrySet().iterator(); i.hasNext(); ){
            Map.Entry ent    = (Map.Entry)i.next();
            Integer clientId = (Integer)ent.getKey();
            HashMap dsnMap   = (HashMap)ent.getValue();
            ValueList[] dsns;
            int dsnIdx;

            dsns   = new ValueList[dsnMap.size()];
            dsnIdx = 0;
            
            for(Iterator j=dsnMap.entrySet().iterator(); j.hasNext(); ){
                Map.Entry dsnEnt = (Map.Entry)j.next();
                Integer   dsnID  = (Integer)dsnEnt.getKey();
                ArrayList vals   = (ArrayList)dsnEnt.getValue();
                MetricValue[] data;
                int dataIdx;

                data    = new MetricValue[vals.size()];
                dataIdx = 0;
                
                for(Iterator k=vals.iterator(); k.hasNext(); ){
                    MetricValue val = 
                        (MetricValue)k.next();

                    data[dataIdx++] = val;
                }

                dsns[dsnIdx] = new ValueList();
                dsns[dsnIdx].setDsnId(dsnID.intValue());
                dsns[dsnIdx].setValues(data);
                dsnIdx++;
            }
            
            cids[cidIdx] = new DSNList();
            cids[cidIdx].setClientId(clientId.intValue());
            cids[cidIdx].setDsns(dsns);
            cidIdx++;
        }

        return cids;
    }

    public static void dumpReport(DSNList[] cids){
        for(int i=0; i<cids.length; i++){
            ValueList[] dsns = cids[i].getDsns();

            System.out.println(cids[i].getClientId());

            for(int j=0; j<dsns.length; j++){
                MetricValue[] data = dsns[j].getValues();

                System.out.println("\t" + dsns[j].getDsnId());

                for(int k=0; k<data.length; k++){
                    System.out.println("\t\t" + data[k].getTimestamp() +
                                       " " + data[k].getValue());
                }
            }
        }
    }

    public static void main(String[] args){
        MeasurementReportConstructor c;

        c = new MeasurementReportConstructor();
        c.addDataPoint(1, 123, new MetricValue(3, 4));
        c.addDataPoint(1, 123, new MetricValue(2, 75));
        c.addDataPoint(2, 456, new MetricValue(8, 67));
        c.addDataPoint(2, 789, new MetricValue(9, 172));
        c.addDataPoint(2, 456, new MetricValue(100, 59));
        c.addDataPoint(2, 789, new MetricValue(101, 84));

        dumpReport(c.constructDSNList());
    }
}
