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

package org.hyperic.hq.bizapp.server.session;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.PlatformType;
import org.hyperic.hq.appdef.shared.AppdefCompatException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayConstants;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplaySummary;
import org.hyperic.hq.bizapp.shared.uibeans.MetricDisplayValue;
import org.hyperic.hq.bizapp.shared.uibeans.ProblemMetricSummary;
import org.hyperic.hq.grouping.CritterList;
import org.hyperic.hq.grouping.CritterTranslationContext;
import org.hyperic.hq.grouping.CritterTranslator;
import org.hyperic.hq.grouping.critters.DescendantProtoCritterType;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.shared.AvailabilityManagerLocal;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.timer.StopWatch;

public class MetricSessionEJB extends BizappSessionEJB {
    private Log log = LogFactory.getLog(MetricSessionEJB.class.getName());

    protected SessionManager manager = SessionManager.getInstance();
    private static final double AVAIL_DOWN = MeasurementConstants.AVAIL_DOWN;

    /**
     * Fetch the metric summaries for specified resources and templates
     * @param resources the list of resources
     * @param tmpls the list of measurement templates
     * @param begin the beginning of time range
     * @param end the end of time range
     * @param showNoCollect whether or not to include templates that have not
     *        collected data
     * @return Map where key = category, value = List of summary beans
     */
    protected Map getResourceMetrics(AuthzSubject subject, List resources,
                                     List tmpls, long begin, long end,
                                     Boolean showNoCollect)
        throws AppdefCompatException
    {
        List templates;
        Integer[] tids;

        // Create Map of all resources
        HashMap resmap = new HashMap(MeasurementConstants.VALID_CATEGORIES.length);

        if (tmpls.size() == 0 || resources.size() == 0)
            return resmap;
            
        if (tmpls.get(0) instanceof MeasurementTemplate) {
            templates = tmpls;
            tids = new Integer[templates.size()];
            for (int i = 0; i < templates.size(); i++ ) {
                MeasurementTemplate t = (MeasurementTemplate)templates.get(i);
                tids[i] = t.getId();
            }
        }
        else {
            // If templates are just ID's, we have to look them up
            tids = (Integer[])tmpls.toArray(new Integer[tmpls.size()]);
            try {
                templates = getTemplateManager().getTemplates(tids,
                                                              PageControl.PAGE_ALL);
            } catch (TemplateNotFoundException e) {
                templates = new ArrayList(0);
                // Well, if we don't find it, *shrug*
            }
        }
        
        // Create the EntityIds array and map of counts
        Integer[] eids = new Integer[resources.size()];
        AppdefEntityID[] aeids = new AppdefEntityID[resources.size()];
        AppdefEntityID aeid;
        Map totalCounts = new HashMap();
        Iterator it = resources.iterator();
        for (int i = 0; it.hasNext(); i++) {
            // We understand two types
            Object resource = it.next();
            if (resource instanceof AppdefResourceValue) {
                AppdefResourceValue resVal = (AppdefResourceValue) resource;
                aeid = resVal.getEntityId();

                // Increase count
                String type = resVal.getAppdefResourceTypeValue().getName();
                int count = 0;
                if (totalCounts.containsKey(type)) {
                    count = ((Integer) totalCounts.get(type)).intValue();
                }
                totalCounts.put(type, new Integer(++count));
            }
            else if (resource instanceof AppdefEntityID) {
                aeid = (AppdefEntityID) resource;
            }
            else {
                throw new AppdefCompatException("getResourceMetrics() does " +
                    "not understand resource class: " + resource.getClass());
            }
            
            eids[i]  = aeid.getId();
            aeids[i] = aeid;
        }
            
        // Now get the aggregate data, keyed by template ID's
        final MeasurementManagerLocal mMan = getMetricManager();
        final DataManagerLocal dMan = getDataMan();
        final List measurements = mMan.getMeasurements(tids, eids);
        final Map datamap =
            dMan.getAggregateDataByTemplate(measurements, begin, end);

        // Get the intervals, keyed by template ID's as well
        final Map intervals = (showNoCollect == null) ?
            new HashMap() : mMan.findMetricIntervals(subject, aeids, tids);

        for (it = templates.iterator(); it.hasNext(); ) {
            MeasurementTemplate tmpl = (MeasurementTemplate) it.next();
    
            int total = eids.length;
            String type = tmpl.getMonitorableType().getName();
            if (totalCounts.containsKey(type)) {
                total = ((Integer) totalCounts.get(type)).intValue();
            }
    
            double[] data = (double[]) datamap.get(tmpl.getId());
                
            if (data == null &&
                (showNoCollect == null || showNoCollect.equals(Boolean.FALSE)))
                continue;
    
            String category = tmpl.getCategory().getName();
            TreeSet summaries = (TreeSet) resmap.get(category);
            if (summaries == null) {
                summaries = new TreeSet();
                resmap.put(category, summaries);
            }
                
            Long interval = (Long) intervals.get(tmpl.getId());
    
            // Now create a MetricDisplaySummary and add it to the list
            MetricDisplaySummary summary =
                getMetricDisplaySummary(tmpl, interval, begin, end,
                                        data, total);

            summaries.add(summary);
        }
        
        return resmap;
    }

