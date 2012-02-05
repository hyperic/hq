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

package org.hyperic.hq.measurement.agent.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.measurement.agent.ScheduledMeasurement;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.util.encoding.Base64;


/**
 * Class which does the storage/retrieval of schedule information from
 * the Agent's simple storage provider.
 */

class MeasurementSchedule {
    private static final String PROP_MSCHED = "measurement_schedule";
    private static final String PROP_MSRNS =  "measurement_srn";
    
    private final AgentStorageProvider store;      
    private final ArrayList            srnList;
    private final Log                  log;

    MeasurementSchedule(AgentStorageProvider store, Properties bootProps) {
        String info = bootProps.getProperty(PROP_MSCHED);
        if (info != null) {
            store.addOverloadedInfo(PROP_MSCHED, info);
        }

        this.store    = store;
        this.srnList  = new ArrayList();
        this.log      = LogFactory.getLog(MeasurementSchedule.class);

        this.populateSRNInfo();
    }

    private void populateSRNInfo(){
        ByteArrayInputStream bIs;
        DataInputStream dIs;
        HashSet seenEnts;
        String encSRNList;

        this.srnList.clear();

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
                    this.log.warn("Entity '" + ent + "' contained more than " +
                                  "once in SRN storage.  Ignoring");
                    continue;
                }

                seenEnts.add(ent);
                this.srnList.add(new SRN(ent, revNo));
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
                
                for(Iterator i=this.srnList.iterator(); i.hasNext(); ){
                    SRN srn = (SRN)i.next();
                    AppdefEntityID ent = srn.getEntity();
                    
                    dOs.writeInt(ent.getType());
                    dOs.writeInt(ent.getID());
                    dOs.writeInt(srn.getRevisionNumber());
                }
                this.store.setValue(MeasurementSchedule.PROP_MSRNS, 
                                    Base64.encode(bOs.toByteArray()));
            } catch(IOException exc){
                this.log.error("Error encoding SRN list");
                return;
            }
        }
    }

   
    /**
     * Converts all the records (Strings) from the collection into ScheduledMeasurement objects
     * and returns an Iterator to a collection containing this metrics
     */
    private Iterator<ScheduledMeasurement> createMeasurementList(Collection<String> records){
        ArrayList<ScheduledMeasurement> metrics = new ArrayList<ScheduledMeasurement>();
        long i = -1;
        for (String value : records) {
            i++;
        	ScheduledMeasurement metric;
            if((metric = ScheduledMeasurement.decode(value)) == null){
                this.log.error("Unable to decode metric from storage, deleting.");
                try {
					this.store.removeFromList(MeasurementSchedule.PROP_MSCHED, i);
				} catch (AgentStorageException e) {
					log.debug(e,e);
				}
                continue;
            }
            metrics.add(metric);
        }
        log.info("Number of metrics decoded from the storage - " + metrics.size());
        return metrics.iterator();
    }

    /**
     * Get a list of all the measurements within the storage.
     * @throws IOException 
     */
    public Iterator<ScheduledMeasurement> getMeasurementList() throws IOException {
    	Collection<String> records = new ArrayList<String>();
    	try {
    		readRecordsFromStorage(records);
    	}
    	catch (Exception e) {
    		//For version 4.6.5 the default record size for Disk list was changed from 1024 to 4000
    		//If we get an exception here this is probably because this is the first startup after an
    		//upgrade from a pre 4.6.5 version and we need to try to fix the list 
    		log.warn("Error reading measurement list from storage = '" + e.getMessage() + "' ," +
    				" trying to convert the list records size");
    		this.store.convertListToCurrentRecordSize(MeasurementSchedule.PROP_MSCHED);
    		//If this time readRecordsFromStorage() we don't want to catch the exception,
    		//the AgentDeamon will catch this exception and fail the agent startup
    		readRecordsFromStorage(records);
    	}
    	return createMeasurementList(records);
    }

    /**
     * Reads all the records from the measurement_schedule storage list and writes
     * them into the collection
     * @param records - adds all the records from the storage to this collection
     */
    private void readRecordsFromStorage(Collection<String> records) {
    	Iterator<String> i;
    	records.clear();
    	i = this.store.getListIterator(MeasurementSchedule.PROP_MSCHED);
    	for(; i != null && i.hasNext(); ){
    		String value = i.next();
    		records.add(value);
    	}
    }

    void storeMeasurement(ScheduledMeasurement newMeas)
        throws AgentStorageException 
    {
        this.store.addToList(MeasurementSchedule.PROP_MSCHED,
                             newMeas.encode());
        this.store.flush();
    }

    void updateSRN(SRN updSRN)
        throws AgentStorageException
    {
        AppdefEntityID ent = updSRN.getEntity();
        boolean toWrite = false, found = false;

        synchronized(this.srnList){
            final boolean debug = log.isDebugEnabled();
            for(Iterator i=this.srnList.iterator(); i.hasNext(); ){
                SRN srn = (SRN) i.next();

                if(found = srn.getEntity().equals(ent)){
                    if (toWrite =
                        srn.getRevisionNumber() != updSRN.getRevisionNumber()) {
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

                if (srn.getEntity().equals(ent)) {
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
     * Delete measurements matching a specific client IDs.
     * 
     * @param clientID Metrics matching this ID will be deleted
     * @param srnInfo  Updated SRN information for the entity which
     *                 matches the clientID.  If this value is null
     *                 (i.e. it is the last clientID for a resource, or
     *                 an update of the SRN is unnecessary), then the
     *                 SRN will not be updated.
     */
    void deleteMeasurements(Set<AppdefEntityID> aeids) throws AgentStorageException  {
        if (aeids == null || aeids.isEmpty()) {
            return;
        }
        final Iterator<String> i = store.getListIterator(MeasurementSchedule.PROP_MSCHED);
        while (i != null && i.hasNext()){
            final String value = i.next();
            final ScheduledMeasurement meas = ScheduledMeasurement.decode(value);
            if (null == meas) {
                log.error("Unable to decode metric from storage, removing metric for entity");
                i.remove();
                continue;
            }
            final AppdefEntityID entity = meas.getEntity();
            if (aeids.contains(entity)) {
                log.debug("Removing scheduled measurement " + meas);
                i.remove();
            }
        }
        final Iterator<SRN> it = srnList.iterator();
        // Clear out the SRN 
        synchronized(this.srnList){
            while (it.hasNext()) {
                final SRN srn = it.next();
                final AppdefEntityID entity = srn.getEntity();
                if (aeids.contains(entity)) {
                    it.remove();
                }
            }
            writeSRNs();
        }
        store.flush();
    }

    SRN[] getSRNsAsArray(){
        synchronized(this.srnList){
            return (SRN[]) this.srnList.toArray(new SRN[0]);
        }
    }
}
