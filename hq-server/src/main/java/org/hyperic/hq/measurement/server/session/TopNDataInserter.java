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

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.shared.DataManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TopNDataInserter implements DataInserter<TopNData> {

    private static final Log LOG = LogFactory.getLog(TopNDataInserter.class);
    private final DataManager dataManager;
    private static final int QUEUE_SIZE = 2000;
    private static final int FLUSH_INTERVAL = 60000;


    private final BlockingQueue<TopNData> creationQueue;
    private final AtomicInteger overflowCounter;

    // STAT fields
    private final AtomicInteger totalQueueAdds = new AtomicInteger();
    private final AtomicInteger totalQueueFlushes = new AtomicInteger();
    private final AtomicInteger totalOverFlowCounter = new AtomicInteger();
    private final AtomicLong totalFlushTime = new AtomicLong();
    private final AtomicLong totalAddTime = new AtomicLong();
    private final AtomicLong maxAddTime = new AtomicLong(Long.MIN_VALUE);
    private final AtomicLong maxFlushTime = new AtomicLong(Long.MIN_VALUE);

    @Autowired
    public TopNDataInserter(DataManager dMan) {
        dataManager = dMan;
        creationQueue = new ArrayBlockingQueue<TopNData>(QUEUE_SIZE);
        overflowCounter = new AtomicInteger(0);
    }

    public void insertData(List<TopNData> topNData) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(topNData);
        }
        final long startTime = System.currentTimeMillis();
        for (TopNData data : topNData) {
            if (!creationQueue.offer(data)) {
                overflowCounter.incrementAndGet();
                totalOverFlowCounter.incrementAndGet();
                if (LOG.isTraceEnabled()) {
                    LOG.trace(String.format("creation queue is full, dropping topn data of resourceId: %d from: %s",
                            data.getResourceId(), data.getTime()));

                }
            } else {
                totalQueueAdds.incrementAndGet();
            }
        }
        long endTime = System.currentTimeMillis();
        long addTime = endTime - startTime;
        totalAddTime.addAndGet(addTime);
        if (maxAddTime.longValue() < addTime) {
            maxAddTime.set(addTime);
        }
    }

    @Transactional
    @Scheduled(fixedDelay = FLUSH_INTERVAL)
    private void flushTopNData() {
        if (!creationQueue.isEmpty()) {
            final long startTime = System.currentTimeMillis();
            final List<TopNData> topNDatas = new ArrayList<TopNData>(creationQueue.size());
            if ((creationQueue.remainingCapacity() == 0)) {
                LOG.warn("Creation queue is full, number of TopN data lost - " + overflowCounter.getAndSet(0));
            }
            creationQueue.drainTo(topNDatas);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("drained topn data queue with %d elements", topNDatas.size()));
            }
            dataManager.addTopData(topNDatas);
            long endTime = System.currentTimeMillis();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Persisting " + topNDatas.size() + " topn data took: " + DurationFormatUtils
.formatDurationHMS(endTime - startTime));
            }
            totalQueueFlushes.incrementAndGet();
            long flushTime = endTime - startTime;
            totalFlushTime.addAndGet(flushTime);
            if (maxFlushTime.longValue() < flushTime) {
                maxFlushTime.set(flushTime);
            }
        }
    }

    public int getQueueSize() {
        return QUEUE_SIZE;
    }

    public int getTotalAddsToQueue() {
        return totalQueueAdds.get();
    }

    public int getTotalFlushes() {
        return totalQueueFlushes.get();
    }

    public int getNumberOfElementsInQueue() {
        return creationQueue.size();
    }

    public int getTotalOverflowCounter() {
        return totalOverFlowCounter.get();
    }


    public int getFlushInterval() {
        return FLUSH_INTERVAL;
    }

    public long getTotalAddToQueueTime() {
        return totalAddTime.get();
    }

    public long getTotalFlushTime() {
        return totalFlushTime.get();
    }

    public long getMaxAddTime() {
        return maxAddTime.get();
    }

    public long getMaxFlushTime() {
        return maxFlushTime.get();
    }

    public Object getLock() {
        return null;
    }

    public void insertMetrics(List<DataPoint> metricData, boolean isPriority) throws InterruptedException,
            DataInserterException {
        throw new NotImplementedException();
    }


    public void insertMetricsFromServer(List<DataPoint> metricData) throws InterruptedException, DataInserterException {
        throw new NotImplementedException();
    }
}