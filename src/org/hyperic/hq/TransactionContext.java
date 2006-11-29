package org.hyperic.hq;

import java.util.HashMap;
import java.util.List;

/**
 */
public abstract class TransactionContext extends HashMap
{
    private static final Object LIST_RESULT = new Object();

    public abstract TransactionContext run(TransactionContext context);

    public List getList()
    {
        return (List)get(LIST_RESULT);
    }

    public List setList(List result)
    {
        return (List)put(LIST_RESULT, result);
    }
}
