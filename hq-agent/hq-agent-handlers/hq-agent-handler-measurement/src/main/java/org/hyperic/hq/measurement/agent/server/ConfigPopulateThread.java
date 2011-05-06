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

package org.hyperic.hq.measurement.agent.server;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.client.MeasurementCallbackClient;
import org.hyperic.hq.measurement.shared.MeasurementConfigEntity;
import org.hyperic.hq.measurement.shared.MeasurementConfigList;

import org.hyperic.hq.product.ConfigTrackPlugin;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.LogTrackPluginManager;
import org.hyperic.hq.product.ConfigTrackPluginManager;
import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is used to query the HQ server for the configurations
 * that HQ knows about (for entities which the agent should be
 * servicing).  It retries until it can get the configurations, after
 * which it will exit.  Really only useful when the agent started up.
 *
 * This class will only return those entities that have been configured
 * for log or config file tracking.
 */
class ConfigPopulateThread 
    extends Thread 
{
    private static final long MAX_TIME_TO_SLEEP = 10 * 60 * 1000;

    private MeasurementCallbackClient client;
    private LogTrackPluginManager  ltManager;
    private ConfigTrackPluginManager ctManager;

    private Log log = LogFactory.getLog(ConfigPopulateThread.class.getName());

    ConfigPopulateThread(MeasurementCallbackClient client, 
                         LogTrackPluginManager ltManager,
                         ConfigTrackPluginManager ctManager)
    {
        this.client  = client;
        this.ltManager = ltManager;
        this.ctManager = ctManager;
    }
    
    public void run(){
        MeasurementConfigEntity[] ents;
        MeasurementConfigList configs;
        long timeToSleep = 10 * 1000;

        this.log.info("Starting config populate thread");

        while(true){
            try {
                configs = this.client.getMeasurementConfigs();
                break;
            } catch(Exception exc){
                this.log.warn("Unable to get entities for agent: " + 
                              exc.getMessage());
                this.log.warn("Sleeping for " + timeToSleep / 1000 + 
                              " seconds to fetch entities");
                timeToSleep *= 2;
                if(timeToSleep > MAX_TIME_TO_SLEEP)
                    timeToSleep = MAX_TIME_TO_SLEEP;
            }

            try {
                Thread.sleep(timeToSleep);
            } catch(InterruptedException exc){
                this.log.warn("Interrupted ConfigPopulateThread");
                return;
            }
        }

        ents = configs.getEntities();
        for(int i=0; i < ents.length; i++){
            ConfigResponse config;

            try {
                AppdefEntityID id;
                byte[] encConfig;

                encConfig = ents[i].getConfig();
                if(encConfig == null || encConfig.length == 0)
                    continue;

                config = ConfigResponse.decode(encConfig);
                id = new AppdefEntityID(ents[i].getPluginName());
                this.log.info("Received measurement configuration for " +
                              ents[i].getPluginType() + " '" + 
                              ents[i].getPluginName() + "'");


                if(ConfigTrackPlugin.isEnabled(config, id.getType())) {
                    this.log.info("Creating config track plugin " + 
                                  ents[i].getPluginName());
                    this.ctManager.createPlugin(ents[i].getPluginName(),
                                                ents[i].getPluginType(),
                                                config);
                }

                if(LogTrackPlugin.isEnabled(config, id.getType())) {
                    this.log.info("Creating log track plugin " + 
                                  ents[i].getPluginName());
                    this.ltManager.createPlugin(ents[i].getPluginName(),
                                                ents[i].getPluginType(),
                                                config);
                }
            } catch(Exception exc){
                this.log.error("Unable to create plugin: " +
                               ents[i].getPluginName() + " '" +
                               ents[i].getPluginType() + "':" +
                               exc.getMessage(), exc);
                continue;
            }

        }
    }
}
