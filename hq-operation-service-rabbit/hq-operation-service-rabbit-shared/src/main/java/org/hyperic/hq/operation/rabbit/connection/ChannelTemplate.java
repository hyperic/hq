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

package org.hyperic.hq.operation.rabbit.connection;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public final class ChannelTemplate implements ChannelOperations {

    private final Log logger = LogFactory.getLog(getClass());

    /**
     * ConnectionFactory to create {@link com.rabbitmq.client.Connection connections}.
     */
    private final com.rabbitmq.client.ConnectionFactory connectionFactory;

    /**
     * Creates a new instance
     * @param connectionFactory {@link com.rabbitmq.client.ConnectionFactory}
     */
    public ChannelTemplate(com.rabbitmq.client.ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Execute an action while managing a {@link Channel}'s lifecycle: instantiate, configure, and tear down.
     * @param action to call the channel
     * @return The return value from the callback
     * @throws ChannelException
     */
    public <T> T execute(ChannelCallback<T> action) throws ChannelException {
        if (action == null) throw new IllegalArgumentException("Callback object must not be null");

        Channel channel = null;

        try {
            channel = createChannel();
            return action.doInChannel(channel);
        }
        catch (IOException e) {
            throw new ChannelException("Unable to create channel", e);
        }
        finally {
            releaseResources(channel);
        }
    }

    public Channel createChannel() throws ChannelException {
        try {
            return this.connectionFactory.newConnection().createChannel();
        } catch (IOException e) {
            logger.error(e);
            throw new ChannelException("Unable to create channel", e);
        }
    }

    public boolean validateCredentials() {
        Channel channel = null;
        try {
            channel = createChannel();
            return channel != null;
        }
        catch (Exception e) {
            logger.error("Unable to connect with username=" + this.connectionFactory.getUsername()
                    + " password=" + this.connectionFactory.getPassword()
                    + " host=" + this.connectionFactory.getHost()
                    + " port=" + this.connectionFactory.getPort(), e);
            return false;
        }
        finally {
            releaseResources(channel);
        }
    }

    /**
     * Close the given Channel and Connection.
     * @param channel Channel to close (may be <code>null</code>)
     */
    public void releaseResources(Channel channel) {
        if (channel == null) return;

        try {
            channel.close();
            closeConnection(channel.getConnection()); 
        }
        catch (Exception e) {
            logger.debug("Connection is already closed.", e);
        }
    }

    private void closeConnection(Connection conn) {
        if (conn == null) return;

        try {
            conn.close();
        }
        catch (IOException e) {
            logger.error(e);
        }
    }

}
