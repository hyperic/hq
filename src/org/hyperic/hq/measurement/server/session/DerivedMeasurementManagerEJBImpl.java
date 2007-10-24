/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ConfigManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.ResourceCreatedZevent;
import org.hyperic.hq.appdef.server.session.ResourceRefreshZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManagerLocal;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerLocal;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.measurement.EvaluationException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.ext.DownMetricValue;
import org.hyperic.hq.measurement.ext.depgraph.DerivedNode;
import org.hyperic.hq.measurement.ext.depgraph.Graph;
import org.hyperic.hq.measurement.ext.depgraph.InvalidGraphException;
import org.hyperic.hq.measurement.ext.depgraph.Node;
import org.hyperic.hq.measurement.ext.depgraph.RawNode;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerUtil;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.TrackerManagerLocal;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.server.MBeanUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.quartz.SchedulerException;

/** The DerivedMeasurementManagerEJB class is a stateless session bean that can
 * be used to interact with DerivedMeasurement EJB's
 *
 * @ejb:bean name="DerivedMeasurementManager"
 *      jndi-name="ejb/measurement/DerivedMeasurementManager"
 *      local-jndi-name="LocalDerivedMeasurementManager"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:transaction type="REQUIRED"
 */
public class DerivedMeasurementManagerEJBImpl extends SessionEJB
    implements SessionBean 
{
    private final Log log =
        LogFactory.getLog(DerivedMeasurementManagerEJBImpl.class);

    protected final String VALUE_PROCESSOR =
        PagerProcessor_measurement.class.getName();
     
    private Pager valuePager = null;

    /**
     * Needed for calls back into the manager that require setting up a new
     * transaction. (i.e. creation of measurements.
     */
    private DerivedMeasurementManagerLocal getDMManager() {
        return DerivedMeasurementManagerEJBImpl.getOne();
    }

    private RawMeasurementManagerLocal getRmMan() {
        return RawMeasurementManagerEJBImpl.getOne();
    }

    private DerivedMeasurement updateMeasurementInterval(Integer tid,
                                                         Integer iid,
                                                         long interval)
        throws FinderException
    {
        DerivedMeasurement m =
            getDerivedMeasurementDAO().findByTemplateForInstance(tid, iid);
        if (m == null) {
            // Fix me
            throw new FinderException();
        }

        m.setEnabled(interval != 0);
        m.setInterval(interval);
        return m;
    }

    private Integer getRawIdByTemplateAndInstance(Integer tid, Integer iid) {
        RawMeasurement m =
            getRawMeasurementDAO().findByTemplateForInstance(tid, iid);
        if (m == null) {
            return null;
        }
        return m.getId();
    }

    private RawMeasurement createRawMeasurement(Integer instanceId, 
                                                Integer templateId,
                                                ConfigResponse props)
        throws MeasurementCreateException {
        return getRmMan().createMeasurement(templateId, instanceId, props);
    }

    private DerivedMeasurement createDerivedMeasurement(AppdefEntityID id,
                                                        MeasurementTemplate mt,
                                                        long interval)
        throws MeasurementCreateException
    {
        Integer instanceId = id.getId();
        MonitorableType monTypeVal = mt.getMonitorableType();

        if(monTypeVal.getAppdefType() != id.getType()) {
            throw new MeasurementCreateException("Appdef entity (" + id + ")" +
                                                 "/template type (ID: " +
                                                 mt.getId() + ") mismatch");
        }

        return getDerivedMeasurementDAO().create(instanceId, mt, interval);
    }

    /**
     * Look up a derived measurement's appdef entity ID
     */
    private AppdefEntityID getAppdefEntityId(DerivedMeasurement dm) {
        return new AppdefEntityID(dm.getAppdefType(), dm.getInstanceId());
    }

    private void sendAgentSchedule(Serializable obj) {
        if (obj != null) {
            Messenger sender = new Messenger();
            sender.sendMessage("queue/agentScheduleQueue", obj);
        }
    }
    
    private void unscheduleJobs(Integer[] mids) {
        for (int i = 0; i < mids.length; i++) {
            // Remove the job
            String jobName =
                CalculateDerivedMeasurementJob.getJobName(mids[i]);
            try {
                Object job = getScheduler().getJobDetail
                    (jobName, CalculateDerivedMeasurementJob.SCHEDULER_GROUP);

                if (job != null) {
                    getScheduler().deleteJob(
                        jobName,
                        CalculateDerivedMeasurementJob.SCHEDULER_GROUP);
                }
            } catch (SchedulerException e) {
                log.debug("No job for " + jobName);
            }

            // Remove the schedule
            String schedName =
                CalculateDerivedMeasurementJob.getScheduleName(mids[i]);
            try {
                Object schedule = getScheduler().getTrigger
                    (schedName, CalculateDerivedMeasurementJob.SCHEDULER_GROUP);

                if (null != schedule) {
                    getScheduler().unscheduleJob(
                        schedName,
                        CalculateDerivedMeasurementJob.SCHEDULER_GROUP);
                }
            } catch (SchedulerException e) {
                log.debug("No schedule for " + schedName);
            }            
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
     * @return a List of the associated DerivedMeasurement objects
     * @ejb:transaction type="REQUIRESNEW"
     * @ejb:interface-method
     */
    public List createMeasurements(AppdefEntityID id, Integer[] templates,
                                   long[] intervals, ConfigResponse props)
        throws MeasurementCreateException, TemplateNotFoundException
    {
        Integer instanceId = id.getId();
        ArrayList dmList   = new ArrayList();

        if(intervals.length != templates.length){
            throw new IllegalArgumentException("The templates and intervals " +
                                               " lists must be the same size");
        }

        try {
            Graph[] graphs = new Graph[templates.length];
            for (int i = 0; i < templates.length; i++) {
                Integer dTemplateId = templates[i];
                long interval = intervals[i];

                graphs[i] = GraphBuilder.buildGraph(dTemplateId);

                DerivedNode derivedNode = (DerivedNode)
                    graphs[i].getNode( dTemplateId.intValue() );
                MeasurementTemplate derivedTemplateValue =
                    derivedNode.getMeasurementTemplate();

                // we will fill this variable with the actual derived 
                // measurement that is being enabled
                DerivedMeasurement argDm = null;
    
                // first handle simple IDENTITY derived case
                if (MeasurementConstants.TEMPL_IDENTITY.
                    equals(derivedTemplateValue.getTemplate()))
                {
                    RawNode rawNode = (RawNode)
                        derivedNode.getOutgoing().iterator().next();
                    MeasurementTemplate rawTemplateValue =
                        rawNode.getMeasurementTemplate();

                    // Check the raw node
                    Integer rmId =
                        getRawIdByTemplateAndInstance(rawTemplateValue.getId(),
                                                      instanceId);
                    if (rmId == null) {
                        if (props == null) {
                            // No properties, go on to the next template
                            continue;
                        }

                        createRawMeasurement(instanceId,
                                             rawTemplateValue.getId(),
                                             props);
                    }
                    
                    // if no DM already exists, then we need to create the
                    // raw and derived and make note to schedule raw
                    DerivedMeasurement dm;
                    try {
                        dm = updateMeasurementInterval(dTemplateId,
                                                       instanceId, interval);
                    } catch (FinderException e) {
                        dm = createDerivedMeasurement(id, derivedTemplateValue,
                                                      interval);
                    }

                    argDm = dm;
                } else {
                    // we're not an identity DM template, so we need
                    // to make sure that measurements are enabled for
                    // the whole graph
                    for (Iterator graphNodes = graphs[i].getNodes().iterator();
                         graphNodes.hasNext();) {
                        Node node = (Node)graphNodes.next();
                        MeasurementTemplate templArg =
                            node.getMeasurementTemplate();
    
                        if (node instanceof DerivedNode) {
                            DerivedMeasurement dm;
                            try {
                                dm = updateMeasurementInterval(templArg.getId(),
                                                               instanceId,
                                                               interval);
                            } catch (FinderException e) {
                                dm = createDerivedMeasurement(id, templArg,
                                                              interval);
                            }

                            if (dTemplateId.equals(templArg.getId())) {
                                argDm = dm;
                            }
                        } else {
                            // we are a raw node
                            Integer rmId =
                                getRawIdByTemplateAndInstance(templArg.getId(), 
                                                              instanceId);
    
                            if (rmId == null) {
                                createRawMeasurement(instanceId,
                                                     templArg.getId(),
                                                     props);
                            }
                        }
                    }
                }

                dmList.add(argDm);
            }
        } catch (InvalidGraphException e) {
            throw new MeasurementCreateException("InvalidGraphException:", e);
        } finally {
            // Force a flush to ensure the metrics are stored
            getDerivedMeasurementDAO().getSession().flush();
        }
        return dmList;
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
        
        for (Iterator i=events.iterator(); i.hasNext(); ) {
            ResourceZevent z = (ResourceZevent)i.next();
            AuthzSubjectValue subject = z.getAuthzSubjectValue();
            AppdefEntityID id = z.getAppdefEntityID();
            boolean isCreate, isRefresh;

            isCreate = z instanceof ResourceCreatedZevent;
            isRefresh = z instanceof ResourceRefreshZevent;

            try {
                // Handle reschedules for when agents are updated.
                if (isRefresh) {
                    log.info("Refreshing metric schedule for [" + id + "]");
                    reschedule(id);
                    continue;
                }

                // For either create or update events, schedule the default
                // metrics
                if (getEnabledMetricsCount(subject, id) == 0) {
                    log.info("Enabling default metrics for [" + id + "]");
                    AuthzSubject subj = aman.findSubjectById(subject.getId());
                    enableDefaultMetrics(subj, id);
                }

                if (isCreate) {
                    // On initial creation of the service check if log or config
                    // tracking is enabled.  If so, enable it.  We don't auto
                    // enable log or config tracking for update events since
                    // in the callback we don't know if that flag has changed.
                    ConfigResponse c =
                        cm.getMergedConfigResponse(subject,
                                                   ProductPlugin.TYPE_MEASUREMENT,
                                                   id, true);
                    tm.enableTrackers(subject, id, c);
                }

            } catch (ConfigFetchException e) {
                log.debug("Config not set for [" + id + "]", e);
            } catch(Exception e) {
                log.warn("Unable to enable default metrics for [" + id + "]", e);
            }
        }
    }

    /**
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

        // Call back into ourselves to force a new transation to be created.
        List dmList = getDMManager().createMeasurements(id, templates,
                                                        intervals, props);
        sendAgentSchedule(id);
        return dmList;
    }

    /**
     * Create Measurement objects based their templates and default intervals
     *
     * @param templates   List of Integer template IDs to add
     * @param id          instance ID (appdef resource) the templates are for
     * @param props       Configuration data for the instance
     *
     * @return a List of the associated DerivedMeasurementValue objects
     * @ejb:interface-method
     */
    public List createMeasurements(AuthzSubject subject, 
                                   AppdefEntityID id, Integer[] templates,
                                   ConfigResponse props)
        throws PermissionException, MeasurementCreateException,
               TemplateNotFoundException {
        long[] intervals = new long[templates.length];
        for (int i = 0; i < templates.length; i++) {
            MeasurementTemplate tmpl =
                getMeasurementTemplateDAO().findById(templates[i]);
            intervals[i] = tmpl.getDefaultInterval();
        }
        
        return createMeasurements(subject, id, templates, intervals,props);
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
     * @return a List of the associated DerivedMeasurementValue objects
     * @ejb:interface-method
     */
    public List createDefaultMeasurements(AuthzSubject subject,
                                          AppdefEntityID id,
                                          String mtype,
                                          ConfigResponse props)
        throws TemplateNotFoundException, PermissionException,
               MeasurementCreateException {
        // We're going to make sure there aren't metrics already
        List dms = findMeasurements(subject, id, null, PageControl.PAGE_ALL);
        if (dms.size() != 0) {
            return dms;
        }

        // Find the templates
        Collection mts =
            getMeasurementTemplateDAO().findTemplatesByMonitorableType(mtype);

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
     * Create Measurement object based on its template
     *
     * @param template Integer template ID to add
     * @param id Appdef ID the templates are for
     * @param interval Millisecond interval that the measurement is polled
     * @param config Configuration data for the instance
     *
     * @return an associated DerivedMeasurement object
     * @ejb:interface-method
     */
    public DerivedMeasurement createMeasurement(AuthzSubject subject,
                                                Integer template,
                                                AppdefEntityID id,
                                                long interval,
                                                ConfigResponse config)
        throws PermissionException, MeasurementCreateException,
               TemplateNotFoundException
    {
        // Authz check
        super.checkModifyPermission(subject.getId(), id);

        List dmvs = createMeasurements(subject, id,
                                       new Integer[] { template },
                                       new long[]    { interval },
                                       config);

        return (DerivedMeasurement) dmvs.get(0);
    }

    /**
     * Update the derived measurements of a resource
     * @ejb:interface-method
     */
    public void updateMeasurements(AuthzSubject subject,
                                   AppdefEntityID id, ConfigResponse props)
        throws PermissionException, MeasurementCreateException
    {
        // Update all of the raw measurements first
        try {
            getRmMan().updateMeasurements(id, props);
            
            // Now see which derived measurements need to be rescheduled
            List mcol = getDerivedMeasurementDAO().findByInstance(id.getType(),
                                                                  id.getID());
            
            for (Iterator i = mcol.iterator(); i.hasNext();) {
                DerivedMeasurement dm = (DerivedMeasurement)i.next();
                if (dm.isEnabled()) {
                    // A little short-cut.  We just end up looking up the
                    // derived measurement twice.
                    Integer tid = dm.getTemplate().getId();
                    createMeasurement(subject, tid, id, dm.getInterval(), props);
                }
            }
        } catch (TemplateNotFoundException e) {
            // Would not happen since we're creating measurements with the
            // template that we just looked up
            log.error(e);
        }
    }

    /**
     * Remove all measurements for multiple instances
     *
     * @ejb:interface-method
     */
    public void removeMeasurements(AuthzSubjectValue subject,
                                   AppdefEntityID agentEnt,
                                   AppdefEntityID[] entIds)
        throws RemoveException, PermissionException
    {
        MetricDeleteCallback cb = 
            MeasurementStartupListener.getMetricDeleteCallbackObj();
        DerivedMeasurementDAO dao = getDerivedMeasurementDAO();
        /* Authz check should be performed on entity removal
        for (int i = 0; i < entIds.length; i++) {
            // Authz check
            checkDeletePermission(subject, entIds[i]);
        }
        */

        for (Iterator i=dao.findByInstances(entIds).iterator(); i.hasNext(); ) {
            DerivedMeasurement dm = (DerivedMeasurement)i.next();

            cb.beforeMetricDelete(dm);
            dao.remove(dm);
        }

        // send queue message to unschedule
        UnScheduleArgs unschBean = new UnScheduleArgs(agentEnt, entIds);
        log.debug("Sending unschedule message to SCHEDULE_QUEUE: " + unschBean);
        sendAgentSchedule(unschBean);
    }

    /** 
     * Look up a derived measurement for an instance and an alias
     * and an alias.
     *
     * @return a DerivedMeasurement value
     * @ejb:interface-method
     */
    public DerivedMeasurement getMeasurement(AuthzSubject subject,
                                             AppdefEntityID id,
                                             String alias)
        throws MeasurementNotFoundException {

        DerivedMeasurement m = 
            getDerivedMeasurementDAO().findByAliasAndID(alias, id.getType(),
                                                        id.getID());
        if (m == null) {
            throw new MeasurementNotFoundException(alias + " for " + id + 
                                                   " not found.");
        }

        return m;
    }

    /**
     * Look up a DerivedMeasurement by Id.
     * @ejb:interface-method
     */
    public DerivedMeasurement getMeasurement(Integer mid) {
        return getDerivedMeasurementDAO().get(mid);
    }

    /**
     * Get the live measurement values for a given resource.
     * @param id The id of the resource
     * @ejb:interface-method
     */
    public void getLiveMeasurementValues(AuthzSubjectValue subject,
                                         AppdefEntityID id)
        throws EvaluationException, PermissionException,
               LiveMeasurementException, MeasurementNotFoundException
    {
        List mcol = 
            getDerivedMeasurementDAO().findByInstance(id.getType(),
                                                      id.getID(),
                                                      true);
        Integer[] mids = new Integer[mcol.size()];
        Integer availMeasurement = null; // For insert of AVAIL down
        Iterator it = mcol.iterator();

        for (int i = 0; it.hasNext(); i++) {
            DerivedMeasurement dm = (DerivedMeasurement)it.next();
            mids[i] = dm.getId();
            
            MeasurementTemplate template = dm.getTemplate();

            if (template.getAlias().equals(Metric.ATTR_AVAIL)) {
                availMeasurement = dm.getId();
            }
        }

        log.info("Getting live measurements for " + mids.length +
                 " measurements");
        try {
            getLiveMeasurementValues(subject, mids);
        } catch (LiveMeasurementException e) {
            log.info("Resource " + id + " reports it is unavailable, setting " +
                     "measurement ID " + availMeasurement + " to DOWN");
            if (availMeasurement != null) {
                MetricValue val =
                    new MetricValue(MeasurementConstants.AVAIL_DOWN);
                DataManagerLocal dataMan = getDataMan();
                dataMan.addData(availMeasurement, val, true);
            }
        }
    }

    /**
     * Get the live measurement value - assumes all measurement ID's share
     * the same agent connection
     * @param mids The array of metric id's to fetch
     * @ejb:interface-method
     */
    public MetricValue[] getLiveMeasurementValues(AuthzSubjectValue subject,
                                                  Integer[] mids)
        throws EvaluationException, PermissionException,
               LiveMeasurementException, MeasurementNotFoundException {
        try {
            DataManagerLocal dataMan = getDataMan();

            DerivedMeasurement[] dms = new DerivedMeasurement[mids.length];
            Integer[] identRawIds = new Integer[mids.length];
            Arrays.fill(identRawIds, null);
            
            HashSet rawIdSet = new HashSet();
            HashSet derIdSet = new HashSet();
            for (int i = 0; i < mids.length; i++) {
                // First, find the derived measurement
                dms[i] = getMeasurement(mids[i]);
                
                if (!dms[i].isEnabled())
                    throw new LiveMeasurementException("Metric ID: " +
                                                       mids[i] + 
                                                       " is not currently " +
                                                       "enabled");
                
                // Now get the IDs
                Integer[] metIds = getArgumentIds(dms[i]);

                if (dms[i].getFormula().equals(
                    MeasurementConstants.TEMPL_IDENTITY)) {
                    rawIdSet.add(metIds[0]);
                    identRawIds[i] = metIds[0];
                } else {
                    derIdSet.addAll(Arrays.asList(metIds));
                }
            }

            // Now look up the measurements            
            HashMap dataMap = new HashMap();
            
            // Get the raw measurements
            if (rawIdSet.size() > 0) {
                Integer[] rawIds = (Integer[])
                    rawIdSet.toArray(new Integer[rawIdSet.size()]);
                
                MetricValue[] vals =
                    getRmMan().getLiveMeasurementValues(rawIds);
                for (int i = 0; i < rawIds.length; i++) {
                    dataMap.put(rawIds[i], vals[i]);
                    // Add data to database
                    dataMan.addData(rawIds[i], vals[i], true); 
                }
            }
            
            // Get the derived measurements
            if (derIdSet.size() > 0) {
                Integer[] derIds = (Integer[])
                    derIdSet.toArray(new Integer[derIdSet.size()]);
                
                MetricValue[] vals = getLiveMeasurementValues(subject, derIds);
                for (int i = 0; i < derIds.length; i++) {
                    dataMap.put(derIds[i], vals[i]);
                }
            }

            MetricValue[] res = new MetricValue[dms.length];
            // Now go through each derived measurement and calculate the value
            for (int i = 0; i < dms.length; i++) {
                // If the template string consists of just RawMeasurement (ARG1)
                // then bypass the expression evaluation. Otherwise, evaluate.
                if (identRawIds[i] != null) {
                    res[i] = (MetricValue) dataMap.get(identRawIds[i]);
                    
                    if (res[i] == null) {
                        log.debug("Did not receive live value for " +
                                  identRawIds[i]);
                    }
                } else {
                    Double result = evaluateExpression(dms[i], dataMap);
                    res[i] = new MetricValue(result.doubleValue());
                }

                if (res[i] != null)
                    dataMan.addData(dms[i].getId(), res[i], true);
            }

            return res;
        } catch (FinderException e) {
            throw new MeasurementNotFoundException(
                StringUtil.arrayToString(mids), e);
        }
    }

    /**
     * Count of metrics enabled for a particular entity
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     */
    public int getEnabledMetricsCount(AuthzSubjectValue subject,
                                      AppdefEntityID id) {
        List mcol = 
            getDerivedMeasurementDAO().findByInstance(id.getType(), id.getID(),
                                                      true);
        return mcol.size();
    }

    /**
     * Look up a derived measurement value for a resource
     *
     * @return a DerivedMeasurement value
     * @ejb:interface-method
     */
    public DerivedMeasurement findMeasurement(Integer tid, Integer iid)
        throws MeasurementNotFoundException {
        DerivedMeasurement dm =
            getDerivedMeasurementDAO().findByTemplateForInstance(tid, iid);
            
        if (dm == null) {
            throw new MeasurementNotFoundException("No measurement found " +
                                                   "for " + iid + " with " +
                                                   "template " + tid);
        }
        return dm;
    }

    /**
     * Look up a derived measurement POJO
     *
     * @return a DerivedMeasurement value
     * @ejb:interface-method
     */
    public DerivedMeasurement findMeasurement(AuthzSubject subject,
                                              Integer tid, Integer iid)
        throws MeasurementNotFoundException {
        DerivedMeasurement dm = findMeasurement(tid, iid);
            
        if (dm == null) {
            throw new MeasurementNotFoundException("No measurement found " +
                                                   "for " + iid + " with " +
                                                   "template " + tid);
        }
        return dm;
    }

    /**
     * Look up a derived measurement POJO, allowing for the query to return a 
     * stale copy of the derived measurement (for efficiency reasons).
     *
     * @param subject The subject.
     * @param tid The template Id.
     * @param iid The instance Id.
     * @param allowStale <code>true</code> to allow stale copies of an alert 
     *                   definition in the query results; <code>false</code> to 
     *                   never allow stale copies, potentially always forcing a 
     *                   sync with the database.
     * @return a DerivedMeasurement value
     * @ejb:interface-method
     */
    public DerivedMeasurement findMeasurement(AuthzSubjectValue subject,
                                              Integer tid, 
                                              Integer iid,
                                              boolean allowStale)
        throws MeasurementNotFoundException {
        
        DerivedMeasurement dm = getDerivedMeasurementDAO()
            .findByTemplateForInstance(tid, iid, allowStale);
            
        if (dm == null) {
            throw new MeasurementNotFoundException("No measurement found " +
                                                   "for " + iid + " with " +
                                                   "template " + tid);
        }
        
        return dm;
    }
        
    /**
     * Look up a list of derived measurement EJBs for a template and instances
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     */
    public List findMeasurements(AuthzSubject subject, Integer tid,
                                 Integer[] ids) {
        ArrayList results = new ArrayList();
        for (int i = 0; i < ids.length; i++) {
            try {
                results.add(findMeasurement(subject, tid, ids[i])); 
            } catch (MeasurementNotFoundException e) {
                continue;
            }
        }
        return results;
    }

    /**
     * Look up a list of derived measurement EJBs for a template and instances
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     */
    public Integer[] findMeasurementIds(AuthzSubject subject, Integer tid,
                                        Integer[] ids) {
        List results =
            getDerivedMeasurementDAO().findIdsByTemplateForInstances(tid, ids); 
        return (Integer[]) results.toArray(new Integer[results.size()]);
    }

    private List sortMetrics(List mcol, PageControl pc) {
        // Clearly, assuming that we are sorting by name, in the future we may
        // need to pay attention to the PageControl passed in if we sort by
        // more attributes
        if (pc.getSortorder() == PageControl.SORT_DESC) {
            Collections.sort(mcol, new Comparator() {
                
                public int compare(Object arg0, Object arg1) {
                    DerivedMeasurement dm0 = (DerivedMeasurement) arg0;
                    DerivedMeasurement dm1 = (DerivedMeasurement) arg1;
                    return dm1.getTemplate().getName()
                        .compareTo(dm0.getTemplate().getName());
                }
                
            });
        }
        else {
            Collections.sort(mcol, new Comparator() {
                
                public int compare(Object arg0, Object arg1) {
                    DerivedMeasurement dm0 = (DerivedMeasurement) arg0;
                    DerivedMeasurement dm1 = (DerivedMeasurement) arg1;
                    return dm0.getTemplate().getName()
                        .compareTo(dm1.getTemplate().getName());
                }
                
            });
        }
        
        return mcol;
    }
    
    /**
     * Look up a list of derived measurement EJBs for a category
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     */
    public PageList findMeasurements(AuthzSubject subject,
                                     AppdefEntityID id, String cat,
                                     PageControl pc) {
        List mcol;
            
        // See if category is valid
        if (cat == null || Arrays.binarySearch(
            MeasurementConstants.VALID_CATEGORIES, cat) < 0) {
            mcol = getDerivedMeasurementDAO()
                .findByInstance(id.getType(), id.getID(), true);
        } else {
            mcol = getDerivedMeasurementDAO()
                .findByInstanceForCategory(id.getType(), id.getID(), true, cat);
        }

        // Need to order the metrics, as the HQL does not allow us to order
        mcol = sortMetrics(mcol, pc);
    
        return valuePager.seek(mcol, pc);
    }

    /**
     * Look up a list of enabled derived measurements for a category
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     */
    public List findEnabledMeasurements(AuthzSubjectValue subject,
                                        AppdefEntityID id, String cat) {
        List mcol;
            
        // See if category is valid
        if (cat == null || Arrays.binarySearch(
            MeasurementConstants.VALID_CATEGORIES, cat) < 0) {
            mcol = getDerivedMeasurementDAO().findByInstance(id.getType(),
                                                             id.getID(),
                                                             true);
        } else {
            mcol = getDerivedMeasurementDAO().
                findByInstanceForCategory(id.getType(), id.getID(),
                                          true, cat);
        }
        return mcol;
    }

    /**
     * Look up a list of designated measurement EJBs for an entity
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     */
    public List findDesignatedMeasurements(AppdefEntityID id) {
        return getDerivedMeasurementDAO().findDesignatedByInstance(id.getType(),
                                                                   id.getID());
    }

    /**
     * Look up a list of designated measurement EJBs for an entity for
     * a category
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     */
    public List findDesignatedMeasurements(AuthzSubject subject,
                                           AppdefEntityID id, String cat) {
        return getDerivedMeasurementDAO()
            .findDesignatedByInstanceForCategory(id.getType(), id.getID(), cat);
    }

    /**
     * Look up an availability measurement EJBs for an instance
     * @throws MeasurementNotFoundException
     * @ejb:interface-method
     */
    public DerivedMeasurement getAvailabilityMeasurement(AuthzSubject subject,
                                                         AppdefEntityID id)
        throws MeasurementNotFoundException
    {
        List mlocals = getDerivedMeasurementDAO().
            findDesignatedByInstanceForCategory(id.getType(), id.getID(),
            MeasurementConstants.CAT_AVAILABILITY);
        
        if (mlocals.size() == 0) {
            throw new MeasurementNotFoundException("No availability metric " +
                                                   "found for " + id);
        }

        return (DerivedMeasurement) mlocals.get(0);
    }

    /**
     * Look up a list of DerivedMeasurement objects by category
     *
     * @ejb:interface-method
     */
    public List findMeasurementsByCategory(String cat)
    {
        return getDerivedMeasurementDAO().findByCategory(cat);
    }

    /**
     * Look up a list of derived measurement EJBs for a category
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     */
    public Map findDesignatedMeasurementIds(AuthzSubject subject,
                                            AppdefEntityID[] ids, String cat)
        throws MeasurementNotFoundException {

        Map midMap = new HashMap();
        for (int i = 0; i < ids.length; i++) {
            AppdefEntityID id = ids[i];
            try {
                List metrics = getDerivedMeasurementDAO().
                    findDesignatedByInstanceForCategory(id.getType(),
                                                        id.getID(), cat);

                if (metrics.size() == 0)
                    throw new FinderException("No metrics found");
                
                DerivedMeasurement dm = null;
                if (metrics.size() > 1 &&
                    MeasurementConstants.CAT_AVAILABILITY.equals(cat)) {
                    // We'll check for the right template
                    for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                        DerivedMeasurement dmIt =(DerivedMeasurement) it.next();
                        if (dmIt.getTemplate().getAlias().
                            compareToIgnoreCase(
                            MeasurementConstants.CAT_AVAILABILITY) == 0) {
                            dm = dmIt;
                            break;
                        }
                    }
                }

                if (dm == null) {
                    // We'll take the first one
                    dm = (DerivedMeasurement) metrics.get(0);
                }

                midMap.put(id, dm.getId());
                
            } catch (FinderException e) {
                // Throw an exception if we're only looking for one measurement
                if (ids.length == 1)
                    throw new MeasurementNotFoundException(
                                                           cat + " metric for " + id + " not found");
            }
        }
        return midMap;
    }

    /**
     * Look up a list of derived metric intervals for template IDs.
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
        DerivedMeasurementDAO ddao = getDerivedMeasurementDAO();
        Map intervals = new HashMap(tids.length);
        Map derivedMeasToMetrics = ddao.findByInstance(aeids);

        for (Iterator it = derivedMeasToMetrics.entrySet().iterator();
            it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            List metrics = (List)entry.getValue();
            for (Iterator i = metrics.iterator(); i.hasNext();)
            {
                DerivedMeasurement dm = (DerivedMeasurement) i.next();
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
        
        List tidList = Arrays.asList(tids);
        // Copy the keys, since we are going to be modifying the interval map
        Set keys = new HashSet(intervals.keySet());
        for (Iterator i = keys.iterator(); i.hasNext();) {
            Integer templateId = (Integer) i.next();

            if (tidList.indexOf(templateId) == -1 || // Wasn't asked for
                disabled.equals(intervals.get(templateId))) { // Disabled
                // so don't return it
                intervals.remove(templateId);
            }
        }

        return intervals;
    }

    /**
     * Enable or Disable measurement in a new transaction.  We need to have the
     * transaction finalized before sending out messages
     * @ejb:transaction type="REQUIRESNEW"
     * @ejb:interface-method
     */
    public void enableMeasurement(DerivedMeasurement m, boolean enabled)
        throws MeasurementNotFoundException {
        m.setEnabled(enabled);
    }

    /**
     * Set the interval of Measurements based their ID's
     *
     * @ejb:interface-method
     */
    public void enableMeasurements(AuthzSubject subject, Integer[] mids,
                                   long interval)
        throws MeasurementNotFoundException, MeasurementCreateException,
               PermissionException {

        // Organize by AppdefEntity
        HashMap resMap = new HashMap();
        
        // Get the list of measurements
        Collection measurements =
            getDerivedMeasurementDAO().findByIds(mids);
        
        for (Iterator it = measurements.iterator(); it.hasNext(); ) {
            DerivedMeasurement dm = (DerivedMeasurement)it.next();
            if (dm.getTemplate().getMeasurementArgs() == null) {
                throw new MeasurementNotFoundException(dm.getId() + " is a " +
                                                       "raw measurement");
            }

            AppdefEntityID id = getAppdefEntityId(dm);

            HashSet tids;
            if (resMap.containsKey(id)) {
                tids = (HashSet)resMap.get(id);
            } else {
                // Authz check
                super.checkModifyPermission(subject.getId(), id);        
                tids = new HashSet();
                resMap.put(id, tids);
            }
                
            tids.add(dm.getTemplate().getId());
        }
        
        for (Iterator it = resMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            AppdefEntityID id = (AppdefEntityID) entry.getKey();
            HashSet mtidSet = (HashSet) entry.getValue();
            Integer[] mtids = (Integer[]) mtidSet.toArray(new Integer[0]);
            long[] intervals = new long[mtids.length];
            Arrays.fill(intervals, interval);

            // A little short-cut.  We just end up looking up the derived
            // measurement twice.
            try {
                createMeasurements(subject, id, mtids, intervals, null);
            } catch (TemplateNotFoundException e) {
                // This shouldn't happen as the measurement is already created
                throw new MeasurementNotFoundException("Template not found", e);
            }
        }
    }

    /**
     * Set the interval of Measurements based their template ID's
     *
     * @ejb:interface-method
     */
    public void enableMeasurements(AuthzSubject subject, AppdefEntityID[] aeids,
                                   Integer[] mtids, long interval)
        throws MeasurementNotFoundException, MeasurementCreateException,
               TemplateNotFoundException, PermissionException {
        
        long[] intervals = new long[mtids.length];
        Arrays.fill(intervals, interval);
        
        for (int i = 0; i < aeids.length; i++) {
            AppdefEntityID id = aeids[i];

            createMeasurements(subject, id, mtids, intervals, null);
        }
    } 

    /**
     * Disable all derived measurement EJBs for an instance
     *
     * @ejb:interface-method
     */
    public void disableMeasurements(AuthzSubject subject, AppdefEntityID id)
        throws PermissionException {
        // Authz check
        checkModifyPermission(subject.getId(), id);        

        List mcol = getDerivedMeasurementDAO().findByInstance(id.getType(),
                                                              id.getID());
        Integer[] mids = new Integer[mcol.size()];
        Iterator it = mcol.iterator();
        for (int i = 0; it.hasNext(); i++) {
            DerivedMeasurement dm = (DerivedMeasurement)it.next();
            try {
                // Call back into ourselves to force a new transaction to be
                // created.
                getDMManager().enableMeasurement(dm, false);
            } catch (MeasurementNotFoundException e) {
                // This is quite impossible, as we have just looked it up
                throw new SystemException(e);
            }
            mids[i] = dm.getId();
        }

        // Now unschedule the DerivedMeasurment
        unscheduleJobs(mids);
        sendAgentSchedule(id);
    }

    /**
     * Disable all derived measurements for an instance
     *
     * @ejb:interface-method
     */
    public void disableMeasurements(AuthzSubject subject, Integer[] mids)
        throws PermissionException, MeasurementNotFoundException {
        AppdefEntityID aid = null;
        for (int i = 0; i < mids.length; i++) {
            DerivedMeasurement m = 
                getDerivedMeasurementDAO().findById(mids[i]);

            if (m == null) {
                throw new MeasurementNotFoundException("Measurement id " +
                                                       mids[i] + " not " +
                                                       "found");
            }

            // Check removal permission
            if (aid == null) {
                aid = getAppdefEntityId(m);
                checkModifyPermission(subject.getId(), aid);
            }
            // Call back into ourselves to force a new transaction to be
            // created.
            getDMManager().enableMeasurement(m, false);
        }

        // Now unschedule the DerivedMeasurment
        unscheduleJobs(mids);
        sendAgentSchedule(aid);
    }

    /**
     * Disable measurements for an instance
     *
     * @ejb:interface-method
     */
    public void disableMeasurements(AuthzSubject subject, AppdefEntityID id,
                                    Integer[] tids)
        throws PermissionException {
        // Authz check
        checkModifyPermission(subject.getId(), id);        

        List mcol = getDerivedMeasurementDAO().findByInstance(id.getType(),
                                                              id.getID());
        HashSet tidSet = null;
        if (tids != null) {
            tidSet = new HashSet(Arrays.asList(tids));
        }            
        
        List toUnschedule = new ArrayList();
        for (Iterator it = mcol.iterator(); it.hasNext(); ) {
            DerivedMeasurement dm = (DerivedMeasurement)it.next();
            // Check to see if we need to remove this one
            if (tidSet != null && 
                !tidSet.contains(dm.getTemplate().getId()))
                    continue;
                
            try {
                enableMeasurement(dm, false);
            } catch (MeasurementNotFoundException e) {
                // This is quite impossible, we just looked it up
                throw new SystemException(e);
            }
            
            toUnschedule.add(dm.getId());
        }

        // Now unschedule the DerivedMeasurment
        unscheduleJobs((Integer[])toUnschedule.toArray(new Integer[0]));
        sendAgentSchedule(id);
    }

    
    private static String getMonitorableType(AuthzSubject subject,
                                             AppdefEntityID id)
        throws AppdefEntityNotFoundException, PermissionException 
    {
        if (id.isPlatform() || id.isServer() | id.isService()) {
            AppdefEntityValue av = new AppdefEntityValue(id, subject);
            return av.getMonitorableType();
        } 
        return null;
    }

    /**
     * Enable the default metrics for a resource.  This should only
     * be called by the {@link MeasurementEnabler}.  If you want the behavior
     * of this method, use the {@link MeasurementEnabler} 
     * @ejb:interface-method
     */
    public void enableDefaultMetrics(AuthzSubject subj, 
                                     AppdefEntityID id) 
        throws AppdefEntityNotFoundException, PermissionException 
    {
        RawMeasurementManagerLocal rawMan =
            RawMeasurementManagerEJBImpl.getOne();
        ConfigManagerLocal cfgMan = ConfigManagerEJBImpl.getOne();
        ConfigResponse config;
        String mtype;

        AuthzSubjectValue subject = subj.getAuthzSubjectValue();
        try {
            mtype = getMonitorableType(subj, id);
            // No monitorable type
            if (mtype == null) {
                return;
            }

            config = 
                cfgMan.getMergedConfigResponse(subject,
                                               ProductPlugin.TYPE_MEASUREMENT,
                                               id, true);
        } catch (ConfigFetchException e) {
            log.debug("Unable to enable default metrics for [" + id + "]", e);
            return;
        }  catch (Exception e) {
            log.error("Unable to enable default metrics for [" + id + "]", e);
            return;
        }

        // Check the configuration
        try {
            rawMan.checkConfiguration(subj, id, config);
        } catch (InvalidConfigException e) {
            log.warn("Error turning on default metrics, configuration (" +
                      config + ") " + "couldn't be validated", e);
            cfgMan.setValidationError(subject, id, e.getMessage());
            return;
        } catch (Exception e) {
            log.warn("Error turning on default metrics, " +
                      "error in validation", e);
            cfgMan.setValidationError(subject, id, e.getMessage());
            return;
        }

        // Enable the metrics
        try {
            createDefaultMeasurements(subj, id, mtype, config);
            cfgMan.clearValidationError(subject, id);

            // Execute the callback so other people can do things when the
            // metrics have been created (like create type-based alerts)
            MeasurementStartupListener.getDefaultEnableObj().metricsEnabled(id);
        } catch (Exception e) {
            log.warn("Unable to enable default metrics for id=" + id +
                      ": " + e.getMessage(), e);
        }
    }

    /**
     * @ejb:interface-method
     */
    public void reschedule(AppdefEntityID id) {
        String objName = "hyperic.jmx:type=Service,name=MeasurementSchedule";
        String meth = "refreshSchedule";

        MBeanServer server = MBeanUtil.getMBeanServer();

        try {
            ObjectName obj = new ObjectName(objName);
            server.invoke(obj, meth,
                          new Object[] { id },
                          new String[] { AppdefEntityID.class.getName() });
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * Get the list of DownMetricValues that represent the resources that are
     * currently down
     * 
     * @ejb:interface-method
     */
    public List getUnavailEntities() {
        List unavailMetrics =
            MetricDataCache.getInstance().getUnavailableMetrics();
        List unavailEntities = new ArrayList();
        DerivedMeasurementDAO dao = getDerivedMeasurementDAO();
        for (Iterator it = unavailMetrics.iterator(); it.hasNext(); ) {
            Element el = (Element) it.next();
            Integer mid = (Integer) el.getKey();
            MetricValue mv = (MetricValue) el.getValue();
            
            // Look up the metric for the appdef entity ID
            DerivedMeasurement dm = dao.findById(mid);
            unavailEntities.add(new DownMetricValue(dm.getEntityId(), mv));
        }
        return unavailEntities;
    }
    
    /**
     * @ejb:interface-method
     */
    public void preload() {
        getDerivedMeasurementDAO().findAllCollected();
        getMeasurementTemplateDAO().findAllUsed();
    }
    
    public static DerivedMeasurementManagerLocal getOne() {
        try {
            return DerivedMeasurementManagerUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {
        try {
            valuePager = Pager.getPager(VALUE_PROCESSOR);
        } catch (Exception e) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }

    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx){}
}
