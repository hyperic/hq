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
import org.hyperic.hq.hqapi1.types.ResourcePrototype;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServiceResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.hyperic.hq.plugin.rabbitmq.core.DetectorConstants;

/**
 * RabbitTransientResourceManager deletes transient RabbitMQ services
 * from the HQ inventory. It requires HQApi to be configured in the
 * agent.properties for this functionality to work properly.
 * 
 * @author Patrick Nguyen
 * 
 */
public class RabbitTransientResourceManager implements TransientResourceManager {

    private static final Log logger = LogFactory.getLog(RabbitTransientResourceManager.class);
    private static final String RABBITMQ_TYPE = "RabbitMQ";
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

        if (rabbitResources == null) {
            return;
        }

        int numResourcesDeleted = 0;

        try {
            Resource rabbitMQ = getRabbitMQServer();

            if (rabbitMQ == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Could not find a RabbitMQ server");
                }
                return;
            }

            List<String> resources = new ArrayList();
            for (ServiceResource s : rabbitResources) {
                logger.info(s.getType() + s.getName());
                resources.add(s.getType() + s.getName());
                if (logger.isDebugEnabled()) {
                    logger.debug("HQ RabbitMQ service={"
                            + "type=" + s.getName()
                            + ", name=" + s.getName()
                            + "}");
                }
            }

            Collections.sort(resources);
            for (Resource service : rabbitMQ.getResource()) {
                String sname = service.getResourcePrototype().getName() + service.getName();
                if (Collections.binarySearch(resources, sname) < 0) {
                    commandsClient.deleteResource(service);
                    logger.debug("HQ RabbitMQ service deleted={"
                            + "type=" + service.getResourcePrototype().getName()
                            + ", name=" + service.getName()
                            + "}");
                    numResourcesDeleted++;
                }
            }
        } catch (Exception e) {
            // TODO: log here?
            throw e;
        } finally {
            if (numResourcesDeleted > 0) {
                logger.info(numResourcesDeleted + " transient RabbitMQ services deleted");
            }
        }
    }

    private Resource getRabbitMQServer()
            throws IOException, PluginException {

        Resource rabbit = null;
        ResourcePrototype rezProto = commandsClient.getResourcePrototype(RABBITMQ_TYPE);
        List<Resource> resources = commandsClient.getResources(rezProto, true, true);

        for (Resource r : resources) {
            logger.debug(r.getName());
            if (isResourceConfigMatch(r.getResourceConfig(), props)) {
                rabbit = r;
                break;
            }
        }

        return rabbit;
    }

    private boolean isResourceConfigMatch(List<ResourceConfig> configs, Properties props) {
        boolean portMatches = false;
        boolean addrMatches = false;

        for (ResourceConfig c : configs) {
            if (DetectorConstants.PORT.equals(c.getKey())) {
                if (c.getValue().equals(props.get(DetectorConstants.PORT))) {
                    portMatches = true;
                }
            } else if (DetectorConstants.ADDR.equals(c.getKey())) {
                if (c.getValue().equals(props.get(DetectorConstants.ADDR))) {
                    addrMatches = true;
                }
            }
        }

        return portMatches && addrMatches;
    }
}
