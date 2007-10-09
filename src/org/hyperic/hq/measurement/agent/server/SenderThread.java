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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.agent.server.monitor.AgentMonitorException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.MeasurementCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.measurement.data.DSNList;
import org.hyperic.hq.measurement.data.MeasurementReport;
import org.hyperic.hq.measurement.data.MeasurementReportConstructor;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.encoding.Base64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Deals with sending measurements back to the server (including 
 * persisting them on disk.)
 */

public class SenderThread 
    extends AgentMonitorSimple
    implements Runnable 
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
    private static final String DATA_LISTNAME = "measurement_spool";

    private volatile boolean                   shouldDie;
    private          Log                       log;
    private          MeasurementCallbackClient client;
    private          AgentStorageProvider      storage;
    private          String                    agentToken;
    private          LinkedList                transitionQueue;
    // This toggle will avoid displaying non-stop messages about server down 
    private          boolean                   serverError = false;
    private          int                       metricDup = 0;
    private          int                       maxBatchSize = MAX_BATCHSIZE;
    private          Set                       metricDebug;
    private          MeasurementSchedule       schedule;

    // Current difference time between the server and agent in ns.
    // Update on each call to sendMeasurementReport().
    private long serverDiff = 0;

    // Statistics
    private long stat_numBatchesSent     = 0;
    private long stat_totBatchSendTime   = 0;
    private long stat_totMetricsSent     = 0;

    SenderThread(Properties bootProps, AgentStorageProvider storage,
                 MeasurementSchedule schedule)
        throws AgentStartException 
    {
        String sMetricDup, sMaxBatchSize, sMetricDebug;
        this.log             = LogFactory.getLog(SenderThread.class);
        this.shouldDie       = false;
        this.storage         = storage;
        this.client          = setupClient();
        this.transitionQueue = new LinkedList();
        this.metricDebug     = new HashSet();
        this.schedule        = schedule;

        // Setup our storage list
        try {
            // Create list early since we want a smaller recordsize
            // than the default of 1k.
            this.storage.createList(DATA_LISTNAME, PROP_RECSIZE);
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
                for(Iterator i=StringUtil.explode(sMetricDebug, " ").iterator();
                    i.hasNext();)
                {
                    Integer metId = new Integer((String)i.next());

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

        this.log.info("Maximum metric batch size set to " + 
                      this.maxBatchSize);
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
        int              dsnId;
        MetricValue data;
        int              derivedID;

        private Record(int dsnId, MetricValue data, int derivedID){
            this.dsnId     = dsnId;
            this.data      = data;
            this.derivedID = derivedID;
        }
    }

    private static Record decodeRecord(String val)
        throws IOException
    {
        ByteArrayInputStream bIs;
        DataInputStream dIs;
        MetricValue measVal;
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

    private static String encodeRecord(int dsnId, long timestamp, 
                                       double value, int derivedID)
        throws IOException 
    {
        ByteArrayOutputStream bOs;
        DataOutputStream dOs;

        bOs = new ByteArrayOutputStream();
        dOs = new DataOutputStream(bOs);

        dOs.writeInt(derivedID);
        dOs.writeLong(timestamp);
        dOs.writeInt(dsnId);
        dOs.writeDouble(value);
        return Base64.encode(bOs.toByteArray());
    }

    void processData(int dsnId, MetricValue data, int derivedID){
        String encodedRec;
        double val;

        val = data.getValue();
        try {
            encodedRec = encodeRecord(dsnId, data.getTimestamp(), val, 
                                      derivedID);
        } catch(IOException exc){
            this.log.error("Unable to encode record", exc);
            return;
        }
        
        if(this.metricDebug.contains(new Integer(derivedID))){
            this.log.info("metricDebug:  Placing DSN='" + dsnId + 
                          "' derivedID=" + derivedID + " value=" + val + 
                          " time=" + data.getTimestamp() + 
                          " into transition queue");
        }

        synchronized(this.transitionQueue){
            this.transitionQueue.add(encodedRec);
        }

        if(this.metricDup != 0){
            ArrayList dupeList;
            long dTime;
            
            dupeList = new ArrayList();
            dTime    = data.getTimestamp();
            for(int i=0; i<this.metricDup; i++){
                try {
                    encodedRec = encodeRecord(dsnId, dTime + i + 1, val,
                                              derivedID);
                } catch(IOException exc){
                    this.log.error("Unable to encode record", exc);
                }
                dupeList.add(encodedRec);
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
        synchronized(this.transitionQueue){
            for(Iterator i=this.transitionQueue.iterator(); i.hasNext(); ){
                String val = (String)i.next();
                
                try {
                    this.storage.addToList(DATA_LISTNAME, val);
                } catch(Exception exc){
                    this.log.error("Unable to store data", exc);
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
    private Long sendBatch(){
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

        for(Iterator i=this.storage.getListIterator(DATA_LISTNAME);
            i != null && i.hasNext() && numUsed < this.maxBatchSize;
            numUsed++)
        {                
            Record rec;
            
            if(numUsed == 0){  // Only on the first loop iteration
                this.log.debug("Sending batch to server:");
            }

            try {
                rec = SenderThread.decodeRecord((String)i.next());
            } catch(IOException exc){
                this.log.error("Error accessing record -- deleting: " +
                               exc);
                continue;
            }

            lastMetricTime = rec.data.getTimestamp();
            
            if(log.isDebugEnabled()){
                this.log.debug("    Data:  d=" + rec.derivedID + 
                               " r=" + rec.dsnId + " v=" + rec.data);
            }

            if(this.metricDebug.contains(new Integer(rec.derivedID))){
                numDebuggedSent++;
                this.log.info("metricDebug:  Pulled DSN=" + rec.dsnId + 
                              " derivedID=" + rec.derivedID + " value=" + 
                              rec.data.getValue() + " from tQueue -- sending");
            }

            constructor.addDataPoint(rec.derivedID, rec.dsnId, rec.data);
        }

        // If we don't have anything to send -- move along
        if(numUsed == 0)
            return null;
        
        clientIds = constructor.constructDSNList();
        
        success = false;
        try {
            MeasurementReport report;
            SRN[] srnList;

            srnList = (SRN[])this.schedule.getSRNs().toArray(new SRN[0]);
            if (srnList.length == 0) {
                log.error("Agent does not have valid SRNs, but has metric " +
                          "data to send, measurement report blocked");
                return null;
            }
            
            if(log.isDebugEnabled()){
                for(int i=0; i<srnList.length; i++){
                    this.log.debug("    SRN: " + srnList[i].getEntity() + 
                                   "=" + srnList[i].getRevisionNumber());
                }
            }

            report = new MeasurementReport();
            if (this.agentToken == null) {
                this.agentToken =
                    storage.getValue(CommandsAPIInfo.PROP_AGENT_TOKEN);
            }
            report.setAgentToken(this.agentToken);
            report.setClientIdList(clientIds);
            report.setSRNList(srnList);
            batchStart = System.currentTimeMillis();
            serverTime= this.client.measurementSendReport(report);
            batchEnd = System.currentTimeMillis();

            // Compute offset from server (will include network latency)
            this.serverDiff = Math.abs(serverTime - batchEnd);
            if (this.serverDiff > MAX_SERVERDIFF) {
                // Complain if we are ahead or behind.  This may be a bit
                // too excessive.
                if (serverTime < batchEnd) {
                    this.log.error("Agent is " + this.serverDiff / 1000 + 
                                   " seconds ahead of the server.  To " +
                                   "ensure accuracy of the charting and " +
                                   "alerting make sure the agent and server " +
                                   "clocks are syncronized");
                } else {
                    this.log.error("Agent is " + this.serverDiff / 1000 + 
                                   " seconds behind the server.  To " +
                                   "ensure accuracy of the charting and " +
                                   "alerting make sure the agent and server " +
                                   "clocks are syncronized");
                }
            }
            success = true;
            serverError = false; // it's working fine
        } catch(AgentCallbackClientException exc){
            if( ! serverError ) {
                // serverError was false, i.e. it worked before
                this.log.error("Error sending measurements: " + 
                               exc.getMessage());
                serverError = true;
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
            int j = 0;
            
            for(Iterator i=this.storage.getListIterator(DATA_LISTNAME);
                i != null && i.hasNext() && j < numUsed;
                j++)
            {
                i.next();
                i.remove();
            }
            
            try {
                this.storage.flush();
            } catch(AgentStorageException exc){
                this.log.error("Failed to flush agent storage", exc);
            }
            
            if(j != numUsed){
                this.log.error("Failed to remove " + (numUsed - j) + 
                               "records");
            }

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
    public double getTotMetricsSent() 
        throws AgentMonitorException 
    {
        return this.stat_totMetricsSent;
    }

    /**
     * MONITOR METHOD:  Get the offset in ms between the agent and server
     */
    public double getServerOffset()
        throws AgentMonitorException
    {
        return this.serverDiff;
    }

    public void run(){
        Long lastMetricTime;
        boolean checkTime = true;
        
        while(this.shouldDie == false){
            try {
                Thread.sleep(SenderThread.SEND_INTERVAL);
            } catch(InterruptedException exc){
                this.log.info("Measurement sender interrupted");
                return;
            }

            lastMetricTime = this.sendBatch();
            if(lastMetricTime != null){
                String backlogNum = "";
                int numConsec = 0;

                // Give it a single shot to catch up before starting to
                // squawk
                while((lastMetricTime = this.sendBatch()) != null){
                    long now = System.currentTimeMillis(),
                        tDiff = now - lastMetricTime.longValue();
                    String backlog;

                    numConsec++;

                    // Log a warning if we send 4 or more complete
                    // batches consecutively.
                    if(numConsec == 3){
                        this.log.warn("The Agent is having a hard time " +
                                      "keeping up with the frequency of " +
                                      "metrics taken.  " +
                                      "Consider increasing your collection " +
                                      "interval.");
                    }

                    backlog = Long.toString(tDiff / (60 * 1000));
                    if(tDiff / (60 * 1000) > 1 &&
                       backlog.equals(backlogNum) == false)
                    {
                        backlogNum = backlog;
                        this.log.warn(backlog + 
                                      " minute(s) of metrics backlogged");
                    }

                    if(this.shouldDie == true){
                        this.log.info("Dying with measurements backlogged");
                        return;
                    }
                }

                if(numConsec >= 3){
                    this.log.info("Agent measurements no longer backlogged");
                }
            }
        }
    }
}
