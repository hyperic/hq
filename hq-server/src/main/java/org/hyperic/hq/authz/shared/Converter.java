package org.hyperic.hq.authz.shared;

public interface Converter<T, K> {
    
    public T convert(K id);

}
