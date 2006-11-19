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

package org.hyperic.hq.measurement.server.mbean;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.appdef.shared.AgentManagerUtil;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AgentValue;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.util.EjbModuleLifecycle;
import org.hyperic.hq.common.shared.util.EjbModuleLifecycleListener;
import org.hyperic.hq.ha.shared.Mode;
import org.hyperic.hq.measurement.MeasurementScheduleException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.ext.depgraph.InvalidGraphException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.monitor.MonitorCreateException;
import org.hyperic.hq.measurement.server.mdb.AgentScheduleSynchronizer;
import org.hyperic.hq.measurement.shared.MeasurementProcessorLocal;
import org.hyperic.hq.measurement.shared.MeasurementProcessorUtil;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.hq.measurement.shared.SRNManagerUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This job is responsible for verifying measurement jobs.
 * It is possible for the agent's schedule to become lost. In
 * these cases, its desireable for the server to push the
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
    implements ScheduleVerificationServiceMBean, MBeanRegistration,
               EjbModuleLifecycleListener {

    private Log log =
        LogFactory.getLog(ScheduleVerificationService.class.getName());

    private MBeanServer server = null;
    private EjbModuleLifecycle camListener = null;
    private boolean started = false;
    private boolean firstTime = true;
    
    private MeasurementProcessorLocal mproc = null;
    private MeasurementProcessorLocal getMeasurementProcessor() {
        if (mproc == null) {
            try {
                mproc = MeasurementProcessorUtil.getLocalHome().create();
            } catch (CreateException e) {
                throw new SystemException(e);
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        }

        return mproc;
    }

    private SRNManagerLocal srnManager;
    private SRNManagerLocal getSrnManager() {
        if (srnManager == null) {
            try {
                srnManager = SRNManagerUtil.getLocalHome().create();
            } catch (Exception e) {
                throw new SystemException(e);
            }
        }
        return srnManager;
    }

    private AgentScheduleSynchronizer agentSync =
        AgentScheduleSynchronizer.getInstance();
    
    //---------------------------------------------------------------------
    //-- managed operations
    //---------------------------------------------------------------------
    /**
     * SchedVerification service is only active on master and standalone servers
     * @jmx:managed-operation
     */
    public boolean isActive () {
        return Mode.getInstance().isActivated() ?  started : false;
    }
    
    /**
     * Refresh the schedule of
     * a given platform entity
     * @param aid - appdefEntityId for a platform
     * @jmx:managed-operation
     * 
     */
    public void refreshSchedule(AppdefEntityID aid) {
        try {
            // rescheduling every time
            this.agentSync.reschedule(aid);
        } catch (MonitorAgentException e) {
            log.error("Unable to communicate with agent for entity: " +
                      aid.getID() + " to refresh metric schedule");
        } catch (Exception e) {
            log.error("Failed to refresh schedule for entity: " + aid, e);
        }
    }
    
    /**
     * Send the message.
     *
     * @jmx:managed-operation
     */
    public void hit(Date lDate) {
        if (!isActive()) return;
        
        // Skip first schedule verification, let the server warm up a bit
        if (firstTime) {
            firstTime = false;
            return;
        }

        // First make sure that the cache is initialized
        SRNManagerLocal srnManager = getSrnManager();

        AgentManagerLocal agentMan;

        try {
            agentMan = AgentManagerUtil.getLocalHome().create();
        } catch (CreateException e) {
            // this is a schedulable MBean -- we will try again later,
            // so just log a "not ready" message
            log.info("Measurement Schedule Verification: " +
                     "Agent or Data Manager not ready.");
            return;
        } catch (NamingException e) {
            // this is a schedulable MBean -- we will try again later,
            // so just log a "not ready" message
            log.info("Measurement Schedule Verification: " +
                     "Agent or Data Manager not ready.");
            return;
        }

        // Ask the SRNCache what requires rescheduling
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
                    if (this.getMeasurementProcessor().ping(agentVal)) {
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
                log.debug("Measurement Schedule Verification: " +
                          "Agent not found for " + entId);
                // Resource not found, remove from SRN
                srnManager.removeSrn(entId);
            } catch (PermissionException e) {
                log.debug("Measurement Schedule Verification: " +
                          "No permission to look up " + entId);
            } catch (MonitorCreateException e) {
                log.debug("Measurement Schedule Verification: " +
                          "Could not create a monitor to connect to agent " +
                          agentVal);
                downAgents.add(agentVal);
            } catch (MonitorAgentException e) {
                log.debug("Measurement Schedule Verification: " +
                          "Could not connect to agent " + agentVal);
                downAgents.add(agentVal);
            } catch (MeasurementScheduleException e) {
                this.log.debug(
                    "Scheduling error during rescheduling of " + entId);
            } catch (MeasurementUnscheduleException e) {
                this.log.debug(
                    "Scheduling error during unscheduling of " + entId);
            } catch (FinderException e) {
                // No measurements to reschedule
            } catch (InvalidGraphException e) {
                this.log.debug("Invalid graph for rescheduling of " + entId);
            }
        }
    }

    //---------------------------------------------------------------------
    //-- mbean control methods
    //---------------------------------------------------------------------
    /**
     * @jmx:managed-operation
     */
    public void init() {
    }

    /**
     * @jmx:managed-operation
     */
    public void start() throws Exception {
        camListener = new EjbModuleLifecycle(this.server, this,
                                             HQConstants.EJB_MODULE_PATTERN);
        camListener.start();

    }

    /**
     * @jmx:managed-operation
     */
    public void stop() {
        log.info("Stopping ScheduleVerificationService");
        camListener.stop();
    }

    /**
     * @jmx:managed-operation
     */
    public void destroy() {
        // do nothing
    }

    /* (non-Javadoc)
     * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
     */
    public ObjectName preRegister(MBeanServer server, ObjectName name)
        throws Exception {
        this.server = server;
        return name;
    }

    /* (non-Javadoc)
     * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
     */
    public void postRegister(Boolean arg0) {
        // do nothing
    }

    /* (non-Javadoc)
     * @see javax.management.MBeanRegistration#preDeregister()
     */
    public void preDeregister() throws Exception {
        // do nothing
    }

    /* (non-Javadoc)
     * @see javax.management.MBeanRegistration#postDeregister()
     */
    public void postDeregister() {
        // do nothing
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.common.shared.util.EjbModuleLifecycleListener#ejbModuleStarted()
     */
    public void ejbModuleStarted() {
        log.info("Starting ScheduleVerificationService");
        this.started = true;
    }

    /* (non-Javadoc)
     * @see org.hyperic.hq.common.shared.util.EjbModuleLifecycleListener#ejbModuleStopped()
     */
    public void ejbModuleStopped() {
        // do nothing
    }
}
