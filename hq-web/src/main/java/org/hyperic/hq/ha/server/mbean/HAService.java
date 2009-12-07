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

package org.hyperic.hq.ha.server.mbean;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

/**
 * The HAService starts all internal HQ processes.
 *
 *
 */
@ManagedResource("hyperic.jmx:type=Service,name=HAService")
@Service
public class HAService
    implements HAServiceMBean
{
    private final Log log = LogFactory.getLog(HAService.class);
    private MBeanServer mbeanServer;
    
    

    @Autowired
    public HAService(MBeanServer mbeanServer) {
        this.mbeanServer = mbeanServer;
    }

    /**
     * 
     */
    @ManagedOperation
    public void startSingleton() {
      
        //Reset in-memory triggers
        RegisteredTriggers.reset();
        
        log.info("Starting HA Services");

        startAvailCheckService();
        startAgentAIScanService();
    }

    /**
     *
     */
    @ManagedOperation
    public void stopSingleton(String gracefulShutdown) {
        // XXX: shut down services
    }

    private void startAvailCheckService() {
        try {
            invoke("hyperic.jmx:service=Scheduler,name=AvailabilityCheck",
                    "startSchedule");

        } catch (Exception e) {
            log.info("Unable to start service: " + e);
        }
    }

    private void startAgentAIScanService() {
        try {
            invoke( "hyperic.jmx:service=Scheduler,name=AgentAIScan",
                    "startSchedule");
        } catch (Exception e) {
            log.info("Unable to start service: " + e);
        }
    }

    private void invoke(String mbean, String method)
            throws Exception {
        ObjectName o = new ObjectName(mbean);
        log.info("Invoking " + o.getCanonicalName() + "." + method);
        mbeanServer.invoke(o, method, new Object[] {}, new String[] {});
    }
}
