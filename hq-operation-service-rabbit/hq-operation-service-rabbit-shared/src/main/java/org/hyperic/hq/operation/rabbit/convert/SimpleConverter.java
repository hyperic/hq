package org.hyperic.hq.operation.rabbit.convert;

import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.rabbit.util.MessageConstants;

/**
 * @author Helena Edelson
 */
public class SimpleConverter implements Converter<String, byte[]> {
 
    public byte[] write(String source) {
        return source.getBytes(MessageConstants.CHARSET);
    }

    public String read(byte[] source, Class<?> type) {
        return new String(source);
    }
}
