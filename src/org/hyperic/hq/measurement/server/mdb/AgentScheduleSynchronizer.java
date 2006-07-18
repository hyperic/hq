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

/*
 * AgentScheduleSynchronizer.java
 * 
 * Created on Jun 18, 2003
 */
package org.hyperic.hq.measurement.server.mdb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.AuthzSubjectManagerUtil;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.MeasurementScheduleException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.ext.depgraph.DerivedNode;
import org.hyperic.hq.measurement.ext.depgraph.Graph;
import org.hyperic.hq.measurement.ext.depgraph.GraphBuilder;
import org.hyperic.hq.measurement.ext.depgraph.InvalidGraphException;
import org.hyperic.hq.measurement.ext.depgraph.Node;
import org.hyperic.hq.measurement.ext.depgraph.RawNode;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.DerivedMeasurementManagerUtil;
import org.hyperic.hq.measurement.shared.DerivedMeasurementValue;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocal;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocalHome;
import org.hyperic.hq.measurement.shared.MeasurementProcessorUtil;
import org.hyperic.hq.measurement.shared.MeasurementTemplateValue;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.RawMeasurementManagerUtil;
import org.hyperic.hq.measurement.shared.RawMeasurementValue;
import org.hyperic.util.pager.PageControl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class serves to provide synchronized calls into the EJBs to avoid
 * too many concurrent accesses
 * 
 * 
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
    
    private HashMap dmMap          = new HashMap();
    
    private void cacheDM(DerivedMeasurementValue dmVo) {
        Integer tid = dmVo.getTemplate().getId();
        if (!dmMap.containsKey(tid)) {
            dmMap.put(tid, new HashMap());
        }
                
        HashMap tmplMap = (HashMap) dmMap.get(tid);
        tmplMap.put(dmVo.getInstanceId(), dmVo);
    }
    
    private AuthzSubjectValue subject = null;
    private AuthzSubjectValue getSubject()
        throws FinderException {
        try {
            if (subject == null)
                subject = AuthzSubjectManagerUtil.getLocalHome().create()
                    .findOverlord();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
                
        return subject;
    }

    private MeasurementProcessorLocal measurementProc = null;
    private MeasurementProcessorLocal getMeasurementProcessor() {
        try {
            if (measurementProc == null) {
                MeasurementProcessorLocalHome sHome =
                    (MeasurementProcessorLocalHome) 
                        MeasurementProcessorUtil.getLocalHome();
                measurementProc = sHome.create();
            }
            
            return measurementProc;
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }

    private DerivedMeasurementManagerLocal dman = null;
    private DerivedMeasurementManagerLocal getDMan() {
        try {
            if (dman == null)
                dman = DerivedMeasurementManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }

        return dman; 
    }

    private RawMeasurementManagerLocal rman = null;
    private RawMeasurementManagerLocal getRMan() {
        try {
            if (rman == null)
                rman = RawMeasurementManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }

        return rman; 
    }

    private DerivedMeasurementValue getDMByTemplateAndInstance(
        Integer tid, Integer instanceId)
        throws MeasurementNotFoundException, CreateException, FinderException {
        HashMap tmplMap = (HashMap) this.dmMap.get(tid);
        if (tmplMap != null && tmplMap.containsKey(instanceId)) {
            return (DerivedMeasurementValue) tmplMap.get(instanceId);
        }

        // Otherwise, we need to look it up
        DerivedMeasurementValue dmVo =
            this.getDMan().findMeasurement(this.getSubject(), tid, instanceId);
        this.cacheDM(dmVo);
        
        return dmVo;
    }
    

    private RawMeasurementValue getRMByTemplateAndInstance(Integer tid, 
                                                           Integer instanceId) {
        return this.getRMan().findMeasurement(tid, instanceId);
    }

    private void reschedule(AppdefEntityID entId, List dmVos)
        throws InvalidGraphException, PermissionException,
               MeasurementScheduleException, MonitorAgentException {
        
        HashSet agentSchedule  = new HashSet();
        HashSet serverSchedule = new HashSet();
        Graph[] graphs = new Graph[dmVos.size()];

        Iterator it = dmVos.iterator();
        for (int i = 0; it.hasNext(); i++) {
            DerivedMeasurementValue dmVo = (DerivedMeasurementValue) it.next();
            MeasurementTemplateValue tmpl = dmVo.getTemplate();
            
            // Build the graph
            graphs[i] = GraphBuilder.buildGraph(new Graph(), tmpl);
            if ( log.isDebugEnabled() ) {
                String gString = graphs[i].toString();
                log.debug(gString);
            }
            
            DerivedNode derivedNode = (DerivedNode)
                graphs[i].getNode( tmpl.getId().intValue() );

            // first handle simple IDENTITY derived case
            if ( MeasurementConstants.TEMPL_IDENTITY.equals(tmpl.getTemplate()))
            {
                // If this node is an identity, there's only one node below...
                // the raw node. Fetch it.
                RawNode rawNode = (RawNode)
                    derivedNode.getOutgoing().iterator().next();

                // Now grab the raw measurement template
                MeasurementTemplateValue rawTemplateValue =
                    rawNode.getMeasurementTemplateValue();

                // Set the new interval time
                derivedNode.setInterval(dmVo.getInterval());

                // Check the raw node
                RawMeasurementValue rmVal =
                    getRMByTemplateAndInstance(rawTemplateValue.getId(),
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
                    MeasurementTemplateValue templArg =
                        node.getMeasurementTemplateValue();

                    if (node instanceof DerivedNode) {
                        try {
                            DerivedMeasurementValue tmpDm =
                                getDMByTemplateAndInstance(
                                    templArg.getId(), dmVo.getInstanceId());
                            long targetInterval = tmpDm.getInterval();

                            if ( dmVo.getId().equals( templArg.getId() ) )
                                dmVo = tmpDm;

                            ( (DerivedNode)node ).setInterval(targetInterval);
                        } catch (CreateException e) {
                            // Move on to the next one
                            continue;
                        } catch (FinderException e) {
                            // Move on to the next one
                            continue;
                        } catch (MeasurementNotFoundException e) {
                            continue;
                        }
                    } else {
                        // we are a raw node
                        RawMeasurementValue rmVal =
                            getRMByTemplateAndInstance(templArg.getId(), 
                                                       dmVo.getInstanceId());

                        agentSchedule.add( rmVal.getId() );
                    } // end if template arg is DM else RM
                } // end for each template arg
            } // end if identity else
        }

        this.getMeasurementProcessor().schedule(
            entId, graphs, agentSchedule, serverSchedule);
    }
    
    private void unschedule(AppdefEntityID eid)
        throws MeasurementUnscheduleException, PermissionException {
        log.debug("Unschedule metrics for " + eid);
        this.getMeasurementProcessor().unschedule(eid);
    }

    public void reschedule(AppdefEntityID eid)
        throws InvalidGraphException, MeasurementScheduleException,
               MonitorAgentException, FinderException, PermissionException,
               MeasurementUnscheduleException {
        log.debug("Reschedule metrics for " + eid);
        List dms = this.getDMan().findMeasurements(this.getSubject(), eid, true,
                                                   null, PageControl.PAGE_ALL);
        
        if (dms.size() > 0)
            this.reschedule(eid, dms);
        else
            this.unschedule(eid);
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
            } catch (FinderException e) {
                log.debug("Unable to look up super user");
            }
        }
    }
}
