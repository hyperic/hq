/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.ServerConfigManager;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.stats.StatCollector;
import org.hyperic.util.stats.StatUnreachableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



/**
 * This job is responsible for filling in missing availabilty metric values.
 */
@SuppressWarnings("restriction")
@Service("availabilityCheckService")
@Transactional
public class AvailabilityCheckServiceImpl implements AvailabilityCheckService {
	
    private final Log log = LogFactory.getLog(AvailabilityCheckServiceImpl.class);
    private static final String AVAIL_BACKFILLER_TIME = ConcurrentStatsCollector.AVAIL_BACKFILLER_TIME;
    private static final String PROP_NUMWORKERS = "AVAIL_FALLBACK_CHECKERS";
    private static final String PROP_BATCHSIZE = "AVAIL_FALLBACK_BATCHSIZE";
    private static final String PROP_QUEUESIZE = "AVAIL_FALLBACK_QUEUE";
	private final int FALLBACK_CHECK_DELAY_SECONDS = 120;
	private final int DEFAULT_NUM_FALLBACK_CHECKERS = 1;
	private final boolean MANAGE_FALLBACK_CHECK_WITH_THREADS = false;

    
    private long startTime = 0;
    private long wait = 5 * MeasurementConstants.MINUTE;
    private final Object IS_RUNNING_LOCK = new Object();
    private final AtomicBoolean hasStarted = new AtomicBoolean(false);
    private boolean isRunning = false;
    private ConcurrentStatsCollector concurrentStatsCollector;
    private AvailabilityManager availabilityManager;
    private AvailabilityCache availabilityCache;
    private BackfillPointsService backfillPointsService;
    private ServerConfigManager serverConfigManager;

    private List<AvailabilityPlatformFallbackCheckThread> fallbackCheckThreads;
    private List<AvailabilityFallbackChecker> fallbackCheckers;
    @SuppressWarnings("unused")
	private int numOfFallbackCheckers, maxBatchFallbackCheck, maxFallbackCheckQueSize;
	private ScheduledThreadPoolExecutor checkersExecturor;
	private AvailabilityFallbackCheckQue checkQue;
	private AvailabilityFallbackChecker fallbackChecker = null;
	private ResourceManager resourceManager;
    

