/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.stats;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.context.Bootstrap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class ConcurrentStatsWriter
extends AbstractStatsWriter
implements ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {
	private static final Log log = LogFactory.getLog(ConcurrentStatsWriter.class);
	private static final String FILE_BASENAME = "hqstats";
	
	private ApplicationContext applicationContext;

    private TaskScheduler taskScheduler;
    
    @Autowired
    public ConcurrentStatsWriter(ConcurrentStatsCollector concurrentStatsCollector, 
                                 @Value("#{scheduler}")TaskScheduler taskScheduler) {
        super(concurrentStatsCollector, Retention.YEARLY, FILE_BASENAME);
        this.taskScheduler = taskScheduler;
    }
    
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() != this.applicationContext) {
            return;
        }
        startWriter();
    }

    @Override
    protected ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
        return taskScheduler.scheduleWithFixedDelay(task, startTime, delay);
    }

    @Override
    protected String getAndSetupBasedir() {
    	final char fs = File.separatorChar;
    	String basedir = null;
    	try {
            // Start 2 levels up from where the webapp is deployed (engine home)
            String restartStorageDir =
                Bootstrap.getResource("/").getFile().getParentFile().getParentFile().getAbsolutePath();
            String logDir = "logs";
            File logDirectory = new File(restartStorageDir + fs + logDir);
            if (!(logDirectory.exists())) {
                logDirectory.mkdir();
            }
            String logSuffix = logDir + fs + "hqstats" + fs;
            basedir = restartStorageDir + fs + logSuffix;
            log.info("using hqstats baseDir " + basedir);
            final File dir = new File(basedir);
            if (!dir.exists()) {
                dir.mkdir();
            }
        } catch (IOException e) {
            log.error("Error setting up stats directory for logging.  Stats will not be logged: " + e, e);
        }
        return basedir;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
       this.applicationContext = applicationContext;
    }

}