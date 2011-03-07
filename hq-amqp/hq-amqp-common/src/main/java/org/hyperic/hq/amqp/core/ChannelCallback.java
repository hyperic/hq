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

import java.io.IOException;

/**
 * Callback interface allowing the user access to a {@link com.rabbitmq.client.Channel}.
 * The lifecycle of a Channel, including creation and cleanup, is handled for the user. 
 * @author Helena Edelson
 */
public interface ChannelCallback<T> {

    /**
     * A {@link com.rabbitmq.client.Channel} to do some work.
     * @param channel The channel to work with
     * @return Any value that needs to be returned to the original caller
     * @throws ChannelException
     */
    T doInChannel(Channel channel) throws ChannelException, IOException;
}
