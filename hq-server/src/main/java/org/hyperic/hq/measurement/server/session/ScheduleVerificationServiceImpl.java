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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.mgmt.domain.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementScheduleException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.MeasurementProcessor;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * This job is responsible for verifying measurement jobs.
 * It is possible for the agent's schedule to become lost. In
 * these cases, its desirable for the server to push the
 * schedule out to the agent. At present, the agent scheduling
 * process is synchronous along with the server scheduling
 * process.  The intent is to allow this to be asynchronous.
 *
 * Note: We can't schedule this job to run any more frequently than the
 * shortest DM's interval. Otherwise, we'd keep blowing away and recreating
 * the schedule due to the absence of data rows.
 *
 * 
 * 
 */

@Service("scheduleVerificationService")
public class ScheduleVerificationServiceImpl implements ScheduleVerificationService
{
    private Log log =
        LogFactory.getLog(ScheduleVerificationServiceImpl.class.getName());

    private boolean firstTime = true;

    private MeasurementProcessor measurementProcessor;
    private SRNManager srnManager;
    private AgentManager agentManager;
   
    
    
    @Autowired
    public ScheduleVerificationServiceImpl(MeasurementProcessor measurementProcessor, SRNManager srnManager,
                                       AgentManager agentManager) {
        this.measurementProcessor = measurementProcessor;
        this.srnManager = srnManager;
        this.agentManager = agentManager;
    }
    
    @Transactional
    public void verifySchedules() {
      try {
        
        // Skip first schedule verification, let the server warm up a bit
        // XXX: We should add a wait attribute for this, similar to the
        //      AvailCheckService --RPM
        if (firstTime) {
            firstTime = false;
            return;
        }

       

        // Ask the SRNCache what requires rescheduling
       
        Collection<AppdefEntityID> toResched = srnManager.getOutOfSyncEntities();
        
        HashSet<Agent> downAgents = new HashSet<Agent>();
        HashSet<Agent> upAgents   = new HashSet<Agent>();
        Map<Integer, List<AppdefEntityID>> aeids = new HashMap<Integer,List<AppdefEntityID>>();

        for (AppdefEntityID entId : toResched) {
          
            Agent agent = null;
            // Get the agent connection
            try {
                agent = agentManager.getAgent(entId);

                // If this is an entity on a platform whose agent is down,
                // continue without rescheduling
                if (downAgents.contains(agent))
                    continue;
                
                if (!upAgents.contains(agent)) {
                    if (measurementProcessor.ping(agent)) {
                        upAgents.add(agent);
                    }
                    else {
                        downAgents.add(agent);
                        continue;
                    }
                }

                List<AppdefEntityID> list;
                if (null == (list = aeids.get(agent.getId()))) {
                    list = new ArrayList<AppdefEntityID>();
                    aeids.put(agent.getId(), list);
                }
                list.add(entId);
            } catch (AgentNotFoundException e) {
                log.debug("Measurement Schedule Verification: " +
                           "Agent not found for " + entId);
                // Resource not found, remove from SRN
                srnManager.removeSrn(entId);
            } catch (PermissionException e) {
                log.debug("Measurement Schedule Verification: " +
                           "No permission to look up " + entId);
            }
        }
        // Now reschedule all metrics for these entities per agent
        for (Map.Entry<Integer,List<AppdefEntityID>> entry : aeids.entrySet() ) {
           
            Integer agentId = entry.getKey();
           ;
            try {
                srnManager.reschedule(entry.getValue());
            } catch (MeasurementScheduleException e) {
                log.debug("Scheduling error during rescheduling of agentId " +
                    agentId);
            } catch (MeasurementUnscheduleException e) {
                log.debug("Scheduling error during unscheduling of agentId " +
                    agentId);
            } catch (MonitorAgentException e) {
                log.debug("Measurement Schedule Verification: " +
                           "Could not connect to agent " + agentId);
            }
        }
      } catch (Exception e) {
          throw new SystemException(e);
      }
    }

}