    @Autowired
    public AvailabilityCheckServiceImpl(ConcurrentStatsCollector concurrentStatsCollector,
                                        AvailabilityManager availabilityManager,
                                        AvailabilityCache availabilityCache,
                                        BackfillPointsService backfillPointsService,
                                        ServerConfigManager serverConfigManager,
                                        ResourceManager resourceManager) {
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.availabilityCache = availabilityCache;
        this.availabilityManager = availabilityManager;
        this.backfillPointsService = backfillPointsService;
        this.serverConfigManager = serverConfigManager;
        this.resourceManager = resourceManager;
        setup();
    }

    
    private void logDebug(String message) {
    	if (availabilityManager.isDevDebug())
    		log.info("aaa==========:" + message);
    	else
    		log.debug(message);
    }

    
    private void setup() {
        this.checkQue = availabilityManager.getFallbackCheckQue();
        Properties cfg = new Properties();

        try {
            cfg = serverConfigManager.getConfig();
        } catch (Exception e) {
            log.warn("Error getting server config", e);
        }

        int queueSize = 100000;
        int maxBatch = 1000;

        int numWorkers = 2;
        try {
            maxBatch = Integer.parseInt(cfg.getProperty(PROP_BATCHSIZE, "1000"));
        } catch (NumberFormatException e) {
            String msg = "Error retrieving max batch size for aggregate" +
                         " inserter, using default of " + maxBatch;
            log.warn(msg);
        }
        this.maxBatchFallbackCheck = maxBatch;

        try {
            numWorkers = Integer.parseInt(cfg.getProperty(PROP_NUMWORKERS, "1"));
            numWorkers = (numWorkers == 0) ? DEFAULT_NUM_FALLBACK_CHECKERS : numWorkers;
        } catch (NumberFormatException e) {
            String msg = "Error retrieving number of aggregate inserters," + " using default of " +
                         numWorkers;
            log.warn(msg);
        }
        this.numOfFallbackCheckers = numWorkers;

        try {
            queueSize = Integer.parseInt(cfg.getProperty(PROP_QUEUESIZE, "100000"));
        } catch (NumberFormatException e) {
            String msg = "Error retrieving default queue size for aggregate" +
                         " inserter, using default of " + queueSize;
            log.warn(msg);
        }
        this.maxFallbackCheckQueSize = queueSize;

        log.info("Creating AvailabilityCheckServiceImpl with maxBatch=" + maxBatch + " numWorkers=" + numWorkers +
                 " queueSize=" + queueSize);
        createFallbackCheckers();
    }

    
    @SuppressWarnings("unused")
	private void createFallbackCheckers() {
    	//TODO the current threads are not managed properly.
    	// should use the same timer as the backfill, and use an ExecutorPool with expanding Threads and Callable checkers instead
    	logDebug("createFallbackCheckers: " + this.numOfFallbackCheckers);
    	
    	this.checkersExecturor = new ScheduledThreadPoolExecutor(numOfFallbackCheckers);
    	if (!MANAGE_FALLBACK_CHECK_WITH_THREADS) 
    		return;
    	
    	this.fallbackCheckThreads = new ArrayList<AvailabilityPlatformFallbackCheckThread>();
    	this.fallbackCheckers = new ArrayList<AvailabilityFallbackChecker>();
        for (int i=0; i<numOfFallbackCheckers; ++i) {
        	logDebug("createFallbackCheckers: creating thread #" + i);
        	AvailabilityFallbackChecker checker = new AvailabilityFallbackChecker(availabilityManager, availabilityCache, resourceManager);
        	this.fallbackCheckers.add(checker);
        	AvailabilityPlatformFallbackCheckThread checkThread = new AvailabilityPlatformFallbackCheckThread(checker);
            fallbackCheckThreads.add(checkThread);
            checkersExecturor.scheduleWithFixedDelay(checkThread, FALLBACK_CHECK_DELAY_SECONDS+i, FALLBACK_CHECK_DELAY_SECONDS, TimeUnit.SECONDS);        	
        }
    	logDebug("createFallbackCheckers: " + this.numOfFallbackCheckers);
   	}




    @PostConstruct
    public void initStats() {
        concurrentStatsCollector.register(AVAIL_BACKFILLER_TIME);
    }

    public void backfill() {
        backfill(System.currentTimeMillis(), false);
    }

    public void backfill(long timeInMillis) {
        // since method is used for unittests no need to check if alert triggers
        // have initialized
        backfill(timeInMillis, true);
    }

    public void testBackfill(long current) {
    	if (this.checkersExecturor != null)
    		this.checkersExecturor.shutdown();
    	checkPlatformsAvailabilityNoThreads(current, true);
    }
    
    private void backfill(long current, boolean forceStart) {
    	if (this.MANAGE_FALLBACK_CHECK_WITH_THREADS) {
    		checkPlatformsAvailabilityUsingThreads(current,forceStart);
    	}
    	else {
    		checkPlatformsAvailabilityNoThreads(current, forceStart);
    	}
    }

