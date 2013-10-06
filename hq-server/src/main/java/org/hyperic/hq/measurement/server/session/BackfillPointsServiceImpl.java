/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.measurement.server.session;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.AgentDAO;
import org.hyperic.hq.appdef.server.session.AgentPluginSyncRestartThrottle;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.bizapp.server.session.LatherDispatcher;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.stats.ConcurrentStatsCollector;
import org.hyperic.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default implementation of {@link BackfillPointsService} Code was extracted
 * unmodified from AvailabilityCheckServiceImpl to allow for that class to add
 * data points in separate transactions
 * @author jhickey
 * 
 */
@Transactional(readOnly = true)
@Service
public class BackfillPointsServiceImpl implements BackfillPointsService {

    private static final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;
    private static final double AVAIL_PAUSED = MeasurementConstants.AVAIL_PAUSED;
    private static final double AVAIL_NULL = MeasurementConstants.AVAIL_NULL;
    private static final long MINUTE = MeasurementConstants.MINUTE;
    private static final String AVAIL_BACKFILLER_NUMPLATFORMS =
        ConcurrentStatsCollector.AVAIL_BACKFILLER_NUMPLATFORMS;
    private final Log log = LogFactory.getLog(BackfillPointsServiceImpl.class);
    private final AvailabilityManager availabilityManager;
    private final PermissionManager permissionManager;
    private final AvailabilityCache availabilityCache;
    private final AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle;
    private final AgentDAO agentDAO;
    private final ConcurrentStatsCollector concurrentStatsCollector;
    private final AgentManager agentManager;

    @Autowired
    public BackfillPointsServiceImpl(AvailabilityManager availabilityManager,
                                     PermissionManager permissionManager,
                                     AgentPluginSyncRestartThrottle agentPluginSyncRestartThrottle,
                                     AgentDAO agentDAO,
                                     AvailabilityCache availabilityCache,
                                     ConcurrentStatsCollector concurrentStatsCollector,
                                     AgentManager agentManager) {
        this.availabilityManager = availabilityManager;
        this.permissionManager = permissionManager;
        this.availabilityCache = availabilityCache;
        this.agentPluginSyncRestartThrottle = agentPluginSyncRestartThrottle;
        this.agentDAO = agentDAO;
        this.concurrentStatsCollector = concurrentStatsCollector;
        this.agentManager = agentManager;
    }

    @PostConstruct
    public void initStats() {
        concurrentStatsCollector.register(AVAIL_BACKFILLER_NUMPLATFORMS);
    }

    public Map<Integer, ResourceDataPoint> getBackfillPlatformPoints(long current) {
        Map<Integer, ResourceDataPoint> downPlatforms = getDownPlatforms(current);
        log.debug("getBackfillPlatformPoints: found " + downPlatforms.size() + " downPlatforms for resource IDs: " + downPlatforms.keySet());
        removeRestartingAgents(downPlatforms);
        log.debug("getBackfillPlatformPoints: after removeRestartingAgents: " + downPlatforms.size() + " downPlatforms for resource IDs: " + downPlatforms.keySet());
        if (downPlatforms != null) {
            concurrentStatsCollector.addStat(downPlatforms.size(), AVAIL_BACKFILLER_NUMPLATFORMS);
        }
        return downPlatforms;
    }

    private void removeRestartingAgents(Map<Integer, ResourceDataPoint> backfillData) {
        if (backfillData.isEmpty()) {
            return;
        }
        final long now = now();
        final Map<Integer, Long> restarting = agentPluginSyncRestartThrottle.getAgentIdsInRestartState();
        final Set<Integer> processed = new HashSet<Integer>();
        for (final Entry<Integer, Long> entry : restarting.entrySet()) {
            final Integer agentId = entry.getKey();
            final long restartTime = entry.getValue();
            processed.add(agentId);
            removeAssociatedPlatforms(agentId, backfillData, restartTime, true);
            if (backfillData.isEmpty()) {
                return;
            }
        }
        // [HHQ-4937] allow agents up to 10 minutes after they checkin to start sending availability
        // before marking them down
        final Map<Integer, Long> lastCheckins = agentPluginSyncRestartThrottle.getLastCheckinInfo();
        for (final Entry<Integer, Long> entry : lastCheckins.entrySet()) {
            final Integer agentId = entry.getKey();
            final long lastCheckin = entry.getValue();
            if (((lastCheckin + (10*MINUTE)) < now) || processed.contains(agentId)) {
                continue;
            }
            removeAssociatedPlatforms(agentId, backfillData, lastCheckin, false);
            if (backfillData.isEmpty()) {
                return;
            }
        }
    }

