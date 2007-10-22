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
package org.hyperic.hq.measurement.server.mdb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.MeasurementScheduleException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.ext.depgraph.DerivedNode;
import org.hyperic.hq.measurement.ext.depgraph.Graph;
import org.hyperic.hq.measurement.ext.depgraph.InvalidGraphException;
import org.hyperic.hq.measurement.ext.depgraph.Node;
import org.hyperic.hq.measurement.ext.depgraph.RawNode;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.DerivedMeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.server.session.GraphBuilder;
import org.hyperic.hq.measurement.server.session.MeasurementProcessorEJBImpl;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.measurement.server.session.RawMeasurement;
import org.hyperic.hq.measurement.server.session.RawMeasurementManagerEJBImpl;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocal;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocal;
import org.hyperic.util.pager.PageControl;

/**
 * This class is used to schedule and unschedule metrics for a given entity.
 * The schedule operation is synchronized to throttle rescheduling.
 */
public class AgentScheduleSynchronizer {
    
    private static final Log log =
        LogFactory.getLog(AgentScheduleSynchronizer.class.getName());

    private static AgentScheduleSynchronizer singleton =
        new AgentScheduleSynchronizer();

    public static AgentScheduleSynchronizer getInstance() {
        return AgentScheduleSynchronizer.singleton;
    }

    private AgentScheduleSynchronizer() {
        // Singleton class, private constructor
    }

    private HashMap dmMap = new HashMap();

    private void cacheDM(DerivedMeasurement dmVo) {
        Integer tid = dmVo.getTemplate().getId();
        if (!dmMap.containsKey(tid)) {
            dmMap.put(tid, new HashMap());
        }

        HashMap tmplMap = (HashMap) dmMap.get(tid);
        tmplMap.put(dmVo.getInstanceId(), dmVo);
    }

    private AuthzSubjectValue subject = null;
    private AuthzSubjectValue getSubject() throws SubjectNotFoundException {
        if (subject == null)
            subject = AuthzSubjectManagerEJBImpl.getOne().findOverlord();
                
        return subject;
    }

    private MeasurementProcessorLocal measurementProc = null;
    private MeasurementProcessorLocal getMeasurementProcessor() {
        if (measurementProc == null) {
            measurementProc = MeasurementProcessorEJBImpl.getOne();
        }

        return measurementProc;
    }

    private DerivedMeasurementManagerLocal dman = null;
    private DerivedMeasurementManagerLocal getDMan() {
        if (dman == null)
            dman = DerivedMeasurementManagerEJBImpl.getOne();
        return dman; 
    }

    private RawMeasurementManagerLocal rman = null;
    private RawMeasurementManagerLocal getRMan() {
        if (rman == null)
            rman = RawMeasurementManagerEJBImpl.getOne();
        return rman;
    }

    private DerivedMeasurement getDMByTemplateAndInstance(
        Integer tid, Integer instanceId)
        throws MeasurementNotFoundException, CreateException,
               SubjectNotFoundException {
        HashMap tmplMap = (HashMap) this.dmMap.get(tid);
        if (tmplMap != null && tmplMap.containsKey(instanceId)) {
            return (DerivedMeasurement) tmplMap.get(instanceId);
        }

        // Otherwise, we need to look it up
        DerivedMeasurement dmVo = getDMan().findMeasurement(tid, instanceId);
        cacheDM(dmVo);
        
        return dmVo;
    }
    

    private RawMeasurement getRMByTemplateAndInstance(Integer tid,
                                                      Integer instanceId) {
        return getRMan().findMeasurement(tid, instanceId);
    }

