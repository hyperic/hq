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
 *  along with this program; if not, fromObject to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.operation.rabbit.util;

import com.rabbitmq.client.AMQP;
import org.hyperic.hq.operation.AbstractOperation;

import java.nio.charset.Charset;
import java.util.Random;

/**
 * @author Helena Edelson
 */
public final class MessageConstants {

    /**
     * Default virtual host
     */
    public static final String DEFAULT_VHOST = "/";

    /**
     * Default RabbitMQ port
     */
    public static final int DEFAULT_PORT = AMQP.PROTOCOL.PORT;

    /**
     * The encoding of the message
     */
    public static final String ENCODING = "UTF-8";

    /**
     * The {@link Charset} that maps to the encoding
     */
    public static final Charset CHARSET = Charset.forName(ENCODING);
 
    /**
     * The content type of the message
     */
    public static final String JSON_CONTENT_TYPE = "application/json";

    public static final String TEXT_CONTENT_TYPE = "text/plain";

    /**
     * The delivery mode of the message
     */
    public static final Integer DELIVERY_MODE_NON_PERSISTENT = 1;

    public static final Integer DELIVERY_MODE_PERSISTENT = 2;

    /**
     * The priority of the message
     */
    public static final Integer PRIORITY = 0;

    /**
     * The standard message properties
     */
    public static final AMQP.BasicProperties DEFAULT_MESSAGE_PROPERTIES;
 
    static {
        DEFAULT_MESSAGE_PROPERTIES = new AMQP.BasicProperties();
        DEFAULT_MESSAGE_PROPERTIES.setContentType(JSON_CONTENT_TYPE); // plain for early dev work
        DEFAULT_MESSAGE_PROPERTIES.setContentEncoding(ENCODING);
        DEFAULT_MESSAGE_PROPERTIES.setDeliveryMode(DELIVERY_MODE_NON_PERSISTENT);
        DEFAULT_MESSAGE_PROPERTIES.setPriority(PRIORITY);
    }

    private MessageConstants() {
    }

    /**
     * Refactor this to a service
     * @param data
     * @return
     */
    public static AMQP.BasicProperties getBasicProperties(Object data) {
        AMQP.BasicProperties bp = MessageConstants.DEFAULT_MESSAGE_PROPERTIES;

        if (data != null && data.getClass().isAssignableFrom(AbstractOperation.class)) {
            bp.setCorrelationId(((AbstractOperation) data).getOperationName());
        } else {
            bp.setCorrelationId(new Random().toString());
        }
        return bp;
    }

}
