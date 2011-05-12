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

package org.hyperic.hq.operation.rabbit.convert;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.QueueingConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.operation.rabbit.util.MessageConstants;

import java.util.Map;
import java.util.UUID;

/**
 * @author Helena Edelson
 */
public class PropertiesConverter {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final Class<?> convertToType;

    public PropertiesConverter(Class<?> convertToType) {
        this.convertToType = convertToType;
    }

    /* TODO */
    public AMQP.BasicProperties buildDefaultProperties(String replyToExchange) {
        AMQP.BasicProperties props = new AMQP.BasicProperties();
        props.setContentType(MessageConstants.JSON_CONTENT_TYPE);
        props.setContentEncoding(MessageConstants.ENCODING);
        props.setDeliveryMode(MessageConstants.DELIVERY_MODE_NON_PERSISTENT);
        props.setPriority(MessageConstants.PRIORITY);

        if (replyToExchange != null) props.setReplyTo(replyToExchange);
        props.setCorrelationId(getCorrelationId(null));

        return props;
    }

    /* TODO */
    public String getCorrelationId(QueueingConsumer.Delivery delivery) {
        if (delivery != null && delivery.getProperties().getCorrelationId() != null) {
           return delivery.getProperties().getCorrelationId();
        }
        else {
            return UUID.randomUUID().toString();// TODO lighter weight
        }
    }

    /* TODO */
    public AMQP.BasicProperties convertProperties(QueueingConsumer.Delivery delivery) {

        AMQP.BasicProperties target = new AMQP.BasicProperties();
        AMQP.BasicProperties source = delivery.getProperties();

        if (source != null) {
            Map<String, Object> targetHeaders = source.getHeaders();
            targetHeaders.put(MessageConstants.REQUEST_CONVERSION_TYPE, convertToType);

            if (source.getHeaders() != null) {
                /* temp */
                Map<String, Object> headers = source.getHeaders();
                for (Map.Entry entry : headers.entrySet()) {
                    logger.debug("header: " + entry);
                } 
            }

            target.setHeaders(targetHeaders);
            target.setCorrelationId(getCorrelationId(delivery));
            target.setTimestamp(source.getTimestamp());
            target.setMessageId(source.getMessageId());
            target.setUserId(source.getUserId());
            target.setAppId(source.getAppId());
            target.setClusterId(source.getClusterId());
            target.setType(source.getType());

            Integer deliverMode = source.getDeliveryMode();
            if (deliverMode != null) {
                target.setDeliveryMode(deliverMode);
            }
            target.setExpiration(source.getExpiration());
            target.setPriority(source.getPriority());
            target.setContentType(source.getContentType());
            target.setContentEncoding(source.getContentEncoding());

            String replyTo = source.getReplyTo();
            logger.debug("source header reply to=" + replyTo);
            if (replyTo != null) {
                target.setReplyTo(replyTo);
            }
        }


        return target;
    }
}
