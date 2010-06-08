/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], Hyperic, Inc.
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

package org.hyperic.hq.measurement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ejb.FinderException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.shared.PlatformManagerLocal;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceEdge;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.common.DiagnosticObject;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.hibernate.SessionManager.SessionRunner;
import org.hyperic.hq.measurement.server.session.AvailabilityManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.MetricDataCache;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.pager.PageControl;

public class MetricsNotComingInDiagnostic implements DiagnosticObject {
    
    private static final Log log = LogFactory.getLog(MetricsNotComingInDiagnostic.class);
    private static MetricsNotComingInDiagnostic instance = new MetricsNotComingInDiagnostic();
    private static final long started = now();
    // 60 minutes
    private static final long THRESHOLD = 1000*60*60;
    
    private MetricsNotComingInDiagnostic() {}
    
    public static MetricsNotComingInDiagnostic getInstance() {
        return instance;
    }

    public String getName() {
        return "Enabled Metrics Not Coming In";
    }

    public String getShortName() {
        return "EnabledMetricsNotComingIn";
    }

    public String getStatus() {
        if ((now() - THRESHOLD) < started) {
            return "Server must be up for " + THRESHOLD/1000/60 +
                   " minutes before this report is valid";
        }
        final StringBuilder rtn = new StringBuilder();
        try {
            SessionManager.runInSession(new SessionRunner() {
                public void run() throws Exception {
                    setStatusBuf(rtn);
                }
                public String getName() {
                    return MetricsNotComingInDiagnostic.class.getSimpleName();
                }
            });
        } catch (Exception e) {
            log.error(e,e);
        }
        return rtn.toString();
    }

    public void setStatusBuf(StringBuilder buf) {
        final Collection platforms = getAllPlatforms();
        final AvailabilityManagerLocal aMan = AvailabilityManagerEJBImpl.getOne();
        final MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        final Collection resources = getResources(platforms);
        final Map measCache = mMan.getAvailMeasurements(resources);
        final Map avails = aMan.getLastAvail(resources, measCache);
        final List children = new ArrayList();
        final Map childrenToPlatform = getChildren(platforms, measCache, avails, children);
        final Collection measurements = mMan.getEnabledMeasurements(children).values();
        final Map values = getLastMetricValues(measurements);
        buf.append(getStatus(measurements, values, avails, childrenToPlatform));
    }

    private StringBuilder getStatus(Collection measurementLists, Map values, Map avails,
                                    Map childrenToPlatform) {
        final Map platHierarchyNotReporting = new HashMap();
        for (final Iterator it=measurementLists.iterator(); it.hasNext(); ) {
            final List mList = (List) it.next();
            for (final Iterator mit=mList.iterator(); mit.hasNext(); ) {
                final Measurement m = (Measurement) mit.next();
                if (m != null && !m.getTemplate().isAvailability()
                              && !values.containsKey(m.getId())) {
                    final Platform platform = (Platform) childrenToPlatform.get(m.getResource());
                    if (platform == null) {
                        continue;
                    }
                    List tmp;
                    if (null == (tmp = (List)platHierarchyNotReporting.get(platform))) {
                        tmp = new ArrayList();
                        platHierarchyNotReporting.put(platform, tmp);
                    }
                    tmp.add(new StringBuilder(128)
                       .append("\nmid=").append(m.getId())
                       .append(", name=").append(m.getTemplate().getName())
                       .append(", resid=").append(m.getResource().getId())
                       .append(", resname=").append(m.getResource().getName())
                       .toString());
                }
            }
        }
        final StringBuilder rtn = new StringBuilder(platHierarchyNotReporting.size()*128);
        rtn.append("\nEnabled metrics not reported in for ")
           .append(THRESHOLD/1000/60).append(" minutes (by platform hierarchy)\n");
        rtn.append("------------------------------------------------------------------------\n");
        for (final Iterator it=platHierarchyNotReporting.entrySet().iterator(); it.hasNext(); ) {
            final Entry entry = (Entry) it.next();
            final Platform platform = (Platform) entry.getKey();
            final List children = (List) entry.getValue();
            rtn.append("\nfqdn=").append(platform.getFqdn())
               .append(" (").append(children.size()).append(" not collecting):");
            for (final Iterator xx=children.iterator(); xx.hasNext(); ) {
                rtn.append(xx.next().toString());
            }
        }
        return rtn.append("\n");
    }