    private void reschedule(AppdefEntityID entId, List dmVos)
        throws InvalidGraphException, PermissionException,
               MeasurementScheduleException, MonitorAgentException,
               SubjectNotFoundException {
        
        HashSet agentSchedule  = new HashSet();
        HashSet serverSchedule = new HashSet();
        Graph[] graphs = new Graph[dmVos.size()];

        Iterator it = dmVos.iterator();
        for (int i = 0; it.hasNext(); i++) {
            DerivedMeasurement dmVo = (DerivedMeasurement) it.next();
            MeasurementTemplate tmpl = dmVo.getTemplate();
            
            // Build the graph
            graphs[i] = GraphBuilder.buildGraph(tmpl);
            
            DerivedNode derivedNode = (DerivedNode)
                graphs[i].getNode( tmpl.getId().intValue() );

            // first handle simple IDENTITY derived case
            if (MeasurementConstants.TEMPL_IDENTITY.equals(tmpl.getTemplate()))
            {
                // If this node is an identity, there's only one node below...
                // the raw node. Fetch it.
                RawNode rawNode = (RawNode)
                    derivedNode.getOutgoing().iterator().next();

                // Now grab the raw measurement template
                MeasurementTemplate rawTemplate =
                    rawNode.getMeasurementTemplate();

                // Set the new interval time
                derivedNode.setInterval(dmVo.getInterval());

                // Check the raw node
                RawMeasurement rmVal =
                    getRMByTemplateAndInstance(rawTemplate.getId(),
                                               dmVo.getInstanceId());

                if (rmVal == null) {    // Don't reschedule if no raw metric
                    log.error("AgentScheduleSynchronizer: Cannot look up " +
                              "raw metric by template and instance IDs");
                    continue;
                }
                
                // Add the raw measurement to the schedule
                agentSchedule.add( rmVal.getId() );
            } else {
                // First add to server schedule
                serverSchedule.add(dmVo.getId());

                // we're not an identity DM template, so we need
                // to make sure that measurements are enabled for
                // the whole graph
                for (Iterator graphNodes = graphs[i].getNodes().iterator();
                     graphNodes.hasNext();) {

                    // we don't yet know whether its an RM or DM
                    Node node = (Node)graphNodes.next();

                    // Fetch the template
                    MeasurementTemplate templArg =
                        node.getMeasurementTemplate();

                    if (node instanceof DerivedNode) {
                        try {
                            DerivedMeasurement tmpDm =
                                getDMByTemplateAndInstance(
                                    templArg.getId(), dmVo.getInstanceId());
                            long targetInterval = tmpDm.getInterval();

                            if ( dmVo.getId().equals( templArg.getId() ) )
                                dmVo = tmpDm;

                            ( (DerivedNode)node ).setInterval(targetInterval);
                        } catch (CreateException e) {
                            // Move on to the next one
                            continue;
                        } catch (MeasurementNotFoundException e) {
                            continue;
                        }
                    } else {
                        // we are a raw node
                        RawMeasurement rmVal =
                            getRMByTemplateAndInstance(templArg.getId(), 
                                                       dmVo.getInstanceId());

                        agentSchedule.add( rmVal.getId() );
                    } // end if template arg is DM else RM
                } // end for each template arg
            } // end if identity else
        }

        getMeasurementProcessor().schedule(entId, graphs, agentSchedule,
                                           serverSchedule);
    }
    
    private void unschedule(AppdefEntityID eid)
        throws MeasurementUnscheduleException, PermissionException {
        log.debug("Unschedule metrics for " + eid);
        getMeasurementProcessor().unschedule(eid);
    }

    public void reschedule(AppdefEntityID eid)
        throws InvalidGraphException, MeasurementScheduleException,
               MonitorAgentException, PermissionException,
               MeasurementUnscheduleException, SubjectNotFoundException 
    {
        log.debug("Reschedule metrics for " + eid);
        List dms = getDMan().findEnabledMeasurements(getSubject(), eid, null);
                
        if (dms.size() > 0)
            reschedule(eid, dms);
        else
            unschedule(eid);
    }

    public static void schedule(AppdefEntityID eid) {
        synchronized (log) {
            try {
                AgentScheduleSynchronizer.singleton.reschedule(eid);
            } catch (PermissionException e) {
                log.debug("No permission to look up agent", e);
            } catch (MonitorAgentException e) {
                log.debug("Could not contact agent", e);
            } catch (MeasurementScheduleException e) {
                log.debug("Schedule exception", e);
            } catch (MeasurementUnscheduleException e) {
                log.debug("Unschedule exception", e);
            } catch (SystemException e) {
                log.debug("Unable to look up measurement processor", e);
            } catch (InvalidGraphException e) {
                log.debug("Unable to create valid measurement graphs");
            } catch (SubjectNotFoundException e) {
                log.debug("Unable to look up super user");
            }
        }
    }
}
