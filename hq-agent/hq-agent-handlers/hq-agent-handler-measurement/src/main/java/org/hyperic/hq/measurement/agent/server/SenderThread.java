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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.agent.server.AgentStartException;
import org.hyperic.hq.agent.server.AgentStorageException;
import org.hyperic.hq.agent.server.AgentStorageProvider;
import org.hyperic.hq.agent.server.monitor.AgentMonitorException;
import org.hyperic.hq.agent.server.monitor.AgentMonitorSimple;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.MeasurementCallbackClient;
import org.hyperic.hq.bizapp.client.StorageProviderFetcher;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.agent.server.SendBatchResult.StatusBatchResult;
import org.hyperic.hq.measurement.data.DSNList;
import org.hyperic.hq.measurement.data.MeasurementReport;
import org.hyperic.hq.measurement.data.MeasurementReportConstructor;
import org.hyperic.hq.measurement.server.session.SRN;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.util.Reference;
import org.hyperic.hq.util.properties.PropertiesUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.encoding.Base64;
/**
 * Deals with sending measurements back to the server (including 
 * persisting them on disk.)
 */

public class SenderThread extends AgentMonitorSimple implements Sender, Runnable {

    private enum ConnectionPolicy {
        CLOSE,KEEP_ALIVE,CLOSE_ON_LAST_BATCH
    }
    private static final String PROP_METRICDUP = "agent.metricDup";
    private static final String PROP_MAXBATCHSIZE = "agent.maxBatchSize";
    private static final String PROP_METRICDEBUG = "agent.metricDebug";

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
    private static final String TIMEDIFF_STORAGE_KEY = "server_agent_timediff";
    private static final long MINIMAL_TIMEDIFF_SYNC_INTERVAL = TimeUnit.SECONDS.toMillis(60);
    private static final int MEASURENENT_MAX_RETRY_TIME = 5;

    private volatile boolean shouldDie;
    private final Log log;
    private final MeasurementCallbackClient client;
    private final AgentStorageProvider storage;
    private final LinkedList<Record> transitionQueue;
    private final AtomicBoolean regularMetricsReadyForProcess;
    // This toggle will avoid displaying non-stop messages about server down
    private int metricDup = 0;
    private int maxBatchSize = MAX_BATCHSIZE;
    private final Set metricDebug;
    private final MeasurementSchedule schedule;
    private final SchedulerOffsetManager schedulerOffsetManager;

    // Current difference time between the server and agent in ns.
    // Update on each call to sendMeasurementReport().
    private long serverDiff = 0;


    private long stat_numBatchesSent = 0;
    private long stat_totBatchSendTime = 0;
    private long stat_totMetricsSent = 0;

    private boolean deductServerTimeDiff;
    private boolean storedServerTimeDiff;


