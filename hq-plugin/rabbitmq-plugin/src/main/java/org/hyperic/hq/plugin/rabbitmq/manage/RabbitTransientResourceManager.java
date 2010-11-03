/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.rabbitmq.manage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.bizapp.agent.client.HQApiCommandsClient;
import org.hyperic.hq.bizapp.agent.client.HQApiFactory;
import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.hqapi1.types.Resource;
import org.hyperic.hq.hqapi1.types.ResourceConfig;
import org.hyperic.hq.hqapi1.types.ResourceProperty;
import org.hyperic.hq.hqapi1.types.ResourcePrototype;
import org.hyperic.hq.hqapi1.types.ResourcesResponse;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServiceResource;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * RabbitTransientResourceManager is in development.
 * 
 */
public class RabbitTransientResourceManager implements TransientResourceManager {

	private static final Log logger = LogFactory.getLog(RabbitTransientResourceManager.class);

	private static final String RABBITMQ_TYPE = "RabbitMQ";

	private static final String NODE_PATH_PROPERTY = "node.path";
	private static final String SERVER_PATH_PROPERTY = "server.path";
	private static final String USERNAME_CONFIG = "username";
	private static final String PASSWORD_CONFIG = "password";

	private HQApiCommandsClient commandsClient;
	private Properties props;

	public RabbitTransientResourceManager(Properties props)
		throws PluginException {

		this.props = props;
		HQApi api = HQApiFactory.getHQApi(AgentDaemon.getMainInstance(), props);
		this.commandsClient = new HQApiCommandsClient(api);
	}

	public void syncServices(List<ServiceResource> rabbitResources)
		throws Exception {

		try {
			Resource rabbitMQ = getRabbitMQServer();

			if (rabbitMQ == null) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not find a RabbitMQ server");
				}
				return;
			}

			for (Resource service : rabbitMQ.getResource()) {
				if (isTransientResource(service, rabbitResources)) {
					commandsClient.deleteResource(service);
				}
			}
		} catch (Exception e) {
			// TODO: log here?
			throw e;
		}
	}

	private boolean isTransientResource(Resource hqResource, List<ServiceResource> rabbitResources) {
		boolean isTransient = true;

		// TODO: Need to optimize
		for (ServiceResource s : rabbitResources) {
			if (s.getType().equals(hqResource.getResourcePrototype().getName())
					&& s.getName().equals(hqResource.getName())) {
				isTransient = false;
				break;
			}
		}

		return isTransient;
	}

	private Resource getRabbitMQServer() 
		throws IOException, PluginException {

		Resource rabbit = null;
		ResourcePrototype rezProto = commandsClient.getResourcePrototype(RABBITMQ_TYPE);
		List<Resource> resources = commandsClient.getResources(rezProto, true, true);

		for (Resource r : resources) {
			if (isResourcePropertyMatch(r.getResourceProperty(), props)
					&& isResourceConfigMatch(r.getResourceConfig(), props)) {
				rabbit = r;
				break;
			}
		}

		return rabbit;
	}

	private boolean isResourcePropertyMatch(List<ResourceProperty> resourceProps, Properties props) {
		boolean nodePathMatches = false;

		for (ResourceProperty p : resourceProps) {
			if (NODE_PATH_PROPERTY.equals(p.getKey())) {
				if (p.getValue().equals(props.get(SERVER_PATH_PROPERTY))) {
					nodePathMatches = true;
					break;
				}
			}
		}

		return nodePathMatches;
	}

	private boolean isResourceConfigMatch(List<ResourceConfig> configs, Properties props) {
		boolean usernameMatches = false;
		boolean passwordMatches = false;

		for (ResourceConfig c : configs) {
			if (USERNAME_CONFIG.equals(c.getKey())) {
				if (c.getValue().equals(props.get(USERNAME_CONFIG))) {
					usernameMatches = true;
				}
			} else if (PASSWORD_CONFIG.equals(c.getKey())) {
				if (c.getValue().equals(props.get(PASSWORD_CONFIG))) {
					passwordMatches = true;
				}
			}
		}

		return usernameMatches && passwordMatches;
	}

}