package org.hyperic.hq.plugin.wsmq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

import com.ibm.mq.MQException;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.pcf.CMQCFC;
import com.ibm.mq.pcf.PCFMessage;
import com.ibm.mq.pcf.PCFMessageAgent;

public class MQSeriesChannelService {
	private static Log log = LogFactory.getLog(MQSeriesChannelService.class);
	private String name;
	private int type;
	
	public MQSeriesChannelService(String name, int type) {
		super();
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public int getType() {
		return type;
	}
	
	public static List findChannels(String mgrName) throws PluginException {
		PCFMessageAgent agent = null;
		List names = new ArrayList();

		try {
			agent = new PCFMessageAgent(mgrName);

			PCFMessage request = new PCFMessage(CMQCFC.MQCMD_INQUIRE_CHANNEL_NAMES);
			request.addParameter(MQConstants.MQCACH_CHANNEL_NAME, "*");
			request.addParameter(MQConstants.MQIACH_CHANNEL_TYPE, MQConstants.MQCHT_ALL);

			PCFMessage[] responses = agent.send(request);

			for (int i = 0; i < responses.length; i++) {
				log.trace("responses["+i+"]="+responses[i]);
				int[] type = (int[]) responses[i].getParameterValue(MQConstants.MQIACH_CHANNEL_TYPES);
				String[] name = (String[]) responses[i].getParameterValue(MQConstants.MQCACH_CHANNEL_NAMES);
				
				for(int n=0;n<name.length;n++)
				{
					log.debug(name[n]+" => "+type[n]);
					if(!name[n].startsWith("SYSTEM")) // ignore system channels.
						names.add(new MQSeriesChannelService(name[n].trim(),type[n])); 
				}
			}
			return names;
		} catch (MQException e) {
			// XXX is there an mq class with these constants?
			final int QMGR_NOT_AVAIL = 2059;
			if (e.reasonCode == QMGR_NOT_AVAIL) {
				// queue manager is not running, cannot discover queues.
				return names;
			}
			throw new PluginException(e.getMessage(), e);
		} catch (IOException e) {
			throw new PluginException(e.getMessage(), e);
		} finally {
			if (agent != null) {
				try {
					agent.disconnect();
				} catch (MQException de) {
				}
			}
		}
	}

}
