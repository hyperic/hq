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

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.common.shared.util.EjbModuleLifecycle;
import org.hyperic.hq.common.shared.util.EjbModuleLifecycleListener;

/**
 * AvailService - call the appropriate initializers when system is available
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=Avail"
 */
public class AvailService 
    implements AvailServiceMBean, MBeanRegistration, EjbModuleLifecycleListener
{
    private final Log _log = LogFactory.getLog(AvailService.class);
    private MBeanServer        _server;
    private EjbModuleLifecycle _listener;
    
    public AvailService() {}

    /**
     * @jmx:managed-operation
     */
    public void stop() {
        _log.info("Stopping AvailService");
        _listener.stop();
    }

    /**
     * @jmx:managed-operation
     */
    public void start() {
        _log.info("Starting AvailService");
        _listener = new EjbModuleLifecycle(_server, this,
                                          HQConstants.EJB_MODULE_PATTERN);   
        _listener.start();
    }

    /**
     * @jmx:managed-operation
     */
    public void restart() {
        _log.info("Restarting HighAvailService");
        ejbModuleStarted();
    }

    /**
     * @jmx:managed-operation
     */
    public void init() {}

    /**
     * @jmx:managed-operation
     */
    public void destroy() {}

    
    public ObjectName preRegister(MBeanServer server, ObjectName name) {
        _server = server;
        return name;
    }

    public void postRegister(Boolean registrationDone) {}

    public void preDeregister() {}

    public void postDeregister() {}

    public void ejbModuleStopped() {}

    public void ejbModuleStarted() {
        startSchedulerService();
        startDataPurgeService();
    }

    private void startSchedulerService() {
        startMBean("SchedulerService",
                   "hyperic.jmx:type=Service,name=Scheduler", 
                   "startScheduler");
    }
    
    private void startDataPurgeService() {
        startMBean("DataPurgeService",
                   "hyperic.jmx:type=Service,name=DataPurge", 
                   "startPurgeService");
    }
    
    private void startMBean(String shortName, String mbeanName,
                            String method) 
    {
        try {
            _log.info("Starting " + shortName);

            ObjectName objName = new ObjectName(mbeanName);
            _server.invoke(objName, method, new Object[] {}, new String[] {});
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
}
