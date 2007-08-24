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

import java.util.Date;

import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.scheduler.shared.SchedulerLocal;
import org.hyperic.hq.scheduler.shared.SchedulerUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;

/**
 * Data purge service that handles the simple task of registering the class
 * that purges old measurement data.  This is a one time event, we just
 * pull the config and register the job with quartz.
 *
 * @jmx:mbean name="hyperic.jmx:type=Service,name=DataPurge"
 */
public class DataPurgeService 
    implements DataPurgeServiceMBean
{
    private static final String JOB = "Data Purge Job";
    private static final String TRIGGER = "Data Purge Trigger";
    private static final String GROUP = "Data Purge Group";

    protected Log log = LogFactory.getLog(DataPurgeService.class.getName());
    
    /* Every hour at 10 after */
    private String schedule = "0 10 0-23 * * ?";

    public DataPurgeService() {}

    /**
     * @jmx:managed-operation
     */
    public void stop() {}

    /**
     * @jmx:managed-operation
     */
    public void start() {}

    /**
     * @jmx:managed-operation
     */
    public void startPurgeService() {

        Class jobClass;
        String jobName = ProductProperties.getProperty("hyperic.hq.dataPurge");
        try {
            jobClass = Class.forName(jobName);
        } catch (Exception e) {
            log.error("Unable to load HQ Data Manager class=" + jobName, e);
            return;
        }

        log.info("Starting HQ Data Manager Service using " + jobName);

        try {
            // Get a reference to the scheduler
            SchedulerLocal scheduler = SchedulerUtil.getLocalHome().create();

            // Unschedule previous job
            if (scheduler.getTrigger(TRIGGER, GROUP) != null) {
                scheduler.unscheduleJob(TRIGGER, GROUP);
            }
                        
            JobDetail job = new JobDetail(JOB, GROUP, jobClass);

            // See CronTrigger javadoc for more info
            CronTrigger trigger = new CronTrigger(TRIGGER, GROUP, JOB, 
                                                  GROUP, new Date(), 
                                                  null, this.getSchedule());
            trigger.setMisfireInstruction(CronTrigger.
                MISFIRE_INSTRUCTION_DO_NOTHING);

            scheduler.scheduleJob(job, trigger);
        } catch (Exception e) {
            // This probably isnt fatal.  
            this.log.error("Unable to start HQ Data Manager Service", e);
        }
    }

    /**
     * @jmx:managed-operation
     */
    public void init() {}

    /**
     * @jmx:managed-operation
     */
    public void destroy() {}

    /**
     * @jmx:managed-attribute
     */
    public String getSchedule() {
        return schedule;
    }
    /**
     * @jmx:managed-attribute
     */
    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }
}
