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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.autoinventory.server.session.AutoinventoryManagerEJBImpl;
import org.hyperic.hq.common.SessionMBeanBase;

/**
 * This job is responsible for filling in missing availabilty metric values.
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=AgentAIScan"
 */
public class AgentAIScanService
    extends SessionMBeanBase
    implements AgentAIScanServiceMBean
{
    private Log _log = LogFactory.getLog(AgentAIScanService.class);

    /**
     * @jmx:managed-operation
     */
    public void hit(Date lDate) {
        super.hit(lDate);
    }
    
    protected void hitInSession(Date lDate) {
        _log.debug("Agent AI Scan Service started executing: "+lDate);  
        
        AutoinventoryManagerEJBImpl.getOne().notifyAgentsNeedingRuntimeScan();
        
        _log.debug("Agent AI Scan Service finished executing: "+lDate); 
    }

    /**
     * @jmx:managed-operation
     */
    public void init() {}

    /**
     * @jmx:managed-operation
     */
    public void start() {}

    /**
     * @jmx:managed-operation
     */
    public void stop() {
    }

    /**
     * @jmx:managed-operation
     */
    public void destroy() {}
}