    /**
     * @return {@link Map} of {@link Resource}s to their top level {@link Platform}
     */
    private Map getChildren(Collection platforms, Map measCache, Map avails, List children) {
        final Map rtn = new HashMap(platforms.size());
        final long now = now();
        final List resources = new ArrayList(platforms.size());
        for (final Iterator it=platforms.iterator(); it.hasNext(); ) {
            final Platform platform = (Platform) it.next();
            final Resource r = platform.getResource();
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
            if ((now - platform.getCreationTime()) < THRESHOLD  ||
                !measCache.containsKey(r.getId()) || 
                !platformIsAvailable(platform, measCache, avails)) {
                continue;
            }
            resources.add(platform.getResource());
        }
        final ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
        final PlatformManagerLocal pMan = PlatformManagerEJBImpl.getOne();
        final Collection edges = rMan.findResourceEdges(rMan.getContainmentRelation(), resources);
        for (final Iterator it=edges.iterator(); it.hasNext(); ) {
            final ResourceEdge edge = (ResourceEdge) it.next();
            try {
                final Platform platform = pMan.findPlatformById(edge.getFrom().getInstanceId());
                final Resource child = edge.getTo();
                children.add(child);
                rtn.put(child, platform);
            } catch (PlatformNotFoundException e) {
                log.debug(e);
            }
        }
        return rtn;
    }

    private Collection getResources(Collection platforms) {
        final Collection resources = new ArrayList(platforms.size());
        for (final Iterator it=platforms.iterator(); it.hasNext(); ) {
            final Platform platform = (Platform) it.next();
            resources.add(platform.getResource());
        }
        return resources;
    }

    /**
     * @return {@link Map} of {@link Integer} of measurementIds to {@link MetricValue}
     */
    private Map getLastMetricValues(Collection measLists) {
        final MetricDataCache cache = MetricDataCache.getInstance();
        final List mids = new ArrayList();
        for (final Iterator it=measLists.iterator(); it.hasNext(); ) {
            final List measList = (List) it.next();
            for (final Iterator mit=measList.iterator(); mit.hasNext(); ) {
                final Measurement m = (Measurement) mit.next();
                mids.add(m.getId());
            }
        }
        return cache.getAll(mids, now()-THRESHOLD);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private boolean platformIsAvailable(Platform platform, Map measCache, Map avails) {
        final Resource resource = platform.getResource();
        final List measurements = (List) measCache.get(resource.getId());
        final Measurement availMeas = (Measurement) measurements.get(0);
        MetricValue val = (MetricValue) avails.get(availMeas.getId());
        return (val.getValue() == MeasurementConstants.AVAIL_DOWN) ? false : true;
    }

    private Collection getAllPlatforms() {
        AuthzSubject overlord = AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        PlatformManagerLocal pMan = PlatformManagerEJBImpl.getOne();
        Collection platforms;
        try {
            platforms = pMan.getAllPlatforms(overlord, PageControl.PAGE_ALL);
        } catch (PermissionException e1) {
            return Collections.EMPTY_LIST;
        } catch (FinderException e1) {
            return Collections.EMPTY_LIST;
        }
        Collection rtn = new ArrayList(platforms.size());
        for (final Iterator it=platforms.iterator(); it.hasNext(); ) {
            PlatformValue p = (PlatformValue) it.next();
            try {
                rtn.add(pMan.findPlatformById(p.getId()));
            } catch (PlatformNotFoundException e) {
                continue;
            }
        }
        return rtn;
    }

}
