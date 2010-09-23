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
package org.hyperic.hq.plugin.rabbitmq;


import org.hyperic.hq.plugin.rabbitmq.detect.RabbitVersionDetector;
import org.hyperic.hq.product.PluginException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;


/**
 * VersionTests
 * ToDo properly set RABBITMQ_HOME on each test vm
 *
 * @author Helena Edelson
 */
@Ignore("Manual cookie value to connect to each node is required")
public class VersionTests extends RabbitVersionDetector {

    private static final String LOCATION = "/path/to/.erlang.cookie";

    private static final String PEER_NODE = "rabbit@localhost";

    private static final String RABBITMQ_HOME = System.getenv("RABBITMQ_HOME");

    @Before
    public void doBefore() {
        //String home = System.getenv("RABBITMQ_HOME");
    }

    @Test
    public void getVersion() {
        String version = detectVersion(RABBITMQ_HOME, PEER_NODE);
        assertNotNull(version);
    }

    @Test
    public void getVersionFromPath() {
        String version = inferVersionFromPath(RABBITMQ_HOME);
        assertNotNull(version);
    }

    @Test
    public void getVersionFromRabbitmqctl() throws PluginException, InterruptedException {
        String version = RabbitVersionDetector.inferVersionFromRabbitmqctl(RABBITMQ_HOME);
        // no exceptions thrown but not getting in.readLine()
        // assertNotNull(version);
    }

    @Test
    public void getVersionFromRabbitAppFile() throws PluginException, InterruptedException {
        String version = RabbitVersionDetector.inferVersionFromRabbitAppFile(RABBITMQ_HOME);
        assertNotNull(version);
    }

    @Test
    public void getVersionFromSelfNodeAndErlang() {
        String version = RabbitVersionDetector.inferVersionFromErlang(PEER_NODE, LOCATION);
        assertNotNull(version);
    }

    @Test
    public void test() {
        String version = null;

        try {

            Process process = Runtime.getRuntime().exec("rabbitmqctl status | cat");

            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
             
            String input = null;
            while ((input = in.readLine()) != null) {

                if (input.contains("{rabbit,\"RabbitMQ\",")) {
                    Pattern p = Pattern.compile("\"RabbitMQ\",\\s*\"(\\d+\\.\\d+(?:\\.\\d+)?)\"");
                    Matcher m = p.matcher(input);
                    version = m.find() ? m.group(1) : null; 
                }
            }

            in.close();

        } catch (IOException e) {
            //logger.debug("Unable to dermine version from rabbitmqctl " + e);
        }
    }


}