    /**
     * Fetch all metric summaries for specified resources
     * @param resources the list of resources
     * @param begin the beginning of time range
     * @param end the end of time range
     * @param showNoCollect TODO
     * @return Map where key = category, value = List of summary beans
     * @throws AppdefCompatException
     */
    protected Map getResourceMetrics(AuthzSubject subject, List resources,
                                     String resourceType, long filters,
                                     String keyword, long begin, long end,
                                     boolean showNoCollect)
        throws AppdefCompatException {
        // Need to get the templates for this type
        List tmpls = getTemplateManager().findTemplates(resourceType, filters,
                                                        keyword);
    
        // Look up the metric summaries of associated servers
        return getResourceMetrics(subject, resources, tmpls, begin, end,
                                  Boolean.valueOf(showNoCollect));
    }

    protected MetricDisplaySummary
        getMetricDisplaySummary(MeasurementTemplate tmpl, Long interval,
                                long begin, long end, double[] data,
                                int totalConfigured) {
        // Create a new metric summary bean
        MetricDisplaySummary summary = new MetricDisplaySummary();
            
        // Set the time range
        summary.setBeginTimeFrame(new Long(begin));
        summary.setEndTimeFrame(new Long(end));
            
        // Set the template info
        summary.setLabel(tmpl.getName());
        summary.setTemplateId(tmpl.getId());
        summary.setTemplateCat(tmpl.getCategory().getId());
        summary.setCategory(tmpl.getCategory().getName());
        summary.setUnits(tmpl.getUnits());
        summary.setCollectionType(new Integer(tmpl.getCollectionType()));
        summary.setDesignated(Boolean.valueOf(tmpl.isDesignate()));
        summary.setMetricSource(tmpl.getMonitorableType().getName());
        
        summary.setCollecting(interval != null);
        
        if (summary.getCollecting())
            summary.setInterval(interval.longValue());
    
        if (data == null)
            return summary;
        
        // Set the data values
        summary.setMetric(MetricDisplayConstants.MIN_KEY,
            new MetricDisplayValue(data[MeasurementConstants.IND_MIN]));
        summary.setMetric(MetricDisplayConstants.AVERAGE_KEY,
            new MetricDisplayValue(data[MeasurementConstants.IND_AVG]));
        summary.setMetric(MetricDisplayConstants.MAX_KEY,
            new MetricDisplayValue(data[MeasurementConstants.IND_MAX]));
        
        // Groups get sums, not last value
        if (totalConfigured == 1 ||
            tmpl.getCollectionType() == MeasurementConstants.COLL_TYPE_STATIC) {
            summary.setMetric(MetricDisplayConstants.LAST_KEY,
                new MetricDisplayValue(
                        data[MeasurementConstants.IND_LAST_TIME]));
        } else {
            // Percentage metrics (including Availability) do not need to be summed
            if (MeasurementConstants.UNITS_PERCENTAGE.equals(tmpl.getUnits())) {
                summary.setMetric(MetricDisplayConstants.LAST_KEY,
                    new MetricDisplayValue(data[MeasurementConstants.IND_AVG]));
            } else {
                summary.setMetric(MetricDisplayConstants.LAST_KEY,
                    new MetricDisplayValue(
                        data[MeasurementConstants.IND_AVG] *
                        data[MeasurementConstants.IND_CFG_COUNT]));
            }
        }
                        
        // Number configured
        summary.setAvailUp(
            new Integer((int) data[MeasurementConstants.IND_CFG_COUNT]));
        summary.setAvailUnknown(new Integer(totalConfigured));
    
        return summary;
    }

