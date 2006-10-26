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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.NoSuchEntityException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.measurement.EvaluationException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementCreateException;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.TemplateNotFoundException;
import org.hyperic.hq.measurement.ext.MetricOperationEvent;
import org.hyperic.hq.measurement.ext.depgraph.DerivedNode;
import org.hyperic.hq.measurement.ext.depgraph.Graph;
import org.hyperic.hq.measurement.ext.depgraph.GraphBuilder;
import org.hyperic.hq.measurement.ext.depgraph.InvalidGraphException;
import org.hyperic.hq.measurement.ext.depgraph.Node;
import org.hyperic.hq.measurement.ext.depgraph.RawNode;
import org.hyperic.hq.measurement.monitor.LiveMeasurementException;
import org.hyperic.hq.measurement.shared.BaselineLocal;
import org.hyperic.hq.measurement.shared.BaselineUtil;
import org.hyperic.hq.measurement.shared.BaselineValue;
import org.hyperic.hq.measurement.shared.DataManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerUtil;
import org.hyperic.hq.measurement.shared.DerivedMeasurementPK;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocal;
import org.hyperic.hq.measurement.shared.MeasurementProcessorUtil;
import org.hyperic.hq.measurement.shared.MeasurementTemplateLocal;
import org.hyperic.hq.measurement.shared.MeasurementTemplatePK;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.measurement.shared.MonitorableTypeValue;
import org.hyperic.hq.measurement.shared.RawMeasurementLocal;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocalHome;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerUtil;
import org.hyperic.hq.measurement.shared.RawMeasurementPK;
import org.hyperic.hq.measurement.shared.RawMeasurementValue;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
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
 * 
 * Note: Don't use primary key for lookup, since DerivedMeasurement and
 * RawMeasurement share the same table, and EJB doesn't know which is which.
 * Use findById() instead.
 */
