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

import java.util.Map;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;



import com.ibm.mq.pcf.CMQC;
import com.ibm.mq.pcf.CMQCFC;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;

public class CmdInquireQ extends MQSeriesCmd {

    public static final String PROP_Q = "Queue";

    private static final int[] ATTRS = {
        CMQC.MQIA_CURRENT_Q_DEPTH,
        CMQC.MQIA_OPEN_INPUT_COUNT,
        CMQC.MQIA_OPEN_OUTPUT_COUNT,
    };

    private static final String[] ATTR_NAMES = {
        "CurrentDepth",
        "OpenInputCount",
        "OpenOutputCount",
    };

    public Double getValue(MQAgent agent, Metric metric)
        throws PluginException,
               MetricNotFoundException,
               MetricUnreachableException {

        String attr = metric.getAttributeName();

        Map values = getCacheValues(metric);

        if (values.isEmpty()) {
            String qname = metric.getObjectProperty(PROP_Q);

            PCFMessage request = getRequest(qname);

            PCFMessage[] responses = sendRequest(agent, request);

            for (int i=0; i<ATTRS.length; i++) {
                int value;

                try {
                    value = responses[0].getIntParameterValue(ATTRS[i]);
                } catch (PCFException e) {
                    throw new MetricNotFoundException(e.getMessage(), e);
                }

                values.put(ATTR_NAMES[i], 
                           new Double(value));
            }

            values.put(ATTR_AVAIL, AVAIL_UP);
        }

        Double value = (Double)values.get(attr);
            
        if (value == null) {
            throw new MetricInvalidException("Unknown attribute: " + attr);
        }

        return value;
    }

    private PCFMessage getRequest(String name) {
        PCFMessage msg = new PCFMessage(CMQCFC.MQCMD_INQUIRE_Q);

        msg.addParameter(CMQC.MQCA_Q_NAME, name);
        msg.addParameter(CMQC.MQIA_Q_TYPE, CMQC.MQQT_LOCAL);
        msg.addParameter(CMQCFC.MQIACF_Q_ATTRS, ATTRS);

        return msg;
    }
}
