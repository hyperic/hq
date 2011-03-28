package org.hyperic.hq.amqp.unit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.security.SecurityUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

/**
 * Example possible bindings:
 * hq.#, hq.agents.#, hq.servers.#
 * hq.agents.*.operations.* , hq.agent.*.operations.#, hq.agent.*.operations.metrics.#
 *
 * @author Helena Edelson
 */
public class RoutingKeyTests {

    protected final Log logger = LogFactory.getLog(this.getClass().getName());

    protected final int agents = 1000;


    protected final String agentRoutingKeyPrefix = "hq.agents.agent-";

    protected String serverRoutingKeyPrefix = "hq.servers.server-";

    protected final String[] agentOperations = {
            "metrics.report.request", "metrics.availability.request", "metrics.schedule.response", "metrics.unschedule.response", "metrics.config.response",
            "scans.runtime.request", "scans.default.request", "scans.autodiscovery.start.response", "scans.autodiscovery.stop.response", "scans.autodiscovery.config.response",
            "ping.request", "user.authentication.request", "config.authentication.request", "config.registration.request",
            "config.upgrade.response", "config.bundle.request", "config.restart.response", "config.update.request",
            "events.track.log.request", "events.track.config.request",
            "controlActions.results.request", "controlActions.config.response", "controlActions.execute.response",
            "plugin.metadata.request", "plugin.liveData.request",
            "plugin.control.add.response", "plugin.track.add.response", "plugin.track.remove.response"
    };

    protected final String[] serverOperations = {
            "metrics.report.response", "metrics.availability.response", "metrics.schedule.request", "metrics.unschedule.request", "metrics.config.request",
            "scans.runtime.response", "scans.default.response", "scans.autodiscovery.start.request", "scans.autodiscovery.stop.request", "scans.autodiscovery.config.request",
            "ping.response", "user.authentication.response", "config.authentication.response", "config.registration.response",
            "config.upgrade.request", "config.bundle.response", "config.restart.request", "config.update.response",
            "events.track.log.response", "events.track.config.response",
            "controlActions.results.response", "controlActions.config.request", "controlActions.execute.request",
            "plugin.metadata.response", "plugin.liveData.response", "plugin.control.add.request",
            "plugin.track.add.request", "plugin.track.remove.request"
    };

    @Before
    public void prepare() throws UnknownHostException {
        logger.debug("Created routing keys for " + agentOperations.length + " agent and " + serverOperations.length + " server operations"); 
        this.serverRoutingKeyPrefix += InetAddress.getLocalHost().getHostAddress();
    }

    /**
     * Each agent will generate a  unique set of routing keys in order to bind
     * it's exchange to the existing agent exchanges which will route to their respective queues
     * and eventually route messages to the server and to bind its queue to the existing
     * server exchanges
     * @throws UnsupportedEncodingException
     */
    @Test
    public void agentRoutingKeyLength() throws UnsupportedEncodingException {
        for (int count = 0; count < agents; count++) {
            String agentToken = SecurityUtil.generateRandomToken();

            for (String operation : agentOperations) {
                String routingKey = new StringBuilder(agentRoutingKeyPrefix).append(agentToken).append(".operations.").append(operation).toString();
                //System.out.println(routingKey);
                testKey(routingKey);
            }
        }
    }

    @Test
    public void serverRoutingKeyLength() {
        for (String operation : serverOperations) {
            String routingKey = new StringBuilder(serverRoutingKeyPrefix).append(".operations.").append(operation).toString();
            //System.out.println(routingKey);
            testKey(routingKey);
        }
    }

    /**
     * TODO improve these assertion tests for accuracy
     * @param routingKey
     * @throws UnsupportedEncodingException
     */
    private void testKey(String routingKey) {
        try {
            assertTrue(routingKey.getBytes("UTF8").length <= 255);
            assertTrue(routingKey.getBytes("UTF16").length <= 255);
        } catch (UnsupportedEncodingException e) {
            logger.error("", e);
        }
    }

    /**
     * What is actually returned from AgentDao is indeed unique as it
     * checks agaist existing agent tokens for uniqueness before returning
     * a token String.
     */
    @Test
    public void agentTokenDuplication() {
        int failures = 0;
        String agentToken = null;

        for (int count = 0; count < agents; count++) {
            String tmp = SecurityUtil.generateRandomToken();

            if (tmp.equalsIgnoreCase(agentToken)) {
                logger.debug("Generated agent token already exists. Attempt=" + count);
                failures++;
            } else {
                assertFalse(tmp.equalsIgnoreCase(agentToken));
                agentToken = tmp;
                assertEquals(agentToken.length(), agentToken.getBytes().length);
            }
        }
        logger.debug(failures + " failures out of " + agents + " attempts");
    }
}
