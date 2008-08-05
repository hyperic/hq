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

package org.hyperic.hq.plugin.wsmq;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

import com.ibm.mq.MQException;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.CMQC;
import com.ibm.mq.pcf.CMQCFC;
import com.ibm.mq.pcf.PCFException;
import com.ibm.mq.pcf.PCFMessage;

public class CmdInquireCH extends MQSeriesCmd {
	private Log log = LogFactory.getLog(CmdInquireCH.class);

	public static final String PROP_CH = "Channel";

	private static final int[] ATTRS = {MQConstants.MQIACH_MSGS}; // { CMQC.MQIA_CURRENT_Q_DEPTH, CMQC.MQIA_OPEN_INPUT_COUNT, CMQC.MQIA_OPEN_OUTPUT_COUNT, };

	private static final String[] ATTR_NAMES = { "Msgs" };

	public Double getValue(MQAgent agent, Metric metric) throws PluginException, MetricNotFoundException, MetricUnreachableException {

		String attr = metric.getAttributeName();

		Map values = getCacheValues(metric);

		if (values.isEmpty()) {
			String qname = metric.getObjectProperty(PROP_CH);

			PCFMessage request = getRequest(qname);

			try {
				PCFMessage[] responses = agent.send(request);
				log.debug(Arrays.asList(responses));
				for (int i = 0; i < ATTRS.length; i++) {
					int value;

					value = responses[0].getIntParameterValue(ATTRS[i]);
					values.put(ATTR_NAMES[i], new Double(value));

				}
				// XXX mejorar
				log.debug("MQIACH_CHANNEL_STATUS -> "+responses[0].getIntParameterValue(MQConstants.MQIACH_CHANNEL_STATUS));
				values.put(ATTR_AVAIL, new Double((responses[0].getIntParameterValue(MQConstants.MQIACH_CHANNEL_STATUS) == MQConstants.MQCHS_RUNNING) ? Metric.AVAIL_UP : Metric.AVAIL_DOWN));
			} catch (MQException e) {
				if (e.reasonCode == MQConstants.MQRCCF_CHL_STATUS_NOT_FOUND)
					values.put(ATTR_AVAIL, new Double(Metric.AVAIL_PAUSED));
				else
					throw new MetricNotFoundException(e.getMessage(), e);
			} catch (IOException e) {
				throw new MetricNotFoundException(e.getMessage(), e);
			}
		}

		Double value = (Double) values.get(attr);

		if (value == null) {
			value=new Double(MetricValue.VALUE_NONE);
			log.error("Unavailable attribute: " + attr);
		}

		return value;
	}

	private PCFMessage getRequest(String name) {
		PCFMessage msg = new PCFMessage(MQConstants.MQCMD_INQUIRE_CHANNEL_STATUS);
		log.debug("MQConstants.MQCACH_CHANNEL_NAME => " + name);
		msg.addParameter(MQConstants.MQCACH_CHANNEL_NAME, name);

		return msg;
	}
}