    private void removeAssociatedPlatforms(int agentId, Map<Integer, ResourceDataPoint> backfillData,
                                           long timems, boolean restarting) {
        final boolean debug = log.isDebugEnabled();
        final Agent agent = agentDAO.get(agentId);
        if (agent == null) {
            return;
        }
        final Collection<Platform> platforms = agent.getPlatforms();
        for (final Platform platform : platforms) {
            if (debug) {
                if (restarting) {
                    log.debug(new StringBuilder(64)
                        .append("removing platformId=").append(platform.getId())
                        .append(" since its agentId=").append(agentId)
                        .append(" just restarted at ").append(TimeUtil.toString(timems))
                        .toString());
                } else {
                    log.debug(new StringBuilder(64)
                        .append("removing platformId=").append(platform.getId())
                        .append(" since its agentId=").append(agentId)
                        .append(" is in restart state since ").append(TimeUtil.toString(timems))
                        .toString());
                }
            }
            backfillData.remove(platform.getResource().getId());
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private Map<Integer, ResourceDataPoint> getDownPlatforms(long timeInMillis) {
        final boolean debug = log.isDebugEnabled();
        final List<Measurement> platformResources = availabilityManager.getPlatformResources();
        final long now = TimingVoodoo.roundDownTime(timeInMillis, MINUTE);
        final String nowTimestamp = TimeUtil.toString(now);
        final Map<Integer, ResourceDataPoint> rtn = new HashMap<Integer, ResourceDataPoint>(platformResources.size());
        final LatherDispatcher latherDispatcher = Bootstrap.getBean(LatherDispatcher.class);
        synchronized (availabilityCache) {
            for (final Measurement meas : platformResources) {
                final long interval = meas.getInterval();
                /** 
                 * minDowntime says that the agent must be down for a minimum of 3 minutes for a platform to be marked
                 *  down, not 2x availability interval for an availability interval of one minute (as it was in the 
                 *  past). The reason is that if the SenderThread fails once to send due to a connection issue
                 *  (if the server is too busy), then it will only send out again one minute later.  For an availability
                 *  interval of 1 minute, the platform will be marked down since the agent will only send again after
                 *  one full minute + latency.  Setting to a minimum of 3 minutes allows the agent time to have one
                 *  more attempt after a failure
                 */
                final long minDowntime = Math.max(2*interval, 3*MINUTE);
                final long end = getEndWindow(now, meas);
                final long begin = getBeginWindow(end, meas);
                final DataPoint defaultPt = new DataPoint(meas.getId().intValue(), AVAIL_NULL, end);
                final DataPoint last = availabilityCache.get(meas.getId(), defaultPt);
                final long lastTimestamp = last.getTimestamp();
                if (debug) {
                    String msg = "Checking availability for " + last + ", CacheValue=(" +
                                 TimeUtil.toString(lastTimestamp) + ") vs. Now=(" + nowTimestamp + ")";
                    log.debug(msg);
                }
                if (begin > end) {
                    // this represents the scenario where the measurement mtime
                    // was modified recently and therefore we need to wait
                    // another interval
                    log.info("skipping measurement " + meas.getId() + ": begin=" + begin + " > end=" + end);
                    continue;
                }
                if (!meas.isEnabled()) {
                    final long t = TimingVoodoo.roundDownTime(now - interval, interval);
                    final DataPoint point = new DataPoint(meas.getId(), new MetricValue(AVAIL_PAUSED, t));
                    Resource resource = meas.getResource();
                    log.info("adding resourceId=" + resource.getId() +
                             " to list of down platforms, metric is not enabled");
                    rtn.put(resource.getId(), new ResourceDataPoint(resource, point));
                } else if ((last.getValue() == AVAIL_DOWN) || ((now - lastTimestamp) > minDowntime)) {
                    // HQ-1664: This is a hack: Give a 5 minute grace period for the agent and HQ
                    // to sync up if a resource was recently part of a downtime window
                    if ((last.getValue() == AVAIL_PAUSED) && ((now - lastTimestamp) <= (5 * 60 * 1000))) {
                        continue;
                    }
                    long t = (last.getValue() != AVAIL_DOWN) ?
                        lastTimestamp + interval : TimingVoodoo.roundDownTime(now - interval, interval);
                    t = (last.getValue() == AVAIL_PAUSED) ? TimingVoodoo.roundDownTime(now, interval) : t;
                    DataPoint point = new DataPoint(meas.getId(), new MetricValue(AVAIL_DOWN, t));
                    Resource resource = meas.getResource();
                    final long lastFromLather = getLastLatherConnectTime(resource, latherDispatcher);
                    if ((lastFromLather == Long.MIN_VALUE) || ((now - lastFromLather) > minDowntime)) {
                        rtn.put(resource.getId(), new ResourceDataPoint(resource, point));
                            final String msg = new StringBuilder(256)
                                .append("Marking availability DOWN for ").append(last)
                                .append(", CacheValue=(").append(TimeUtil.toString(lastTimestamp))
                                .append(", datapt=").append(last.getValue())
                                .append(") vs. Now=(").append(nowTimestamp).append(") vs. Lather=(")
                                .append(TimeUtil.toString(lastFromLather)).append(")")
                                .toString();
                        log.info(msg);
                    }
                    rtn.put(resource.getId(), new ResourceDataPoint(resource, point));
                }
            }
        }
        if (!rtn.isEmpty()) {
            permissionManager.getHierarchicalAlertingManager().performSecondaryAvailabilityCheck(rtn);
        }
        return rtn;
    }
    
    private long getLastLatherConnectTime(Resource resource, LatherDispatcher latherDispatcher) {
        long rtn = Long.MIN_VALUE;
        try {
            String agentToken = agentManager.getAgent(AppdefUtil.newAppdefEntityId(resource)).getAgentToken();
            rtn = latherDispatcher.getLastCommunication(agentToken);
        } catch (AgentNotFoundException e) {
            log.debug(e,e);
        }
        return rtn;
    }

    private long getBeginWindow(long end, Measurement meas) {
        final long interval = 0;
        final long wait = 5 * MINUTE;
        long measInterval = meas.getInterval();

        // We have to get at least the measurement interval
        long maxInterval = Math.max(Math.max(interval, wait), measInterval);

        // Begin is maximum of interval or measurement create time
        long begin = Math.max(end - maxInterval, meas.getMtime() + measInterval);
        return TimingVoodoo.roundDownTime(begin, measInterval);
    }

    // End is at least more than 1 interval away
    private long getEndWindow(long current, Measurement meas) {
        return TimingVoodoo.roundDownTime((current - meas.getInterval()), meas.getInterval());
    }
    

}
