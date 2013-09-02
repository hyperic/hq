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

package org.hyperic.hq.measurement.agent.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.agent.server.monitor.AgentMonitorException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.MeasurementCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.data.DSNList;
import org.hyperic.hq.measurement.data.MeasurementReport;
import org.hyperic.hq.measurement.data.MeasurementReportConstructor;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.util.Reference;
import org.hyperic.util.StringUtil;
import org.hyperic.util.encoding.Base64;

/**
 * Deals with sending measurements back to the server (including 
 * persisting them on disk.)
 */

public class SenderThread 
    extends AgentMonitorSimple
    implements Sender, Runnable 
{
    private static final String PROP_METRICDUP  = 
        "agent.metricDup";
    private static final String PROP_MAXBATCHSIZE  = 
        "agent.maxBatchSize";
    private static final String PROP_METRICDEBUG  = 
        "agent.metricDebug";

    // The threshold for logging server offset issues in the agent log.
    // XXX: May need to revisit this, potentially alot of output for
    //      agents that are more than 3 minute ahead or behind the server.
    //      (this may be a good thing)
    private static final long   MAX_SERVERDIFF = 3 * 60 * 1000;

    private static final int    PROP_RECSIZE  = 34; // 34 byte records.

    private static final int    SEND_INTERVAL = 60000;
    private static final int    MAX_BATCHSIZE = 500;
    private static final String MEASURENENT_LISTNAME = "measurement_spool";
    private static final String AVAILABILITY_LISTNAME = "availability_spool";
    
    private volatile boolean                   shouldDie;
    private final          Log                       log;
    private final          MeasurementCallbackClient client;
    private final          AgentStorageProvider      storage;
    private          String                    agentToken;
    private final          LinkedList<Record>        transitionQueue;
    // This toggle will avoid displaying non-stop messages about server down 
    private          int                       metricDup = 0;
    private          int                       maxBatchSize = MAX_BATCHSIZE;
    private final          Set                       metricDebug;
    private final          MeasurementSchedule       schedule;

    // Current difference time between the server and agent in ns.
    // Update on each call to sendMeasurementReport().
    private long serverDiff = 0;

    // Statistics
    private long stat_numBatchesSent     = 0;
    private long stat_totBatchSendTime   = 0;
    private long stat_totMetricsSent     = 0;



    SenderThread(Properties bootProps, AgentStorageProvider storage, MeasurementSchedule schedule)
    throws AgentStartException {
        String sMetricDup, sMaxBatchSize, sMetricDebug;
        this.log             = LogFactory.getLog(SenderThread.class);
        this.shouldDie       = false;
        this.storage         = storage;
        this.client          = setupClient();
        this.transitionQueue = new LinkedList<Record>();
        this.metricDebug     = new HashSet();
        this.schedule        = schedule;

        String measuementInfo = bootProps.getProperty(MEASURENENT_LISTNAME);
        if (measuementInfo != null) {
            storage.addOverloadedInfo(MEASURENENT_LISTNAME, measuementInfo);  
        }
        String AvailabilityInfo = bootProps.getProperty(AVAILABILITY_LISTNAME);
        if (AvailabilityInfo != null) {
            storage.addOverloadedInfo(AVAILABILITY_LISTNAME, AvailabilityInfo);  
        }
        // Setup our storage list
        try {
            // Create list early since we want a smaller recordsize
            // than the default of 1k.
            this.storage.createList(MEASURENENT_LISTNAME, PROP_RECSIZE);
            this.storage.createList(AVAILABILITY_LISTNAME, PROP_RECSIZE);
        } catch (AgentStorageException ignore) {
            // Most likely an agent update where the existing rt schedule
            // already exists.  Will fall back to the old 1k size.
        }

        sMetricDup = bootProps.getProperty(PROP_METRICDUP);
        if(sMetricDup != null){
            try {
                this.metricDup = Integer.parseInt(sMetricDup);
            } catch(NumberFormatException exc){
                throw new AgentStartException(PROP_METRICDUP + " is not " +
                                              "a valid integer ('" + 
                                              sMetricDup + "')");
            }
            
            this.log.info("Duplicating metrics " + this.metricDup + " times");
        }

        sMaxBatchSize = bootProps.getProperty(PROP_MAXBATCHSIZE);
        if(sMaxBatchSize != null){
            try {
                this.maxBatchSize = Integer.parseInt(sMaxBatchSize);
            } catch(NumberFormatException exc){
                throw new AgentStartException(PROP_MAXBATCHSIZE + " is not " +
                                              "a valid integer ('" + 
                                              sMaxBatchSize + "')");
            }

        }

        sMetricDebug = bootProps.getProperty(PROP_METRICDEBUG);
        if(sMetricDebug != null){
            try {
                for (Object element : StringUtil.explode(sMetricDebug, " ")) {
                    Integer metId = new Integer((String)element);

                    this.metricDebug.add(metId);
                    this.log.info("metricDebug:  Enabling special debugging " +
                                  "for metric id=" + metId);
                }
            } catch(NumberFormatException exc){
                throw new AgentStartException(PROP_METRICDEBUG + " must " +
                                              "contain integers seperated by "+
                                              "spaces");
            }
        }

        this.log.info("Maximum metric batch size set to " +  this.maxBatchSize);
    }

    private MeasurementCallbackClient setupClient()
        throws AgentStartException 
    {
        StorageProviderFetcher fetcher;

        fetcher = new StorageProviderFetcher(this.storage);
        return new MeasurementCallbackClient(fetcher);
    }

    void die(){
        this.shouldDie = true; 
    }

    // Use a small class which holds a bunch of the data we need, just so
    // we can pass it back in 1 method call
    private static class Record {
        final int dsnId,
                  derivedID;
        final MetricValue data;
        private Integer hashCode = null;
        //This field is not serialized 
        private final boolean isAvail;

        private Record(int dsnId, MetricValue data, int derivedID, boolean isAvail){
            this.dsnId     = dsnId;
            this.data      = data;
            this.derivedID = derivedID;
            this.isAvail  = isAvail;
        }
        public Record(int dsnID, MetricValue measVal, int derivedID) {
			this (dsnID, measVal, derivedID, false);
		}
		@Override
        public int hashCode() {
            if (hashCode != null) {
                return hashCode.intValue();
            }
            final Long timestamp = new Long(data.getTimestamp()*71);
            final Integer mId = new Integer(derivedID*71);
            hashCode =
                new Integer(7 + (timestamp.hashCode()*71) + (mId.hashCode()*71));
            return hashCode.intValue();
        }
        @Override
        public boolean equals(Object rhs) {
            if (this == rhs) {
                return true;
            }
            if (rhs instanceof Record) {
                Record r = (Record)rhs;
                return ((r.derivedID           == derivedID) &&
                        (r.data.getTimestamp() == data.getTimestamp()));
            }
            return false;
        }
        @Override
        public String toString() {
            return "mId="+derivedID+",timestamp="+data.getTimestamp()+
                ",value="+data.getValue()+",isAvail="+isAvail;
        }
    }

    private static Record decodeRecord(String val)
        throws IOException
    {
        ByteArrayInputStream bIs;
        DataInputStream dIs;
        MetricValue measVal;
        boolean isAvail;
        int derivedID, dsnID;
        long retTime;
        
        bIs = new ByteArrayInputStream(Base64.decode(val));
        dIs = new DataInputStream(bIs);

        derivedID = dIs.readInt();
        retTime   = dIs.readLong();
        dsnID     = dIs.readInt();
        
        measVal = new MetricValue(dIs.readDouble(), retTime);
        return new Record(dsnID, measVal, derivedID);
    }

    private static String encodeRecord(Record record)
        throws IOException 
    {
        ByteArrayOutputStream bOs;
        DataOutputStream dOs;

        bOs = new ByteArrayOutputStream();
        dOs = new DataOutputStream(bOs);

        dOs.writeInt(record.derivedID);
        dOs.writeLong(record.data.getTimestamp());
        dOs.writeInt(record.dsnId);
        dOs.writeDouble(record.data.getValue());
        return Base64.encode(bOs.toByteArray());
    }

    public void processData(int dsnId, MetricValue data, int derivedID, boolean isAvail){
        double val;

        val = data.getValue();
        
        if(this.metricDebug.contains(new Integer(derivedID))){
            this.log.info("metricDebug:  Placing DSN='" + dsnId + 
                          "' derivedID=" + derivedID + " value=" + val + 
                          " time=" + data.getTimestamp() + 
                          " into transition queue");
        }

        synchronized(this.transitionQueue){
        	MetricValue measVal = new MetricValue(val, data.getTimestamp());
            this.transitionQueue.add(new Record(dsnId, measVal, derivedID, isAvail));
        }

        if(this.metricDup != 0){
            ArrayList<Record> dupeList;
            long dTime;
            
            dupeList = new ArrayList<Record>();
            dTime    = data.getTimestamp();
            for(int i=0; i<this.metricDup; i++){
                MetricValue measVal = new MetricValue(val, dTime + i + 1);
                dupeList.add(new Record(dsnId, measVal, derivedID, isAvail));
            }

            synchronized(this.transitionQueue){
                this.transitionQueue.addAll(dupeList);
            }
        }
    }

    /**
     * This routine moves all the data from the transition queue into
     * the storage provider, so it can be shipped to the server.
     */
    private void processTransitionQueue(){
    	String encodedRec;
    	synchronized(this.transitionQueue){
    		for (Record rec : this.transitionQueue) {
    			try {
    				encodedRec = encodeRecord(rec);
    				if (rec.isAvail) {
    					this.storage.addToList(AVAILABILITY_LISTNAME, encodedRec);
    				}
    				else {
    					this.storage.addToList(MEASURENENT_LISTNAME, encodedRec);
    				}
    			} catch(Exception exc){
    				this.log.error("Unable to store data: " + exc, exc);
    			}
    		}

            this.transitionQueue.clear();

            try {
                this.storage.flush();
            } catch(Exception exc){
                this.log.error("Unable to flush storage", exc);
            }
        }
    }

    /**
     * Send a batch of measurement points back to the server.  This
     * method sends at most maxBatchSize elements back.
     *
     * @return null if all the measurements in the list have been
     *          sent, otherwise it returns the timestamp of the last
     *          measurement sent to the server.
     */
    private Long sendBatch(String listName, Reference<Integer> numSent) {
        MeasurementReportConstructor constructor;
        DSNList[] clientIds;
        long batchStart = 0, batchEnd = 0, lastMetricTime = 0, serverTime = 0;
        boolean success;
        int numUsed, numDebuggedSent;

        // Before sending the data off, make sure our transition queue is
        // empty

        this.processTransitionQueue();

        numUsed         = 0;
        numDebuggedSent = 0;
        constructor     = new MeasurementReportConstructor();
        
        final boolean debug = log.isDebugEnabled();
        log.debug("Sending batch to server:");
        Set<Record> records = new HashSet<Record>();
        // first we are going to ensure that all the data points that
        // we send over to the server are unique
        for (Iterator<String> it=storage.getListIterator(listName); (it!=null) && it.hasNext() && (numUsed < maxBatchSize); numUsed++) {
            try {
                Record r = SenderThread.decodeRecord(it.next());
                boolean didNotAlreadyExist = records.add(r); 
                if (!didNotAlreadyExist) {
                    // nuke the dup
                    if (debug) {
                        log.debug("Dropping duplicate entry for " + r);
                    }
                    numUsed--;
                }
            } catch(IOException exc){
                this.log.error("Error accessing record -- deleting: " + exc, exc);
                continue;
            }
        }

        int num = 0;
        long firstMetricTime = Long.MAX_VALUE;

        for (Iterator<Record> it=records.iterator(); it.hasNext(); num++) {
            Record rec = it.next();

            lastMetricTime = rec.data.getTimestamp();

            if(log.isDebugEnabled()){
                this.log.debug("    Data:  d=" + rec.derivedID + 
                               " r=" + rec.dsnId +
                               " t=" + rec.data.getTimestamp() +
                               " v=" + rec.data);
            }

            if(this.metricDebug.contains(new Integer(rec.derivedID))){
                numDebuggedSent++;
                this.log.info("metricDebug:  Pulled DSN=" + rec.dsnId + 
                              " derivedID=" + rec.derivedID + " value=" + 
                              rec.data.getValue() + " from tQueue -- sending");
            }
            constructor.addDataPoint(rec.derivedID, rec.dsnId, rec.data);
        }
        numSent.set(num);

        // If we don't have anything to send -- move along
        if(numUsed == 0) {
            return null;
        }
        
        clientIds = constructor.constructDSNList();
        
        success = false;
        try {
            MeasurementReport report;

            SRN[] srnList = this.schedule.getSRNsAsArray();
            if (srnList.length == 0) {
                log.error("Agent does not have valid SRNs, but has metric data to send, removing measurements");
                removeMeasurements(numUsed, listName);
                return null;
            }
            
            if(log.isDebugEnabled()){
                for (SRN element : srnList) {
                    this.log.debug("    SRN: " + element.getEntity() +  "=" + element.getRevisionNumber());
                }
            }

            report = new MeasurementReport();
            if (this.agentToken == null) {
                this.agentToken = storage.getValue(CommandsAPIInfo.PROP_AGENT_TOKEN);
            }
            report.setAgentToken(this.agentToken);
            report.setClientIdList(clientIds);
            report.setSRNList(srnList);
            batchStart = now();
            try {
                serverTime = this.client.measurementSendReport(report);
            } catch (IllegalArgumentException e) {
                throw new SystemException("error sending report: " + e + ", report=" + report, e);
            }
            batchEnd = now();

            // Compute offset from server (will include network latency)
            this.serverDiff = Math.abs(serverTime - batchEnd);

            // Update the ServerTimeDiff object with the time offset between the
            // agent and the server and update the last sync time to now
            ServerTimeDiff.getInstance().setServerTimeDiff(serverTime - batchEnd);
            ServerTimeDiff.getInstance().setLastSync(batchEnd);

            if (this.serverDiff > MAX_SERVERDIFF) {
                // Complain if we are ahead or behind.  This may be a bit
                // too excessive.
                if (serverTime < batchEnd) {
                    this.log.error("Agent is " + (this.serverDiff / 1000) + 
                                   " seconds ahead of the server.  To " +
                                   "ensure accuracy of the charting and " +
                                   "alerting make sure the agent and server " +
                                   "clocks are synchronized");
                } else {
                    this.log.error("Agent is " + (this.serverDiff / 1000) + 
                                   " seconds behind the server.  To " +
                                   "ensure accuracy of the charting and " +
                                   "alerting make sure the agent and server " +
                                   "clocks are synchronized");
                }
            }
            success = true;
        } catch(AgentCallbackClientException exc){
            log.error("Error sending measurements: " +  exc.getMessage(), exc);
            // return this so that the caller will attempt a retry on everything except Connection refused
            if (!exc.getMessage().toLowerCase().endsWith("refused")) {
                log.info("retrying measurement send");
                return firstMetricTime;
            }
        } finally {
            if(numDebuggedSent != 0){
                if(success){
                    this.log.info("metricDebug:  Successfully sent " +
                                  numDebuggedSent + " debugged metrics to " +
                                  "server");
                } else {
                    this.log.info("metricDebug:  Server reported failure " +
                                  "when sent " + numDebuggedSent + 
                                  " debugged metrics");
                }
            }
        }
        
        if(success){
            removeMeasurements(numUsed, listName);

            this.stat_numBatchesSent++;
            this.stat_totBatchSendTime += (batchEnd - batchStart);
            this.stat_totMetricsSent += numUsed;

            if(numUsed == this.maxBatchSize){
                return new Long(lastMetricTime);
            } else {
                return null;
            }
        }
        return null;
    }

    private long now() {
        return System.currentTimeMillis();
    }

    /**
     * @return The number of measurements removed from the metric storage.
     *
     * @param num The maximum number of datapoints to remove.
     */
    private int removeMeasurements(int num, String listName) {
        int j = 0;

        for (Iterator i = this.storage.getListIterator(listName);
             (i != null) && i.hasNext() && (j < num);
             j++) {
            i.next();
            i.remove();
        }

        try {
            this.storage.flush();
        } catch (AgentStorageException exc) {
            this.log.error("Failed to flush agent storage", exc);
        }

        if(j != num){
            this.log.error("Failed to remove " + (num - j) + "records");
        }

        return j;
    }

    /**
     * MONITOR METHOD:  Get the number of batches which have successfully 
     *                  sent to the server
     */
    public double getNumBatchesSent() 
        throws AgentMonitorException 
    {
        return this.stat_numBatchesSent;
    }

    /**
     * MONITOR METHOD:  Get the total amount of time that the client has
     *                  spent sending batches
     */
    public double getTotBatchSendTime() 
        throws AgentMonitorException 
    {
        return this.stat_totBatchSendTime;
    }

    /**
     * MONITOR METHOD:  Get the total number of metrics which have been
     *                  transmitted to the server
     */
    public double getTotMetricsSent() throws AgentMonitorException {
        return this.stat_totMetricsSent;
    }

    /**
     * MONITOR METHOD:  Get the offset in ms between the agent and server
     */
    public double getServerOffset() throws AgentMonitorException {
        return this.serverDiff;
    }

    public void run(){
       
        Calendar controlCal = Calendar.getInstance();
        controlCal.setTimeInMillis(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        controlCal.set(Calendar.SECOND, 5);
        
        while (this.shouldDie == false) {
            try {
                try {
                    controlCal.add(Calendar.MINUTE, 1);
                    long now = System.currentTimeMillis();
                    cal.setTimeInMillis(now + SEND_INTERVAL);
                    // want to keep some randomness for all agents to send their
                    // data.  This way an agent is not pegged to a certain
                    // second interval
                    if (cal.get(Calendar.MINUTE) != controlCal.get(Calendar.MINUTE)) {
                        long sleeptime = controlCal.getTimeInMillis() - now;
                        if (sleeptime > 0) {
                            Thread.sleep(controlCal.getTimeInMillis() - now);
                        }
                    } else {
                        Thread.sleep(SEND_INTERVAL);
                    }
                } catch(InterruptedException exc){
                    this.log.info("Measurement sender interrupted");
                    return;
                }

                if (log.isDebugEnabled()) {
                    log.debug("Woke up, sending batch of metrics.");
                }
                //We are sending 2 different batches, the first is for availability and the second
                //is for measurement. We are doing this because we want to make sure that availability 
                //data will get processed by the server even if the server is not able to process measurement
                //data at the moment (Jira issue [HHQ-5566])
                Reference<Integer> numSent = new Reference<Integer>(0);
                sendData(AVAILABILITY_LISTNAME, numSent);
                sendData(MEASURENENT_LISTNAME, numSent);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }
    
    private void sendData(String listName, Reference<Integer> numSent) {
    	Long lastMetricTime;
        lastMetricTime = this.sendBatch(listName, numSent);
        if(lastMetricTime != null){
            String backlogNum = "";
            final long start = System.currentTimeMillis();
            // Give it a single shot to catch up before starting to squawk
            while((lastMetricTime = this.sendBatch(listName, numSent)) != null) {
                long now = System.currentTimeMillis();
                long tDiff = now - lastMetricTime.longValue();
                String backlog = Long.toString(tDiff / (60 * 1000));
                if(((tDiff / (60 * 1000)) > 1) && (backlog.equals(backlogNum) == false)) {
                    backlogNum = backlog;
                    this.log.info(backlog +  " minute(s) of metrics backlogged");
                }
                if(this.shouldDie == true){
                    this.log.info("Dying with measurements backlogged");
                    return;
                }
            }
            final long total = System.currentTimeMillis() - start;
            if (total > SEND_INTERVAL) {
                log.info("Agent took " + (total/1000) + " seconds to send its " + listName + " metrics to the HQ Server.");
            } else if (log.isDebugEnabled()) {
                log.debug("Agent took " + total + " ms to send its " + listName);
            }
        }
    }
}
