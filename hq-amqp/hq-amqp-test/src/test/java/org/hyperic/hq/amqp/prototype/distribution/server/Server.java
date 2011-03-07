/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
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

package org.hyperic.hq.amqp.prototype.distribution.server;

import org.apache.log4j.Logger;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Helena Edelson
 */
public class Server {

    private static Logger logger = Logger.getLogger(Server.class);

    @Autowired private List<AmqpTemplate> templates;
 
    private int rocketsLaunched;

    @PostConstruct
    public void prepare() {
        Assert.isTrue(this.templates.size() > 1);
        buildAndSend();
    }

    private void buildAndSend() {
        List<String> rockets = RocketBuilder.build();

        for (AmqpTemplate template : templates) {  
            for (String rocket : rockets) {
                template.convertAndSend(rocket);
                rocketsLaunched++; 
            }
        }
        logger.debug("Successfully sent " + rocketsLaunched +
                " rockets to this and other galaxies far away...");
    }

    /**
     * Builds rockets initialized with destination planet.
     */
    public static class RocketBuilder {
 
        public static List<String> build() {
            List<String> rocketsTo = new ArrayList<String>();

            rocketsTo.add("Mercury");
            rocketsTo.add("Venus");
            rocketsTo.add("Earth");
            rocketsTo.add("Mars");
            rocketsTo.add("Jupiter");
            rocketsTo.add("Saturn");
            rocketsTo.add("Uranus");
            rocketsTo.add("Neptune");
            rocketsTo.add("Alfheimr");
            rocketsTo.add("Midgard");
            rocketsTo.add("Muspellheim");
            rocketsTo.add("Alderaan");

            return rocketsTo;
        }
    }
}

