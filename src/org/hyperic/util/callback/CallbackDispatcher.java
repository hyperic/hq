package org.hyperic.util.callback;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The CallbackDispatcher is able to generate listeners and callers for
 * the proxied callback pattern.    The Proxy Callback pattern is a way to
 * execute callbacks with type checking and declared exceptions.
 */
public class CallbackDispatcher {
    private Map _ifaceToHandler = new HashMap();

    private static class CallbackHandler implements InvocationHandler {
        private Set          _listeners = new HashSet();
        private CallbackType _type;
        
        public CallbackHandler(CallbackType type) {
            _type = type;
        }
        
        private void addListener(Object o) {
            synchronized (_listeners) {
                _listeners.add(o);
            }
        }
        
        public Object invoke(Object proxy, Method meth, Object[] methArgs) 
            throws Throwable 
        {
            Set listeners; 
        
            synchronized (_listeners) {
                listeners = new HashSet(_listeners);
            }
        
            return _type.callListeners(meth, methArgs, listeners);
        }
    }

    /**
     * Register a listener for a given interface.  If the caller has not
     * been registered, then an error will be thrown. 
     *
     * @param iFace  Interface to listen on.  Must previously be registered
     *               via {@link #generateCaller(Class)}
     * @param o      Object to invoke (as a callback) when the caller executes
     *               methods on the interface.
     */
    public void registerListener(Class iFace, Object o) {
        CallbackHandler handler;
        
        if (!iFace.isAssignableFrom(o.getClass())) {
            throw new IllegalArgumentException("Object [" + o + "] does not " +
                                               "implement [" + iFace.getName() +
                                               "]");
        }
        
        synchronized (_ifaceToHandler) {
            handler = (CallbackHandler) _ifaceToHandler.get(iFace);
        }

        if (handler == null) {
            // TODO:  Maybe relax this in the future so that listeners
            //        can register before callers?
            throw new IllegalArgumentException("No callback for interface [" +
                                               iFace.getName() + "] exists");
        }

        handler.addListener(o);
    }

    /**
     * Generates a caller with the callback type of 
     * {@link CallbackType.RETURN_LAST}
     * 
     * @see #generateCaller(Class, CallbackType)
     */
    public Object generateCaller(Class iFace) {
        return generateCaller(iFace, CallbackType.RETURN_LAST);
    }
    
    /**
     * Generate an instance of an interface which can be called by the caller.
     * When methods on this object are invoked, callbacks will be executed,
     * and depending on the {@link CallbackType}, exceptions will be thrown
     * and values will be returned (from the callback).
     *
     * @param iFace Interface to generate the caller for
     * @param type  Type specifying the way that the caller will process
     *              callbacks
     *
     * @return An object implementing the passed interface.  The methods on
     *         this object will execute callbacks on registered listeners.
     */
    public Object generateCaller(Class iFace, CallbackType type) {
        CallbackHandler newHandler;
        Object src;
        
        if (!iFace.isInterface()) {
            throw new IllegalArgumentException("Class [" + iFace.getName() +
                                               "] is not an interface");
        }
        
        newHandler = new CallbackHandler(type);
        src = Proxy.newProxyInstance(iFace.getClassLoader(), 
                                     new Class[] { iFace }, newHandler);
        synchronized(_ifaceToHandler) {
            if (_ifaceToHandler.containsKey(iFace)) {
                throw new IllegalArgumentException("Caller already generated " +
                                                   " for interface [" + 
                                                   iFace.getName() + "]");
            }
            
            _ifaceToHandler.put(iFace, newHandler);
        }
        return src;
    }
    
    public static interface MyInterface {
        int addTwo(int a, int b);
    }
    
    public static void main(String[] args) {
        CallbackDispatcher x = new CallbackDispatcher();
        
        MyInterface caller = (MyInterface)x.generateCaller(MyInterface.class);

        MyInterface listener = new MyInterface() {
            public int addTwo(int a, int b) {
                System.out.println("Target called");
                return a + b;
            }
        };
        
        x.registerListener(MyInterface.class, listener);
        System.out.println(caller.addTwo(3, 5));
    }
}