public class DerivedMeasurementManagerEJBImpl extends SessionEJB
    implements SessionBean {
    private final Log log = LogFactory.getLog(
        "org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl");

    protected final String VALUE_PROCESSOR =
        PagerProcessor_measurement.class.getName();
     
    private Pager valuePager = null;

    private DerivedMeasurementManagerLocal dmMan = null;
    private DerivedMeasurementManagerLocal getDMManager() {
        try {
            if (dmMan == null) {
                dmMan =
                    DerivedMeasurementManagerUtil.getLocalHome().create();
            }
    
            return dmMan;
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    private RawMeasurementManagerLocal rmMan = null;
    private RawMeasurementManagerLocal getRmMan() {
        try {
            if (rmMan == null) {
                RawMeasurementManagerLocalHome rmHome =
                    RawMeasurementManagerUtil.getLocalHome();
                rmMan = rmHome.create();
            }
            return rmMan;
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    private MeasurementProcessorLocal mproc = null;
    private MeasurementProcessorLocal getMeasurementProcessor() {
        try {
            if (mproc == null) 
                mproc = MeasurementProcessorUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }

        return mproc;
    }

    private DerivedMeasurementValue updateMeasurementInterval(Integer tid,
                                                              Integer iid,
                                                              long interval)
        throws FinderException {
        long current = System.currentTimeMillis();
        
        DerivedMeasurementLocal dmLoc = this.getDmHome().
            findByTemplateForInstance(tid, iid);
        dmLoc.setInterval(interval);
        dmLoc.setEnabled(interval != 0);
        dmLoc.setMtime(current);
        
        /* XXX
         * Manually update the derived measurement value object, because
         * the CMP does not appear to expire the cache when setting the local
         * object.
         */
        DerivedMeasurementValue dmv = dmLoc.getDerivedMeasurementValue();
        dmv.setInterval(interval);
        dmv.setEnabled(interval != 0);
        dmv.setMtime(current);
        
        DMValueCache.getInstance().put(dmv);
        
        return dmv;
    }

    private Integer getIdByTemplateAndInstance(Integer tid,
                                               Integer instanceId)
        throws FinderException {
        DerivedMeasurementLocal dmLoc = getDmHome().
            findByTemplateForInstance(tid, instanceId);
        return ((DerivedMeasurementPK) dmLoc.getPrimaryKey()).getId();
    }

    private Integer getRawIdByTemplateAndInstance(Integer tid, Integer iid) {
        try {
            RawMeasurementLocal rawLoc = getRmHome().
                findByTemplateForInstance(tid, iid);
            return ((RawMeasurementPK)rawLoc.getPrimaryKey()).getId();
        } catch (FinderException fe) {
            return null;
        }
    }

    // Create RM or blow chunks
    private RawMeasurementValue createRawMeasurement(Integer instanceId, 
                                                     Integer templateId,
                                                     ConfigResponse props)
        throws MeasurementCreateException {
        return getRmMan().createMeasurement(templateId, instanceId, props);
    }

    // Create DM or blow chunks.
    private DerivedMeasurementValue createDerivedMeasurement(
        AppdefEntityID id, MeasurementTemplateValue mtVal, long interval)
        throws MeasurementCreateException, TemplateNotFoundException {
            
        DerivedMeasurementValue retVal              = null;
        MonitorableTypeValue    monTypeVal          = null;
        Integer                 instanceId          = id.getId();
        
        monTypeVal = mtVal.getMonitorableType();
        if(monTypeVal.getAppdefType() != id.getType() )
            throw new MeasurementCreateException(
                "Appdef entity (" + id + ")/template type (ID: " +
                mtVal.getId() + ") mismatch");

        try {
            MeasurementTemplateLocal mtLoc = getMtHome().findByPrimaryKey
                ( new MeasurementTemplatePK( mtVal.getId() ) );
            
            DerivedMeasurementLocal dmLoc =
                getDmHome().create(mtLoc, instanceId, interval);
            
            if (interval == 0)
                dmLoc.setEnabled(false);
            
            retVal = dmLoc.getDerivedMeasurementValue();
            
            DMValueCache.getInstance().put(retVal);
            
            return retVal;
        } catch (FinderException e) {
            throw new TemplateNotFoundException(mtVal.getId(), e);
        } catch (CreateException e) {
            throw new MeasurementCreateException(
                "createDerivedMeasurement() failed", e);
        }
    }

    /**
     * Look up a derived measurement's appdef entity ID
     */
    private AppdefEntityID getAppdefEntityId(DerivedMeasurementLocal dm) {
        AppdefEntityID aid =
            new AppdefEntityID(dm.getAppdefType(), dm.getInstanceId());
        
        return aid;
    }

    private void sendAgentSchedule(AppdefEntityID aid) {
        if (aid != null) {
            Messenger sender = new Messenger();
            sender.sendMessage(MeasurementConstants.SCHEDULE_QUEUE, aid);
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
     * @param interval    Millisecond interval that the measurement is polled
     * @param protoProps  Configuration data for the instance
     *
     * @return a List of the associated DerivedMeasurementValue objects
     * @ejb:transaction type="REQUIRESNEW"
     * @ejb:interface-method
     */
    public List createMeasurements(AppdefEntityID id, Integer[] templates,
                                   long[] intervals, ConfigResponse props)
        throws MeasurementCreateException, TemplateNotFoundException {
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
                
                if ( log.isDebugEnabled() ) {
                    // a gString may be expensive, make sure debug is enabled
                    String gString = graphs[i].toString();
                    log.debug(gString);
                }

                DerivedNode derivedNode = (DerivedNode)
                    graphs[i].getNode( dTemplateId.intValue() );
                MeasurementTemplateValue derivedTemplateValue =
                    derivedNode.getMeasurementTemplateValue();

                // we will fill this variable with the actual derived 
                // measurement that is being enabled
                DerivedMeasurementValue argDmVal = null;
    
                // first handle simple IDENTITY derived case
                if ( MeasurementConstants.TEMPL_IDENTITY.equals(
                        derivedTemplateValue.getTemplate() ) ) {
                    RawNode rawNode = (RawNode)
                        derivedNode.getOutgoing().iterator().next();
                    MeasurementTemplateValue rawTemplateValue =
                        rawNode.getMeasurementTemplateValue();

                    // Check the raw node
                    Integer rmId =
                        getRawIdByTemplateAndInstance(rawTemplateValue.getId(),
                                                      instanceId);
                    if (rmId == null) {
                        if (props == null) {
                            // No properties, go on to the next template
                            continue;
                        }

                        RawMeasurementValue rmVal =
                            createRawMeasurement(instanceId,
                                                 rawTemplateValue.getId(),
                                                 props);
                        rmId = rmVal.getId();
                    }
                    
                    // if no DM already exists, then we need to create the
                    // raw and derived and make note to schedule raw
                    DerivedMeasurementValue dmVal;
                    try {
                        dmVal = this.updateMeasurementInterval(
                            dTemplateId, instanceId, interval);
                    } catch (FinderException e) {
                        dmVal =
                            createDerivedMeasurement(id, derivedTemplateValue,
                                                     interval);
                    }

                    argDmVal = dmVal;
                } else {
                    // we're not an identity DM template, so we need
                    // to make sure that measurements are enabled for
                    // the whole graph
                    for (Iterator graphNodes = graphs[i].getNodes().iterator();
                         graphNodes.hasNext();) {
                        Node node = (Node)graphNodes.next();
                        MeasurementTemplateValue templArg =
                            node.getMeasurementTemplateValue();
    
                        if (node instanceof DerivedNode) {
                            DerivedMeasurementValue dmVal;
                            try {
                                dmVal = this.updateMeasurementInterval(
                                        templArg.getId(), instanceId, interval);
                            } catch (FinderException e) {
                                dmVal = createDerivedMeasurement(id, templArg,
                                                                 interval);
                            }

                            if ( dTemplateId.equals( templArg.getId() ) ) {
                                argDmVal = dmVal;
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
                        } // end if template arg is DM else RM
                    } // end while template args has next
                } // else not an identity passthroughs

                dmList.add(argDmVal);
            } // for each DM template arg
        } catch (InvalidGraphException e) {
            throw new MeasurementCreateException("InvalidGraphException:", e);
        }
        return dmList;
    } // end create

    /**
     * @ejb:interface-method
     */
    public List createMeasurements(AuthzSubjectValue subject, 
                                   AppdefEntityID id, Integer[] templates,
                                   long[] intervals, ConfigResponse props)
        throws PermissionException, MeasurementCreateException,
               TemplateNotFoundException {
        // Authz check
        super.checkModifyPermission(subject, id);        

        List dmList = this.getDMManager().createMeasurements(
            id, templates, intervals, props);
        
        // Make sure that the cache does not have outdated derived measurements
        DMValueCache cache = DMValueCache.getInstance();
        for (Iterator it = dmList.iterator(); it.hasNext();) {
            DerivedMeasurementValue dmv = (DerivedMeasurementValue) it.next();
            cache.put(dmv);
        }
        
        this.sendAgentSchedule(id);
        return dmList;
    }

    /**
     * Create Measurement objects based their templates and default intervals
     *
     * @param templates   List of Integer template IDs to add
     * @param id          instance ID (appdef resource) the templates are for
     * @param protoProps  Configuration data for the instance
     *
     * @return a List of the associated DerivedMeasurementValue objects
     * @ejb:interface-method
     */
    public List createMeasurements(AuthzSubjectValue subject, 
                                   AppdefEntityID id, Integer[] templates,
                                   ConfigResponse props)
        throws TemplateNotFoundException, PermissionException,
               MeasurementCreateException {
        long[] intervals = new long[templates.length];
        for (int i = 0; i < templates.length; i++) {
            try {
                MeasurementTemplateLocal tmpl =
                    this.getMtHome().findByPrimaryKey(
                        new MeasurementTemplatePK(templates[i]));
                intervals[i] = tmpl.getDefaultInterval();
            } catch (FinderException e) {
                throw new TemplateNotFoundException(templates[i]);
            }
        }
        
        return this.createMeasurements(subject, id, templates, intervals,props);
    }

    /**
     * Create Measurement objects for an appdef entity based on default
     * templates.  This method will only create them if there currently no
     * metrics enabled for the appdef entity.
     *
     * @param subject     Spider subject
     * @param id          appdef entity ID of the resource
     * @param mtyp        The string name of the plugin type
     * @param props       Configuration data for the instance
     *
     * @return a List of the associated DerivedMeasurementValue objects
     * @ejb:interface-method
     */
    public List createDefaultMeasurements(AuthzSubjectValue subject, 
                                          AppdefEntityID id,
                                          String mtype,
                                          ConfigResponse props)
        throws TemplateNotFoundException, PermissionException,
               MeasurementCreateException {
        // We're going to make sure there aren't metrics already
        List dms =
            this.findMeasurements(subject, id, null, PageControl.PAGE_ALL);
        if (dms.size() != 0) {
            return dms;
        }

        // Find the templates
        try {
            Collection mts =
                this.getMtHome().findDerivedByMonitorableType(mtype);

            Integer[] tids = new Integer[mts.size()];
            long[] intervals = new long[mts.size()];
            
            Iterator it = mts.iterator();
            for (int i = 0; it.hasNext(); i++) {
                MeasurementTemplateLocal tmpl =
                    (MeasurementTemplateLocal) it.next();
                tids[i] =
                    ((MeasurementTemplatePK) tmpl.getPrimaryKey()).getId();
                
                if (tmpl.getDefaultOn())
                    intervals[i] = tmpl.getDefaultInterval();
                else
                    intervals[i] = 0;
            }

            return this.createMeasurements(subject, id, tids, intervals, props);
        } catch (FinderException e) {
            // No measurements to enable, return empty list
            log.debug("No default metrics to enable for " + id);
            return new ArrayList(0);
        }
    }
    
    /**
     * Create Measurement object based on its template
     *
     * @param template    Integer template ID to add
     * @param id          Appdef ID the templates are for
     * @param interval    Millisecond interval that the measurement is polled
     * @param protoProps  Configuration data for the instance
     *
     * @return an associated DerivedMeasurementValue object
     * @ejb:interface-method
     */
    public DerivedMeasurementValue createMeasurement(AuthzSubjectValue subject,
                                                     Integer template,
                                                     AppdefEntityID id,
                                                     long interval,
                                                     ConfigResponse protoProps)
        throws PermissionException, TemplateNotFoundException,
               MeasurementCreateException {
        // Authz check
        super.checkModifyPermission(subject, id);

        List dmvs = this.createMeasurements(subject, id,
                                            new Integer[] { template },
                                            new long[]    { interval },
                                            protoProps);
        
        return (DerivedMeasurementValue) dmvs.get(0);
    }

    /**
     * Update the derived measurements of a resource
     * @ejb:interface-method
     */
    public void updateMeasurements(AuthzSubjectValue subject,
                                   AppdefEntityID id, ConfigResponse props)
        throws PermissionException, MeasurementCreateException {
        try {
            // Update all of the raw measurements first
            getRmMan().updateMeasurements(id, props);
            
            // Now see which derived measurements need to be rescheduled
            List mcol = getDmHome().findByInstance(id.getType(), id.getID());
            
            for (Iterator i = mcol.iterator(); i.hasNext(); ) {
                DerivedMeasurementLocal dmLocal =
                    (DerivedMeasurementLocal) i.next();

                if (dmLocal.getEnabled()) {
                    // A little short-cut.  We just end up looking up the
                    // derived measurement twice.
                    createMeasurement(subject, ((MeasurementTemplatePK)
                        dmLocal.getTemplate().getPrimaryKey()).getId(),
                        id, dmLocal.getInterval(), props);
                }
            }
        } catch (FinderException e) {
            // No measurements to update
        } catch (TemplateNotFoundException e) {
            // Would not happen since we're creating measurements with the
            // template that we just looked up
            this.log.error(e);
        }
    }

    /**
     * Update a derived measurement
     *
     * @return a DerivedMeasurement value
     * @ejb:interface-method
     */
    public DerivedMeasurementValue updateMeasurement(
        AuthzSubjectValue subject, DerivedMeasurementValue dmValue)
        throws MeasurementNotFoundException {
        try {
            DerivedMeasurementLocal dm = getDmHome().findById(dmValue.getId());
            
            // make sure that baseline's updated as well
            if (dmValue.getBaseline() != null) {
                BaselineValue bval = dmValue.getBaseline();
                
                // Make sure the baseline has the right measurement ID
                bval.setMeasurementId(dm.getId());
                
                BaselineLocal blocal = dm.getBaseline();
                
                if (blocal == null) {
                    try {
                        // Have to create a baseline
                        blocal = BaselineUtil.getLocalHome().create();
                        dm.setBaseline(blocal);
                    } catch (CreateException e) {
                        throw new SystemException(e);
                    } catch (NamingException e) {
                        throw new SystemException(e);
                    }
                }
                
                blocal.setBaselineValue(dmValue.getBaseline());
                dmValue.setBaseline(blocal.getBaselineValue());
            }
            else {
                dm.setBaseline(null);
            }

            // now saved the value
            dm.setDerivedMeasurementValue(dmValue);
            dmValue = dm.getDerivedMeasurementValue();

            // Must also update DMV in cache
            DMValueCache.getInstance().put(dmValue);

            return dmValue;
        } catch (FinderException e) {
            throw new MeasurementNotFoundException(dmValue.getId(), e);
        }
    }

    /**
     * @param mids
     */
    private void sendRemovedMetricsEvent(Integer[] mids) {
        // Now send a message that we've deleted the metrics
        Messenger sender = new Messenger();
        MetricOperationEvent event =
            new MetricOperationEvent(MetricOperationEvent.ACTION_DELETE,
                                     mids);
        sender.publishMessage(EventConstants.EVENTS_TOPIC, event);
    }

    /**
     * Remove a measurement 
     *
     * @ejb:interface-method
     */
    public void removeMeasurement(AuthzSubjectValue subject, Integer mid)
        throws RemoveException, PermissionException {
        // Try DerivedMeasurement
        try {
            DerivedMeasurementLocal m = getDmHome().findById(mid);
            
            // Check removal permission
            AppdefEntityID aid = this.getAppdefEntityId(m);
            super.checkDeletePermission(subject, aid);
            
            Integer[] mids = new Integer[] { mid };
            
            // Remove the measurement        
            m.remove();

            // Now unschedule the DerivedMeasurment
            this.unscheduleJobs(mids);
            this.sendAgentSchedule(aid);
            
            sendRemovedMetricsEvent(mids);
        } catch (FinderException e) {
            // Not a problem
            log.debug("FinderException", e);
        }
    }

    /**
     * Remove all measurements for an instance
     *
     * @ejb:interface-method
     */
    public void removeMeasurements(AuthzSubjectValue subject,
                                   AppdefEntityID id, Integer[] tids)
        throws RemoveException, PermissionException {
        // Authz check
        super.checkDeletePermission(subject, id);
        
        try {
            // First find them, then delete them
            List mcol = getDmHome().findByInstance(id.getType(), id.getID());

            HashSet tidSet = null;
            if (tids != null) {
                tidSet = new HashSet(Arrays.asList(tids));
            }
            
            List toUnschedule = new ArrayList();
            for (Iterator it = mcol.iterator(); it.hasNext(); ) {
                DerivedMeasurementLocal m = (DerivedMeasurementLocal) it.next();

                // Check to see if we need to remove this one
                if (tidSet != null && !tidSet.contains(((MeasurementTemplatePK)
                        m.getTemplate().getPrimaryKey()).getId()))
                    continue;

                Integer mid =
                    ((DerivedMeasurementPK) m.getPrimaryKey()).getId();
                
                toUnschedule.add(mid);
                m.remove();
            }

            // Now unschedule the DerivedMeasurments
            this.unscheduleJobs(
                (Integer[]) toUnschedule.toArray(new Integer[0]));
            
            // Check to see if this is a total removal operation
            if (tids == null) {
                // Skip the middleman (agent schedule synchronizer)
                this.getMeasurementProcessor().unschedule(id);
            }
            else {
                this.sendAgentSchedule(id);
            }
            
            // Send metrics removed event
            Integer[] mids = (Integer[])
                toUnschedule.toArray(new Integer[toUnschedule.size()]);
            sendRemovedMetricsEvent(mids);
        } catch (FinderException e) {
            // Not a problem
            log.debug("FinderException", e);
        } catch (MeasurementUnscheduleException e) {
            log.error("Error unscheduling metrics for entity: " + id, e);
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
        throws RemoveException, PermissionException {
        
        List toUnschedule = new ArrayList();

        for (int i = 0; i < entIds.length; i++) {
            // Authz check
            super.checkDeletePermission(subject, entIds[i]);
            
            try {
                // First find them, then delete them
                List mcol = getDmHome().findByInstance(entIds[i].getType(),
                                                       entIds[i].getID());

                for (Iterator it = mcol.iterator(); it.hasNext(); ) {
                    DerivedMeasurementLocal m =
                        (DerivedMeasurementLocal) it.next();

                    Integer mid =
                        ((DerivedMeasurementPK) m.getPrimaryKey()).getId();

                    toUnschedule.add(mid);
                    m.remove();
                }
            } catch (FinderException e) {
                // Not a problem
                log.debug("FinderException", e);
            }
        }

        // Now unschedule the DerivedMeasurments
        Integer[] mids = (Integer[])
            toUnschedule.toArray(new Integer[toUnschedule.size()]);
        this.unscheduleJobs(mids);

        // send queue message to unschedule
        UnScheduleArgs unschBean = new UnScheduleArgs(agentEnt, entIds);
        Messenger msg = new Messenger();
        log.info("Sending unschedule message to SCHEDULE_QUEUE: " + unschBean);
        msg.sendMessage(MeasurementConstants.SCHEDULE_QUEUE, unschBean);

        sendRemovedMetricsEvent(mids);
    }

    /**
     * Remove and unschedule specific measurements
     *
     * @ejb:interface-method
     */
    public void removeMeasurements(AuthzSubjectValue subject, Integer[] mids)
        throws RemoveException, PermissionException {
        try {
            AppdefEntityID aid = null;
            for (int i = 0; i < mids.length; i++) {
                DerivedMeasurementLocal m = this.getDmHome().findById(mids[i]);

                // Check removal permission
                if (aid == null) {
                    aid = this.getAppdefEntityId(m);
                    super.checkDeletePermission(subject, aid);
                }

                m.remove();
            }

            // Now unschedule the DerivedMeasurments
            this.unscheduleJobs(mids);
            this.sendAgentSchedule(aid);
            
            sendRemovedMetricsEvent(mids);
        } catch (FinderException e) {
            // Not a problem
            log.debug("FinderException", e);
        }
    }

    /** 
     * Look up a derived measurement for an instance
     * and an alias.
     *
     * @return a DerivedMeasurement value
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public DerivedMeasurementValue getMeasurement(AuthzSubjectValue subject,
                                                  AppdefEntityID id,
                                                  String alias)
        throws MeasurementNotFoundException {
        try {
            DerivedMeasurementLocal meas =
                this.getDmHome().findByAliasAndID(alias, id.getType(),
                                                  id.getID());
            
            DMValueCache cache = DMValueCache.getInstance();
            DerivedMeasurementValue dmv = cache.get(meas.getId());
            if (dmv == null) {
                dmv = meas.getDerivedMeasurementValue();
                cache.put(dmv);
            }
            return dmv;
        } catch (FinderException e) {
            throw new MeasurementNotFoundException(
                alias + " for " + id + " not found.", e);
        }
    }

    /**
     * Look up a derived measurement EJB
     *
     * @return a DerivedMeasurement value
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public DerivedMeasurementValue getMeasurement(Integer mid)
        throws MeasurementNotFoundException {
        DMValueCache cache = DMValueCache.getInstance();
        DerivedMeasurementValue dmv = cache.get(mid);
        if (dmv == null) {
            try {
                DerivedMeasurementLocal dm = getDmHome().findById(mid);
                dmv = dm.getDerivedMeasurementValue();
                cache.put(dmv);
            } catch (FinderException e) {
                throw new MeasurementNotFoundException(mid, e);
            }
        }
        return dmv;
    }

    /**
     * Get the live measurement values for a given resource.
     * @param id The id of the resource
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public void getLiveMeasurementValues(AuthzSubjectValue subject,
                                         AppdefEntityID id)
        throws EvaluationException, PermissionException,
               LiveMeasurementException, MeasurementNotFoundException
    {
        try {
            List mcol = getDmHome().findByInstance(id.getType(), id.getID(),
                                                   true);
            Integer[] mids = new Integer[mcol.size()];
            Integer availMeasurement = null; // For insert of AVAIL down
            Iterator it = mcol.iterator();

            for (int i = 0; it.hasNext(); i++) {
                DerivedMeasurementLocal dm =
                    (DerivedMeasurementLocal) it.next();
                DerivedMeasurementPK pk =
                    (DerivedMeasurementPK) dm.getPrimaryKey(); 
                mids[i] = pk.getId();
                
                MeasurementTemplateLocal template = dm.getTemplate();
                if (template.getAlias().equals(Metric.ATTR_AVAIL)) {
                    availMeasurement = pk.getId();
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
        } catch (FinderException e) {
            // Shouldn't happen
            log.error("Unable to find measurements for: " + id, e);
        }
    }

    /**
     * Get the live measurement value - assumes all measurement ID's share
     * the same agent connection
     * @param mtype the name of the monitorable type
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public MetricValue[] getLiveMeasurementValues(
        AuthzSubjectValue subject, Integer[] mids)
        throws EvaluationException, PermissionException,
               LiveMeasurementException, MeasurementNotFoundException {
        try {
            DataManagerLocal dataMan = getDataMan();

            DerivedMeasurementValue[] dms =
                new DerivedMeasurementValue[mids.length];
            Integer[] identRawIds = new Integer[mids.length];
            Arrays.fill(identRawIds, null);
            
            HashSet rawIdSet = new HashSet();
            HashSet derIdSet = new HashSet();
            for (int i = 0; i < mids.length; i++) {
                // First, find the derived measurement
                dms[i] = this.getMeasurement(mids[i]);
                
                if (!dms[i].getEnabled())
                    throw new LiveMeasurementException(
                        "Metric ID: " + mids[i] + " is not currently enabled");
                
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
        try {
            List mcol = getDmHome().findByInstance(id.getType(), id.getID(),
                                                   true);
            return mcol.size();
        } catch (FinderException e) {
            return 0;
        }
    }

    /**
     * Look up a derived measurement EJB
     *
     * @return a DerivedMeasurement value
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public DerivedMeasurementValue findMeasurement(AuthzSubjectValue subject,
                                                   Integer tid, Integer iid)
        throws MeasurementNotFoundException {
        try {
            DerivedMeasurementLocal dm =
                getDmHome().findByTemplateForInstance(tid, iid);
            
            DMValueCache cache = DMValueCache.getInstance();
            DerivedMeasurementValue dmv = cache.get(dm.getId());
            if (dmv == null) {
                // Save it in cache
                dmv = dm.getDerivedMeasurementValue();
                cache.put(dmv);
            }
            
            return dmv;
        } catch (FinderException e) {
            throw new MeasurementNotFoundException(
                "No measurement found for " + iid + " with template " + tid);
        }
    }

    /**
     * Look up a list of derived measurement EJBs for a template and instances
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public List findMeasurements(AuthzSubjectValue subject, Integer tid,
                                 Integer[] ids) {
        ArrayList results = new ArrayList();
        for (int i = 0; i < ids.length; i++) {
            try {
                results.add(this.findMeasurement(subject, tid, ids[i])); 
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
     * @ejb:transaction type="SUPPORTS"
     */
    public Integer[] findMeasurementIds(AuthzSubjectValue subject, Integer tid,
                                        Integer[] ids) {
        ArrayList results = new ArrayList();
        for (int i = 0; i < ids.length; i++) {
            try {
                results.add(this.getIdByTemplateAndInstance(tid, ids[i]));
            } catch (FinderException e) {
                continue;
            }
        }
        return (Integer[]) results.toArray(new Integer[0]);
    }

    /**
     * Look up a list of derived measurement EJBs for a category
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public PageList findMeasurements(AuthzSubjectValue subject,
                                     AppdefEntityID id, String cat,
                                     PageControl pc) {
        try {
            List mcol;
            
            // See if category is valid
            if (cat == null || Arrays.binarySearch(
                MeasurementConstants.VALID_CATEGORIES, cat) < 0) {
                mcol = getDmHome().findByInstance(id.getType(), id.getID(),
                                                  true);
            }
            else {
                mcol = getDmHome().findByInstanceForCategory(id.getType(), 
                                                             id.getID(), true,
                                                             cat);
            }
                
            if (pc.getSortorder() == PageControl.SORT_DESC)
                Collections.reverse(mcol);
    
            return valuePager.seek(mcol, pc);
        } catch (FinderException e) {
            // Not a problem
            log.debug("FinderException", e);
            return new PageList();
        }
    }

    /**
     * Look up a list of derived measurement EJBs for a category
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public PageList findMeasurements(AuthzSubjectValue subject,
                                     AppdefEntityID id, boolean enabled,
                                     String cat, PageControl pc) {
        try {
            List mcol;
            
            // See if category is valid
            if (cat == null || Arrays.binarySearch(
                MeasurementConstants.VALID_CATEGORIES, cat) < 0) {
                mcol = getDmHome().findByInstance(id.getType(), id.getID(),
                                                  enabled);
            }
            else {
                mcol = getDmHome().findByInstanceForCategory(id.getType(), 
                                                             id.getID(),
                                                             enabled, cat);
            }
            
            if (pc.getSortorder() == PageControl.SORT_DESC)
                Collections.reverse(mcol);
    
            return valuePager.seek(mcol, pc);
        } catch (FinderException e) {
            // Not a problem
            log.debug("FinderException", e);
            return new PageList();
        }
    }

    /**
     * Look up a list of designated measurement EJBs for an entity
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public List findDesignatedMeasurements(AuthzSubjectValue subject,
                                           AppdefEntityID id) {
        try {
            List mlocals = getDmHome().findDesignatedByInstance(id.getType(),
                                                                id.getID());            
            return this.valuePager.seek(mlocals, PageControl.PAGE_ALL);
        } catch (FinderException e) {
            // Not a problem
            return new ArrayList();
        }
    }

    /**
     * Look up a list of designated measurement EJBs for an entity for
     * a category
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public List findDesignatedMeasurements(AuthzSubjectValue subject,
                                           AppdefEntityID id,
                                           String cat) {
        try {
            List mlocals =
                getDmHome().findDesignatedByInstanceForCategory(
                    id.getType(), id.getID(), cat);            
            return this.valuePager.seek(mlocals, PageControl.PAGE_ALL);
        } catch (FinderException e) {
            // Not a problem
            return new ArrayList();
        }
    }

    /**
     * Look up an availability measurement EJBs for an instance
     * @throws MeasurementNotFoundException
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public DerivedMeasurementValue getAvailabilityMeasurement(
            AuthzSubjectValue subject, AppdefEntityID id)
        throws MeasurementNotFoundException {
        try {
            List mlocals = this.getDmHome().findDesignatedByInstanceForCategory(
                id.getType(), id.getID(),
                MeasurementConstants.CAT_AVAILABILITY);
            
            // We know there is only want one metric
            for (Iterator it = mlocals.iterator(); it.hasNext(); ) {
                DerivedMeasurementLocal dm =
                    (DerivedMeasurementLocal) it.next();
                // We'll check for the right template
                if (dm.getTemplate().getAlias().equalsIgnoreCase(
                        MeasurementConstants.CAT_AVAILABILITY))
                    return dm.getDerivedMeasurementValue();
            }
            
            throw new FinderException("No metric found");
        } catch (FinderException e) {
            throw new MeasurementNotFoundException(
                "No availability metric found for " + id);
        }
    }

    /**
     * Look up a list of derived measurement EJBs for a category
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public Map findDesignatedMeasurementIds(AuthzSubjectValue subject,
                                            AppdefEntityID[] ids, String cat)
        throws MeasurementNotFoundException {
        Map midMap = new HashMap();
        for (int i = 0; i < ids.length; i++) {
            AppdefEntityID id = ids[i];
            try {
                List metrics =
                    this.getDmHome().findDesignatedByInstanceForCategory(
                        id.getType(), id.getID(), cat);

                if (metrics.size() == 0)
                    throw new FinderException("No metrics found");
                
                DerivedMeasurementLocal dm = null;
                if (metrics.size() > 1 &&
                    MeasurementConstants.CAT_AVAILABILITY.equals(cat)) {
                    // We'll check for the right template
                    for (Iterator it = metrics.iterator(); it.hasNext(); ) {
                        DerivedMeasurementLocal dmIt =
                            (DerivedMeasurementLocal) it.next();
                        if (dmIt.getTemplate().getAlias().compareToIgnoreCase(
                                MeasurementConstants.CAT_AVAILABILITY) == 0) {
                            dm = dmIt;
                            break;
                        }
                    }
                }

                if (dm == null)
                    // We'll take the first one
                    dm = (DerivedMeasurementLocal) metrics.get(0);

                midMap.put(id,
                           ((DerivedMeasurementPK) dm.getPrimaryKey()).getId());
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
     * Look up a list of derived measurement EJBs for a category.  Used by
     * the AvailabilityCheckService
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public List findEnabledMeasurements(String cat) {
        try {
            List mlocals = getDmHome().findEnabledForCategory(cat);
            return this.valuePager.seek(mlocals, PageControl.PAGE_ALL);
        } catch (FinderException e) {
            // Not a problem
            return new ArrayList();
        }
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
     * @ejb:transaction type="SUPPORTS"
     */
    public Map findMetricIntervals(AuthzSubjectValue subject,
                                   Integer[] eids, Integer[] tids) {
        HashMap intervals = new HashMap(tids.length);
        for (int a = 0; a < eids.length; a++) {
            for (int i = 0; i < tids.length; i++) {
                try {
                    DerivedMeasurementLocal dmLocal = getDmHome()
                        .findByTemplateForInstance(tids[i], eids[a]);
                    
                    if (intervals.containsKey(tids[i])) {
                        // Compare with existing value
                        if (!intervals.get(tids[i]).equals(
                                new Long(dmLocal.getInterval())))
                            intervals.put(tids[i], new Long(0));
                    }
                    else {
                        // There's no point if measurement is disabled
                        if (!dmLocal.getEnabled()) {
                            continue;
                        }

                        // Not the first one, so there must have been others
                        // that were disabled
                        if (a > 0)
                            intervals.put(tids[i], new Long(0));
                        else
                            intervals.put(tids[i],
                                          new Long(dmLocal.getInterval()));
                    }
                } catch (FinderException e) {
                    // Not a problem
                    continue;
                }
            }
        }
        return intervals;
    }

    /**
     * Enable or Disable measurement in a new transaction.  We need to have the
     * transaction finalized before sending out messages
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRESNEW"
     */
    public void enableMeasurement(DerivedMeasurementPK pk, boolean enabled)
        throws MeasurementNotFoundException {
        try {
            DerivedMeasurementLocal dm = this.getDmHome().findByPrimaryKey(pk);
            dm.setEnabled(enabled);
    
            // Must also update DMV in cache
            DMValueCache cache = DMValueCache.getInstance();
            DerivedMeasurementValue dmv = cache.get(pk.getId());
            if (dmv != null)
                dmv.setEnabled(enabled);
        } catch (FinderException e) {
            throw new MeasurementNotFoundException(
                "No measurement found for primary key: " + pk);
        }
    }

    /**
     * Set the interval of Measurements based their ID's
     *
     * @param templates   List of Integer template IDs to add
     * @param id          instance ID (appdef resource) the templates are for
     * @param interval    Millisecond interval that the measurement is polled
     * @param protoProps  Configuration data for the instance
     *
     * @return a List of the associated DerivedMeasurementValue objects
     * @ejb:interface-method
     */
    public void enableMeasurements(AuthzSubjectValue subject,
                                   Integer[] mids, long interval)
        throws MeasurementNotFoundException, MeasurementCreateException,
               PermissionException {

        // Organize by AppdefEntity
        HashMap resMap = new HashMap();
        
        // Get the list of measurements
        Collection measurements;
        try {
            measurements = this.getDmHome().findByIds(mids);
        } catch (FinderException e1) {
            // This should not happen
            throw new MeasurementNotFoundException(
                "Invalid IDs: " + StringUtil.arrayToString(mids));
        }
        
        for (Iterator it = measurements.iterator(); it.hasNext(); ) {
            DerivedMeasurementLocal dm = (DerivedMeasurementLocal) it.next();

            // Check to make sure this is a derived measurement
            try {
                Collection col = dm.getTemplate().getMeasurementArgs();
                boolean check = col.size() > 0;
            } catch (NoSuchEntityException e) {
                throw new MeasurementNotFoundException(
                    "Measurement ID is not a valid measurement: " + dm.getId());
            } catch (NullPointerException e) {
                throw new MeasurementNotFoundException(
                    "Measurement ID is a raw measurement: " + dm.getId());
            }

            AppdefEntityID id =
                new AppdefEntityID(
                    dm.getTemplate().getMonitorableType().getAppdefType(),
                    dm.getInstanceId().intValue());

            HashSet tids;
            if (resMap.containsKey(id)) {
                tids = (HashSet) resMap.get(id);
            }
            else {
                // Authz check
                super.checkModifyPermission(subject, id);        

                tids = new HashSet();
                resMap.put(id, tids);
            }
                
            tids.add(((MeasurementTemplatePK)
                    dm.getTemplate().getPrimaryKey()).getId());
        }
        
        for (Iterator it = resMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            AppdefEntityID id = (AppdefEntityID) entry.getKey();
            HashSet mtidSet = (HashSet) entry.getValue();
            Integer[] mtids = (Integer[]) mtidSet.toArray(new Integer[0]);
            long[] intervals = new long[mtids.length];
            Arrays.fill(intervals, interval);

            try {
                // A little short-cut.  We just end up looking up the derived
                // measurement twice.
                this.createMeasurements(subject, id, mtids, intervals, null);
            } catch (TemplateNotFoundException e) {
                // This shouldn't happen as the measurement is already created
                throw new MeasurementNotFoundException("Template not found", e);
            }
        }
    } // end enable

    /**
     * Set the interval of Measurements based their template ID's
     *
     * @param templates   List of Integer template IDs to add
     * @param id          instance ID (appdef resource) the templates are for
     * @param interval    Millisecond interval that the measurement is polled
     * @param protoProps  Configuration data for the instance
     *
     * @return a List of the associated DerivedMeasurementValue objects
     * @ejb:interface-method
     */
    public void enableMeasurements(AuthzSubjectValue subject,
                                   AppdefEntityID[] aeids, Integer[] mtids,
                                   long interval)
        throws MeasurementNotFoundException, MeasurementCreateException,
               PermissionException {
        
        long[] intervals = new long[mtids.length];
        Arrays.fill(intervals, interval);
        
        for (int i = 0; i < aeids.length; i++) {
            AppdefEntityID id = aeids[i];

            try {
                this.createMeasurements(subject, id, mtids, intervals, null);
            } catch (TemplateNotFoundException e) {
                // This shouldn't happen as the measurement is already created
                throw new MeasurementNotFoundException(
                    "Template " + mtids[i] + " is not found", e);
            }
        }
    } // end enable

    /**
     * Disable all derived measurement EJBs for an instance
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     */
    public void disableMeasurements(AuthzSubjectValue subject,
                                    AppdefEntityID id)
        throws PermissionException {
        // Authz check
        super.checkModifyPermission(subject, id);        

        try {
            List mcol = getDmHome().findByInstance(id.getType(), id.getID());
            Integer[] mids = new Integer[mcol.size()];
            Iterator it = mcol.iterator();
            for (int i = 0; it.hasNext(); i++) {
                DerivedMeasurementLocal dm =
                    (DerivedMeasurementLocal) it.next();
                DerivedMeasurementPK pk =
                    (DerivedMeasurementPK) dm.getPrimaryKey(); 
                try {
                    this.getDMManager().enableMeasurement(pk, false);
                } catch (MeasurementNotFoundException e) {
                    // This is quite impossible, as we have just looked it up
                    throw new SystemException(e);
                }
                mids[i] = pk.getId();
            }

            // Now unschedule the DerivedMeasurment
            this.unscheduleJobs(mids);
            this.sendAgentSchedule(id);
        } catch (FinderException e) {
            // Not a problem
            log.debug("FinderException", e);
        }
    }

    /**
     * Disable all derived measurement EJBs for an instance
     *
     * @return a list of DerivedMeasurement value
     * @ejb:interface-method
     */
    public void disableMeasurements(AuthzSubjectValue subject, Integer[] mids)
        throws PermissionException, MeasurementNotFoundException {
        AppdefEntityID aid = null;
        for (int i = 0; i < mids.length; i++) {
            try {
                DerivedMeasurementLocal m = this.getDmHome().findById(mids[i]);
    
                // Check removal permission
                if (aid == null) {
                    aid = this.getAppdefEntityId(m);
                    super.checkModifyPermission(subject, aid);
                }
    
                this.getDMManager().enableMeasurement(
                    (DerivedMeasurementPK) m.getPrimaryKey(), false);
            } catch (FinderException e) {
                // Why are you passing a bad measurement ID?
                throw new MeasurementNotFoundException(mids[i], e);
            }
        }

        // Now unschedule the DerivedMeasurment
        this.unscheduleJobs(mids);
        this.sendAgentSchedule(aid);
    }

    /**
     * Disable measurements for an instance
     *
     * @ejb:interface-method
     */
    public void disableMeasurements(
        AuthzSubjectValue subject, AppdefEntityID id, Integer[] tids)
        throws PermissionException {
        // Authz check
        super.checkModifyPermission(subject, id);        
        try {
            List mcol = getDmHome().findByInstance(id.getType(), id.getID());
            HashSet tidSet = null;
            if (tids != null) {
                tidSet = new HashSet(Arrays.asList(tids));
            }            
            
            List toUnschedule = new ArrayList();
            for (Iterator it = mcol.iterator(); it.hasNext(); ) {
                DerivedMeasurementLocal dm =
                    (DerivedMeasurementLocal) it.next();
                // Check to see if we need to remove this one
                if (tidSet != null && !tidSet.contains(((MeasurementTemplatePK)
                        dm.getTemplate().getPrimaryKey()).getId()))
                    continue;
                
                DerivedMeasurementPK pk =
                    (DerivedMeasurementPK) dm.getPrimaryKey();
                
                try {
                    this.getDMManager().enableMeasurement(pk, false);
                } catch (MeasurementNotFoundException e) {
                    // This is quite impossible, we just looked it up
                    throw new SystemException(e);
                }
                
                toUnschedule.add(pk.getId());
            }

            // Now unschedule the DerivedMeasurment
            this.unscheduleJobs(
                    (Integer[]) toUnschedule.toArray(new Integer[0]));
            this.sendAgentSchedule(id);
        } catch (FinderException e) {
            // Not a problem
            log.debug("FinderException", e);
        }
    }

    /**
     * @see javax.ejb.SessionBean#ejbCreate()
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {
        try {
            valuePager = Pager.getPager(VALUE_PROCESSOR);
        } catch (Exception e) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }

    /**
     * @see javax.ejb.SessionBean#ejbPostCreate()
     */
    public void ejbPostCreate() {}

    /**
     * @see javax.ejb.SessionBean#ejbActivate()
     */
    public void ejbActivate() {}

    /**
     * @see javax.ejb.SessionBean#ejbPassivate()
     */
    public void ejbPassivate() {}

    /**
     * @see javax.ejb.SessionBean#ejbRemove()
     */
    public void ejbRemove() {
        this.ctx = null;
    }

    /**
     * @see javax.ejb.SessionBean#setSessionContext(SessionContext)
     */
    public void setSessionContext(SessionContext ctx)
        throws EJBException, RemoteException {
        this.ctx = ctx;
    }
}
