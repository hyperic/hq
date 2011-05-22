/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.agent.handler.measurement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.operation.SRN;
import org.hyperic.util.encoding.Base64;

import java.io.*;
import java.util.*;


/**
 * Class which does the storage/retrieval of schedule information from
 * the Agent's simple storage provider.
 */

class MeasurementSchedule {

    private Log                  log = LogFactory.getLog(MeasurementSchedule.class);

    private static final String PROP_MSCHED = "measurement_schedule";

    private static final String PROP_MSRNS =  "measurement_srn";
    
    private AgentStorageProvider store;      

    private final List<org.hyperic.hq.operation.SRN> newSrnList;
    private final List<org.hyperic.hq.measurement.server.session.SRN> srnList;

    MeasurementSchedule(AgentStorageProvider store, Properties bootProps) {
        String info = bootProps.getProperty(PROP_MSCHED);
        if (info != null) store.addOverloadedInfo(PROP_MSCHED, info);

        this.store    = store;
        this.newSrnList  = new ArrayList<org.hyperic.hq.operation.SRN>();
        this.srnList  = new ArrayList<org.hyperic.hq.measurement.server.session.SRN>();
        this.populateSRNInfo();
    }

    private void populateSRNInfo(){
        ByteArrayInputStream bIs;
        DataInputStream dIs;
        HashSet seenEnts;
        String encSRNList;
        this.srnList.clear();
        this.newSrnList.clear();

        encSRNList = this.store.getValue(MeasurementSchedule.PROP_MSRNS);
        if(encSRNList == null){
            return;
        }

        seenEnts = new HashSet();

        bIs = new ByteArrayInputStream(Base64.decode(encSRNList));
        dIs = new DataInputStream(bIs);

        try {
            int numSRNs = dIs.readInt();
            int entType, entID, revNo;

            for(int i=0; i<numSRNs; i++){
                AppdefEntityID ent;

                entType = dIs.readInt();
                entID   = dIs.readInt();
                revNo   = dIs.readInt();

                ent = new AppdefEntityID(entType, entID);
                if(seenEnts.contains(ent)){
                    this.log.warn("Entity '" + ent + "' contained more than once in SRN storage.  Ignoring");
                    continue;
                }

                seenEnts.add(ent);
                this.srnList.add(new org.hyperic.hq.measurement.server.session.SRN(ent, revNo));
            }
        } catch(IOException exc){
            this.log.error("Unable to decode SRN list: " + exc.getMessage());
        }
    }

    private void writeSRNs()
        throws AgentStorageException
    {
        ByteArrayOutputStream bOs;
        DataOutputStream dOs;

        bOs = new ByteArrayOutputStream();
        dOs = new DataOutputStream(bOs);

        synchronized(this.srnList){
            try {
                dOs.writeInt(this.srnList.size());

                for (org.hyperic.hq.measurement.server.session.SRN srn : this.srnList) {
                    AppdefEntityID ent = srn.getEntity(); 
                    dOs.writeInt(ent.getType());
                    dOs.writeInt(ent.getID());
                    dOs.writeInt(srn.getRevisionNumber());
                }
                this.store.setValue(MeasurementSchedule.PROP_MSRNS, Base64.encode(bOs.toByteArray()));
            } catch(IOException exc){
                this.log.error("Error encoding SRN list");
                return;
            }
        }
    }

    /**
     * Get a list of all the measurements within the storage.
     */
    Iterator getMeasurementList(){
        ArrayList r = new ArrayList();
        Iterator i;

        i = this.store.getListIterator(MeasurementSchedule.PROP_MSCHED);
        
        for(; i != null && i.hasNext(); ){
            String value = (String)i.next();
            ScheduledMeasurement metric;

            if((metric = ScheduledMeasurement.decode(value)) == null){
                this.log.error("Unable to decode metric from storage, deleting.");
                i.remove();
                continue;
            }

            r.add(metric);
        }
        return r.iterator();
    }

    void storeMeasurement(ScheduledMeasurement newMeas)
        throws AgentStorageException 
    {
        this.store.addToList(MeasurementSchedule.PROP_MSCHED,
                             newMeas.encode());
        this.store.flush();
    }

    void updateSRN(org.hyperic.hq.measurement.server.session.SRN updSRN)
        throws AgentStorageException
    {
        org.hyperic.hq.appdef.shared.AppdefEntityID ent = updSRN.getEntity();
        boolean toWrite = false, found = false;

        synchronized(this.srnList){
            final boolean debug = log.isDebugEnabled();
            for (Object aSrnList : this.srnList) {
                SRN srn = (SRN) aSrnList;

                if (found = srn.getId().equals(ent)) {
                    if (toWrite = srn.getRevisionNumber() != updSRN.getRevisionNumber()) {
                        if (debug) {
                            log.debug("Updating SRN for " + ent +
                                    " from " + srn.getRevisionNumber() +
                                    " to " + updSRN.getRevisionNumber());
                        }
                        srn.setRevisionNumber(updSRN.getRevisionNumber());
                    }
                    break;
                }
            }

            if(!found){
                this.log.debug("Adding new SRN for entity " + ent + 
                               ": Initial value = " + 
                               updSRN.getRevisionNumber());
                this.srnList.add(updSRN);
                toWrite = true;
            }

            if(toWrite){
                this.writeSRNs();
            }
        }
    }

    void removeSRN(AppdefEntityID ent) throws AgentStorageException {
        boolean debug = this.log.isDebugEnabled();
        boolean toWrite = false, found = false;

        synchronized (this.srnList) {
            for (Iterator i = this.srnList.iterator(); i.hasNext();) {
                SRN srn = (SRN) i.next();

                if (srn.getId().equals(ent)) {
                    found = true;
                    i.remove();
                    toWrite = true;
                    break;
                }
            }

            if (found) {
                if (debug) {
                    this.log.debug("SRN for entity " + ent + " removed");
                }
            } else {
                if (debug) {
                    this.log.debug("SRN for entity " + ent + " not found");
                }
            }

            if (toWrite) {
                this.writeSRNs();
            }
        }
    }

    /**
     * Delete measurements matching a specific client ID.  
     * 
     * @param ent Metrics matching this ID will be deleted 
     */
    void deleteMeasurements(org.hyperic.hq.appdef.shared.AppdefEntityID ent)
        throws AgentStorageException 
    {
        boolean debug = this.log.isDebugEnabled();
        Iterator i;

        i = this.store.getListIterator(MeasurementSchedule.PROP_MSCHED);
        for(; i != null && i.hasNext(); ){
            String value = (String)i.next();
            ScheduledMeasurement meas;

            if((meas = ScheduledMeasurement.decode(value)) == null){
                this.log.error("Unable to decode metric from storage, "
                                    + "removing metric for entity " + ent);
                i.remove();
                continue;
            }

            if(meas.getEntity().equals(ent)){
                this.log.debug("Removing scheduled measurement " + meas);
                i.remove();
            }
        }

        // Clear out the SRN 
        synchronized(this.srnList){
            for(i=this.srnList.iterator(); i.hasNext(); ){
                SRN srn = (SRN)i.next();

                if(srn.getId().equals(ent)){
                    i.remove();
                }
            }

            this.writeSRNs();
        }

        this.store.flush();
    }

    public List<org.hyperic.hq.operation.SRN> getSrnList() {
        return newSrnList;
    }
}
