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

package org.hyperic.hq.plugin.mqseries;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RuntimeDiscoverer;
import org.hyperic.hq.product.RuntimeResourceReport;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

public class MQSeriesRuntimeDiscoverer extends GenericPlugin
    implements RuntimeDiscoverer {

    private static Log log =
        LogFactory.getLog(MQSeriesRuntimeDiscoverer.class.getName());

    public RuntimeResourceReport discoverResources(int serverId,
                                                   AIPlatformValue aiplatform,
                                                   ConfigResponse config)
        throws PluginException {

        RuntimeResourceReport rrr = new RuntimeResourceReport(serverId);
        AIServiceValue[] services;

        List productServices =
            MQSeriesMgrService.findServices(MQSeriesProductPlugin.VERSION_5_NAME);

        services = new AIServiceValue[productServices.size()];
        Long nowTime = new Long(System.currentTimeMillis());

        for (int i=0; i<productServices.size(); i++) {
            MQSeriesService mqsvc =
                (MQSeriesService)productServices.get(i);

            log.debug("discovered: " + mqsvc.getFullName());

            AIServiceValue service = new AIServiceValue();
            service.setServiceTypeName(mqsvc.getTypeName());
            service.setServerId(serverId);
            service.setName("%serverName%" + " " + mqsvc.getFullName());
            //service.setAutoinventoryIdentifier(mqsvc.getName());
            service.setCTime(nowTime);
            service.setMTime(nowTime);

            try {
                Map props = mqsvc.getProductConfig();
                service.setProductConfig(new ConfigResponse(props).encode());

                service.setMeasurementConfig(ConfigResponse.EMPTY_CONFIG);
                //XXX tbd
                //service.setControlConfig(new ConfigResponse().encode());
            } catch (EncodingException e) {
                throw new PluginException(e.getMessage(), e);
            }

            services[i] = service;
        }

        AIServerExtValue thisServer = new AIServerExtValue();
        thisServer.setPlaceholder(true);
        thisServer.setId(new Integer(serverId));
        thisServer.setAIServiceValues(services);

        aiplatform.addAIServerValue(thisServer);

        rrr.addAIPlatform(aiplatform);

        return rrr;
    }
}

