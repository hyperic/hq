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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.autoinventory.server.mbean.AgentAIScanService;
import org.hyperic.hq.events.ext.RegisteredTriggers;
import org.hyperic.hq.measurement.server.mbean.AvailabilityCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The HAService starts all internal HQ processes.
 *
 *
 */

@Service
public class HAServiceMBeanImpl implements HAServiceMBean
{
    private final Log log = LogFactory.getLog(HAServiceMBeanImpl.class);
    private AvailabilityCheckService availabilityCheckService;
    private AgentAIScanService agentAIScanService;
    

    @Autowired
    public HAServiceMBeanImpl(AvailabilityCheckService availabilityCheckService, AgentAIScanService agentAIScanService) {
        this.availabilityCheckService = availabilityCheckService;
        this.agentAIScanService = agentAIScanService;
    }

    /**
     * 
     */
  
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
  

    public void stopSingleton(String gracefulShutdown) {
        // XXX: shut down services
    }

    private void startAvailCheckService() {
        try {
          availabilityCheckService.startSchedule();

        } catch (Exception e) {
            log.info("Unable to start service: " + e);
        }
    }

    private void startAgentAIScanService() {
        try {
           agentAIScanService.startSchedule();
        } catch (Exception e) {
            log.info("Unable to start service: " + e);
        }
    }

}
