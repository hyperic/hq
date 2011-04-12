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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class AgentPluginStartupSync extends Thread
implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private static final Log log = LogFactory.getLog(AgentPluginStartupSync.class);
    private ApplicationContext ctx;
    private AgentManager agentManager;

    @Autowired
    public AgentPluginStartupSync(AgentManager agentManager) {
        super("AgentPluginStartupSync");
        setDaemon(true);
        this.agentManager = agentManager;
    }

    public void run() {
        try {
            // want all agents to be able to check in their PluginInfo before this runs
            log.info("will start agent plugin sync in 5 minutes");
            Thread.sleep(5*MeasurementConstants.MINUTE);
        } catch (InterruptedException e) {
            log.debug(e,e);
        }
        try {
            log.info("starting agent plugin sync");
            agentManager.syncAllAgentPlugins();
            log.info("agent plugin sync complete");
        } catch (Throwable t) {
            log.error("error running plugin sync to agents",t);
        }
    }

    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.ctx = ctx;
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        if(event.getApplicationContext() != this.ctx) {
            return;
        }
        start();
    }

}
