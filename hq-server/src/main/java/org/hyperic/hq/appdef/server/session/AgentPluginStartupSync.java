/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2011], VMware, Inc.
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

package org.hyperic.hq.appdef.server.session;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.common.shared.TransactionRetry;
import org.hyperic.hq.measurement.MeasurementConstants;
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
public class AgentPluginStartupSync implements ApplicationContextAware,
    ApplicationListener<ContextRefreshedEvent> {

    private final Log log = LogFactory.getLog(AgentPluginStartupSync.class);
    private ApplicationContext ctx;
    private AgentManager agentManager;
    private TaskScheduler taskScheduler;
    private TransactionRetry transactionRetry;

    @Autowired
    public AgentPluginStartupSync(AgentManager agentManager,
                                  TransactionRetry transactionRetry,
                                  @Value("#{scheduler}")TaskScheduler taskScheduler) {
        this.agentManager = agentManager;
        this.taskScheduler = taskScheduler;
        this.transactionRetry = transactionRetry;
    }

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext() != this.ctx) {
            return;
        }
        taskScheduler.schedule(new Runnable() {
            public void run() {
                try {
                    log.info("starting agent plugin sync");
                    final Runnable runner = new Runnable() {
                        public void run() {
                            agentManager.syncAllAgentPlugins();
                        }
                    };
                    transactionRetry.runTransaction(runner, 3, 1000);
                    log.info("agent plugin sync complete");
                } catch (Throwable t) {
                    log.error("error running plugin sync to agents", t);
                }

            }

        }, new Date(System.currentTimeMillis() + (5 * MeasurementConstants.MINUTE)));
    }

}
