/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefCompatException;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.appdef.shared.PlatformTypeValue;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
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
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.pager.PageControl;

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
        Integer[] templateIds;

        // Create Map of all resources
        HashMap resmap = new HashMap(MeasurementConstants.VALID_CATEGORIES.length);

        if (tmpls.size() == 0 || resources.size() == 0)
            return resmap;
            
        if (tmpls.get(0) instanceof MeasurementTemplate) {
            templates = tmpls;
            templateIds = new Integer[templates.size()];
            for (int i = 0; i < templates.size(); i++ ) {
                MeasurementTemplate t = (MeasurementTemplate)templates.get(i);
                templateIds[i] = t.getId();
            }
        }
        else {
            // If templates are just ID's, we have to look them up
            templateIds = (Integer[])tmpls.toArray(new Integer[tmpls.size()]);
            try {
                templates = getTemplateManager().getTemplates(templateIds,
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
        Map datamap = getDataMan().getAggregateData(templates, eids, begin, end);

        // Get the intervals, keyed by template ID's as well
        Map intervals = showNoCollect == null ? new HashMap() :
            getMetricManager().findMetricIntervals(subject, aeids, templateIds);

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
        }
        else {
            // Availability does not need to be summed
            if (tmpl.isAvailability()) {
                summary.setMetric(MetricDisplayConstants.LAST_KEY,
                    new MetricDisplayValue(data[MeasurementConstants.IND_AVG]));
            }
            else {
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
        CritterTranslationContext ctx       = new CritterTranslationContext();
        
        for (int i = 0; i < aids.length; i++) {
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
        return res;
    }
    
    protected double[] getAvailability(AuthzSubject subject,
                                       AppdefEntityID[] ids)
        throws AppdefEntityNotFoundException, PermissionException {
        long current = System.currentTimeMillis();
    
        AgentManagerLocal agentMan = AgentManagerEJBImpl.getOne();
        
        long liveMillis = MeasurementConstants.ACCEPTABLE_PLATFORM_LIVE_MILLIS;
        
        // Allow for the maximum window based on collection interval
        Map midMap = new HashMap(ids.length);        
        for (int i = 0; i < ids.length; i++) {
            Measurement m =  getMetricManager()
                .getAvailabilityMeasurement(subject, ids[i]);
    
            if (m != null) {
                liveMillis = Math.max(liveMillis, 3 * m.getInterval());
                midMap.put(ids[i], m.getId());
            }
        }
        
        long acceptable =
            liveMillis == 0 ? MeasurementConstants.TIMERANGE_UNLIMITED:
                              current - liveMillis; 
    
        double[] result = new double[ids.length];
        Arrays.fill(result, MeasurementConstants.AVAIL_UNKNOWN);
        
        Map data = new HashMap(0);
        if (midMap.size() > 0) {
            Integer[] mids = (Integer[]) midMap.values().
                toArray(new Integer[midMap.values().size()]);
            data = getAvailManager().getLastAvail(mids, acceptable);
        }
    
        // Organize by agent
        HashMap toGetLive = new HashMap();
        
        for (int i = 0; i < ids.length; i++) {
            if (midMap.containsKey(ids[i])) {
                Integer mid = (Integer) midMap.get(ids[i]);
                if (data.containsKey(mid)) {
                    MetricValue mval = (MetricValue) data.get(mid);
                    result[i] = mval.getValue();
                } else {
                    // First figure out if the agent of this appdef entity
                    // already has a list
                    try {
                        Agent agent = agentMan.getAgent(ids[i]);
                        
                        List toGetLiveList;
                        if (null == (toGetLiveList = (List)toGetLive.get(agent))) {
                            toGetLiveList = new ArrayList();
                            toGetLive.put(agent, toGetLiveList);
                        }
                        // Now add to list
                        toGetLiveList.add(new Integer(i));
                    } catch (AgentNotFoundException e) {
                        result[i] = AVAIL_DOWN;
                    }
                }
            } else {
                // cases for abstract resources whose availability are xor'd
                switch (ids[i].getType()) {
                    case AppdefEntityConstants.APPDEF_TYPE_APPLICATION :
                        AppdefEntityValue appVal =
                            new AppdefEntityValue(ids[i], subject);
                        AppdefEntityID[] services =
                            appVal.getFlattenedServiceIds();
    
                        result[i] = getAggregateAvailability(subject, services);
                        break;
                    case AppdefEntityConstants.APPDEF_TYPE_GROUP :
                        // determine the aggregate from the AppdefEntityID's
                        List grpMembers =
                            GroupUtil.getGroupMembers(subject, ids[i], null);
                        result[i] = getAggregateAvailability(subject, 
                            toAppdefEntityIDArray(grpMembers));
                        break;
                    default :
                        break;
                }
            }
        }
    
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
     * Given an array of AppdefEntityID's, disqulifies their aggregate
     * availability (with the disqualifying status) for all of them if any are
     * down or unknown, otherwise the aggregate is deemed available
     * 
     * If there's nothing in the array, then aggregate is not populated. Ergo,
     * the availability shall be disqualified as unknown i.e. the (?)
     * representation
     */
    protected double getAggregateAvailability(AuthzSubject subject,
                                              AppdefEntityID[] ids)
        throws AppdefEntityNotFoundException, PermissionException {
        if (ids.length == 0)
            return MeasurementConstants.AVAIL_UNKNOWN;
        
        // Break them up and do 5 at a time
        int length = 5;
        double sum = 0;
        int count = 0;
        int unknownCount = 0;
        for (int ind = 0; ind < ids.length; ind += length) {
            
            if (ids.length - ind < length)
                length = ids.length - ind;
    
            AppdefEntityID[] subids = new AppdefEntityID[length];
            
            for (int i = ind; i < ind + length; i++) {
                subids[i - ind] = ids[i];
            }
            
            double[] avails = getAvailability(subject, subids);
            
            for (int i = 0; i < avails.length; i++) {
                 if (avails[i] == MeasurementConstants.AVAIL_UNKNOWN) {
                     unknownCount++;
                 }
                 else {
                     sum += avails[i];
                     count++;
                 }
             }
        }
        
        if (unknownCount == ids.length)
            // All resources are unknown
            return MeasurementConstants.AVAIL_UNKNOWN;
        
        return sum / count;
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
        AuthzSubject subject = manager.getSubjectPojo(sessionId);
        
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
        AuthzSubject subject = manager.getSubjectPojo(sessionId);
        
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
        if(ctype.getType() != AppdefEntityConstants.APPDEF_TYPE_PLATFORM) {
            throw new IllegalArgumentException(ctype.getType() + 
                    " is not a platform type");
        }
        Integer[] platIds = 
            getPlatformManager().getPlatformIds(subject, ctype.getId());
        List entIds = new ArrayList(platIds.length);
        for(int i = 0; i < platIds.length; i++) {
            entIds.add(
                new AppdefEntityID(AppdefEntityConstants.APPDEF_TYPE_PLATFORM,
                                   platIds[i]));
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
        AuthzSubject subject = manager.getSubjectPojo(sessionId);
    
        // Get the member IDs
        List platforms = getPlatformAG(subject, platTypeId);
        
        // Get resource type name
        PlatformTypeValue platType =
            getPlatformManager().findPlatformTypeValueById(platTypeId.getId());
    
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
        AuthzSubject subject = manager.getSubjectPojo(sessionId);
        
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
}