    protected List getAGMemberIds(AuthzSubject subject,
                                  AppdefEntityID parentAid,
                                  AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException {
        return getAGMemberIds(subject, new AppdefEntityID[] { parentAid }, ctype);
    }

    protected List getAGMemberIds(AuthzSubject subject,
                                  AppdefEntityID[] aids,
                                  AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException 
    {
        List res = new ArrayList();        
        ResourceManagerLocal rman = ResourceManagerEJBImpl.getOne();
        Resource proto = rman.findResourcePrototype(ctype); 

        if (proto == null) {
            log.warn("Unable to find prototype for ctype=[" + ctype + "]");
            return res;
        }
        
        DescendantProtoCritterType descType = new DescendantProtoCritterType();
        CritterTranslator trans             = new CritterTranslator();
        CritterTranslationContext ctx       =
            new CritterTranslationContext(subject);
        
        for (int i = 0; i < aids.length; i++) {
            if (aids[i].isApplication()) {
                AppdefEntityValue rv = new AppdefEntityValue(aids[i], subject);
                Collection services = rv.getAssociatedServices(ctype.getId(),
                                                               PageControl.PAGE_ALL);
                for (Iterator it = services.iterator(); it.hasNext();) {
                    AppdefResourceValue r = (AppdefResourceValue) it.next();
                    res.add(r.getEntityId());
                }
            }
            else {
                Resource r = rman.findResource(aids[i]);
                List critters = new ArrayList(1);
                critters.add(descType.newInstance(r, proto));
                CritterList cList = new CritterList(critters, false);

                List children = trans.translate(ctx, cList).list();
                for (Iterator j=children.iterator(); j.hasNext(); ) {
                    Resource child = (Resource)j.next();                
                    res.add(new AppdefEntityID(child));
                }
            }
        }
        return res;
    }

    protected double[] getAvailability(AuthzSubject subject,
                                       AppdefEntityID[] ids)
        throws AppdefEntityNotFoundException,
               PermissionException {
        // Allow for the maximum window based on collection interval
        return getAvailability(subject, ids, getMidMap(getResources(ids)), null);
    }
    
    private final List getResources(AppdefEntityID[] ids) {
        final List resources = new ArrayList(ids.length);
        final ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
        for (int i = 0; i < ids.length; i++) {
            final Resource r = rMan.findResource(ids[i]);
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
            resources.add(rMan.findResource(ids[i]));
        }
        return resources;
    }
    
    /**
     * @param resources {@link List} of {@link Resource}
     * @return {@link Map} of {@link Integer} to {@link Measurement}.
     * Integer = Resource.getId()
     */
    private final Map getMidMap(Collection resources) {
        final List aeids = new ArrayList();
        for (final Iterator it=resources.iterator(); it.hasNext(); ) {
            final Resource r = (Resource)it.next();
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
            aeids.add(new AppdefEntityID(r));
        }
        final AppdefEntityID[] ids =
            (AppdefEntityID[])aeids.toArray(new AppdefEntityID[0]);
        return getMidMap(ids, null);
    }

    /**
     * @param midMap {@link Map} of {@link Integer} to {@link Measurement}
     * Integer = Resource.getId()
     * @param availCache {@link Map} of {@link Integer} to {@link MetricValue}
     * Integer = Measurement.getId()
     */
    protected double[] getAvailability(final AuthzSubject subject,
                                       final AppdefEntityID[] ids,
                                       final Map midMap,
                                       final Map availCache)
        throws ApplicationNotFoundException,
               AppdefEntityNotFoundException,
               PermissionException {
        final double[] result = new double[ids.length];
        Arrays.fill(result, MeasurementConstants.AVAIL_UNKNOWN);
        final Map data = new HashMap();
        final MeasurementManagerLocal mMan = getMetricManager();
        final ResourceManagerLocal rMan = getResourceManager();
        final AvailabilityManagerLocal aMan = getAvailManager();
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        final Map prefetched = new HashMap();
        if (midMap.size() > 0) {
            final List mids = new ArrayList();
            final List aeids = Arrays.asList(ids);
            final int size = aeids.size();
            for (final Iterator it=aeids.iterator(); it.hasNext(); ) {
                final AppdefEntityID aeid = (AppdefEntityID)it.next();
                if (debug) watch.markTimeBegin("findResource size=" + size);
                final Resource r = rMan.findResource(aeid);
                if (debug) watch.markTimeEnd("findResource size=" + size);
                prefetched.put(aeid, r);
                if (r == null || r.isInAsyncDeleteState()) {
                    continue;
                }
                Measurement meas;
                if (null == midMap
                        || null == (meas = (Measurement)midMap.get(r.getId()))) {
                    if (debug) watch.markTimeBegin("getAvailabilityMeasurement");
                    meas = mMan.getAvailabilityMeasurement(r);
                    if (debug) watch.markTimeEnd("getAvailabilityMeasurement");
                }
                if (meas == null) {
                    continue;
                }
                if (availCache != null) {
                    MetricValue mv = (MetricValue)availCache.get(meas.getId());
                    if (mv != null) {
                        data.put(meas.getId(), mv);
                    } else {
                        mids.add(meas.getId());
                    }
                } else {
                    mids.add(meas.getId());
                }
            }
            if (debug) watch.markTimeBegin("getLastAvail");
            data.putAll(aMan.getLastAvail((Integer[])mids.toArray(new Integer[0])));
            if (debug) watch.markTimeEnd("getLastAvail");
        }
        for (int i = 0; i < ids.length; i++) {
            Resource r = (Resource) prefetched.get(ids[i]);
            if (r == null) {
                r = rMan.findResource(ids[i]);
            }
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
            if (midMap.containsKey(r.getId())) {
                Integer mid = ((Measurement)midMap.get(r.getId())).getId();
                MetricValue mval = null;
                if (null != (mval = (MetricValue)data.get(mid))) {
                    result[i] = mval.getValue();
                }
            } else {
                // cases for abstract resources whose availability are xor'd
                switch (ids[i].getType()) {
                    case AppdefEntityConstants.APPDEF_TYPE_APPLICATION :
                        AppdefEntityValue appVal = new AppdefEntityValue(ids[i], subject);
                        if (debug) watch.markTimeBegin("getFlattenedServiceIds");
                        AppdefEntityID[] services = appVal.getFlattenedServiceIds();
                        if (debug) watch.markTimeEnd("getFlattenedServiceIds");
    
                        if (debug) watch.markTimeBegin("getAggregateAvailability");
                        result[i] = getAggregateAvailability(subject, services, null, availCache);
                        if (debug) watch.markTimeEnd("getAggregateAvailability");
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_GROUP :
                        if (debug) watch.markTimeBegin("getGroupAvailability");
                        result[i] = getGroupAvailability(subject, ids[i].getId(), null, null);
                        if (debug) watch.markTimeEnd("getGroupAvailability");
                        break;
                    default :
                        break;
                }
            }
        }
        if (debug) log.debug(watch);
        return result;
    }

    protected Measurement findAvailabilityMetric(AuthzSubject subject,
                                                 AppdefEntityID id)
    {
        return getMetricManager().getAvailabilityMeasurement(subject, id);
    }

    protected AppdefEntityID[] toAppdefEntityIDArray(List entities) {
        AppdefEntityID[] result = new AppdefEntityID[entities.size()];
        int idx = 0;
        for (Iterator iter = entities.iterator(); iter.hasNext();) {
            Object thisThing = iter.next();
            if (thisThing instanceof AppdefResourceValue) {
                result[idx++] = ((AppdefResourceValue) thisThing).getEntityId();
                continue;
            }
            result[idx++] = (AppdefEntityID) thisThing;
        }
        return result;
    }

    /**
     * @param availCache {@link Map} of {@link Integer} to {@link MetricValue}
     *  Integer => Measurement.getId(), may be null.
     * 
     * Given an array of AppdefEntityID's, disqulifies their aggregate
     * availability (with the disqualifying status) for all of them if any are
     * down or unknown, otherwise the aggregate is deemed available
     * 
     * If there's nothing in the array, then aggregate is not populated. Ergo,
     * the availability shall be disqualified as unknown i.e. the (?)
     * representation
     */
    protected double getAggregateAvailability(AuthzSubject subject,
                                              AppdefEntityID[] ids,
                                              Map measCache,
                                              Map availCache)
        throws AppdefEntityNotFoundException, PermissionException {
        if (ids.length == 0) {
            return MeasurementConstants.AVAIL_UNKNOWN;
        } 
        final StopWatch watch = new StopWatch();
        final boolean debug = log.isDebugEnabled();
        double sum = 0;
        int count = 0;
        int unknownCount = 0;
        final Map midMap = getMidMap(ids, measCache);
        if (debug) watch.markTimeBegin("getAvailability");
        double[] avails = getAvailability(subject, ids, midMap, availCache);
        if (debug) watch.markTimeEnd("getAvailability");
        for (int i = 0; i < avails.length; i++) {
             if (avails[i] == MeasurementConstants.AVAIL_UNKNOWN) {
                 unknownCount++;
             } else {
                 sum += avails[i];
                 count++;
             }
         }
        if (unknownCount == ids.length) {
            // All resources are unknown
            return MeasurementConstants.AVAIL_UNKNOWN;
        }
        if (debug) log.debug(watch);
        return sum / count;
    }

    protected final AppdefEntityID[] getAeids(final Collection resources) {
        final AppdefEntityID[] aeids = new AppdefEntityID[resources.size()];
        int i = 0;
        for (final Iterator it=resources.iterator(); it.hasNext(); i++) {
            final Object o = it.next();
            AppdefEntityID aeid = null;
            if (o instanceof AppdefEntityValue) {
                final AppdefEntityValue rv = (AppdefEntityValue) o;
                aeid = rv.getID();
            } else if (o instanceof AppdefEntityID) {
                aeid = (AppdefEntityID) o;
            } else if (o instanceof AppdefResource) {
                final AppdefResource r = (AppdefResource)o;
                aeid = r.getEntityId();
            } else if (o instanceof Resource) {
                final Resource resource = (Resource) o;
                aeid = new AppdefEntityID(resource);
            } else if (o instanceof ResourceGroup) {
                final ResourceGroup grp = (ResourceGroup) o;
                final Resource resource = grp.getResource();
                aeid = new AppdefEntityID(resource);
            } else {
                final AppdefResourceValue r = (AppdefResourceValue) o;
                aeid = r.getEntityId();
            }
            aeids[i] = aeid;
        }
        return aeids;
        
    }
    
    /**
     * @param measCache {@link Map} of {@link Integer} of Resource.getId() to
     * {@link List} of {@link Measurement}
     * @return {@link Map} of {@link Integer} to {@link Measurement}
     * Integer = Resource.getId()
     */
    protected final Map getMidMap(AppdefEntityID[] ids, Map measCache) {
        final Map rtn = new HashMap(ids.length);
        final ResourceManagerLocal rMan = getResourceManager();
        final MeasurementManagerLocal mMan = getMetricManager();
        final List toGet = new ArrayList();
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        final List aeids = Arrays.asList(ids);
        final int size = aeids.size();
        for (final Iterator it=aeids.iterator(); it.hasNext(); ) {
            final AppdefEntityID id = (AppdefEntityID)it.next();
            if (id == null) {
                continue;
            }
            if (debug) watch.markTimeBegin("findResource size=" + size);
            final Resource res = rMan.findResource(id);
            if (res == null || res.isInAsyncDeleteState()) {
                continue;
            }
            if (debug) watch.markTimeEnd("findResource size=" + size);
            List list;
            if (null != measCache
                   && null != (list = (List)measCache.get(res.getId()))) {
                if (list.size() > 1) {
                    log.warn("resourceId " + res.getId() +
                             " has more than one availability measurement " +
                             " assigned to it");
                } else if (list.size() <= 0) {
                    continue;
                }
                final Measurement m = (Measurement)list.get(0);
                rtn.put(res.getId(), m);
            } else {
                toGet.add(res);
            }
        }
        if (debug) watch.markTimeBegin("getAvailMeasurements");
        final Map measMap = mMan.getAvailMeasurements(toGet);
        if (debug) watch.markTimeEnd("getAvailMeasurements");
        for (final Iterator it=measMap.entrySet().iterator(); it.hasNext(); ) {
            final Map.Entry entry = (Map.Entry)it.next();
            final Integer id = (Integer)entry.getKey();
            final Resource r = rMan.findResourceById(id);
            final List vals = (List)entry.getValue();
            if (vals.size() == 0) {
                continue;
            }
            final Measurement m = (Measurement)vals.get(0);
            if (vals.size() > 1) {
                log.warn("resourceId " + r.getId() +
                         " has more than one availability measurement " +
                         " assigned to it");
            }
            rtn.put(r.getId(), m);
        }
        if (debug) log.debug(watch);
        return rtn;
    }

    /**
     * Given a group, disqualifies their aggregate availability (with the
     * disqualifying status) for all of them if any are down or unknown,
     * otherwise the aggregate is deemed available
     * 
     * If there's nothing in the array, then aggregate is not populated. Ergo,
     * the availability shall be disqualified as unknown i.e. the (?)
     * representation
     * 
     * @param measCache {@link Map} of {@link Resource.getId} to
     *  {@link List} of {@link Measurement}.  May be null.
     * @param availCache {@link Map} of {@link Measurement.getId} to
     *  {@link MetricValue}.  May be null.
     */
    protected double getGroupAvailability(AuthzSubject subject, Integer gid,
                                          Map measCache, Map availCache)
        throws AppdefEntityNotFoundException,
               PermissionException {
        final ResourceGroupManagerLocal resGrpMgr = getResourceGroupManager();
        final ResourceGroup group =
            resGrpMgr.findResourceGroupById(subject, gid);
        if (group == null) {
            return MeasurementConstants.AVAIL_UNKNOWN;
        }
        final Resource resource = group.getResource();
        if (resource == null || resource.isInAsyncDeleteState()) {
            return MeasurementConstants.AVAIL_UNKNOWN;
        }
        if (measCache == null) {
            measCache = getMetricManager()
                .getAvailMeasurements(Collections.singleton(group));
        }
        final List metrics = (List) measCache.get(resource.getId());

        // Allow for the maximum window based on collection interval
        Map midMap = new HashMap(metrics.size());
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
            Measurement m =  (Measurement) it.next();
            try {
                midMap.put(m.getResource().getId(), m);
            } catch (IllegalArgumentException e) {
                // Resource has been deleted, waiting for purging.  Ignore.
            }
        }
        
        AppdefEntityID[] ids = getGroupMemberIDs(subject, gid);
        
        double sum = 0;
        int count = 0;
        double[] avails = getAvailability(subject, ids, midMap, availCache);

        for (int i = 0; i < avails.length; i++) {
             if (avails[i] != MeasurementConstants.AVAIL_UNKNOWN) {
                 sum += avails[i];
                 count++;
             }
         }
        
        final double r = (count == 0) ?
            MeasurementConstants.AVAIL_UNKNOWN : sum / count;
        final BigDecimal b = new BigDecimal(r, new MathContext(10));
        return b.doubleValue();
    }

    protected Map findMetrics(int sessionId, AppdefEntityID entId, long begin,
                              long end, PageControl pc)
        throws SessionTimeoutException, SessionNotFoundException,
            InvalidAppdefTypeException, PermissionException,
            AppdefEntityNotFoundException, AppdefCompatException {
        AppdefEntityID[] entIds = new AppdefEntityID[] { entId };
        return findMetrics(sessionId, entIds, MeasurementConstants.FILTER_NONE,
                           null, begin, end, false);
    }

    protected Map findMetrics(int sessionId, AppdefEntityID[] entIds,
                              long filters, String keyword, long begin,
                              long end, boolean showNoCollect)
        throws SessionTimeoutException, SessionNotFoundException,
            InvalidAppdefTypeException, PermissionException,
            AppdefEntityNotFoundException, AppdefCompatException {
        AuthzSubject subject = manager.getSubject(sessionId);
        
        // Assume all entities are of the same type
        AppdefEntityValue rv = new AppdefEntityValue(entIds[0], subject);
        
        List entArr;
        switch (entIds[0].getType()) {
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                entArr = Arrays.asList(entIds);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                try {
                    entArr = GroupUtil.getCompatGroupMembers(
                        subject, entIds[0], null, PageControl.PAGE_ALL);
                } catch (GroupNotCompatibleException e) {
                    throw new IllegalArgumentException(
                        "Metrics are not available for groups that " +
                        "are not compatible types: " + e.getMessage());
                }                    
                break;
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
                // No metric support for applications
                return new HashMap(0);
            default:
                throw new InvalidAppdefTypeException(
                    "entityID is not valid type, id type: " +
                    entIds[0].getType());
        }
        
        String monitorableType = rv.getMonitorableType();
    
        // Look up the metric summaries of associated servers
        return getResourceMetrics(subject, entArr, monitorableType, filters,
                                  keyword, begin, end, showNoCollect);
    }

    protected Map findMetrics(int sessionId, AppdefEntityID entId, List mtids,
                              long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
            PermissionException, AppdefEntityNotFoundException,
            AppdefCompatException {
        AuthzSubject subject = manager.getSubject(sessionId);
        
        boolean bPlatforms = false, bServers = false, bServices = false;
        
        // Let's get the templates to see what resources to gather
        List templates = getTemplateManager().getTemplates(mtids);
        for (Iterator it = templates.iterator(); it.hasNext(); ) {
            MeasurementTemplate templ = (MeasurementTemplate) it.next();
            int type = templ.getMonitorableType().getAppdefType();
            bPlatforms |= type == AppdefEntityConstants.APPDEF_TYPE_PLATFORM;
            bServers   |= type == AppdefEntityConstants.APPDEF_TYPE_SERVER;
            bServices  |= type == AppdefEntityConstants.APPDEF_TYPE_SERVICE;
        }
    
        AppdefEntityValue rv = new AppdefEntityValue(entId, subject);
        List platforms = null, servers = null, services = null;
        
        // Can't assume that all templates are only for given entity,
        // might be for associated resources, too.
        switch (rv.getID().getType()) {
            // Hierarchical, not actually missing "break" statements
            case AppdefEntityConstants.APPDEF_TYPE_APPLICATION:
            case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
                // Get the platforms
                if (bPlatforms)
                    platforms = rv.getAssociatedPlatforms(PageControl.PAGE_ALL);
            case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                // Get the servers
                if (bServers)
                    servers = rv.getAssociatedServers(PageControl.PAGE_ALL);
            case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                // Get the services
                if (bServices)
                    services = rv.getAssociatedServices(PageControl.PAGE_ALL);
                break;
            case AppdefEntityConstants.APPDEF_TYPE_GROUP:
                // Does not matter what kind of group this is, just use platform
                try {
                    platforms =
                        GroupUtil.getCompatGroupMembers(subject, entId, null,
                                                        PageControl.PAGE_ALL);
                } catch (GroupNotCompatibleException e) {
                    log.debug("Group not compatible");
                }
                break;
            default:
                break;
        }

        // Look up the metric summaries of all associated resources
        Map results = new HashMap();
        if (bPlatforms)
            results.putAll(getResourceMetrics(subject, platforms, mtids,
                                              begin, end, Boolean.TRUE));
        if (bServers)
            results.putAll(getResourceMetrics(subject, servers, mtids,
                                              begin, end, Boolean.TRUE));
        if (bServices)
            results.putAll(getResourceMetrics(subject, services, mtids,
                                              begin, end, Boolean.TRUE));
        return results;
    }

    protected List findAllMetrics(int sessionId, AppdefEntityID aeid,
                                  AppdefEntityTypeID ctype,
                                  long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException,
               AppdefCompatException, InvalidAppdefTypeException {
        ArrayList result = new ArrayList();
        AppdefEntityID[] entIds = new AppdefEntityID[] { aeid };
        
        Map metrics = findAGMetricsByType(sessionId, entIds, ctype,
                                          MeasurementConstants.FILTER_NONE,
                                          null, begin, end, false);
        for (Iterator it = metrics.values().iterator(); it.hasNext(); ) {
            Collection metricColl = (Collection) it.next();
            
            for (Iterator it2 = metricColl.iterator(); it2.hasNext(); ) {
                MetricDisplaySummary summary =
                    (MetricDisplaySummary) it2.next();
                ProblemMetricSummary pms =
                    new ProblemMetricSummary(summary);
                pms.setMultipleAppdefKey(ctype.getAppdefKey());
                result.add(pms);
            }
        }
        return result;
    }

    protected List findAllMetrics(int sessionId, AppdefEntityID[] aeids,
                                  long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException,
               AppdefCompatException, InvalidAppdefTypeException {
        ArrayList result = new ArrayList();
        Map metrics = findMetrics(sessionId, aeids,
                                  MeasurementConstants.FILTER_NONE, null,
                                  begin, end, false);
        for (Iterator it = metrics.values().iterator(); it.hasNext(); ) {
            Collection metricColl = (Collection) it.next();
            
            for (Iterator it2 = metricColl.iterator(); it2.hasNext(); ) {
                MetricDisplaySummary summary =
                    (MetricDisplaySummary) it2.next();
                ProblemMetricSummary pms =
                    new ProblemMetricSummary(summary);
                result.add(pms);
            }
        }
        return result;
    }

    protected List findAllMetrics(int sessionId, AppdefEntityID aeid,
                                  long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
               AppdefEntityNotFoundException, PermissionException,
               AppdefCompatException, InvalidAppdefTypeException {
        ArrayList result = new ArrayList();
        Map metrics = findMetrics(sessionId, aeid, begin, end,
                                  PageControl.PAGE_ALL);
        for (Iterator it = metrics.values().iterator(); it.hasNext(); ) {
            Collection metricColl = (Collection) it.next();
            
            for (Iterator it2 = metricColl.iterator(); it2.hasNext(); ) {
                MetricDisplaySummary summary =
                    (MetricDisplaySummary) it2.next();
                ProblemMetricSummary pms =
                    new ProblemMetricSummary(summary);
                pms.setSingleAppdefKey(aeid.getAppdefKey());
                result.add(pms);
            }
        }
        return result;
    }

    protected List getPlatformAG(AuthzSubject subject, AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException {
        if(!ctype.isPlatform()) {
            throw new IllegalArgumentException(ctype.getType() + 
                    " is not a platform type");
        }
        Integer[] platIds = 
            getPlatformManager().getPlatformIds(subject, ctype.getId());
        List entIds = new ArrayList(platIds.length);
        for(int i = 0; i < platIds.length; i++) {
            entIds.add(AppdefEntityID.newPlatformID(platIds[i]));
        }
        return entIds;
    }

    protected Map findAGPlatformMetricsByType(int sessionId, 
                                              AppdefEntityTypeID platTypeId,
                                              long begin, long end,
                                              boolean showAll)
        throws SessionTimeoutException, SessionNotFoundException,
               InvalidAppdefTypeException, AppdefEntityNotFoundException,
               PermissionException, AppdefCompatException {
        AuthzSubject subject = manager.getSubject(sessionId);
    
        // Get the member IDs
        List platforms = getPlatformAG(subject, platTypeId);
        
        // Get resource type name
        PlatformType platType =
            getPlatformManager().findPlatformType(platTypeId.getId());
    
        // Look up the metric summaries of platforms
        return getResourceMetrics(subject, platforms, platType.getName(),
                                  MeasurementConstants.FILTER_NONE, null,
                                  begin, end, showAll);
    }
    
    protected Map findAGMetricsByType(int sessionId, AppdefEntityID[] entIds,
                                      AppdefEntityTypeID typeId, long filters,
                                      String keyword, long begin, long end,
                                      boolean showAll)
        throws SessionTimeoutException, SessionNotFoundException,
               InvalidAppdefTypeException, PermissionException,
               AppdefEntityNotFoundException, AppdefCompatException {
        AuthzSubject subject = manager.getSubject(sessionId);
        
        List group = new ArrayList();
        for (int i = 0; i < entIds.length; i++) {
            AppdefEntityValue rv = new AppdefEntityValue(entIds[i], subject);
            
            switch (typeId.getType()) {
                case AppdefEntityConstants.APPDEF_TYPE_SERVER:
                    // Get the associated servers
                    group.addAll(rv.getAssociatedServers(typeId.getId(),
                                                         PageControl.PAGE_ALL));
                    break;
                case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
                    // Get the associated services
                    group.addAll(rv.getAssociatedServices(typeId.getId(),
                                                          PageControl.PAGE_ALL));
                    break;
                default:
                    break;
            }
        }
    
        // Need to get the templates for this type, using the first resource
        AppdefResourceValue resource = (AppdefResourceValue) group.get(0);
        String resourceType = resource.getAppdefResourceTypeValue().getName();
    
        // Look up the metric summaries of associated servers
        return getResourceMetrics(subject, group, resourceType, filters,
                                  keyword, begin, end, showAll);
    }

    protected AppdefEntityID[] getGroupMemberIDs(AuthzSubject subject,
                                                 Integer gid)
        throws AppdefEntityNotFoundException, PermissionException {
        final List members = getResourceIds(subject,
                                            AppdefEntityID.newGroupID(gid),
                                            null);
        return (AppdefEntityID[])
            members.toArray(new AppdefEntityID[members.size()]);
    }

    /**
     * Get a List of AppdefEntityIDs for the given resource.
     * @param subject The user to use for searches.
     * @param aeid The entity in question.
     * @param ctype The entity type in question.
     * @return A List of AppdefEntityIDs for the given resource.
     */
    protected List getResourceIds(AuthzSubject subject, AppdefEntityID aeid,
                                  AppdefEntityTypeID ctype)
        throws AppdefEntityNotFoundException, PermissionException {
        final List resources;
        if (ctype == null) {
            if (aeid.isGroup()) {
                final ResourceGroupManagerLocal resGrpMgr =
                    getResourceGroupManager();
                final ResourceGroup group =
                    resGrpMgr.findResourceGroupById(subject, aeid.getId());
        
                final Collection members = resGrpMgr.getMembers(group);
                resources = new ArrayList(members.size());
                for (Iterator it = members.iterator(); it.hasNext(); ) {
                    Resource r = (Resource) it.next();
                    if (r == null || r.isInAsyncDeleteState()) {
                        continue;
                    }
                    resources.add(new AppdefEntityID(r));
                }
            }
            else {
                // Just one
                resources = Collections.singletonList(aeid);
            }
        }
        else {
            // Autogroup
            resources = getAGMemberIds(subject, aeid, ctype);
        }
        return resources;
    }
}
