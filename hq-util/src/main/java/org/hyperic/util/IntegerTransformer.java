package org.hyperic.util;

public abstract class IntegerTransformer<R> extends Transformer<Integer, R> {
    
    @Override
    public abstract R transform(Integer val);

}
