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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
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
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocal;

/**
 * This class is used to schedule and unschedule metrics for a given entity.
 * The schedule operation is synchronized to throttle rescheduling.
 */
public class AgentScheduleSynchronizer {
    
    private static final Log _log =
        LogFactory.getLog(AgentScheduleSynchronizer.class.getName());

    private static AgentScheduleSynchronizer SINGLETON =
        new AgentScheduleSynchronizer();

    public static AgentScheduleSynchronizer getInstance() {
        return AgentScheduleSynchronizer.SINGLETON;
    }

    private AgentScheduleSynchronizer() {
    }

    private AuthzSubjectValue getOverlord() {
        return AuthzSubjectManagerEJBImpl.getOne().getOverlord();
    }

    private DerivedMeasurementManagerLocal getDMan() {
        return DerivedMeasurementManagerEJBImpl.getOne();
    }

    private DerivedMeasurement 
        getDMByTemplateAndInstance(DerivedMeasurementManagerLocal dman, 
                                   Integer tid, Integer instanceId)
        throws MeasurementNotFoundException
    {
        return dman.findMeasurement(tid, instanceId);
    }

    private RawMeasurement 
        getRMByTemplateAndInstance(RawMeasurementManagerLocal rMan, Integer tid,
                                   Integer instanceId) 
    {
        return rMan.findMeasurement(tid, instanceId);
    }

    private void reschedule(AppdefEntityID entId, List dmVos)
        throws InvalidGraphException, PermissionException,
               MeasurementScheduleException, MonitorAgentException
    {
        RawMeasurementManagerLocal rMan = RawMeasurementManagerEJBImpl.getOne();
        DerivedMeasurementManagerLocal dMan = getDMan();
        
        HashSet agentSchedule  = new HashSet();
        Graph[] graphs = new Graph[dmVos.size()];

        Iterator it = dmVos.iterator();
        for (int i = 0; it.hasNext(); i++) {
            DerivedMeasurement dmVo = (DerivedMeasurement) it.next();
            MeasurementTemplate tmpl = dmVo.getTemplate();
            
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

                MeasurementTemplate rawTemplate =
                    rawNode.getMeasurementTemplate();

                derivedNode.setInterval(dmVo.getInterval());

                RawMeasurement rmVal =
                    getRMByTemplateAndInstance(rMan, rawTemplate.getId(),
                                               dmVo.getInstanceId());

                if (rmVal == null) {    // Don't reschedule if no raw metric
                    _log.error("AgentScheduleSynchronizer: Cannot look up " +
                              "raw metric by template and instance IDs");
                    continue;
                }
                
                // Add the raw measurement to the schedule
                agentSchedule.add( rmVal.getId() );
            } else {
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
                                getDMByTemplateAndInstance(dMan, 
                                    templArg.getId(), dmVo.getInstanceId());
                            long targetInterval = tmpDm.getInterval();

                            if ( dmVo.getId().equals( templArg.getId() ) )
                                dmVo = tmpDm;

                            ( (DerivedNode)node ).setInterval(targetInterval);
                        } catch (MeasurementNotFoundException e) {
                            continue;
                        }
                    } else {
                        // we are a raw node
                        RawMeasurement rmVal =
                            getRMByTemplateAndInstance(rMan, templArg.getId(), 
                                                       dmVo.getInstanceId());

                        agentSchedule.add( rmVal.getId() );
                    } 
                } 
            } 
        }

        MeasurementProcessorEJBImpl.getOne().schedule(entId, graphs, 
                                                      agentSchedule);
    }
    
    private void unschedule(AppdefEntityID eid)
        throws MeasurementUnscheduleException, PermissionException 
    {
        if (_log.isDebugEnabled())
            _log.debug("Unschedule metrics for " + eid);
        MeasurementProcessorEJBImpl.getOne().unschedule(eid);
    }

    public void reschedule(AppdefEntityID eid)
        throws InvalidGraphException, MeasurementScheduleException,
               MonitorAgentException, PermissionException,
               MeasurementUnscheduleException
    {
        if (_log.isDebugEnabled())
            _log.debug("Reschedule metrics for " + eid);
        
        List dms = getDMan().findEnabledMeasurements(getOverlord(), eid, null);
                
        if (dms.size() > 0)
            reschedule(eid, dms);
        else
            unschedule(eid);
    }

    public static void schedule(AppdefEntityID eid) {
        try {
            AgentScheduleSynchronizer.SINGLETON.reschedule(eid);
        } catch(Exception e) {
            _log.warn("Exception, scheduling [" + eid + "]", e);
        }
    }
}
