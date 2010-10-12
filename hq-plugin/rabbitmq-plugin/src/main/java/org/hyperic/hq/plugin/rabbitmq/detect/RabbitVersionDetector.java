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
package org.hyperic.hq.plugin.rabbitmq.detect;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.springframework.util.Assert;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RabbitVersionDetector does a 'best effort' to attempt to gain the serve
 * version without relying on any pre-configured information by the user.
 * We run through a few scenarios to infer server version.
 * @author Helena Edelson
 */
public class RabbitVersionDetector {

    private static final Log logger = LogFactory.getLog(RabbitVersionDetector.class);

    /**
     * Make several attempts to acquire the version - users who build from source
     * versus install binary, various operating system install layouts - the version
     * is very difficult to obtain.
     * @param rabbitHome
     * @param node
     * @return version of RabbitMQ we are auto-detecting.
     */
    public static String detectVersion(String rabbitHome, String node) {
        String version = inferVersionFromRabbitmqctl(rabbitHome);

        if (version == null) {
            version = inferVersionFromRabbitAppFile(rabbitHome);
        }

        Assert.notNull(version, "RabbitMQ version must not be null.");
        Assert.hasText(version, "RabbitMQ version must have text.");

        return version;
    }

    /**
     * @param rabbitHome
     * @return
     * @throws PluginException
     * @throws InterruptedException
     */
    protected static String inferVersionFromRabbitmqctl(String rabbitHome) {
        Assert.hasText(rabbitHome);
        String version = null;

        try {

            Process process = Runtime.getRuntime().exec("rabbitmqctl status");

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
            logger.info("Unable to dermine version from rabbitmqctl " + e);
        }

        return version;
    }

    /**
     * Users who compile the broker from source will not have a valid version string such as:
     * {application,rabbit, [{description,"RabbitMQ"},{id,"RabbitMQ"},{vsn,"1.8.1"},
     * but rather will end up with a string such as "%%VSN%%".
     * @param rabbitHome
     * @return
     */
    protected static String inferVersionFromRabbitAppFile(String rabbitHome) {
        Assert.hasText(rabbitHome);

        String version = null;

        StringBuilder app = new StringBuilder(rabbitHome).append(File.separator).append("ebin").append(File.separator);

        try {
            File file = null;

            File fromBin = new File(app.append("rabbit.app").toString());
            if (fromBin.exists()) {
                file = fromBin;
            }
            File fromSrc = new File(app.append("rabbit_app.in").toString());
            if (fromSrc.exists()) {
                file = fromSrc;
            }

            if (file != null && file.exists()) {

                BufferedReader in = new BufferedReader(new FileReader(file));

                String input = null;
                while ((input = in.readLine()) != null) {
                    if (input.contains("{vsn")) {
                        Pattern p = Pattern.compile("[{]vsn,\\s*\"(\\d+\\.\\d+(?:\\.\\d+)?)\"}");
                        Matcher m = p.matcher(input);
                        version = m.find() ? m.group(1) : null;
                    }
                }
                in.close();
            }

        } catch (IOException e) {
            logger.info("Unable to dermine version from rabbitmqclt " + e);
        }

        return version;
    }

}
