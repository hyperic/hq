package org.hyperic.util;

/**
 * A simple transformer to be used when no action is required to 
 * transfer and id list to id list
 * @author andersonm
 */
public class NoActionIntegerTransformer extends IntegerTransformer<Integer> {

    @Override
    public Integer transform(Integer id) {
        return id;
    }
}
