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

package org.hyperic.hq.autoinventory.server.mbean;

import java.util.Date;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.common.SessionMBeanBase;
import org.jboss.varia.scheduler.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

/**
 * This job is responsible for notifying agents needing an auto inventory scan.
 *
 * 
 */
@ManagedResource("hyperic.jmx:type=Service,name=AgentAIScan")
@Service
public class AgentAIScanService
    extends SessionMBeanBase
    implements AgentAIScanServiceMBean
{
    private Log log = LogFactory.getLog(AgentAIScanService.class);
    
    
    private AutoinventoryManager autoinventoryManager;
    
    private MBeanServer mbeanServer;
    
    
    @Autowired
    public AgentAIScanService(AutoinventoryManager autoinventoryManager, MBeanServer mbeanServer) {
        this.autoinventoryManager = autoinventoryManager;
        this.mbeanServer = mbeanServer;
    }
    
    public void startSchedule() throws Exception {
        Scheduler scheduler = new Scheduler();
        scheduler.setSchedulableMBeanMethod("hit( DATE )");
        scheduler.setSchedulableMBean("hyperic.jmx:type=Service,name=AgentAIScan");
        scheduler.setInitialStartDate("NOW");
        scheduler.setSchedulePeriod(300000);
        scheduler.setInitialRepetitions(-1);
        scheduler.setStartAtStartup(false);
        ObjectName schedulerName = new ObjectName("hyperic.jmx:service=Scheduler,name=AgentAIScan");
        mbeanServer.registerMBean(scheduler,schedulerName);
        mbeanServer.invoke(schedulerName, "start", new Object[] {}, new String[] {});
        mbeanServer.invoke(schedulerName, "startSchedule", new Object[] {}, new String[] {});
     }

    /**
     * 
     */
    @ManagedOperation
    public void hit(Date lDate) {
        super.hit(lDate);
    }
    
    protected void hitInSession(Date lDate) {
        log.debug("Agent AI Scan Service started executing: "+lDate);  
        
        autoinventoryManager.notifyAgentsNeedingRuntimeScan();
        
        log.debug("Agent AI Scan Service finished executing: "+lDate); 
    }

    /**
     * 
     */
    @ManagedOperation
    public void init() {}

    /**
     * 
     */
    @ManagedOperation
    public void start() {}

    /**
     * 
     */
    @ManagedOperation
    public void stop() {
    }

    /**
     * 
     */
    @ManagedOperation
    public void destroy() {}
}

