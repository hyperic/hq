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
 */

package org.hyperic.hq.operation.rabbit;

import com.rabbitmq.client.Address;
import com.rabbitmq.client.ConnectionFactory;
import org.hyperic.hq.operation.rabbit.connection.ConnectionStatus;
import org.hyperic.hq.operation.rabbit.connection.RabbitConnectionFactory;
import org.hyperic.hq.operation.rabbit.connection.SecureRabbitConnectionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RabbitDetectorTest {
    public final Address[] validCluster = new Address[]{
            new Address("localhost", ConnectionFactory.DEFAULT_AMQP_PORT)};

    public final Address[] invalidCluster = new Address[]{
            new Address("localhost", ConnectionFactory.DEFAULT_AMQP_PORT),
            new Address("localhost", ConnectionFactory.DEFAULT_AMQP_PORT + 1)};

    private RabbitConnectionFactory valid;

    private SecureRabbitConnectionFactory invalid;

    private AbstractApplicationContext ctx;

    @Before
    public void prepare() {
        ctx = new AnnotationConfigApplicationContext(Config.class);
        valid = ctx.getBean(RabbitConnectionFactory.class);
        invalid = ctx.getBean(SecureRabbitConnectionFactory.class);
    }

    @After
    public void after() {
        ctx.destroy();
    }

    @Test
    public void failSuccess() {
        ConnectionStatus cs = valid.isActive(validCluster);
        assertTrue(cs.isActive());

        ConnectionStatus cs1 = invalid.isActive(invalidCluster);
        assertFalse(cs1.isActive());

        ///invalidConfigFactory.newConnection();

        ctx.destroy();
    }

    @Configuration
    public static class Config {

        @Bean
        public RabbitConnectionFactory valid() {
            return new RabbitConnectionFactory();
        }

        @Bean
        public SecureRabbitConnectionFactory invalid() {
            return new SecureRabbitConnectionFactory("invalid", "user");
        }
    }
}
