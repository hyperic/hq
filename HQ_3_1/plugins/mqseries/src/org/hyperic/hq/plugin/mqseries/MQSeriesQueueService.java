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

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Map;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeBuilder;

import com.ibm.mq.MQException;

import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;
import com.ibm.mq.pcf.CMQC;
import com.ibm.mq.pcf.CMQCFC;

public class MQSeriesQueueService
    extends MQSeriesService {

    private static final int[] ATTRS = {
        CMQC.MQCA_Q_NAME,
        CMQC.MQIA_DEFINITION_TYPE
    };

    private MQSeriesMgrService mgr;
    private String name;
    private String fullname;
    private String type;

    private Properties productConfig = new Properties();

    public MQSeriesQueueService(MQSeriesMgrService mgr) {
        this.mgr = mgr;
    }

    public String getTypeName() {
        return this.type;
    }

    public String getName() {
        return this.name;
    }

    public String getFullName() {
        return this.fullname;
    }

    public Map getProductConfig() {
        this.productConfig.setProperty(MQSeriesProductPlugin.PROP_Q_NAME,
                                       getName());

        this.productConfig.putAll(this.mgr.getProductConfig());

        return this.productConfig;
    }

    private static List findQueues(String mgrName)
        throws PluginException {

        PCFMessageAgent	agent = null;
        List names = new ArrayList();

        try {
            agent = new PCFMessageAgent(mgrName);

            PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);
            request.addParameter(CMQC.MQCA_Q_NAME, "*");
            request.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
            request.addParameter(CMQCFC.MQIACF_Q_ATTRS, ATTRS);

            PCFMessage[] responses = agent.send(request);

            for (int i = 0; i < responses.length; i++) {
                String name;
                int type = responses[i].
                    getIntParameterValue(CMQC.MQIA_DEFINITION_TYPE);

                if (type == CMQC.MQQDT_TEMPORARY_DYNAMIC) {
                    //this type of queue will come and go.
                    continue;
                }

                name = responses[i].getStringParameterValue(CMQC.MQCA_Q_NAME);

                names.add(name.trim()); //queue names have trailing spaces
            }

            return names;
        } catch (MQException e) {
            //XXX is there an mq class with these constants?
            final int QMGR_NOT_AVAIL = 2059;
            if (e.reasonCode == QMGR_NOT_AVAIL) {
                //queue manager is not running, cannot discover queues.
                return names;
            }
            throw new PluginException(e.getMessage(), e);
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        } finally {
            if (agent != null) {
                try {
                    agent.disconnect();
                } catch (MQException de) { }
            }
        }
    }

    public static List findServices(MQSeriesMgrService mgr)
        throws PluginException {

        List names = findQueues(mgr.getName());

        ArrayList services = new ArrayList();

        String type =
            TypeBuilder.composeServiceTypeName(mgr.getServerType(),
                                               MQSeriesProductPlugin.Q_NAME);

        for (int i=0; i<names.size(); i++) {
            MQSeriesQueueService queue =
                new MQSeriesQueueService(mgr);

            queue.name = (String)names.get(i);

            queue.type = type;

            queue.fullname = queue.name;

            services.add(queue);
        }

        return services;
    }
}
