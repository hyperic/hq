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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceEdge;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.common.DiagnosticsLogger;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.ha.HAUtil;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.MetricDataCache;
import org.hyperic.hq.measurement.shared.AvailabilityManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.Transformer;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component("metricsNotComingInDiagnostic")
public class MetricsNotComingInDiagnostic
implements DiagnosticObject, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private final Log log = LogFactory.getLog(MetricsNotComingInDiagnostic.class);
    private AtomicLong last = new AtomicLong(now() - 1000*60*60*11);
    // 12 hours
    private static final long REPORT_THRESHOLD = 1000 * 60 * 60 * 12;
    private static final long VIOLATION_THRESHOLD = 1000 * 60 * 60;
    private static final Object LOCK = new Object();
    private String lastVerboseStatus = null;
    private String lastNonVerboseStatus = null;
    private static String METRICSNOTCOMINGINDIAGNOSTIC_DISABLE = "MetricsNotComingInDiagnostic.disable";
    private DiagnosticsLogger diagnosticsLogger;
    private AuthzSubjectManager authzSubjectManager;
    private AvailabilityManager availabilityManager;
    private MeasurementManager measurementManager;
    private ResourceManager resourceManager;
    private PlatformManager platformManager;
    private MetricDataCache metricDataCache;
    private ApplicationContext ctx;
    private AtomicBoolean disabled = new AtomicBoolean(false);

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

        String isDisabled = System.getProperty(METRICSNOTCOMINGINDIAGNOSTIC_DISABLE, "false");
        if (isDisabled.equals("false")) {
            setDisabled(false);
        } else {
            setDisabled(true);
        }
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

    public Boolean getDisabled() {
        return disabled.get();
    }
    
    public void setDisabled(Boolean disabled) {
        log.info("Setting disabled flag to " + disabled.toString());
        this.disabled.set(disabled);
    }
    
    private String getReport(final boolean isVerbose) {
        if (!HAUtil.isMasterNode()) {
            return "Server must be the primary node in the HA configuration before this report is valid.";
        }

        if (getDisabled()) {
            return "Report disabled";
        }

        if ((now() - last.get()) < REPORT_THRESHOLD) {
            synchronized (LOCK) {
                String rtn = (isVerbose) ? lastVerboseStatus : lastNonVerboseStatus;
                if (rtn == null) {
                    return "report will not be executed until the server is up for 60 minutes\n";
                }
                return rtn;
            }
        }
        final StringBuilder verbose = new StringBuilder();
        final StringBuilder nonVerbose = new StringBuilder();
        try {
            setStatusBuf(nonVerbose, verbose);
        } catch (Throwable e) {
            log.error(e, e);
        } finally {
            last.set(now());
            synchronized (LOCK) {
                lastVerboseStatus = verbose.toString();
                lastNonVerboseStatus = nonVerbose.toString();
            }
        }
        return (isVerbose) ? lastVerboseStatus : lastNonVerboseStatus;
    }

    private void setStatusBuf(StringBuilder nonVerbose, StringBuilder verbose) {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        
        /* !!!PLEASE NOTE!!!
         * This diagostic pulls in a large amount of pojos when it runs.  In order to alleviate
         * the potential of it crashing the server this method does not run in single session
         * anymore.  The reason for this is so that memory may be freed up while the method is
         * resolving all the resources not reporting metrics.  The code looks odd since it is
         * pulling in pojos using hibernate get() to avoid lazy session initialization issues,
         * so please be careful when modifying it.
         */
        if (debug) watch.markTimeBegin("getAllPlatforms");
        final Collection<Platform> platforms = getAllPlatforms();
        if (debug) watch.markTimeEnd("getAllPlatforms");

        if (debug) watch.markTimeBegin("getResources");
        final Map<Integer, Resource> resources = getResources(platforms);
        if (debug) watch.markTimeEnd("getResources");

        final List<Integer> resourceIds = getResourceIds(resources.values());
        if (debug) watch.markTimeBegin("getAvailMeasurements");
        final Map<Integer, List<Measurement>> measCache =
            measurementManager.getAvailMeasurements(resourceIds);
        if (debug) watch.markTimeEnd("getAvailMeasurements");
                
        if (debug) watch.markTimeBegin("getLastPlatformAvail");
        final Map<Integer, MetricValue> avails =
            availabilityManager.getLastAvail(resources.values(), measCache);
        if (debug) watch.markTimeEnd("getLastPlatformAvail");
                
        final List<Resource> children = new ArrayList<Resource>();
        if (debug) watch.markTimeBegin("getChildren");
        final Map<Integer,Platform> childrenToPlatform =
            getChildren(platforms, measCache, avails, resources, children);
        if (debug) watch.markTimeEnd("getChildren");
        
        final List<Integer> childrenIds = getResourceIds(children);
        if (debug) watch.markTimeBegin("filterOutNonAvailableResources");
        filterOutNonAvailableResources(childrenIds, childrenToPlatform);
        if (debug) watch.markTimeEnd("filterOutNonAvailableResources");
        
        if (debug) watch.markTimeBegin("getEnabledMeasurements");
        final Collection<List<Measurement>> measurements =
            measurementManager.getEnabledNonAvailMeasurements(children).values();
        if (debug) watch.markTimeEnd("getEnabledMeasurements");
                
        if (debug) watch.markTimeBegin("getLastMetricValues");
        final Map<Integer,MetricValue> values = getLastMetricValues(measurements);
        if (debug) watch.markTimeEnd("getLastMetricValues");
        
        if (debug) watch.markTimeBegin("getStatus");
        setStatus(measurements, values, avails, childrenToPlatform, resources, nonVerbose, verbose);
        if (debug) watch.markTimeEnd("getStatus");
        
        if (debug) {
            log.debug("getStatus: " + watch
                        + ", { Size: [measCache=" + measCache.size()
                        + "] [lastAvails=" + avails.size()
                        + "] [childrenToPlatform=" + childrenToPlatform.size()
                        + "] [enabledMeasurements=" + measurements.size()
                        + "] [lastMetricValues=" + values.size()
                        + "] }");
        }
    }

    private List<Integer> getResourceIds(Collection<Resource> resources) {
        return new Transformer<Resource, Integer>() {
            @Override
            public Integer transform(Resource r) {
                return r.getId();
            }
        }.transform(resources);
    }

    private void filterOutNonAvailableResources(List<Integer> resources,
                                                Map<Integer, Platform> resourcesToPlatform) {
        final Map<Integer, List<Measurement>> measCache =
            measurementManager.getAvailMeasurements(resources);
        final Map<Integer, MetricValue> avails =
            availabilityManager.getLastAvail(resources, measCache);
        for (final Iterator<Integer> it=resources.iterator(); it.hasNext(); ) {
            final Integer childId = it.next();
            if (!resourceIsAvailable(childId, measCache, avails)) {
                it.remove();
                resourcesToPlatform.remove(childId);
            }
        }
    }

    private void setStatus(Collection<List<Measurement>> measurementLists,
                           Map<Integer, MetricValue> values,
                           Map<Integer, MetricValue> avails,
                           Map<Integer, Platform> childrenToPlatform,
                           Map<Integer, Resource> resources,
                           StringBuilder nonVerbose, StringBuilder verbose) {
        final Map<Platform, List<String>> platHierarchyNotReporting = new HashMap<Platform, List<String>>();
        for (final List<Measurement> mList : measurementLists) {
            for (Measurement m : mList) {
                if (m != null && !values.containsKey(m.getId())) {
                    final Platform platform = childrenToPlatform.get(m.getResource().getId());
                    if (platform == null) {
                        continue;
                    }
                    List<String> tmp;
                    if (null == (tmp = platHierarchyNotReporting.get(platform))) {
                        tmp = new ArrayList<String>();
                        platHierarchyNotReporting.put(platform, tmp);
                    }
                    List<String> list = tmp;
                    Resource res = resources.get(m.getResource().getId());
                    // res should not be null, but just in case
                    if (res == null) {
                        res = resourceManager.getResourceById(m.getResource().getId());
                    }
                    MeasurementTemplate template = measurementManager.getTemplatesByMeasId(m.getId());
                    String templateName = (template == null) ? "UNKNOWN" : template.getName();
                    list.add(new StringBuilder(128)
                        .append("\nmid=").append(m.getId())
                        .append(", name=").append(templateName)
                        .append(", resid=").append(m.getResource().getId())
                        .append(", resname=").append(res.getName())
                        .toString());
                }
            }
        }
        verbose.append("\nReport generated at ").append(TimeUtil.toString(now()))
               .append("\nEnabled metrics not reported in for ")
               .append(VIOLATION_THRESHOLD / 1000 / 60)
               .append(" minutes (by platform hierarchy)\n");
        nonVerbose.append("\nReport generated at ").append(TimeUtil.toString(now()))
                  .append("\nEnabled metrics not reported in for ")
                  .append(VIOLATION_THRESHOLD / 1000 / 60)
                  .append(" minutes (by platform hierarchy)\n");
        verbose.append("------------------------------------------------------------------------\n");
        nonVerbose.append("------------------------------------------------------------------------\n");
        for (final Entry<Platform, List<String>> entry : platHierarchyNotReporting.entrySet()) {
            final Platform platform = entry.getKey();
            verbose.append("\nfqdn=").append(platform.getFqdn()).append(" (");
            nonVerbose.append("\nfqdn=").append(platform.getFqdn()).append(" (");
            final List<String> children = (List<String>) entry.getValue();
            // verbose
            verbose.append(children.size());
            verbose.append(" not collecting):");
            for (String xx : children) {
                verbose.append(xx);
            }
            // non verbose
            nonVerbose.append(children.size());
            nonVerbose.append(" not collecting)");
        }
        verbose.append("\n");
        nonVerbose.append("\n");
    }

    /**
     * @return {@link Map} of {@link Integer}s of resourceIds to their top level {@link Platform}
     */
    private Map<Integer, Platform> getChildren(Collection<Platform> platforms,
                                               Map<Integer, List<Measurement>> measCache,
                                               Map<Integer, MetricValue> avails,
                                               Map<Integer, Resource> resourceMap,
                                               List<Resource> children) {
        final Map<Integer, Platform> rtn = new HashMap<Integer, Platform>();
        final long now = now();
        for (final Platform platform : platforms) {
            if ((now - platform.getCreationTime()) < VIOLATION_THRESHOLD ||
                !measCache.containsKey(platform.getResource().getId()) || 
                !resourceIsAvailable(platform.getResource().getId(), measCache, avails)) {
                resourceMap.remove(platform.getResource().getId());
                continue;
            }
        }
        final Collection<ResourceEdge> edges = resourceManager.findResourceEdges(
            resourceManager.getContainmentRelation(), new ArrayList<Resource>(resourceMap.values()));
        for (final ResourceEdge edge : edges) {
            try {
                Resource from = resourceMap.get(edge.getFrom().getId());
                // res should not be null, but just in case
                if (from == null) {
                    from = resourceManager.getResourceById(edge.getFrom().getId());
                }
                final Platform platform = platformManager.findPlatformById(from.getInstanceId());
                final Resource child = resourceManager.getResourceById(edge.getTo().getId());
                if (child == null || child.isInAsyncDeleteState()) {
                    continue;
                }
                resourceMap.put(child.getId(), child);
                children.add(child);
                rtn.put(child.getId(), platform);
            } catch (PlatformNotFoundException e) {
                log.debug(e);
            }
        }
        return rtn;
    }

    private Map<Integer, Resource> getResources(Collection<Platform> platforms) {
        final Map<Integer, Resource> resources = new HashMap<Integer, Resource>(platforms.size());
        for (final Platform platform : platforms) {
            final Integer resId = platform.getResource().getId();
            final Resource r = resourceManager.getResourceById(resId);
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
            resources.put(resId, r);
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
        return metricDataCache.getAll(mids, now() - VIOLATION_THRESHOLD);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private boolean resourceIsAvailable(Integer resourceId,
                                        Map<Integer, List<Measurement>> measCache,
                                        Map<Integer, MetricValue> avails) {
        final List<Measurement> measurements =  measCache.get(resourceId);
        if (measurements == null || measurements.isEmpty()) {
            return false;
        }
        final Measurement availMeas = (Measurement) measurements.get(0);
        MetricValue val = avails.get(availMeas.getId());
        return (val.getValue() == MeasurementConstants.AVAIL_DOWN) ? false : true;
    }

    private Collection<Platform> getAllPlatforms() {
        AuthzSubject overlord = authzSubjectManager.getOverlordPojo();
        try {
            return platformManager.findAll(overlord);
        } catch (PermissionException e) {
            log.error(e,e);
            return Collections.emptyList();
        }
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() != ctx) {
            return;
        }
        diagnosticsLogger.addDiagnosticObject((DiagnosticObject) Bootstrap.getBean("metricsNotComingInDiagnostic"));
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

}