    SenderThread(AgentConfig bootConfig,
            AgentStorageProvider storage,
            MeasurementSchedule schedule,
            Properties config,
            SchedulerOffsetManager schedulerOffsetManager)
    throws AgentStartException {
        String sMetricDup, sMaxBatchSize, sMetricDebug;
        this.log             = LogFactory.getLog(SenderThread.class);
        this.shouldDie       = false;
        this.storage         = storage;
        this.client          = setupClient();
        this.transitionQueue = new LinkedList<Record>();
        this.regularMetricsReadyForProcess = new AtomicBoolean(false);
        this.metricDebug = new HashSet();
        this.schedule = schedule;
        this.schedulerOffsetManager = schedulerOffsetManager;

        String measuementInfo = bootConfig.getBootProperties().getProperty(MEASURENENT_LISTNAME);
        if (measuementInfo != null) {
            storage.addOverloadedInfo(MEASURENENT_LISTNAME, measuementInfo);  
        }
        String AvailabilityInfo = bootConfig.getBootProperties().getProperty(AVAILABILITY_LISTNAME);
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

        sMetricDup = bootConfig.getBootProperties().getProperty(PROP_METRICDUP);
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

        sMaxBatchSize = bootConfig.getBootProperties().getProperty(PROP_MAXBATCHSIZE);
        if(sMaxBatchSize != null){
            try {
                this.maxBatchSize = Integer.parseInt(sMaxBatchSize);
            } catch(NumberFormatException exc){
                throw new AgentStartException(PROP_MAXBATCHSIZE + " is not " +
                                              "a valid integer ('" + 
                                              sMaxBatchSize + "')");
            }

        }

        sMetricDebug = bootConfig.getBootProperties().getProperty(PROP_METRICDEBUG);
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

        this.log.info("Maximum metric batch size set to " + this.maxBatchSize);
        // The default value of the deduction property is true
        deductServerTimeDiff = PropertiesUtil.getBooleanValue(
        		bootConfig.getBootProperties().getProperty(ServerTimeDiff.PROP_DEDUCT_SERVER_TIME_DIFF), true);

        this.storedServerTimeDiff = false;
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
        final long dsnId,
                    derivedID;
        final MetricValue data;
        private Long hashCode = null;
        // This field is not serialized
        private final boolean isAvail;

        private Record(long dsnId,
                       MetricValue data,
                       long derivedID,
                       boolean isAvail) {
            this.dsnId = dsnId;
            this.data = data;
            this.derivedID = derivedID;
            this.isAvail  = isAvail;
        }

        public Record(long dsnID,
                      MetricValue measVal,
                      long derivedID) {
            this(dsnID, measVal, derivedID, false);
        }

        @Override
        public int hashCode() {
            if (hashCode != null) {
                return hashCode.intValue();
            }
            final Long timestamp = new Long(data.getTimestamp() * 71);
            final Long mId = new Long(derivedID * 71);
            hashCode =
                        new Long(7 + (timestamp.hashCode() * 71) + (mId.hashCode() * 71));
            return hashCode.hashCode();
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
        long derivedID, dsnID;
        long retTime;
        
        bIs = new ByteArrayInputStream(Base64.decode(val));
        dIs = new DataInputStream(bIs);

        derivedID = dIs.readLong();
        retTime = dIs.readLong();
        dsnID = dIs.readLong();

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

        dOs.writeLong(record.derivedID);
        dOs.writeLong(record.data.getTimestamp());
        dOs.writeLong(record.dsnId);
        dOs.writeDouble(record.data.getValue());
        return Base64.encode(bOs.toByteArray());
    }

    public void processData(long dsnId,
                            MetricValue data,
                            long samplingInterval,
                            long derivedID,
                            boolean isAvail) {
        double val;





        if (!data.equals(MetricValue.NONE)) { // If the metric is valid, round down its time stamp to the lower minute.
            roundDownTimeStamp(data, samplingInterval);
        }

        val = data.getValue();

        if (this.metricDebug.contains(new Long(derivedID))) {
            this.log.info("metricDebug:  Placing DSN='" + dsnId +
                        "' derivedID=" + derivedID + " value=" + val +
                        " time=" + data.getTimestamp() +
                        " into transition queue");
        }

        synchronized(this.transitionQueue){
        	MetricValue measVal = new MetricValue(val, data.getTimestamp());
            this.transitionQueue.add(new Record(dsnId, measVal, derivedID, isAvail));
            if (!isAvail) {
                this.regularMetricsReadyForProcess.set(true);
            }
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

    private void roundDownTimeStamp(MetricValue data,
                                    long samplingInterval) {
        if (deductServerTimeDiff
                    && Math.abs(ServerTimeDiff.getInstance().getServerTimeDiff()) > ServerTimeDiff.MIN_OFFSET_FOR_DEDUCTION) {
            // deduct the server time offset from the metric value time stamp
            data.setTimestamp(data.getTimestamp() + ServerTimeDiff.getInstance().getServerTimeDiff());
        }
        long offset = schedulerOffsetManager.getSchedluerOffsetForInterval(samplingInterval);
        data.setTimestamp(TimingVoodoo.roundDownTime(data.getTimestamp(), samplingInterval, offset));
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
            this.regularMetricsReadyForProcess.set(false);
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
    private SendBatchResult sendBatch(String listName,
                                      Reference<Integer> numSent,
                                      ConnectionPolicy connectionPolicy) {
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
        Iterator<String> it=storage.getListIterator(listName);
        for (; (it!=null) && it.hasNext() && (numUsed < maxBatchSize); numUsed++) {
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

        for (Iterator<Record> recordIt=records.iterator(); recordIt.hasNext(); num++) {
            Record rec = recordIt.next();

            lastMetricTime = rec.data.getTimestamp();

            if(log.isDebugEnabled()){
                this.log.debug("    Data:  d=" + rec.derivedID + 
                               " r=" + rec.dsnId +
                               " t=" + rec.data.getTimestamp() +
                               " v=" + rec.data);
            }

            if (this.metricDebug.contains(new Long(rec.derivedID))) {
                numDebuggedSent++;
                this.log.info("metricDebug:  Pulled DSN=" + rec.dsnId + 
                              " derivedID=" + rec.derivedID + " value=" + 
                              rec.data.getValue() + " from tQueue -- sending");
            }
            constructor.addDataPoint(rec.derivedID, rec.dsnId, rec.data);
        }
        numSent.set(num);

        // If we don't have anything to send -- move along
        if (numUsed == 0) {
            return new SendBatchResult(StatusBatchResult.SUCCESS_ALL);
        }
        
        clientIds = constructor.constructDSNList();
        
        success = false;
        try {
            SRN[] srnList = this.schedule.getSRNsAsArray();
            if (srnList.length == 0) {
                log.error("Agent does not have valid SRNs, but has metric data to send, removing measurements");
                removeMeasurements(numUsed, listName);
                return new SendBatchResult(StatusBatchResult.ERROR_ALL);
            }
            
            if(log.isDebugEnabled()){
                for (SRN element : srnList) {
                    this.log.debug("    SRN: " + element.getEntity() +  "=" + element.getRevisionNumber());
                }
            }

            MeasurementReport report = createMeasurementReport(clientIds, srnList);
            batchStart = now();
            boolean closeConn;
            switch (connectionPolicy) {
            case CLOSE:
                log.error("Illegal state - Force closing connection on next Request");
                //Use property file closing property
                closeConn = Boolean.parseBoolean(AgentConfig.getDefaultProperties().getProperty(
                        AgentConfig.PROP_CLOSE_HTTP_CONNECTION_BY_DEFAULT[0]));
                break;
            case KEEP_ALIVE:
                closeConn = false;
                break;
            case CLOSE_ON_LAST_BATCH:
                // If there are no metrics left on next batch, we close the connection.
                // Otherwise we keep it open.
                closeConn =  !it.hasNext();
                break;
            default:
                closeConn = false;
                break;
            }
            serverTime = sendReportToServer(report, closeConn);
            batchEnd = now();
            syncToServerTime(batchEnd, serverTime);
            success = true;
        } catch (AgentCallbackClientException exc) {
            log.error("Error sending measurements: " + exc.getMessage());
            if (log.isDebugEnabled()) {
                log.debug(exc.getStackTrace());
            }
            if (shouldRetry(exc)){
                log.info("retrying measurement send");
                return new SendBatchResult(StatusBatchResult.ERROR_BATCH, firstMetricTime);
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

            if (numUsed == this.maxBatchSize) {
                return new SendBatchResult(StatusBatchResult.SUCCESS_BATCH, lastMetricTime);
            } else {
                return new SendBatchResult(StatusBatchResult.SUCCESS_ALL);
            }
        }
        return new SendBatchResult(StatusBatchResult.ERROR_ALL);
    }

    // return this so that the caller will attempt a retry on everything except :
    // 1. Connection refused
    // 2. Permission Denied
    // 3. Service Unavailable
    // 4. SSLPeerUnverifiedException
    private boolean shouldRetry(AgentCallbackClientException exc) {
        return !(exc.getMessage().toLowerCase().endsWith("refused")) &&
               !(exc.getMessage().endsWith(AgentCallbackClientException.PERMISSION_DENIED_ERROR_MSG))&&
               !(exc.getMessage().indexOf("Service Unavailable") != -1) &&
               (exc.getExceptionOfType(SSLPeerUnverifiedException.class) == null);
    }

    /**
     * Ensures the agent is synced to server time by triggering a sync if needed.
     */
    public void ensureSyncedToServerTime() {
        if (storedServerTimeDiff || isSyncTriedRecently()) {
            return;
        }

        syncToServerTime();
    }

    /**
     * Tries to sync time difference with server by communicating with it, falling back to a persisted time difference
     * from a previous run.
     */
    private void syncToServerTime() {
        long agentTime = now();
        try {
            syncToServerTime(agentTime, requestServerTime());
        } catch (AgentCallbackClientException e) {
            log.info("Couldn't sync time difference with the server. Time difference might"
                        + " not be accurate until communication with the server is set.");
            modifyServerTimeDiff(agentTime, agentTime + fetchServerTimeDiff());
        }
    }

    /**
     * Sync with server time and persist time difference if no one had.
     */
    public synchronized void syncToServerTime(long agentTime, long serverTime) {
        modifyServerTimeDiff(agentTime, serverTime);
        log.debug("Synced time difference with the server.");
        if (!storedServerTimeDiff) {
            storeServerTimeDiff();
        }
    }

    private boolean isSyncTriedRecently() {
        long elapsedTimeFromSync = now() - ServerTimeDiff.getInstance().getLastSync();
        return elapsedTimeFromSync < MINIMAL_TIMEDIFF_SYNC_INTERVAL;
    }

    /**
     * Requests server's time by sending an empty measurement report. The response includes the needed value.
     */
    private long requestServerTime() throws AgentCallbackClientException {
        MeasurementReport report = createMeasurementReport(new DSNList[] {}, new SRN[] {});
        boolean closeConn = Boolean.parseBoolean(AgentConfig.getDefaultProperties().getProperty(
                    AgentConfig.PROP_CLOSE_HTTP_CONNECTION_BY_DEFAULT[0]));

        return sendReportToServer(report, closeConn);
    }

    private long fetchServerTimeDiff() {
        try{
            Long timediff = Long.valueOf(storage.getValue(TIMEDIFF_STORAGE_KEY));
            return timediff;
        } catch (NumberFormatException e) {
            log.warn("Couldn't get server time difference from storage.");
            return 0;
        }
    }

    private void storeServerTimeDiff() {
        String serverTimeDiff = String.valueOf(ServerTimeDiff.getInstance().getServerTimeDiff());
        storage.setValue(TIMEDIFF_STORAGE_KEY, serverTimeDiff);
        try {
            storage.flush();
            log.info("Stored server time difference.");
            storedServerTimeDiff = true;
        } catch (AgentStorageException e) {
            log.warn("Couldn't store server time difference.");
        }

    }

    private MeasurementReport createMeasurementReport(DSNList[] clientIds,
                                                      SRN[] srnList) {
        MeasurementReport report = new MeasurementReport();
        report.setClientIdList(clientIds);
        report.setSRNList(srnList);
        return report;
    }

    /**
     * Sends a repost to the Server
     *
     * @param report the report needed to be sent to the server
     * @return the server's time reported back
     * @throws AgentCallbackClientException in case sending the report failed
     */
    private long sendReportToServer(MeasurementReport report,
                                    boolean closeConn)
        throws AgentCallbackClientException {
        long serverTime;
        try {
            serverTime = this.client.measurementSendReport(report, closeConn);
        } catch (IllegalArgumentException e) {
            throw new SystemException("error sending report: " + e + ", report=" + report, e);
        }
        return serverTime;
    }

    private void modifyServerTimeDiff(long agentTime, long serverTime) {

        // Compute offset from server (will include network latency)
        this.serverDiff = Math.abs(serverTime - agentTime);

        // Update the ServerTimeDiff object with the time offset between the
        // agent and the server and update the last sync time to now
        ServerTimeDiff.getInstance().setServerTimeDiff(serverTime - agentTime);
        ServerTimeDiff.getInstance().setLastSync(agentTime);

        if (this.serverDiff > MAX_SERVERDIFF) {
            // Complain if we are ahead or behind. This may be a bit
            // too excessive.
            if (serverTime < agentTime) {
                this.log.error("Agent is " + (this.serverDiff / 1000) + " seconds ahead of the server.  To "
                            + "ensure accuracy of the charting and " + "alerting make sure the agent and server "
                            + "clocks are synchronized");
            } else {
                this.log.error("Agent is " + (this.serverDiff / 1000) + " seconds behind the server.  To "
                            + "ensure accuracy of the charting and " + "alerting make sure the agent and server "
                            + "clocks are synchronized");
            }
        }
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
                // Get close connection default behavior from agent properties. This
                // conversion is null safe.
                boolean closeConnByDefault = Boolean.parseBoolean(AgentConfig.getDefaultProperties().getProperty(
                            AgentConfig.PROP_CLOSE_HTTP_CONNECTION_BY_DEFAULT[0]));

                // If closeConnByDefault is false, we want to keep the connection open.
                // If closeConnByDefault is true, we would like to close the connection
                // if there are no regular metrics ready to be sent. Otherwise, we close it.
                boolean closeConnAfterAvailabilityReport = closeConnByDefault && !regularMetricsReadyForProcess.get();

                sendData(AVAILABILITY_LISTNAME, numSent, closeConnAfterAvailabilityReport);
                sendData(MEASURENENT_LISTNAME, numSent, closeConnByDefault);
            } catch (Throwable e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private void sendData(String listName,
                          Reference<Integer> numSent,
                          boolean closeConnWithLastBatch) {
        SendBatchResult result;
        String backlogNum = "";
        ConnectionPolicy connectionPolicy = closeConnWithLastBatch ?
                ConnectionPolicy.CLOSE_ON_LAST_BATCH : ConnectionPolicy.KEEP_ALIVE;
        final long start = System.currentTimeMillis();
        result = this.sendBatch(listName, numSent, connectionPolicy);
        if (!result.isDone()) {
            // Give it a single shot to catch up before starting to squawk
            long retries = 1;
            while (!((result = this.sendBatch(listName, numSent, connectionPolicy)).isDone())) {
                long now = System.currentTimeMillis();
                long tDiff = now - result.getTimeStamp();
                String backlog = Long.toString(tDiff / (60 * 1000));
                if(((tDiff / (60 * 1000)) > 1) && (backlog.equals(backlogNum) == false)) {
                    backlogNum = backlog;
                    this.log.info(backlog +  " minute(s) of metrics backlogged");
                }
                if(this.shouldDie == true){
                    this.log.info("Dying with measurements backlogged");
                    return;
                }
                if (StatusBatchResult.ERROR_BATCH.equals(result.getStatus())) {
                    retries++;
                }
                if (StatusBatchResult.SUCCESS_BATCH.equals(result.getStatus())) {
                    // Batch succeed - reset retries counter
                    retries = 0;
                }
                if (retries >= MEASURENENT_MAX_RETRY_TIME) {
                    this.log.info("Reached measurement report max retries - dying with measurements backlogged");
                    // After reached max retries inform the server to close connection
                    // because we are delaying the requests
                    this.sendBatch(listName, numSent, ConnectionPolicy.CLOSE);
                    return;
                }
            }
        }
        final long total = System.currentTimeMillis() - start;
        if (total > SEND_INTERVAL) {
            log.info("Agent took " + (total / 1000) + " seconds to send its " + listName + " metrics to the Server.");
        } else if (log.isDebugEnabled()) {
            log.debug("Agent took " + total + " ms to send its " + listName);
        }

    }
}
