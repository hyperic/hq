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

import java.util.HashMap;
import java.util.Properties;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;



import com.ibm.mq.MQException;

public class MQSeriesCmdHandler {

    private HashMap cmds = new HashMap();

    private static MQSeriesCmdHandler instance = null;

    public static MQSeriesCmdHandler getInstance() {
        if (instance == null) {
            instance = new MQSeriesCmdHandler();
        }

        return instance;
    }

    public MQSeriesCmdHandler() {
        cmds.put("mqseries-mgr",
                 new CmdInquireQMgr());

        cmds.put("mqseries-queue",
                 new CmdInquireQ());
    }

    public Double getValue(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException {

        Properties props = metric.getObjectProperties();

        String mgr = props.getProperty(MQSeriesCmd.PROP_MGR);

        String domain = metric.getDomainName();

        MQSeriesCmd cmd =
            (MQSeriesCmd)this.cmds.get(domain);

        if (mgr == null) {
            throw new MetricInvalidException("Missing " + 
                                          MQSeriesCmd.PROP_MGR +
                                          " Attribute");
        }

        if (cmd == null) {
            throw new MetricInvalidException("No such command: " + domain);
        }

        MQAgent agent = null;

        try {
            agent = new MQAgent(mgr);
            return cmd.getValue(agent, metric);
        } catch (MQException e) {
            throw new MetricUnreachableException(e.getMessage(), e);
        } finally {
            if (agent != null) {
                try {
                    agent.disconnect();
                } catch (MQException e) {}
            }
        }
    }
}
