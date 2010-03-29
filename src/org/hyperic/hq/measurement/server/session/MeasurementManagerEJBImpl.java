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

package org.hyperic.hq.measurement.server.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.AppService;
import org.hyperic.hq.appdef.server.session.AgentManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.AppdefResource;
import org.hyperic.hq.appdef.server.session.Application;
import org.hyperic.hq.appdef.server.session.ApplicationManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.ResourceUpdatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefResourceValue;
import org.hyperic.hq.appdef.shared.ApplicationManagerLocal;
import org.hyperic.hq.appdef.shared.ApplicationNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.authz.server.session.ResourceType;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.authz.shared.ResourceManagerLocal;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.agent.client.AgentMonitor;
import org.hyperic.hq.measurement.ext.MeasurementEvent;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.MeasurementManagerUtil;
import org.hyperic.hq.measurement.shared.TrackerManagerLocal;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.timer.StopWatch;

/**
 * The MeasurementManager provides APIs to deal with Measurement objects.
 *
 * @ejb:bean name="MeasurementManager"
 *      jndi-name="ejb/measurement/MeasurementManager"
 *      local-jndi-name="LocalMeasurementManager"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:transaction type="Required"
 */
public class MeasurementManagerEJBImpl extends SessionEJB
    implements SessionBean 
{
    private final Log log = LogFactory.getLog(MeasurementManagerEJBImpl.class);

    /**
     * Translate a template string into a DSN
     */
    private String translate(String tmpl, ConfigResponse config){
        try {
            return getMPM().translate(tmpl, config);
        } catch (org.hyperic.hq.product.PluginNotFoundException e) {
            return tmpl;
        }
    }
    
    /**
     * Enqueue a {@link MeasurementScheduleZevent} on the zevent queue 
     * corresponding to the change in schedule for the measurement.
     * 
     * @param dm The Measurement
     * @param interval The new collection interval.
     */
    private void enqueueZeventForMeasScheduleChange(Measurement dm,
                                                    long interval) {
        
        MeasurementScheduleZevent event =
            new MeasurementScheduleZevent(dm.getId().intValue(), interval);
        ZeventManager.getInstance().enqueueEventAfterCommit(event);
    }
    
    /**
     * Enqueue a {@link MeasurementScheduleZevent} on the zevent queue 
     * corresponding to collection disabled for the measurements.
     * 
     * @param mids The measurement ids.
     */
    private void enqueueZeventsForMeasScheduleCollectionDisabled(Integer[] mids) {
        List events = new ArrayList(mids.length);
        
        for (int i = 0; i < mids.length; i++) {
            Integer mid = mids[i];
            
            if (mid != null) {
                events.add(new MeasurementScheduleZevent(mid.intValue(), 0));                
            }
        }
        
        ZeventManager.getInstance().enqueueEventsAfterCommit(events);
    }

    private Measurement createMeasurement(Resource instanceId,
                                          MeasurementTemplate mt,
                                          ConfigResponse props,
                                          long interval)
        throws MeasurementCreateException
    {
        String dsn = translate(mt.getTemplate(), props);
        return getMeasurementDAO().create(instanceId, mt, dsn, interval);
    }

    /**
     * Remove Measurements that have been deleted from the DataCache
     * @param mids
     */
    private void removeMeasurementsFromCache(Integer[] mids) {
        MetricDataCache cache = MetricDataCache.getInstance();
        for (int i = 0; i < mids.length; i++) {
            cache.remove(mids[i]);
        }
    }

    /**
     * Create Measurement objects based their templates
     *
     * @param templates   List of Integer template IDs to add
     * @param id          instance ID (appdef resource) the templates are for
     * @param intervals   Millisecond interval that the measurement is polled
     * @param props       Configuration data for the instance
     *
     * @return a List of the associated Measurement objects
     * @ejb:interface-method
     */
    public List createMeasurements(AppdefEntityID id, Integer[] templates,
                                   long[] intervals, ConfigResponse props)
        throws MeasurementCreateException, TemplateNotFoundException
    {
        Resource resource = getResource(id);
        if (resource == null || resource.isInAsyncDeleteState()) {
            return Collections.EMPTY_LIST;
        }
        ArrayList dmList   = new ArrayList();

        if(intervals.length != templates.length){
            throw new IllegalArgumentException(
                "The templates and intervals lists must be the same size");
        }

        MeasurementTemplateDAO tDao = getMeasurementTemplateDAO();
        MeasurementDAO dao = getMeasurementDAO();
        List metrics = dao.findByTemplatesForInstance(templates, resource);

        // Put the metrics in a map for lookup
        Map lookup = new HashMap(metrics.size());
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
            Measurement m = (Measurement) it.next();
            lookup.put(m.getTemplate().getId(), m);
        }

        for (int i = 0; i < templates.length; i++) {
            MeasurementTemplate t = tDao.get(templates[i]);
            if (t == null) {
                continue;
            }
            Measurement m = (Measurement) lookup.get(templates[i]);

            if (m == null) {
                // No measurement, create it
                m = createMeasurement(resource, t, props, intervals[i]);
            } else {
                m.setEnabled(intervals[i] != 0);
                m.setInterval(intervals[i]);
                String dsn = translate(m.getTemplate().getTemplate(), props);
                m.setDsn(dsn);
                enqueueZeventForMeasScheduleChange(m, intervals[i]);
            }
            dmList.add(m);
        }

        return dmList;
    }

    /**
     * Create Measurements and enqueue for scheduling after commit
     * @return {@link List} of {@link Measurement}s
     * @ejb:interface-method
     */
    public List createMeasurements(AuthzSubject subject, AppdefEntityID id,
                                   Integer[] templates, long[] intervals,
                                   ConfigResponse props)
        throws PermissionException, MeasurementCreateException,
               TemplateNotFoundException
    {
        // Authz check
        super.checkModifyPermission(subject.getId(), id);        

        List dmList = createMeasurements(id, templates, intervals, props);
        return dmList;
    }

    /**
     * Create Measurement objects based their templates and default intervals
     *
     * @param templates   List of Integer template IDs to add
     * @param id          instance ID (appdef resource) the templates are for
     * @param props       Configuration data for the instance
     *
     * @return {@link List} of {@link Measurement}s
     * @ejb:interface-method
     */
    public List createMeasurements(AuthzSubject subject, AppdefEntityID id,
                                   Integer[] templates, ConfigResponse props)
        throws PermissionException, MeasurementCreateException,
               TemplateNotFoundException {
        long[] intervals = new long[templates.length];
        for (int i = 0; i < templates.length; i++) {
            MeasurementTemplate tmpl =
                getMeasurementTemplateDAO().findById(templates[i]);
            intervals[i] = tmpl.getDefaultInterval();
        }
        
        return createMeasurements(subject, id, templates, intervals, props);
    }
    
    /**
     * @ejb:interface-method
     */
    public Measurement findMeasurementById(Integer mid) {
        return getMeasurementDAO().findById(mid);
    }

    /**
     * Create Measurement objects for an appdef entity based on default
     * templates.  This method will only create them if there currently no
     * metrics enabled for the appdef entity.
     *
     * @param subject     Spider subject
     * @param id          appdef entity ID of the resource
     * @param mtype       The string name of the plugin type
     * @param props       Configuration data for the instance
     *
     * @return {@link List} of {@link Measurement}s
     */
    private List createDefaultMeasurements(AuthzSubject subject,
                                           AppdefEntityID id,
                                           String mtype,
                                           ConfigResponse props)
        throws TemplateNotFoundException, PermissionException,
               MeasurementCreateException {
        // We're going to make sure there aren't metrics already
        List dms = findMeasurements(subject, id, null, PageControl.PAGE_ALL);

        // Find the templates
        Collection mts =
            getMeasurementTemplateDAO().findTemplatesByMonitorableType(mtype);

        if (mts.size() == 0 || (dms.size() != 0 && dms.size() == mts.size())) {
            return dms;
        }

        Integer[] tids = new Integer[mts.size()];
        long[] intervals = new long[mts.size()];

        Iterator it = mts.iterator();
        for (int i = 0; it.hasNext(); i++) {
            MeasurementTemplate tmpl = (MeasurementTemplate)it.next();
            tids[i] = tmpl.getId();

            if (tmpl.isDefaultOn())
                intervals[i] = tmpl.getDefaultInterval();
            else
                intervals[i] = 0;
        }

        return createMeasurements(subject, id, tids, intervals, props);
    }

    /**
     * Update the Measurements of a resource
     * 
     */
    private void updateMeasurements(AuthzSubject subject, AppdefEntityID id,
                                    ConfigResponse props)
        throws PermissionException, MeasurementCreateException
    {
        try {
            List all = getMeasurementDAO().findByResource(getResource(id));
            List mcol = new ArrayList();
            for (Iterator it = all.iterator(); it.hasNext();) {
                // Translate all dsns
                Measurement dm = (Measurement)it.next();
                dm.setDsn(translate(dm.getTemplate().getTemplate(), props));
                
                // Now see which Measurements need to be rescheduled
                if (dm.isEnabled())
                    mcol.add(dm);
            }

            Integer[] templates = new Integer[mcol.size()];
            long[] intervals = new long[mcol.size()];
            int idx = 0;
            for (Iterator it = mcol.iterator(); it.hasNext(); idx++) {
                Measurement dm = (Measurement)it.next();
                templates[idx] = dm.getTemplate().getId();
                intervals[idx] = dm.getInterval();
            }
            createMeasurements(subject, id, templates, intervals, props);

        } catch (TemplateNotFoundException e) {
            // Would not happen since we're creating measurements with the
            // template that we just looked up
            log.error(e);
        }
    }

    /**
     * Remove all measurements no longer associated with a resource.
     *
     * @ejb:interface-method
     * @return The number of Measurement objects removed.
     */
    public int removeOrphanedMeasurements() {
        final int MAX_MIDS = 200;
        
        StopWatch watch = new StopWatch();
        MetricDeleteCallback cb = 
            MeasurementStartupListener.getMetricDeleteCallbackObj();
        MeasurementDAO dao = getMeasurementDAO();
        List mids = dao.findOrphanedMeasurements();
        
        // Shrink the list down to MAX_MIDS so that we spread out the work over
        // successive data purges
        if (mids.size() > MAX_MIDS) {
            mids = mids.subList(0, MAX_MIDS);
        }
        
        if (mids.size() > 0) {
            cb.beforeMetricsDelete(mids);
            dao.deleteByIds(mids);
        }

        if (log.isDebugEnabled()) {
            log.debug("MeasurementManager.removeOrphanedMeasurements() "+
            		  watch);
        }
        return mids.size();
    }

    /**
     * Look up a Measurement for a Resource and Measurement alias
     * @return a The Measurement for the Resource of the given alias.
     * @ejb:interface-method
     */
    public Measurement getMeasurement(AuthzSubject s, Resource r, String alias)
        throws MeasurementNotFoundException
    {
        Measurement m = getMeasurementDAO().findByAliasAndID(alias, r);
        if (m == null) {
            throw new MeasurementNotFoundException(alias + " for " + r.getName()
                                                   + " not found");
        }
        return m;
    }

    /**
     * Get a Measurement by Id.
     * @ejb:interface-method
     */
    public Measurement getMeasurement(Integer mid) {
        return getMeasurementDAO().get(mid);
    }

    /**
     * Get the live measurement values for a given resource.
     * @param id The id of the resource
     * @ejb:interface-method
     */
    public void getLiveMeasurementValues(AuthzSubject subject,
                                         AppdefEntityID id)
        throws PermissionException, LiveMeasurementException,
               MeasurementNotFoundException
    {
        List mcol = 
            getMeasurementDAO().findEnabledByResource(getResource(id));
        String[] dsns = new String[mcol.size()];
        Integer availMeasurement = null; // For insert of AVAIL down
        Iterator it = mcol.iterator();

        for (int i = 0; it.hasNext(); i++) {
            Measurement dm = (Measurement)it.next();
            dsns[i] = dm.getDsn();
            
            MeasurementTemplate template = dm.getTemplate();

            if (template.getAlias().equals(Metric.ATTR_AVAIL)) {
                availMeasurement = dm.getId();
            }
        }

        log.info("Getting live measurements for " + dsns.length +
                 " measurements");
        try {
            getLiveMeasurementValues(id, dsns);
        } catch (LiveMeasurementException e) {            
            log.info("Resource " + id + " reports it is unavailable, setting " +
                     "measurement ID " + availMeasurement + " to DOWN: "+ e);

            // Only print the full stack trace in debug mode
            if (log.isDebugEnabled()) {
                log.error("Exception details: ", e);
            }

            if (availMeasurement != null) {
                MetricValue val = new MetricValue(MeasurementConstants.AVAIL_DOWN);
                AvailabilityManagerEJBImpl.getOne().addData(availMeasurement,
                                                            val);
            }
        }
    }

    /**
     * Count of metrics enabled for a particular entity
     *
     * @return a The number of metrics enabled for the given entity
     * @ejb:interface-method
     */
    public int getEnabledMetricsCount(AuthzSubject subject, AppdefEntityID id) {
        final Resource res = getResource(id);
        if (res == null || res.isInAsyncDeleteState()) {
            return 0;
        }
        final List mcol = getMeasurementDAO().findEnabledByResource(res);
        return mcol.size();
    }

    /**
     * @param subject {@link AuthzSubject}
     * @param resIdsToTemplIds {@link Map} of {@link Integer} of resourceIds to
     * {@link List} of templateIds
     * @return {@link Map} of {@link Resource} to {@link List} of
     * {@link Measurement}s
     * @throws PermissionException
     * @ejb:interface-method
     */
    public Map findMeasurements(AuthzSubject subject, Map resIdsToTemplIds)
        throws PermissionException
    {
        Map rtn = new HashMap();
        MeasurementDAO dao = getMeasurementDAO();
        ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
        ResourceGroupManagerLocal gMan = ResourceGroupManagerEJBImpl.getOne();
        for (Iterator i=resIdsToTemplIds.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            Integer resId = (Integer)entry.getKey();
            List templs = (List)entry.getValue();
            Integer[] tids = (Integer[])templs.toArray(new Integer[0]);
            Resource resource = rMan.findResourceById(resId);
            // checkModifyPermission(subject.getId(), appId);
            Integer resTypeId = resource.getResourceType().getId();
            if (resTypeId.equals(AuthzConstants.authzGroup)) {
                ResourceGroup grp = gMan.findResourceGroupById(
                    subject, resource.getInstanceId());
                Collection mems = gMan.getMembers(grp);
                for (Iterator it=mems.iterator(); it.hasNext(); ) {
                    Resource res = (Resource)it.next();
                    rtn.put(
                        res, dao.findByTemplatesForInstance(tids, res));
                }
            } else {
                rtn.put(
                    resource, dao.findByTemplatesForInstance(tids, resource));
            }
        }
        return rtn;
    }

    /**
     * Find the Measurement corresponding to the given MeasurementTemplate id
     * and instance id.
     *
     * @param tid The MeasurementTemplate id
     * @param aeid The entity id.
     * @return a Measurement value
     * @ejb:interface-method
     */
    public Measurement findMeasurement(AuthzSubject subject,
                                       Integer tid, AppdefEntityID aeid)
        throws MeasurementNotFoundException {
        List metrics = getMeasurementDAO()
            .findByTemplatesForInstance(new Integer[] {tid}, getResource(aeid));

        if (metrics.size() == 0) {
            throw new MeasurementNotFoundException("No measurement found " +
                                                   "for " + aeid + " with " +
                                                   "template " + tid);
        }
        return (Measurement) metrics.get(0);
    }

    /**
     * Look up a Measurement, allowing for the query to return a stale copy of
     * the Measurement (for efficiency reasons).
     *
     * @param subject The subject.
     * @param tid The template Id.
     * @param iid The instance Id.
     * @param allowStale <code>true</code> to allow stale copies of an alert 
     *                   definition in the query results; <code>false</code> to 
     *                   never allow stale copies, potentially always forcing a 
     *                   sync with the database.
     * @return The Measurement
     * @ejb:interface-method
     */
    public Measurement findMeasurement(AuthzSubject subject,
                                       Integer tid,
                                       Integer iid,
                                       boolean allowStale)
        throws MeasurementNotFoundException {
        
        Measurement dm = getMeasurementDAO()
            .findByTemplateForInstance(tid, iid, allowStale);
            
        if (dm == null) {
            throw new MeasurementNotFoundException("No measurement found " +
                                                   "for " + iid + " with " +
                                                   "template " + tid);
        }
        
        return dm;
    }
        
    /**
     * Look up a list of  Measurements for a template and instances
     *
     * @return a list of Measurement's
     * @ejb:interface-method
     */
    public List findMeasurements(AuthzSubject subject, Integer tid,
                                 AppdefEntityID[] aeids) {
        MeasurementDAO dao = getMeasurementDAO();
        ArrayList results = new ArrayList();
        for (int i = 0; i < aeids.length; i++) {
            results.addAll(dao.findByTemplatesForInstance(new Integer[] { tid },
                                                          getResource(aeids[i])));
        }
        return results;
    }

    /**
     * Look up a list of Measurements for a template and instances
     *
     * @return An array of Measurement ids.
     * @ejb:interface-method
     */
    public Integer[] findMeasurementIds(AuthzSubject subject, Integer tid,
                                        Integer[] ids) {
        List results =
            getMeasurementDAO().findIdsByTemplateForInstances(tid, ids);
        return (Integer[]) results.toArray(new Integer[results.size()]);
    }
    
    /**
     * Look up a list of Measurements for a category
     * XXX: Why is this method called findMeasurements() but only returns
     * enabled measurements if cat == null??
     *
     * @return a List of Measurement objects.
     * @ejb:interface-method
     */
    public List findMeasurements(AuthzSubject subject, AppdefEntityID id,
                                 String cat, PageControl pc) {
        List meas;
            
        // See if category is valid
        if (cat == null || Arrays.binarySearch(
            MeasurementConstants.VALID_CATEGORIES, cat) < 0) {
            meas = getMeasurementDAO().findEnabledByResource(getResource(id));
        } else {
            meas = getMeasurementDAO().findByResourceForCategory(getResource(id),
                                                                 cat);
        }
    
        return meas;
    }

    /**
     * @param aeids {@link List} of {@link AppdefEntityID}s
     * @return {@link Map} of {@link Integer} representing resourceId to
     * {@link List} of {@link Measurement}s
     * @ejb:interface-method
     */
    public Map findEnabledMeasurements(Collection aeids) {
        final List resources = new ArrayList(aeids.size());
        final ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
        for (final Iterator it=aeids.iterator(); it.hasNext(); ) {
            AppdefEntityID aeid = (AppdefEntityID) it.next();
            resources.add(rMan.findResource(aeid));
        }
        return getMeasurementDAO().findEnabledByResources(resources);
    }

    /**
     * Look up a list of enabled Measurements for a category
     *
     * @return a list of {@link Measurement}
     * @ejb:interface-method
     */
    public List findEnabledMeasurements(AuthzSubject subject, AppdefEntityID id,
                                        String cat) {
        List mcol;
            
        // See if category is valid
        if (cat == null || Arrays.binarySearch(
            MeasurementConstants.VALID_CATEGORIES, cat) < 0) {
            mcol = getMeasurementDAO()
                .findEnabledByResource(getResource(id));
        } else {
            mcol = getMeasurementDAO().
                findByResourceForCategory(getResource(id), cat);
        }
        return mcol;
    }

    /**
     * Look up a List of designated Measurements for an entity
     *
     * @return A List of Measurements
     * @ejb:interface-method
     */
    public List findDesignatedMeasurements(AppdefEntityID id) {
        return getMeasurementDAO()
            .findDesignatedByResource(getResource(id));
    }

    /**
     * Look up a list of designated Measurements for an entity for a category
     *
     * @return A List of Measurements
     * @ejb:interface-method
     */
    public List findDesignatedMeasurements(AuthzSubject subject,
                                           AppdefEntityID id, String cat) {
        return getMeasurementDAO()
            .findDesignatedByResourceForCategory(getResource(id), cat);
    }

    /**
     * Look up a list of designated Measurements for an group for a category
     *
     * @return A List of Measurements
     * @ejb:interface-method
     */
    public List findDesignatedMeasurements(AuthzSubject subject,
                                           ResourceGroup g, String cat) {
        return getMeasurementDAO().findDesignatedByCategoryForGroup(g, cat);
    }

    /**
     * Get an Availabilty Measurement by AppdefEntityId
     * @deprecated Use getAvailabilityMeasurement(Resource) instead.
     *
     * @ejb:interface-method
     */
    public Measurement getAvailabilityMeasurement(AuthzSubject subject,
                                                  AppdefEntityID id)
    {
        return getAvailabilityMeasurement(getResource(id));
    }

    /**
     * Get an Availability Measurement by Resource.  May return null.
     * @ejb:interface-method
     */
    public Measurement getAvailabilityMeasurement(Resource r) {
        return getMeasurementDAO().findAvailMeasurement(r);
    }
    
    /**
     * Look up a list of Measurement objects by category
     *
     * @ejb:interface-method
     */
    public List findMeasurementsByCategory(String cat)
    {
        return getMeasurementDAO().findByCategory(cat);
    }

    /**
     * Look up a Map of Measurements for a Category
     *
     * XXX: This method needs to be re-thought.  It only returns a single
     *      designated metric per category even though HQ supports multiple
     *      designates per category.
     *
     *  @return A List of designated Measurements keyed by AppdefEntityID
     *
     * @ejb:interface-method
     */
    public Map findDesignatedMeasurements(AuthzSubject subject,
                                          AppdefEntityID[] ids, String cat)
        throws MeasurementNotFoundException {
        if (ids.length == 0) {
            return Collections.EMPTY_MAP;
        }
        Map midMap = new HashMap(ids.length);
        ResourceManagerLocal rMan = ResourceManagerEJBImpl.getOne();
        ArrayList resources = new ArrayList(ids.length);
        for (int i=0; i<ids.length; i++) {
            AppdefEntityID id = ids[i];
            resources.add(rMan.findResource(id));
        }
        MeasurementDAO dao = getMeasurementDAO();
        List list = dao.findDesignatedByResourcesForCategory(resources, cat);
        for (Iterator it=list.iterator(); it.hasNext(); ) {
            Measurement m = (Measurement) it.next();
            midMap.put(new AppdefEntityID(m.getResource()), m);
        }
        return midMap;
    }

    /**
     * TODO: scottmf, need to do some more work to handle other authz resource
     *  types other than platform, server, service, and group
     * 
     * @return {@link Map} of {@link Integer} to {@link List} of
     * {@link Measurement}s, Integer => Resource.getId(),
     * @ejb:interface-method
     */
    public Map getAvailMeasurements(Collection resources) {
        final Map rtn = new HashMap(resources.size());
        final List res = new ArrayList(resources.size());
        final ResourceManagerLocal resMan = ResourceManagerEJBImpl.getOne();
        final MeasurementDAO dao = getMeasurementDAO();
        for (Iterator it=resources.iterator(); it.hasNext(); ) {
            Object o = it.next();
            Resource resource = null;
            if (o == null) {
                continue;
            } else if (o instanceof AppdefEntityValue) {
                AppdefEntityValue rv = (AppdefEntityValue) o;
                AppdefEntityID aeid = rv.getID();
                resource = resMan.findResource(aeid);
            } else if (o instanceof AppdefEntityID) {
                AppdefEntityID aeid = (AppdefEntityID) o;
                resource = resMan.findResource(aeid);
            } else if (o instanceof AppdefResource) {
                AppdefResource r = (AppdefResource)o;
                resource = resMan.findResource(r.getEntityId());
            } else if (o instanceof Resource) {
                resource = (Resource) o;
            } else if (o instanceof ResourceGroup) {
                ResourceGroup grp = (ResourceGroup) o;
                resource = grp.getResource();
                rtn.put(resource.getId(), dao.findAvailMeasurements(grp));
                continue;
            } else if (o instanceof AppdefResourceValue){
                AppdefResourceValue r = (AppdefResourceValue) o;
                AppdefEntityID aeid = r.getEntityId();
                resource = resMan.findResource(aeid);
            } else {
                resource = resMan.findResourceById((Integer) o);
            }
            try {
                if (resource == null || resource.isInAsyncDeleteState()) {
                    continue;
                }
            } catch (ObjectNotFoundException e) {
                continue;
            }
            final ResourceType type = resource.getResourceType();
            if (type.getId().equals(AuthzConstants.authzGroup)) {
                final ResourceGroupManagerLocal resGrpMan =
                    ResourceGroupManagerEJBImpl.getOne();
                ResourceGroup grp =
                    resGrpMan.getResourceGroupByResource(resource);
                rtn.put(resource.getId(), dao.findAvailMeasurements(grp));
                continue;
            } else if (type.getId().equals(AuthzConstants.authzApplication)) {
                rtn.putAll(getAvailMeas(resource));
                continue;
            }
            res.add(resource);
        }
        List ids = getMeasurementDAO().findAvailMeasurements(res);
        // may be null if measurements have not been configured
        if (ids == null) {
            return Collections.EMPTY_MAP;
        }
        for (Iterator it=ids.iterator(); it.hasNext(); ) {
            Measurement m = (Measurement)it.next();
            rtn.put(m.getResource().getId(), Collections.singletonList(m));
        }
        return rtn;
    }

    private final Map getAvailMeas(Resource application) {
        final Integer typeId = application.getResourceType().getId();
        if (!typeId.equals(AuthzConstants.authzApplication)) {
            return Collections.EMPTY_MAP;
        }
        final ApplicationManagerLocal appMan =
            ApplicationManagerEJBImpl.getOne();
        final AuthzSubject overlord =
            AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        try {
            final Application app = appMan.findApplicationById(
                overlord, application.getInstanceId());
            final Collection appServices = app.getAppServices();
            final List resources = new ArrayList(appServices.size());
            for (final Iterator it=appServices.iterator(); it.hasNext(); ) {
                final AppService appService = (AppService)it.next();
                resources.addAll(getAppResources(appService));
            }
            return getAvailMeasurements(resources);
        } catch (ApplicationNotFoundException e) {
            log.warn("cannot find Application by id = " +
                application.getInstanceId());
        } catch (PermissionException e) {
            log.error("error finding application using overlord", e);
        }
        return Collections.EMPTY_MAP;
    }

    private final List getAppResources(AppService appService) {
        if (!appService.isIsGroup()) {
            final Service service = appService.getService();
            if (service == null || service.getResource() == null ||
                service.getResource().isInAsyncDeleteState()) {
                return Collections.EMPTY_LIST;
            }
            return Collections.singletonList(service.getResource());
        }
        final ResourceGroup group = appService.getResourceGroup();
        final Resource resource = group.getResource();
        if (resource == null || resource.isInAsyncDeleteState()) {
            return Collections.EMPTY_LIST;
        }
        final ResourceGroupManagerLocal rgMan =
            ResourceGroupManagerEJBImpl.getOne();
        return new ArrayList(rgMan.getMembers(group));
    }

    /**
     * Look up a list of Measurement intervals for template IDs.
     *
     * @return a map keyed by template ID and values of metric intervals
     * There is no entry if a metric is disabled or does not exist for the
     * given entity or entities.  However, if there are multiple entities, and
     * the intervals differ or some enabled/not enabled, then the value will
     * be "0" to denote varying intervals.
     *
     * @ejb:interface-method
     */
    public Map findMetricIntervals(AuthzSubject subject, AppdefEntityID[] aeids,
                                   Integer[] tids) {
        final Long disabled = new Long(-1);
        MeasurementDAO ddao = getMeasurementDAO();
        Map intervals = new HashMap(tids.length);
        
        ResourceManagerLocal resMan = ResourceManagerEJBImpl.getOne();

        for (int ind = 0; ind < aeids.length; ind++) {
            Resource res = resMan.findResource(aeids[ind]);
            List metrics = ddao.findByTemplatesForInstance(tids, res);

            for (Iterator i = metrics.iterator(); i.hasNext();)
            {
                Measurement dm = (Measurement) i.next();
                Long interval = new Long(dm.getInterval());

                if (!dm.isEnabled()) {
                    interval = disabled;
                }
                
                Integer templateId = dm.getTemplate().getId();
                Long previous = (Long) intervals.get(templateId);

                if (previous == null) {
                    intervals.put(templateId, interval);
                } else {
                    if (!previous.equals(interval)) {
                        intervals.put(templateId, new Long(0));
                    }
                }
            }
        }
        
        // Filter by template IDs, since we only pay attention to what was
        // passed, but may have more than that in our map.
        for (int i=0; i<tids.length; i++) {
            if (!intervals.containsKey(tids[i]))
                intervals.put(tids[i], null);
        }
        
        // Copy the keys, since we are going to be modifying the interval map
        Set keys = new HashSet(intervals.keySet());
        for (Iterator i = keys.iterator(); i.hasNext();) {
            Integer templateId = (Integer) i.next();

            if (disabled.equals(intervals.get(templateId))) { // Disabled
                // so don't return it
                intervals.remove(templateId);
            }
        }

        return intervals;
    }
    
    /**
     * @return List<Object[]> - [0] = Measurement, [1] MeasurementTemplate
     * @ejb:interface-method
     */
    public List findAllEnabledMeasurementsAndTemplates() {
        MeasurementDAO dao = getMeasurementDAO();
        return dao.findAllEnabledMeasurementsAndTemplates();
    }

    /**
     * Set the interval of Measurements based their template ID's
     * Enable Measurements and enqueue for scheduling after commit
     *
     * @ejb:interface-method
     */
    public void enableMeasurements(AuthzSubject subject, AppdefEntityID[] aeids,
                                   Integer[] mtids, long interval)
        throws MeasurementNotFoundException, MeasurementCreateException,
               TemplateNotFoundException, PermissionException {

        MeasurementDAO dao = getMeasurementDAO();
        // Create a list of IDs
        Integer[] iids = new Integer[aeids.length];
        for (int i = 0; i < aeids.length; i++) {
            checkModifyPermission(subject.getId(), aeids[i]);
            iids[i] = aeids[i].getId();
        }
        
        List mids = new ArrayList(aeids.length * mtids.length);
        for (int i = 0; i < mtids.length; i++) {
            mids.addAll(dao.findIdsByTemplateForInstances(mtids[i], iids));
        }

        for (final Iterator it=mids.iterator(); it.hasNext(); ) {
            final Integer mid = (Integer)it.next();
            final Measurement m = dao.findById(mid);
            m.setEnabled(true);
            m.setInterval(interval);
        }
        
        // Update the agent schedule
        List toSchedule = Arrays.asList(aeids);
        if (!toSchedule.isEmpty()) {
            AgentScheduleSyncZevent event = new AgentScheduleSyncZevent(toSchedule);
            ZeventManager.getInstance().enqueueEventAfterCommit(event);
        }
    } 

    /**
     * Enable a collection of metrics, enqueue for scheduling 
     * after commit
     * @ejb:interface-method
     */
    public void enableMeasurements(AuthzSubject subject, Integer[] mids)
        throws PermissionException
    {
        StopWatch watch = new StopWatch();        
        Integer mid = null;
        Measurement meas = null;
        Resource resource = null;
        AppdefEntityID appId = null;
        List appIdList = new ArrayList();        
        List midsList = Arrays.asList(mids);
        
        watch.markTimeBegin("setEnabled");
        for (Iterator iter=midsList.iterator(); iter.hasNext(); ) {
            mid = (Integer) iter.next();
            
            meas = getMeasurementDAO().get(mid);

            if (!meas.isEnabled()) {                
                resource = meas.getResource();
                appId = new AppdefEntityID(resource);

                checkModifyPermission(subject.getId(), appId);                
                appIdList.add(appId);
                
                meas.setEnabled(true);
            }
        }
        watch.markTimeEnd("setEnabled");

        if (!appIdList.isEmpty()) {
            watch.markTimeBegin("enqueueZevents");
            AgentScheduleSyncZevent event = new AgentScheduleSyncZevent(appIdList);
            ZeventManager.getInstance().enqueueEventAfterCommit(event);
            watch.markTimeEnd("enqueueZevents");

            log.debug("enableMeasurements: total=" + appIdList.size() + ", time=" + watch);
        }
    }
    
    /**
     * Enable the Measurement and enqueue for scheduling after commit
     * @ejb:interface-method
     */
    public void enableMeasurement(AuthzSubject subject, Integer mId,
                                  long interval)
        throws PermissionException
    {
        final List mids = Collections.singletonList(mId);
        Measurement meas = getMeasurementDAO().get(mId);
        if (meas.isEnabled()) {
            return;
        }
        Resource resource = meas.getResource();
        AppdefEntityID appId = new AppdefEntityID(resource);
        checkModifyPermission(subject.getId(), appId);
        MeasurementDAO dao = getMeasurementDAO();
        for (final Iterator it=mids.iterator(); it.hasNext(); ) {
            final Integer mid = (Integer)it.next();
            final Measurement m = dao.findById(mid);
            m.setEnabled(true);
            m.setInterval(interval);
        }
        List eids = Collections.singletonList(appId);
        AgentScheduleSyncZevent event = new AgentScheduleSyncZevent(eids);
        ZeventManager.getInstance().enqueueEventAfterCommit(event);
    }
    
    /**
     * Enable the default on metrics for a given resource, enqueue for scheduling 
     * after commit
     * @ejb:interface-method
     */
    public void enableDefaultMeasurements(AuthzSubject subj, Resource r)
        throws PermissionException {
        AppdefEntityID appId = new AppdefEntityID(r);
        checkModifyPermission(subj.getId(), appId);
        boolean sendToAgent = false;

        List metrics = getMeasurementDAO().findDefaultsByResource(r);
        for (Iterator it = metrics.iterator(); it.hasNext(); ) {
            Measurement dm = (Measurement)it.next();
            if (!dm.isEnabled()) {
                dm.setEnabled(true);
                sendToAgent = true;
            }
        }
        if (sendToAgent) {
            List eids = Collections.singletonList(appId);
            AgentScheduleSyncZevent event = new AgentScheduleSyncZevent(eids);
            ZeventManager.getInstance().enqueueEventAfterCommit(event);
        }
    }
    
    /**
     * @throws PermissionException 
     * @ejb:interface-method
     */
    public void updateMeasurementInterval(AuthzSubject subject, Integer mId,
                                          long interval)
        throws PermissionException
    {
        Measurement meas = getMeasurementDAO().get(mId);
        meas.setEnabled((interval != 0));
        meas.setInterval(interval);
        Resource resource = meas.getResource();
        AppdefEntityID appId = new AppdefEntityID(resource);
        checkModifyPermission(subject.getId(), appId);
        enqueueZeventForMeasScheduleChange(meas, interval);
    }

    /**
     * Disable all measurements for the given resources.
     *
     * @param agentId The entity id to use to look up the agent connection
     * @param ids The list of entitys to unschedule
     * @ejb:interface-method
     *
     * NOTE: This method requires all entity ids to be monitored by the same
     * agent as specified by the agentId
     */
    public void disableMeasurements(AuthzSubject subject, AppdefEntityID agentId,
                                    AppdefEntityID[] ids)
        throws PermissionException, AgentNotFoundException {
        
        Agent agent = AgentManagerEJBImpl.getOne().getAgent(agentId);
        
        disableMeasurements(subject, agent, ids, false);
    }
    
    /**
     * Disable all measurements for the given resources.
     *
     * @param agent The agent for the given resources
     * @param ids The list of entitys to unschedule
     * @param isAsyncDelete Indicates whether it is for async delete
     * @ejb:interface-method
     *
     * NOTE: This method requires all entity ids to be monitored by the same
     * agent as specified by the agent
     */
    public void disableMeasurements(AuthzSubject subject, Agent agent,
                                    AppdefEntityID[] ids, boolean isAsyncDelete)
        throws PermissionException {
        
        MeasurementDAO dao = getMeasurementDAO();
        for (int i = 0; i < ids.length; i++) {
            checkModifyPermission(subject.getId(), ids[i]);
            List mcol = null;
            Resource res = getResource(ids[i]);
            if (isAsyncDelete) {
                // For asynchronous deletes, we need to get all measurements
                // because some disabled measurements are not unscheduled
                // from the agent (like during the maintenance window) and
                // we need to unschedule these measurements
                mcol = findMeasurements(subject, res);
            } else {
                mcol = dao.findEnabledByResource(res);            
            }
            
            if (mcol.isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("No measurements to disable for resource[" 
                                    + ids[i]
                                    + "], isAsyncDelete=" + isAsyncDelete);
                }
                continue;
            }
            
            Integer[] mids = new Integer[mcol.size()];
            Iterator it = mcol.iterator();
            for (int j = 0; it.hasNext(); j++) {
                Measurement dm = (Measurement) it.next();
                dm.setEnabled(false);
                mids[j] = dm.getId();
            }

            removeMeasurementsFromCache(mids);
            
            enqueueZeventsForMeasScheduleCollectionDisabled(mids);
        }

        // Unscheduling of all metrics for a resource could indicate that
        // the resource is getting removed.  Send the unschedule synchronously
        // so that all the necessary plumbing is in place.
        ZeventManager.getInstance().enqueueEventAfterCommit(
            new AgentUnscheduleZevent(Arrays.asList(ids), agent.getAgentToken()));
    }

    /**
     * Disable all Measurements for a resource
     *
     * @ejb:interface-method
     */
    public void disableMeasurements(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        // Authz check
        checkModifyPermission(subject.getId(), id);
        disableMeasurements(subject, getResource(id));
    }
    
    /**
     * Disable all Measurements for a resource
     *
     * @ejb:interface-method
     */
    public void disableMeasurements(AuthzSubject subject, Resource res) 
        throws PermissionException
    {    
        disableMeasurements(subject, res, false);
    }

    /**
     * Disable all Measurements for a resource
     *
     * @ejb:interface-method
     */
    public void disableMeasurements(AuthzSubject subject, 
                                    Resource res,
                                    boolean isAsyncDelete)
        throws PermissionException 
    {
        List mcol = null;
        
        if (isAsyncDelete) {
            // For asynchronous deletes, we need to get all measurements
            // because some disabled measurements are not unscheduled
            // from the agent (like during the maintenance window) and
            // we need to unschedule these measurements
            mcol = findMeasurements(subject, res);
        } else {
            mcol = getMeasurementDAO().findEnabledByResource(res);            
        }
        
        if (mcol.size() == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No measurements to disable for resourceId=" 
                                + res.getId()
                                + ", isAsyncDelete=" + isAsyncDelete);
            }
            return;
        }
        
        Integer[] mids = new Integer[mcol.size()];
        Iterator it = mcol.iterator();
        AppdefEntityID aeid = null;
        for (int i = 0; it.hasNext(); i++) {
            Measurement dm = (Measurement)it.next();
            dm.setEnabled(false);
            mids[i] = dm.getId();
            if (aeid == null) {
                aeid = new AppdefEntityID(dm.getTemplate().getMonitorableType()
                                              .getAppdefType(),
                                          dm.getInstanceId());
            }
        }

        removeMeasurementsFromCache(mids);
        enqueueZeventsForMeasScheduleCollectionDisabled(mids);

        // Unscheduling of all metrics for a resource could indicate that
        // the resource is getting removed.  Send the unschedule synchronously
        // so that all the necessary plumbing is in place.
        try {
            MeasurementProcessorEJBImpl.getOne().unschedule(
                Collections.singletonList(aeid));
        } catch (MeasurementUnscheduleException e) {
            log.error("Unable to disable measurements", e);
        }
    }
    
    /**
     * XXX: not sure why all the findMeasurements require an authz if they
     * do not check the viewPermissions??
     * @ejb:interface-method
     */
    public List findMeasurements(AuthzSubject subject, Resource res) {
        return getMeasurementDAO().findByResource(res);
    }

    /**
     * Disable measurements for an instance
     * Enqueues reschedule events after commit
     *
     * @ejb:interface-method
     */
    public void disableMeasurements(AuthzSubject subject, AppdefEntityID id,
                                    Integer[] tids)
        throws PermissionException {
        // Authz check
        checkModifyPermission(subject.getId(), id);
        
        Resource resource = getResource(id);
        List mcol = getMeasurementDAO().findByResource(resource);
        HashSet tidSet = null;
        if (tids != null) {
            tidSet = new HashSet(Arrays.asList(tids));
        }            
        
        List toUnschedule = new ArrayList();
        for (Iterator it = mcol.iterator(); it.hasNext(); ) {
            Measurement dm = (Measurement)it.next();
            // Check to see if we need to remove this one
            if (tidSet != null && 
                !tidSet.contains(dm.getTemplate().getId()))
                    continue;

            dm.setEnabled(false);
            toUnschedule.add(dm.getId());
        }

        Integer[] mids = 
            (Integer[])toUnschedule.toArray(new Integer[toUnschedule.size()]);
        
        removeMeasurementsFromCache(mids);
        
        enqueueZeventsForMeasScheduleCollectionDisabled(mids);
        
        List eids = Collections.singletonList(id);
        AgentScheduleSyncZevent event = new AgentScheduleSyncZevent(eids);
        ZeventManager.getInstance().enqueueEventAfterCommit(event);
    }

    /**
     * @ejb:interface-method
     * @ejb:transaction type="NotSupported"
     */
    public void syncPluginMetrics(String plugin) {
        ConfigManagerLocal cm = ConfigManagerEJBImpl.getOne();
        List entities = getMeasurementDAO().findMetricsCountMismatch(plugin);
        
        AuthzSubject overlord =
            AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();

        for (Iterator it = entities.iterator(); it.hasNext(); ) {
            Object[] vals = (Object[]) it.next();
            
            java.lang.Number type = (java.lang.Number) vals[0];
            java.lang.Number id = (java.lang.Number) vals[1];
            AppdefEntityID aeid =
                new AppdefEntityID(type.intValue(), id.intValue());

            try {
                log.info("syncPluginMetrics sync'ing metrics for " + aeid);
                ConfigResponse c =
                    cm.getMergedConfigResponse(overlord,
                                               ProductPlugin.TYPE_MEASUREMENT,
                                               aeid, true);
                enableDefaultMetrics(overlord, aeid, c, false);
            } catch (AppdefEntityNotFoundException e) {
                // Move on since we did this query based on measurement table
                // not resource table
            } catch (PermissionException e) {
                // Quite impossible
                assert(false);
            } catch (Exception e) {
                // No valid configuration to use to enable metrics
            }
        }
    }
    
    /**
     * Gets a summary of the metrics which are scheduled for collection, 
     * across all resource types and metrics.
     * 
     * @return a list of {@link CollectionSummary} beans
     * @ejb:interface-method
     */
    public List findMetricCountSummaries() {
        return getMeasurementDAO().findMetricCountSummaries();
    }
    
    /**
     * Find a list of tuples (of size 4) consisting of 
     *   the {@link Agent}
     *   the {@link Platform} it manages 
     *   the {@link Server} representing the Agent
     *   the {@link Measurement} that contains the Server Offset value
     * 
     * @ejb:interface-method
     */
    public List findAgentOffsetTuples() {
        return getMeasurementDAO().findAgentOffsetTuples();
    }
    
    /**
     * Get the # of metrics that each agent is collecting.
     * 
     * @return a map of {@link Agent} onto Longs indicating how many metrics
     *         that agent is collecting. 
     * @ejb:interface-method
     */
    public Map findNumMetricsPerAgent() {
        return getMeasurementDAO().findNumMetricsPerAgent();
    }

    /**
     * Handle events from the {@link MeasurementEnabler}.  This method
     * is required to place the operation within a transaction (and session)
     * 
     * @ejb:interface-method
     */
    public void handleCreateRefreshEvents(List events) {
        ConfigManagerLocal cm = ConfigManagerEJBImpl.getOne();
        TrackerManagerLocal tm = TrackerManagerEJBImpl.getOne();
        AuthzSubjectManagerLocal aman = AuthzSubjectManagerEJBImpl.getOne();
        List eids = new ArrayList(events.size());
        final boolean debug = log.isDebugEnabled();
        
        for (Iterator i=events.iterator(); i.hasNext(); ) {
            ResourceZevent z = (ResourceZevent)i.next();
            AuthzSubject subject = aman.findSubjectById(z.getAuthzSubjectId());
            AppdefEntityID id = z.getAppdefEntityID();
            final Resource r = getResource(id);
            if (r == null || r.isInAsyncDeleteState()) {
                continue;
            }
    
            boolean isCreate = z instanceof ResourceCreatedZevent;
            boolean isRefresh =
                z instanceof ResourceRefreshZevent || z instanceof ResourceUpdatedZevent;
    
            try {
                // Handle reschedules for when agents are updated.
                if (isRefresh) {
                    if (debug) log.debug("Refreshing metric schedule for [" + id + "]");
                    eids.add(id);
                    continue;
                }
    
                // For either create or update events, schedule the default
                // metrics
                ConfigResponse c = cm.getMergedConfigResponse(
                    subject, ProductPlugin.TYPE_MEASUREMENT, id, true);
                if (getEnabledMetricsCount(subject, id) == 0) {
                    if (debug) log.debug("Enabling default metrics for [" + id + "]");
                    List metrics = enableDefaultMetrics(subject, id, c, true);
                    if (!metrics.isEmpty()) {
                        eids.add(id);
                    }
                } else {
                    // Update the configuration
                    updateMeasurements(subject, id, c);
                }
    
                if (isCreate) {
                    // On initial creation of the service check if log or config
                    // tracking is enabled.  If so, enable it.  We don't auto
                    // enable log or config tracking for update events since
                    // in the callback we don't know if that flag has changed.
                    tm.enableTrackers(subject, id, c);
                }
    
            } catch (ConfigFetchException e) {
                log.debug("Config not set for [" + id + "]", e);
            } catch(Exception e) {
                log.warn("Unable to enable default metrics for [" + id + "]", e);
            }
        }
        if (!eids.isEmpty()) {
            AgentScheduleSyncZevent event = new AgentScheduleSyncZevent(eids);
	        ZeventManager.getInstance().enqueueEventAfterCommit(event);
        }
    }

    // XXX scottmf, need to re-evalutate why SAMPLE_SIZE is used
    private final int SAMPLE_SIZE = 10;
    private String[] getTemplatesToCheck(AuthzSubject s,
                                         AppdefEntityID id)
        throws AppdefEntityNotFoundException, PermissionException
    {
        MeasurementTemplateDAO dao = getMeasurementTemplateDAO();
        String mType = (new AppdefEntityValue(id, s)).getMonitorableType();
        List templates = dao.findDefaultsByMonitorableType(mType, id.getType());
        List dsnList = new ArrayList(SAMPLE_SIZE);
        int idx = 0;
        int availIdx = -1;
        MeasurementTemplate template;
        for (int i=0; i<templates.size(); i++) {

            template = (MeasurementTemplate)templates.get(i);

            if (template.isAvailability() && template.isDesignate()) {
                availIdx = idx;
            }

            if (idx == availIdx
                || (availIdx == -1 && idx < (SAMPLE_SIZE-1))
                || (availIdx != -1 && idx < SAMPLE_SIZE))
            {
                dsnList.add(template.getTemplate());
                // Increment only after we have successfully added DSN
                idx++;
                if (idx >= SAMPLE_SIZE) break;
            }
        }

        return (String[]) dsnList.toArray(new String[dsnList.size()]);
    }

    /**
     * Check a configuration to see if it returns DSNs which the agent
     * can use to successfully monitor an entity.  This routine will
     * attempt to get live DSN values from the entity.
     *
     * @param entity Entity to check the configuration for
     * @param config Configuration to check
     *
     * @ejb:interface-method
     */
    public void checkConfiguration(AuthzSubject subject,
                                   AppdefEntityID entity,
                                   ConfigResponse config)
        throws PermissionException, InvalidConfigException,
               AppdefEntityNotFoundException
    {
        String[] templates = getTemplatesToCheck(subject, entity);

        // there are no metric templates, just return
        if (templates.length == 0) {
            log.debug("No metrics to checkConfiguration for " + entity);
            return;
        } else {
            log.debug("Using " + templates.length +
                      " metrics to checkConfiguration for " + entity);
        }

        String[] dsns = new String[templates.length];
        for (int i = 0; i < dsns.length; i++) {
            dsns[i] = translate(templates[i], config);
        }

        try {
            getLiveMeasurementValues(entity, dsns);
        } catch(LiveMeasurementException exc){
            throw new InvalidConfigException("Invalid configuration: " +
                                             exc.getMessage(), exc);
        }
    }
    
    /**
     * @return List {@link Measurement} of MeasurementIds
     * @ejb:interface-method
     */
    public List getMeasurements(Integer[] tids, Integer[] aeids) {
        return getMeasurementDAO().findMeasurements(tids, aeids);
    }

    /**
     * Get live measurement values for a series of DSNs
     *
     * NOTE:  Since this routine allows callers to pass in arbitrary
     *        DSNs, the caller must do all the appropriate translation,
     *        etc.
     *
     * @param entity  Entity to get the measurement values from
     * @param dsns    Translated DSNs to fetch from the entity
     *
     * @return A list of MetricValue objects for each DSN passed
     */
    private MetricValue[] getLiveMeasurementValues(AppdefEntityID entity,
                                                   String[] dsns)
        throws LiveMeasurementException, PermissionException
    {
        try {
            AgentMonitor monitor = new AgentMonitor();
            Agent a = getAgent(entity);

            return monitor.getLiveValues(a, dsns);
        } catch(MonitorAgentException e){
            throw new LiveMeasurementException(e.getMessage(), e);
        }
    }

    /**
     * Resource to be deleted, dissociate metrics from resource
     * @ejb:interface-method
     */
    public void handleResourceDelete(Resource r) {
        getMeasurementDAO().clearResource(r);
    }
    
    /**
     * Enable the default metrics for a resource.  This should only
     * be called by the {@link MeasurementEnabler}.  If you want the behavior
     * of this method, use the {@link MeasurementEnabler} 
     * @return {@link List} of {@link Measurement}s
     */
    private List enableDefaultMetrics(AuthzSubject subj, AppdefEntityID id,
                                      ConfigResponse config, boolean verify)
        throws AppdefEntityNotFoundException, PermissionException 
    {
        List rtn = Collections.EMPTY_LIST;
        ConfigManagerLocal cfgMan = ConfigManagerEJBImpl.getOne();
        String mtype;
    
        try {
            if (id.isPlatform() || id.isServer() || id.isService()) {
                AppdefEntityValue av = new AppdefEntityValue(id, subj);
                try {
                    mtype = av.getMonitorableType();
                } catch (AppdefEntityNotFoundException e) {
                    // Non existent resource, we'll clean it up in
                    // removeOrphanedMeasurements()
                    return rtn;
                }
            }
            else {
                return rtn;
            }
        } catch (Exception e) {
            log.error("Unable to enable default metrics for [" + id + "]", e);
            return rtn;
        }
    
        // Check the configuration
        if (verify) {
            try {
                checkConfiguration(subj, id, config);
            } catch (InvalidConfigException e) {
                log.warn("Error turning on default metrics, configuration (" +
                          config + ") " + "couldn't be validated", e);
                cfgMan.setValidationError(subj, id, e.getMessage());
                return rtn;
            } catch (Exception e) {
                log.warn("Error turning on default metrics, " +
                          "error in validation", e);
                cfgMan.setValidationError(subj, id, e.getMessage());
                return rtn;
            }
        }
    
        // Enable the metrics
        try {
            rtn = createDefaultMeasurements(subj, id, mtype, config);
            cfgMan.clearValidationError(subj, id);
    
            // Execute the callback so other people can do things when the
            // metrics have been created (like create type-based alerts)
            MeasurementStartupListener.getDefaultEnableObj().metricsEnabled(id);
        } catch (Exception e) {
            log.warn("Unable to enable default metrics for id=" + id +
                      ": " + e.getMessage(), e);
        }
        return rtn;
    }
   
   /**
    * Initializes the units and resource properties of a measurement event
    *
    * @ejb:interface-method
    */
   public void buildMeasurementEvent(MeasurementEvent event) {      
       Measurement dm = null;
       
       try {
           dm = getMeasurementDAO().get(event.getInstanceId());
           int resourceType = dm.getTemplate().getMonitorableType()
                                   .getAppdefType();
           event.setResource(new AppdefEntityID(resourceType, dm.getInstanceId()));
           event.setUnits(dm.getTemplate().getUnits());
       } catch (Exception e) {
           if (event == null) {
               log.warn("Measurement event is null");
           } else if (dm == null) {
               log.warn("Measurement is null for measurement event with metric id=" + event.getInstanceId());                
           } else if (event.getResource() == null) {
               log.error("Unable to set resource for measurement event with metric id=" + event.getInstanceId(), e);
           } else if (event.getUnits() == null ) {
               log.error("Unable to set units for measurement event with metric id=" + event.getInstanceId(), e);
           } else {
               log.error("Unable to build measurement event with metric id=" + event.getInstanceId(), e);
           }
       }
   }

    public static MeasurementManagerLocal getOne() {
        try {
            return MeasurementManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {}
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx){}
}
