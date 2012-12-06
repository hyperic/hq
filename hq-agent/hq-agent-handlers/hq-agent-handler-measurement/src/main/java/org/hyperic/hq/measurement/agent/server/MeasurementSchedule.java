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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
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
    private static final String PROP_MSRNS_LENGTH = "measurement_srn_length";
    private static final int MAX_ELEM_SIZE = 10000;
    
    private final AgentStorageProvider store;      
    private final ArrayList<SRN> srnList = new ArrayList<SRN>();
    private final Log log = LogFactory.getLog(MeasurementSchedule.class);

    MeasurementSchedule(AgentStorageProvider store, Properties bootProps) {
        String info = bootProps.getProperty(PROP_MSCHED);
        if (info != null) {
            store.addOverloadedInfo(PROP_MSCHED, info);
        }
        this.store    = store;
        this.populateSRNInfo();
    }

    private void populateSRNInfo(){
        srnList.clear();
        final String lengthBuf = store.getValue(PROP_MSRNS_LENGTH);
        final List<Byte> encSRNBytes = new ArrayList<Byte>();
        if (lengthBuf == null) {
            final String mSchedBuf = store.getValue(PROP_MSRNS);
            if (mSchedBuf == null) {
                log.warn("no srns to retrieve from storage");
                return;
            }
            final byte[] bytes = Base64.decode(mSchedBuf);
            encSRNBytes.addAll(Arrays.asList(ArrayUtils.toObject(bytes)));
        } else {
            final int length = Integer.parseInt(lengthBuf);
            for (int i=0; i<length; i++) {
                final byte[] bytes = Base64.decode(store.getValue(PROP_MSRNS + "_" + i));
                encSRNBytes.addAll(Arrays.asList(ArrayUtils.toObject(bytes)));
            }
        }
        byte[] srnBytes = ArrayUtils.toPrimitive(encSRNBytes.toArray(new Byte[0]));
        HashSet<AppdefEntityID> seenEnts = new HashSet<AppdefEntityID>();
        String srnBuf = new String(srnBytes);
        ByteArrayInputStream bIs = new ByteArrayInputStream(srnBytes);
        DataInputStream dIs = new DataInputStream(bIs);
        try {
            int numSRNs = dIs.readInt();
            int entType, entID, revNo;

            for (int i=0; i<numSRNs; i++) {
                entType = dIs.readInt();
                entID   = dIs.readInt();
                revNo   = dIs.readInt();
                AppdefEntityID ent = new AppdefEntityID(entType, entID);
                if(seenEnts.contains(ent)){
                    log.warn("Entity '" + ent + "' contained more than once in SRN storage.  Ignoring");
                    continue;
                }
                seenEnts.add(ent);
                srnList.add(new SRN(ent, revNo));
            }
        } catch(IOException exc){
            this.log.error("Unable to decode SRN list: " + exc + " srn=\"" + srnBuf + "\"", exc);
        }
    }

    private void writeSRNs() throws AgentStorageException {
        ByteArrayOutputStream bOs;
        DataOutputStream dOs;
        bOs = new ByteArrayOutputStream();
        dOs = new DataOutputStream(bOs);
        synchronized(srnList){
            try {
                dOs.writeInt(srnList.size());
                for(SRN srn : srnList) {
                    AppdefEntityID ent = srn.getEntity();
                    dOs.writeInt(ent.getType());
                    dOs.writeInt(ent.getID());
                    dOs.writeInt(srn.getRevisionNumber());
                }
                List<Byte> bytes = Arrays.asList(ArrayUtils.toObject(bOs.toByteArray()));
                int size = bytes.size();
                if (size > MAX_ELEM_SIZE) {
                    store.setValue(PROP_MSRNS_LENGTH, new Integer((size/MAX_ELEM_SIZE) + 1).toString());
                    int ii=0;
                    for (int i=0; i<size; i+=MAX_ELEM_SIZE) {
                        int start = i;
                        int max = Math.min(i+MAX_ELEM_SIZE, size);
                        List<Byte> subList = bytes.subList(start, max);
                        Byte[] b = subList.toArray(new Byte[0]);
                        store.setValue(MeasurementSchedule.PROP_MSRNS + "_" + ii++, Base64.encode(ArrayUtils.toPrimitive(b)));
                    }
                } else {
                    store.setValue(PROP_MSRNS_LENGTH, "1");
                    Byte[] b = bytes.toArray(new Byte[0]);
                	store.setValue(MeasurementSchedule.PROP_MSRNS + "_0", Base64.encode(ArrayUtils.toPrimitive(b)));
                }
           } catch(IOException exc){
                this.log.error("Error encoding SRN list", exc);
                return;
            }
        }
    }

   
    /**
     * Converts all the records (Strings) from the collection into ScheduledMeasurement objects
     * and returns an Iterator to a collection containing this metrics
     */
    private Iterator<ScheduledMeasurement> createMeasurementList(Collection<String> records){
        Set<ScheduledMeasurement> metrics = new HashSet<ScheduledMeasurement>();
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
    public synchronized Iterator<ScheduledMeasurement> getMeasurementList() throws IOException {
        Collection<String> records = new ArrayList<String>();
        try {
            readRecordsFromStorage(records);
        }
        catch (Exception e) {
            //For version 4.6.5 the default record size for Disk list was changed from 1024 to 4000
            //If we get an exception here this is probably because this is the first startup after an
            //upgrade from a pre 4.6.5 version and we need to try to fix the list 
            log.warn("Error reading measurement list from storage = '" + e + "' ," +
                     " trying to convert the list records size");
            store.convertListToCurrentRecordSize(MeasurementSchedule.PROP_MSCHED);
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
        i = store.getListIterator(MeasurementSchedule.PROP_MSCHED);
        for(; i != null && i.hasNext(); ){
            String value = i.next();
            records.add(value);
        }
    }

    synchronized void storeMeasurements(Collection<ScheduledMeasurement> measurements) throws AgentStorageException {
        for (ScheduledMeasurement m : measurements) {
            final String encoded = m.encode();
            store.addToList(MeasurementSchedule.PROP_MSCHED, encoded.toString());
        }
        store.flush();
    }

    synchronized void storeMeasurement(ScheduledMeasurement newMeas) throws AgentStorageException {
        final String encoded = newMeas.encode();
        store.addToList(MeasurementSchedule.PROP_MSCHED, encoded.toString());
        store.flush();
    }

    void updateSRN(SRN updSRN) throws AgentStorageException {
        AppdefEntityID ent = updSRN.getEntity();
        boolean toWrite = false;
        boolean found = false;
        synchronized(this.srnList){
            final boolean debug = log.isDebugEnabled();
            for (SRN srn : srnList) {
                if(found = srn.getEntity().equals(ent)) {
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
                log.debug("Adding new SRN for entity " + ent +  ": Initial value = " + updSRN.getRevisionNumber());
                srnList.add(updSRN);
                toWrite = true;
            }
            if(toWrite){
                writeSRNs();
            }
        }
    }

    void removeSRN(AppdefEntityID ent) throws AgentStorageException {
        boolean debug = this.log.isDebugEnabled();
        boolean toWrite = false, found = false;

        synchronized (this.srnList) {
            for (Iterator<SRN> i = this.srnList.iterator(); i.hasNext();) {
                SRN srn = i.next();
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
    synchronized void deleteMeasurements(Set<AppdefEntityID> aeids) throws AgentStorageException  {
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
        // Clear out the SRN 
        synchronized(this.srnList){
            final Iterator<SRN> it = srnList.iterator();
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