    private void checkPlatformsAvailabilityNoThreads(long current, boolean forceStart) {
        long start = now();
        Map<Integer, ResourceDataPoint> backfillPoints = null;
        try {
            final boolean debug = log.isDebugEnabled();
            if (debug) {
                Date lDate = new Date(current);
                log.debug("Availability Check Service started executing: " + lDate);
            }
            // Don't start backfilling immediately
            if (!forceStart && !canStart(current)) {
            	log.info("not starting availability check");
                return;
            }
            synchronized (IS_RUNNING_LOCK) {
                if (isRunning) {
                    log.warn("Availability Check Service is already running, bailing out");
                    return;
                } else {
                    isRunning = true;
                }
            }
            try {
                // PLEASE NOTE: This synchronized block directly affects the
                // throughput of the availability metrics, while this lock is
                // active no availability metric will be inserted. This is to
                // ensure the backfilled points will not be inserted after the
                // associated AVAIL_UP value from the agent.
                // The code must be extremely efficient or else it will have
                // a big impact on the performance of availability insertion.
                synchronized (availabilityCache) {
                	logDebug("checkPlatformsAvailabilityNoThreads: before getBackfillPlatformPoints");
                    backfillPoints = backfillPointsService.getBackfillPlatformPoints(current);
                }
                if (backfillPoints.size() > 0)
                	log.info("checkPlatformsAvailabilityNoThreads: got " + backfillPoints.size() + " points. Adding to que.");
                checkQue.addToQue(backfillPoints);
                List<ResourceDataPoint> availabilityDataPoints = pollWorkList();
                if (fallbackChecker == null)
                	fallbackChecker = new AvailabilityFallbackChecker(availabilityManager, availabilityCache, resourceManager);
                fallbackChecker.checkAvailability(availabilityDataPoints, current);
            } finally {
                synchronized (IS_RUNNING_LOCK) {
                    isRunning = false;
                }
            }
        } catch (Exception e) {
            throw new SystemException(e);
        } finally {
            concurrentStatsCollector.addStat(now() - start, AVAIL_BACKFILLER_TIME);
        }
    	
    }
    
    
    private List<ResourceDataPoint> pollWorkList() {
    	logDebug("setWorkList");
    	List<ResourceDataPoint> dataPointList = new ArrayList<ResourceDataPoint>();
        final int batchSize = getBatchSize();
        int i = 0;
        int currSize = 0;
        for (i = 0; i < batchSize && currSize < batchSize; i++) {
            ResourceDataPoint dp = checkQue.poll();
            if (dp == null) {
                break;
            }
            dataPointList.add(dp);
            currSize++;
        }
       	logDebug("setWorkList: current dataPointList size: " + dataPointList.size());
       	return dataPointList;
    }

    
    private void checkPlatformsAvailabilityUsingThreads(long current, boolean forceStart) {
        long start = now();
        Map<Integer, ResourceDataPoint> backfillPoints = null;
        try {
            final boolean debug = log.isDebugEnabled();
            if (debug) {
                Date lDate = new Date(current);
                log.debug("Availability Check Service started executing: " + lDate);
            }
            // Don't start backfilling immediately
            if (!forceStart && !canStart(current)) {
            	log.info("not starting availability check");
                return;
            }
            synchronized (IS_RUNNING_LOCK) {
                if (isRunning) {
                    log.warn("Availability Check Service is already running, bailing out");
                    return;
                } else {
                    isRunning = true;
                }
            }
            try {
                // PLEASE NOTE: This synchronized block directly affects the
                // throughput of the availability metrics, while this lock is
                // active no availability metric will be inserted. This is to
                // ensure the backfilled points will not be inserted after the
                // associated AVAIL_UP value from the agent.
                // The code must be extremely efficient or else it will have
                // a big impact on the performance of availability insertion.
                synchronized (availabilityCache) {
                	logDebug("backfill: before getBackfillPlatformPoints");
                    backfillPoints = backfillPointsService.getBackfillPlatformPoints(current);
                	logDebug("backfill: got " + backfillPoints.size() + " points. Adding to que.");
                    checkQue.addToQue(backfillPoints);
                }
                // send data to event handlers outside of synchronized block
                //availabilityManager.sendDataToEventHandlers(backfillPoints);
            } finally {
                synchronized (IS_RUNNING_LOCK) {
                    isRunning = false;
                }
            }
        } catch (Exception e) {
            throw new SystemException(e);
        } finally {
            concurrentStatsCollector.addStat(now() - start, AVAIL_BACKFILLER_TIME);
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private boolean canStart(long now) {
        if (!hasStarted.get()) {
            if (startTime == 0) {
                startTime = now;
            } else if ((startTime + wait) <= now) {
                // start after the initial wait time only if the
                // availability inserter is not "backlogged"
                if (getAvailabilityInserterQueueSize() < 1000) {
                    hasStarted.set(true);
                }
            }
        }

        return hasStarted.get();
    }

    private long getAvailabilityInserterQueueSize() {
        boolean debug = log.isDebugEnabled();
        String statKey = "AvailabilityInserter_QUEUE_SIZE";
        long currentQueueSize = -1;

        StatCollector stat = (StatCollector) concurrentStatsCollector.getStatKeys().get(statKey);

        if (stat == null) {
            if (debug) {
                log.debug(statKey + " is not registered in the ConcurrentStatsCollector");
            }
        } else {
            try {
                currentQueueSize = stat.getVal();
            } catch (StatUnreachableException e) {
                if (debug) {
                    log.debug("Could not get value for " + statKey + ": " + e.getMessage(), e);
                }
            }
        }

        if (debug) {
            log.debug("Current availability inserter queue size = " + currentQueueSize);
        }

        return currentQueueSize;
    }
    
    private int getBatchSize() {
    	return this.maxBatchFallbackCheck;
    }
    
    
    /**
     * 
     * @author amalia
     *
     */
    private class AvailabilityPlatformFallbackCheckThread implements Runnable {
        private final ArrayList<ResourceDataPoint> dataPointList;
        private final AvailabilityFallbackChecker _executor;
        private final Calendar _cal = Calendar.getInstance();
        
        
        private void logDebug(String message) {
        	if (availabilityManager.isDevDebug())
        		log.info("aaa==========:" + message);
        }


        AvailabilityPlatformFallbackCheckThread(AvailabilityFallbackChecker checker) {
            this.dataPointList = new ArrayList<ResourceDataPoint>(getBatchSize());
            this._executor = checker;
            long now = now();
            _cal.setTimeInMillis(now);
        }


        private void setWorkList() {
        	logDebug("setWorkList");
        	dataPointList.clear();
            final int batchSize = getBatchSize();
            //final boolean debug = log.isDebugEnabled();
            int i = 0;
            int currSize = 0;
            for (i = 0; i < batchSize && currSize < batchSize; i++) {
                ResourceDataPoint dp = checkQue.poll();
                if (dp == null) {
                    break;
                }
                dataPointList.add(dp);
                currSize++;
            }
           	logDebug("setWorkList: current dataPointList size: " + dataPointList.size());
        }

        /**
         * @return List<Long> list of insertTimes
         */
        private void calcData() throws InterruptedException {
           	logDebug("calcData: current dataPointList size: " + dataPointList.size());
            final int batchSize = getBatchSize();
            final boolean debug = log.isDebugEnabled();
            for (int i = 0; i < dataPointList.size(); i += batchSize) {
                long start = now();
                int end = Math.min(i + batchSize, dataPointList.size());
                List<ResourceDataPoint> list = dataPointList.subList(i, end);
                _executor.checkAvailability(list);
                	
                long insertTime = now() - start;
                if (debug) 
                	logDebug(insertTime, list.size(), dataPointList.size()-end);
            }
           	logDebug("calcData: end");
           	
        }

        private void logDebug(long insertTime, int points, int remaining) {
            String name = Thread.currentThread().getName();
            log.debug("[" + name + "] calculated " + points + " points, worker has " + remaining +
                       " points remaining");
        }

        
        public void run() {
           	logDebug("run: start");
            try {
                final Object lock = _executor.getLock();
                synchronized (lock) {
                    while (true) {
                        setWorkList();
                        if (this.dataPointList.size() == 0) {
                            break;
                        }
                        calcData();
                    }
                }
            } catch (Error e) {
                log.fatal("Fatal error while processing", e);
                throw e;
            } catch (Throwable e) {
                log.warn("Error while processing data", e);
            }
           	logDebug("run: end");
        }

    }

    
    @PreDestroy
    public void destroy() {
    	this.checkersExecturor.shutdown();
    }
    
}
