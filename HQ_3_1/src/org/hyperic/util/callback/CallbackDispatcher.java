/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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
 * the Proxied Callback Pattern.    
 * 
 * This dispatcher holds onto the list of all the registered callers and
 * listeners and facilities the communication between them.
 * 
 * DevNote:  Might be better to keep the collection of listeners as a list,
 *           ordered by the order of registration.  Right now that information
 *           is lost.  Alternatively we could just let the callback type 
 *           handle the registered list of listeners. 
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
        
        private void removeListener(Object o) {
            synchronized (_listeners) {
                _listeners.remove(o);
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
     * @param iFace    Interface to listen on.  Must previously be registered
     *                 via {@link #generateCaller(Class)}
     *
     * @param o        Listener which will be invoked when any of the methods
     *                 specified in 'iFace' is called.  This object must 
     *                 implement 'iFace'.        
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
     * Unregister a previously registered listener.
     * 
     * @see #registerListener(Class, Object)
     */
    public void unregisterListener(Class iFace, Object o) {
        CallbackHandler handler;
        
        synchronized (_ifaceToHandler) {
            handler = (CallbackHandler) _ifaceToHandler.get(iFace);
        }
        
        if (handler != null)
            handler.removeListener(o);
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
     * Generates the 'caller' side of the caller/listener relationship.  
     * 
     * When methods on the returned object are invoked, the listeners 
     * registered via {@link #registerListener(Class, Object)} for the given
     * interface will be executed.
     * 
     * The execution of the callbacks, the order they are called, the 
     * exception handling, values returned etc. are all handled by the passed 
     * {@link CallbackType}.
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
