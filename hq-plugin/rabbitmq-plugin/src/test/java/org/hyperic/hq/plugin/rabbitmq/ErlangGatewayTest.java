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


import org.hyperic.hq.plugin.rabbitmq.core.*;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.admin.RabbitBrokerAdmin;

import org.springframework.erlang.ErlangBadRpcException;
import org.springframework.erlang.core.ErlangTemplate;
import org.springframework.erlang.support.converter.ErlangConversionException;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * ErlangGatewayTest
 * @author Helena Edelson
 */
@Ignore("Need to mock the connection for automation")
public class ErlangGatewayTest extends AbstractSpringTest {
 
    @Test @Ignore("These conversions are not in the spring amqp yet")
    public void testErlangTemplate() {
        ErlangTemplate erlangTemplate = new RabbitBrokerAdmin(singleConnectionFactory).getErlangTemplate();
        assertNotNull("erlangTemplate must not be null", erlangTemplate);

        try {
            Object conns = erlangTemplate.executeAndConvertRpc("rabbit_networking", "connection_info_all");
            Object bindings = erlangTemplate.executeAndConvertRpc("rabbit_exchange", "list_bindings");
            Object channels = erlangTemplate.executeAndConvertRpc("rabbit_channel", "info_all");
            Object exchanges = erlangTemplate.executeAndConvertRpc("rabbit_exchange", "list");
        }
        catch (ErlangBadRpcException e) {
            logger.debug(e);
        }
        catch (ErlangConversionException e) {
            logger.debug(e);
        } 
    }
 
    @Test
    public void getVirtualHosts() throws Exception {
        List<String> virtualHosts = erlangGateway.getVirtualHosts();
        assertNotNull(virtualHosts);
        assertTrue(virtualHosts.size() >= 0);
    }

    @Test
    public void getExchanges() throws Exception {
        RabbitBrokerAdmin admin = new RabbitBrokerAdmin(singleConnectionFactory);
        new RabbitBrokerGateway(admin).createExchange(UUID.randomUUID().toString(), ExchangeTypes.FANOUT);
        
        List<String> vHosts = erlangGateway.getVirtualHosts();
        for(String vHost : vHosts) {
            List<Exchange> exchanges = erlangGateway.getExchanges(vHost);
            assertNotNull(exchanges);
        }
    }

    @Test
    public void getConnections() throws IOException {
        com.rabbitmq.client.Connection conn = singleConnectionFactory.createConnection();
        List<Connection> cons = erlangGateway.getConnections();
        assertNotNull(cons);
        conn.close();
    }

    @Test
    public void getChannels() throws IOException {
        com.rabbitmq.client.Connection conn = singleConnectionFactory.createConnection();
        conn.createChannel();
        conn.createChannel();
        conn.close();
        List<Channel> channels = erlangGateway.getChannels();
        assertNotNull(channels);
        assertTrue(channels.size() > 1);
    }

}