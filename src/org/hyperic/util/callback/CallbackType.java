package org.hyperic.util.callback;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

public abstract class CallbackType {
    /**
     * RETURN_LAST is a callback type which invokes all listeners (or until
     * one throws an exception) and returns the last value returned by
     * the invoked method.  If no listeners are registered, a primitive
     * value will be returned from the method.
     */
    public static final CallbackType RETURN_LAST = new CallbackType() {
        public Object callListeners(Method meth, Object[] methArgs,
                                    Set listeners)
            throws Throwable
        {
            Object last = null;
            for (Iterator i=listeners.iterator(); i.hasNext(); ) {
                Object listener = (Object)i.next();
            
                last = meth.invoke(listener, methArgs);
            }
            
            if (last == null) {
                // Nobody listening ...
                return PrimitiveUtil.getBasicValue(meth.getReturnType());
            }

            return last;
        }
    };
    
    /**
     * This method is called by {@link CallbackDispatcher} to invoke listeners
     * which are setup to be called back.  
     * 
     * @param meth       Method to invoke
     * @param methArgs   Arguments to the method
     * @param listeners  Set of {@link Object} listeners which support
     *                   an invocation of 'meth'.
     */
    public abstract Object callListeners(Method meth, Object[] methArgs,
                                         Set listeners)
        throws Throwable;
}
