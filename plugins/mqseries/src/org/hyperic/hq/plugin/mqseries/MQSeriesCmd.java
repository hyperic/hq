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

import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;

import com.ibm.mq.MQException;

import com.ibm.mq.pcf.PCFMessage;

public abstract class MQSeriesCmd {

    public static final Double AVAIL_DOWN =
        new Double(Metric.AVAIL_DOWN);

    public static final Double AVAIL_UP =
        new Double(Metric.AVAIL_UP);

    public static final String ATTR_AVAIL = "Availability";

    public static final String PROP_MGR = "Mgr";

    public abstract Double getValue(MQAgent agent, Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException;

    public Map getCacheValues(Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException {

        return new HashMap(); //XXX
    }

    public PCFMessage[] sendRequest(MQAgent agent,
                                    PCFMessage request)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException {

        try {
            return agent.send(request);
        } catch (MQException e) {
            throw new MetricUnreachableException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MetricUnreachableException(e.getMessage(), e);
        }
    }
}
