/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.bizapp.server.session;

import java.util.concurrent.ScheduledFuture;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.UpdateBoss;
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
public class UpdateFetcher
implements Runnable, ApplicationListener<ContextRefreshedEvent>, ApplicationContextAware {

    private final Log _log = LogFactory.getLog(UpdateFetcher.class);
    private final UpdateBoss updateBoss;
    private final long checkInterval;
    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> updateTask ; 
    private ApplicationContext applicationContext;

    @Autowired
    public UpdateFetcher(UpdateBoss updateBoss,
                         @Value("#{tweakProperties['hq.updateNotify.interval'] }") Long checkInterval,
                         @Value("#{scheduler}") TaskScheduler taskScheduler) {
        this.updateBoss = updateBoss;
        this.checkInterval = checkInterval;
        this.taskScheduler = taskScheduler;
    }

    public void run() {
        try {       
        	// HHQ-5974 - Remove sending post to http://updates.hyperic.com/hq-updates
            _log.info("Hyperic does not check updates.");
        } catch (Exception e) {
            _log.warn("Error getting update notification", e);
        }
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (applicationContext == event.getApplicationContext()) {
            this.updateTask = taskScheduler.scheduleAtFixedRate(this, checkInterval);
        }
    }
    
    @PreDestroy
    public final void destroy() { 
        this.updateTask.cancel(true /*mayInterruptIfRunning*/) ; 
    }//EOM 

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
