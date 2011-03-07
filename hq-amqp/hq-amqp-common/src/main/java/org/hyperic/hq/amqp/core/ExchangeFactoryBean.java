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

package org.hyperic.hq.amqp.core;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.FactoryBean;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class ExchangeFactoryBean implements FactoryBean<String> {

    private static final String TYPE = "direct";

    private static final boolean DURABLE = true;

    private final ChannelTemplate channelTemplate;

    private final String exchangeName;

    /**
     * Creates a new {@link com.rabbitmq.client.AMQP.Exchange}
     *
     * @param connectionFactory Used to get a connection to create the queue with
     * @param exchangeName The name of the exchange
     */
    public ExchangeFactoryBean(ConnectionFactory connectionFactory, String exchangeName) {
        this.channelTemplate = new ChannelTemplate(connectionFactory);
        this.exchangeName = exchangeName;
    }

    public Class<?> getObjectType() {
        return String.class;
    }

    public boolean isSingleton() {
        return true;
    }

    //@Override
    public String getObject() throws ChannelException {
        return this.channelTemplate.execute(new ChannelCallback<String>() {

            //@Override
            public String doInChannel(Channel channel) throws ChannelException {
                try {
                    channel.exchangeDeclare(ExchangeFactoryBean.this.exchangeName, TYPE, DURABLE);
                } catch (IOException e) {
                    throw new ChannelException("Unable to declare exchange", e);
                }
                return ExchangeFactoryBean.this.exchangeName;
            }
        });
    }

}
