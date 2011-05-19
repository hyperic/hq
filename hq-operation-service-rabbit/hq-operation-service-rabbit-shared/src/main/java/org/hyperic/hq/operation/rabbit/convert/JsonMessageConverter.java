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

import com.rabbitmq.client.QueueingConsumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.ConversionException;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;

import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public class JsonMessageConverter implements MessageConverter {

    private final Log logger = LogFactory.getLog(this.getClass());

    private final Converter<Object, String> converter;

    private final Method method;

    private final PropertiesConverter propertiesConverter;

    public JsonMessageConverter(Method method) {
        this.method = method;
        this.converter = new JsonObjectMappingConverter();
        this.propertiesConverter = new PropertiesConverter(method.getReturnType());
    }

    /**
     * Extracts the data from the delivery object consumed
     * @return the converted data from the byte[]
     * @throws ConversionException if an error occurred in the conversion
     */
    public Object extractRequest(QueueingConsumer.Delivery delivery) throws ConversionException {
        Object response;

        try {
            String json = new String(delivery.getBody(), MessageConstants.CHARSET);
            logger.debug("extracting " + json);
            response = converter.read(json, method.getParameterTypes()[0]);
        }
        catch (Exception e) {
            throw new ConversionException("Failed to convert json-based Message content", e);
        }

        if (response == null) response = delivery.getBody();
        return response;
    }

    /* not started yet */

    public Object buildResponse(Object object, QueueingConsumer.Delivery delivery) throws ConversionException {
        final byte[] bytes = converter.write(object).getBytes(MessageConstants.CHARSET);
        propertiesConverter.convertProperties(delivery);
        return null;
    }

}
