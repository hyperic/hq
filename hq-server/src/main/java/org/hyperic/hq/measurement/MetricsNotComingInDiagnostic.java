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

package org.hyperic.hq.measurement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceEdge;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticsLogger;
import org.hyperic.hq.common.NotFoundException;
import org.hyperic.hq.ha.HAUtil;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.hibernate.SessionManager.SessionRunner;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MetricDataCache;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MetricsNotComingInDiagnostic implements DiagnosticObject {

    private final Log log = LogFactory.getLog(MetricsNotComingInDiagnostic.class);
    private final long started = now();
    // 60 minutes
    private static final long THRESHOLD = 1000 * 60 * 60;
    private DiagnosticsLogger diagnosticsLogger;
    private AuthzSubjectManager authzSubjectManager;
    private AvailabilityManager availabilityManager;
    private MeasurementManager measurementManager;
    private ResourceManager resourceManager;
    private PlatformManager platformManager;
    private MetricDataCache metricDataCache;

    @Autowired
    public MetricsNotComingInDiagnostic(DiagnosticsLogger diagnosticsLogger,
                                        AuthzSubjectManager authzSubjectManager,
                                        AvailabilityManager availabilityManager,
                                        MeasurementManager measurementManager,
                                        ResourceManager resourceManager,
                                        PlatformManager platformManager,
                                        MetricDataCache metricDataCache) {
        this.diagnosticsLogger = diagnosticsLogger;
        this.authzSubjectManager = authzSubjectManager;
        this.availabilityManager = availabilityManager;
        this.measurementManager = measurementManager;
        this.resourceManager = resourceManager;
        this.platformManager = platformManager;
        this.metricDataCache = metricDataCache;
    }

    @PostConstruct
    public void register() {
        diagnosticsLogger.addDiagnosticObject(this);
    }

    public String getName() {
        return "Enabled Metrics Not Coming In";
    }

    public String getShortName() {
        return "EnabledMetricsNotComingIn";
    }

    public String getStatus() {
        return getReport(true);
    }
    
    public String getShortStatus() {
        return getReport(false);
    }
    
    private String getReport(final boolean isVerbose) {
        if (!HAUtil.isMasterNode()) {
            return "Server must be the primary node in the HA configuration before this report is valid.";
        }
        if ((now() - THRESHOLD) < started) {
            return "Server must be up for " + THRESHOLD / 1000 / 60 +
                   " minutes before this report is valid";
        }
        final StringBuilder rtn = new StringBuilder();
        try {
            SessionManager.runInSession(new SessionRunner() {
                public void run() throws Exception {
                    setStatusBuf(rtn, isVerbose);
                }

                public String getName() {
                    return MetricsNotComingInDiagnostic.class.getSimpleName();
                }
            });
        } catch (Exception e) {
            log.error(e, e);
        }
        return rtn.toString();
    }

    private void setStatusBuf(StringBuilder buf, boolean isVerbose) {
        StopWatch watch = new StopWatch();
        
        watch.markTimeBegin("getAllPlatforms");
        final Collection<Platform> platforms = getAllPlatforms();
        watch.markTimeEnd("getAllPlatforms");

        watch.markTimeBegin("getResources");
        final Collection<Resource> resources = getResources(platforms);
        watch.markTimeEnd("getResources");

        watch.markTimeBegin("getAvailMeasurements");
        final Map<Integer, List<Measurement>> measCache = measurementManager
            .getAvailMeasurements(resources);
        watch.markTimeEnd("getAvailMeasurements");
                
        watch.markTimeBegin("getLastAvail");
        final Map<Integer, MetricValue> avails = availabilityManager.getLastAvail(resources,
            measCache);
        watch.markTimeEnd("getLastAvail");
                
        watch.markTimeBegin("getChildren");
        final List<Resource> children = new ArrayList<Resource>();
        final Map<Resource,Platform> childrenToPlatform = getChildren(platforms, measCache, avails, children);
        watch.markTimeEnd("getChildren");
        
        watch.markTimeBegin("getEnabledMeasurements");
        final Collection<List<Measurement>> measurements = measurementManager.getEnabledMeasurements(children)
            .values();
        watch.markTimeEnd("getEnabledMeasurements");
                
        watch.markTimeBegin("getLastMetricValues");
        final Map<Integer,MetricValue> values = getLastMetricValues(measurements);
        watch.markTimeEnd("getLastMetricValues");
        
        watch.markTimeBegin("getStatus");
        buf.append(getStatus(measurements, values, avails, childrenToPlatform, isVerbose));
        watch.markTimeEnd("getStatus");
        
        if (log.isDebugEnabled()) {
            log.debug("getStatus: " + watch
                        + ", { Size: [measCache=" + measCache.size()
                        + "] [lastAvails=" + avails.size()
                        + "] [childrenToPlatform=" + childrenToPlatform.size()
                        + "] [enabledMeasurements=" + measurements.size()
                        + "] [lastMetricValues=" + values.size()
                        + "] }");
        }
    }

    private StringBuilder getStatus(Collection<List<Measurement>> measurementLists,
                                    Map<Integer, MetricValue> values,
                                    Map<Integer, MetricValue> avails,
                                    Map<Resource, Platform> childrenToPlatform, boolean isVerbose) {
        final Map<Platform, List<String>> platHierarchyNotReporting = new HashMap<Platform, List<String>>();
        for (final List<Measurement> mList : measurementLists) {
            for (Measurement m : mList) {
                if (m != null && !m.getTemplate().isAvailability() &&
                    !values.containsKey(m.getId())) {
                    final Platform platform = childrenToPlatform.get(m.getResource());
                    if (platform == null) {
                        continue;
                    }
                    List<String> tmp;
                    if (null == (tmp = platHierarchyNotReporting.get(platform))) {
                        tmp = new ArrayList<String>();
                        platHierarchyNotReporting.put(platform, tmp);
                    }
                    if (isVerbose) {
                        tmp.add(new StringBuilder(128).append("\nmid=").append(m.getId()).append(
                            ", name=").append(m.getTemplate().getName()).append(", resid=").append(
                            m.getResource().getId()).append(", resname=").append(
                            m.getResource().getName()).toString());
                    } else {
                        tmp.add(m.toString());
                    }
                }
            }
        }
        final StringBuilder rtn = new StringBuilder(platHierarchyNotReporting.size() * 128);
        rtn.append("\nEnabled metrics not reported in for ").append(THRESHOLD / 1000 / 60).append(
            " minutes (by platform hierarchy)\n");
        rtn.append("------------------------------------------------------------------------\n");
        for (final Entry<Platform, List<String>> entry : platHierarchyNotReporting.entrySet()) {
            final Platform platform = entry.getKey();
            final List<String> children = entry.getValue();
            rtn.append("\nfqdn=").append(platform.getFqdn()).append(" (").append(children.size())
                .append(" not collecting)");
            if (isVerbose) {
                rtn.append(":");
                for (String xx : children) {
                    rtn.append(xx);
                }
            }
        }
        return rtn.append("\n");
    }

    /**
     * @return {@link Map} of {@link Resource}s to their top level
     *         {@link Platform}
     */
    private Map<Resource, Platform> getChildren(Collection<Platform> platforms,
                                                Map<Integer, List<Measurement>> measCache,
                                                Map<Integer, MetricValue> avails,
                                                List<Resource> children) {
        final Map<Resource, Platform> rtn = new HashMap<Resource, Platform>(platforms.size());
        final long now = now();
        final List<Resource> resources = new ArrayList<Resource>(platforms.size());
        for (final Platform platform : platforms) {
            final Resource r = platform.getResource();
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
            if ((now - platform.getCreationTime()) < THRESHOLD ||
                !measCache.containsKey(r.getId()) || 
                !platformIsAvailable(platform, measCache, avails)) {
                continue;
            }
            resources.add(platform.getResource());
        }

        final Collection<ResourceEdge> edges = resourceManager.findResourceEdges(resourceManager
            .getContainmentRelation(), resources);
        for (final ResourceEdge edge : edges) {
            try {
                final Platform platform = platformManager.findPlatformById(edge.getFrom()
                    .getInstanceId());
                final Resource child = edge.getTo();
                children.add(child);
                rtn.put(child, platform);
            } catch (PlatformNotFoundException e) {
                log.debug(e);
            }
        }
        return rtn;
    }

    private Collection<Resource> getResources(Collection<Platform> platforms) {
        final Collection<Resource> resources = new ArrayList<Resource>(platforms.size());
        for (final Platform platform : platforms) {
            resources.add(platform.getResource());
        }
        return resources;
    }

    /**
     * @return {@link Map} of {@link Integer} of measurementIds to
     *         {@link MetricValue}
     */
    private Map<Integer, MetricValue> getLastMetricValues(Collection<List<Measurement>> measLists) {

        final List<Integer> mids = new ArrayList<Integer>();
        for (final List<Measurement> measList : measLists) {
            for (final Measurement m : measList) {
                mids.add(m.getId());
            }
        }
        return metricDataCache.getAll(mids, now() - THRESHOLD);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private boolean platformIsAvailable(Platform platform,
                                        Map<Integer, List<Measurement>> measCache,
                                        Map<Integer, MetricValue> avails) {
        final Resource resource = platform.getResource();
        final List<Measurement> measurements =  measCache.get(resource.getId());
        final Measurement availMeas = (Measurement) measurements.get(0);
        MetricValue val = avails.get(availMeas.getId());
        return (val.getValue() == MeasurementConstants.AVAIL_DOWN) ? false : true;
    }

    @SuppressWarnings("unchecked")
    private Collection<Platform> getAllPlatforms() {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();

        Collection<PlatformValue> platforms;
        try {
            platforms = platformManager.getAllPlatforms(overlord, PageControl.PAGE_ALL);
        } catch (PermissionException e1) {
            return Collections.EMPTY_LIST;
        } catch (NotFoundException e) {
            return Collections.EMPTY_LIST;
        }
        Collection<Platform> rtn = new ArrayList<Platform>(platforms.size());
        for (final PlatformValue p : platforms) {
            try {
                rtn.add(platformManager.findPlatformById(p.getId()));
            } catch (PlatformNotFoundException e) {
                continue;
            }
        }
        return rtn;
    }

}
