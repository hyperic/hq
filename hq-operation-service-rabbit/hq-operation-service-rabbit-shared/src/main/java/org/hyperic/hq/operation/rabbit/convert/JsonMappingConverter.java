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

package org.hyperic.hq.operation.rabbit.convert;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.hyperic.hq.operation.ConversionException;
import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public class JsonMappingConverter implements Converter<Object, String> {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new instance of the converter
     */
    public JsonMappingConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);
    }

    public byte[] writeBytes(Object source) throws ConversionException {
        return this.write(source).getBytes(MessageConstants.CHARSET);
    }

    public String write(Object source) throws ConversionException {
        try {
            return this.objectMapper.writeValueAsString(source);
        } catch (IOException e) {
            throw new ConversionException(e);
        }
    }

    public Object read(String source, Class<?> type) {
        try {
            return this.objectMapper.readValue(source, type);
        } catch (IOException e) {
            throw new ConversionException(e);
        }
    } 
}