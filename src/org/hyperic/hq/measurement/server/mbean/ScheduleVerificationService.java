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

package org.hyperic.hq.measurement.server.mbean;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentManagerUtil;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.auth.shared.SubjectNotFoundException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SessionMBeanBase;
import org.hyperic.hq.measurement.MeasurementScheduleException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.ext.depgraph.InvalidGraphException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.monitor.MonitorCreateException;
import org.hyperic.hq.measurement.server.session.AgentScheduleSynchronizer;
import org.hyperic.hq.measurement.server.session.MeasurementProcessorEJBImpl;
import org.hyperic.hq.measurement.server.session.SRNManagerEJBImpl;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocal;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;

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
 * @jmx:mbean name="hyperic.jmx:type=Service,name=MeasurementSchedule"
 * 
 */
public class ScheduleVerificationService
    extends SessionMBeanBase
    implements ScheduleVerificationServiceMBean
{
    private Log _log =
        LogFactory.getLog(ScheduleVerificationService.class.getName());

    private boolean _firstTime = true;

    private MeasurementProcessorLocal getMeasurementProcessor() {
        return MeasurementProcessorEJBImpl.getOne();
    }

    private AgentScheduleSynchronizer agentSync =
        AgentScheduleSynchronizer.getInstance();

    /**
     * @jmx:managed-operation
     */
    public void hit(final Date lDate) {
        super.hit(lDate);
    }
    
    protected void hitInSession(final Date lDate) {        
        // Skip first schedule verification, let the server warm up a bit
        // XXX: We should add a wait attribute for this, similar to the
        //      AvailCheckService --RPM
        if (_firstTime) {
            _firstTime = false;
            return;
        }

        AgentManagerLocal agentMan;

        try {
            agentMan = AgentManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            // this is a schedulable MBean -- we will try again later,
            // so just log a "not ready" message
            _log.info("Measurement Schedule Verification: " +
                      "Agent or Data Manager not ready.");
            return;
        } catch (NamingException e) {
            // this is a schedulable MBean -- we will try again later,
            // so just log a "not ready" message
            _log.info("Measurement Schedule Verification: " +
                      "Agent or Data Manager not ready.");
            return;
        }

        // Ask the SRNCache what requires rescheduling
        SRNManagerLocal srnManager = SRNManagerEJBImpl.getOne();
        Collection toResched = srnManager.getOutOfSyncEntities();
        
        HashSet downAgents = new HashSet();
        HashSet upAgents   = new HashSet();

        for (Iterator iter = toResched.iterator(); iter.hasNext();) {
            AppdefEntityID entId = (AppdefEntityID) iter.next();
            AgentValue agentVal = null;
            // Get the agent connection
            try {
                agentVal = agentMan.getAgent(entId);

                // If this is an entity on a platform whose agent is down,
                // continue without rescheduling
                if (downAgents.contains(agentVal))
                    continue;
                
                if (!upAgents.contains(agentVal)) {
                    if (getMeasurementProcessor().ping(agentVal)) {
                        upAgents.add(agentVal);
                    }
                    else {
                        downAgents.add(agentVal);
                        continue;
                    }
                }

                // Now reschedule all metrics for this entity
                agentSync.reschedule(entId);
            } catch (AgentNotFoundException e) {
                _log.debug("Measurement Schedule Verification: " +
                           "Agent not found for " + entId);
                // Resource not found, remove from SRN
                srnManager.removeSrn(entId);
            } catch (PermissionException e) {
                _log.debug("Measurement Schedule Verification: " +
                           "No permission to look up " + entId);
            } catch (MonitorCreateException e) {
                _log.debug("Measurement Schedule Verification: " +
                           "Could not create a monitor to connect to agent " +
                          agentVal);
                downAgents.add(agentVal);
            } catch (MonitorAgentException e) {
                _log.debug("Measurement Schedule Verification: " +
                           "Could not connect to agent " + agentVal);
                downAgents.add(agentVal);
            } catch (MeasurementScheduleException e) {
                _log.debug("Scheduling error during rescheduling of " + entId);
            } catch (MeasurementUnscheduleException e) {
                _log.debug("Scheduling error during unscheduling of " + entId);
            } catch (InvalidGraphException e) {
                _log.debug("Invalid graph for rescheduling of " + entId);
            }
        }
    }

    /**
     * @jmx:managed-operation
     */
    public void init() {}

    /**
     * @jmx:managed-operation
     */
    public void start() {
        _log.info("Starting " + getClass().getName());
    }

    /**
     * @jmx:managed-operation
     */
    public void stop() {
        _log.info("Stopping " + getClass().getName());
    }

    /**
     * @jmx:managed-operation
     */
    public void destroy() {}
}
