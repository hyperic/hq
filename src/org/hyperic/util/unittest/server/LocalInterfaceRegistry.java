/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.util.unittest.server;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A registry for looking up EJB local interfaces. This registry is necessary 
 * (as opposed to JNDI local lookups) since JNDI local lookups don't work unless 
 * the current classloader is the EJB deployer classloader.
 * 
 * NOTE: At this time, only unit test method invocations on the local interfaces 
 * are supported.
 */
public class LocalInterfaceRegistry {

    private final ClassLoader _ejbClassLoader;
    
    /**
     * Creates an instance.
     *
     * @param systemCL The system classloader that should already have registered 
     *                  the EJB deployer classloader.
     * @throws IllegalArgumentException if the EJB deployer classloader is not 
     *                                  registered with the system classloader.           
     */
    public LocalInterfaceRegistry(IsolatingDefaultSystemClassLoader systemCL) {
        if (systemCL.getEJBClassLoader() == null) {
            throw new IllegalArgumentException("The EJB deployer classloader was not registered");
        }
        
        _ejbClassLoader = systemCL.getEJBClassLoader();
    }
    
    /**
     * Return a proxy to the local interface retrieved from the EJB impl class 
     * getOne() static factory method.
     * 
     * @param ejbImplClazz The EJB impl class that must have the getOne() static 
     *                      factory method specified for retrieving the EJB 
     *                      local interface.
     * @param localInterface The interface that the EJB local implements.
     * @return The proxy that can be cast to the local interface. At this time,  
     *         the proxy ONLY supports unit test method invocations.
     * @throws Exception
     */
    public Object getLocalInterface(Class ejbImplClazz, Class localInterface) 
        throws Exception { 
        
        Class ejb = _ejbClassLoader.loadClass(ejbImplClazz.getName());

        Method getOne;
        
        try {
            getOne = ejb.getMethod("getOne", new Class[0]);            
        } catch (Exception e) {
            throw new Exception("EJB impl class does not have a static " +
            		            "getOne() factory method: "+ejbImplClazz, e);
        }
        
        Object ejbProxy = getOne.invoke(null, new Object[0]);
        
        return Proxy.newProxyInstance(localInterface.getClassLoader(), 
                                      new Class[]{localInterface}, 
                                      new LocalInterfaceHandler(ejbProxy));
    }
    
    /**
     * The invocation handler that dispatches calls to the proxy returned 
     * by the EJB impl getOne() factory method.
     */
    private static class LocalInterfaceHandler implements InvocationHandler {
        
        private final Object _ejbProxy;
        
        public LocalInterfaceHandler(Object ejbProxy) {
            _ejbProxy = ejbProxy;
        }

        public Object invoke(Object proxy, Method method, Object[] args) 
            throws Throwable {
            
            // have to find the "equivalent" method on the ejb proxy
            Method ejbMethod = _ejbProxy.getClass().getMethod(method.getName(), 
                                                    method.getParameterTypes());
            
            if (isUnitTestMethod(method)) {
                invokeUnitTest(ejbMethod, 
                               _ejbProxy, 
                               method.getDeclaringClass().getName(), 
                               method.getName());
                return null;
            } else {
                // Only unit test method invocations are supported at this time
                throw new IllegalArgumentException("Method invocation not supported: "+
                        method.getDeclaringClass().getName()+"#"+method.getName());
                // return ejbMethod.invoke(_ejbProxy, args);                
            }
        }
        
        private boolean isUnitTestMethod(Method method) {
            return method.getName().startsWith("test") && 
                   method.getReturnType().equals(Void.TYPE) && 
                   method.getParameterTypes().length==0;
        }
        
        /**
         * Unit tests must be isolated from the default system classloader to 
         * prevent loading unmanaged objects (pojos, not, EJBs) from the wrong 
         * classloader.
         */
        private void invokeUnitTest(final Method unitTestMethod, 
                                    final Object proxy, 
                                    String className,
                                    String methodName) 
            throws Throwable {
            
            ExceptionHandlingThreadGroup group = 
                new ExceptionHandlingThreadGroup("unit-test-group");
            
            String threadName = className+"#"+methodName;
            
            Thread thread = new Thread(group, threadName) {
                public void run() {
                    IsolatingDefaultSystemClassLoader cl = 
                        (IsolatingDefaultSystemClassLoader)
                            ClassLoader.getSystemClassLoader();
                    
                    cl.setIsolateDefaultSystemClassloader();
                    
                    try {
                        unitTestMethod.invoke(proxy, new Object[0]);
                    } catch (InvocationTargetException e) {
                        throw new WrapperRuntimeException(e.getCause());
                    } catch (Exception e) {
                        throw new WrapperRuntimeException(e);
                    }
                }
            };
            
            thread.start();
            thread.join();
            
            Throwable uncaughtException = group.getUncaughtException();
            
            if (uncaughtException != null) {
                if (uncaughtException instanceof WrapperRuntimeException) {
                    throw ((WrapperRuntimeException)uncaughtException).getCause();
                } else {
                    throw uncaughtException;                    
                }
            }
       }
        
    }
    
    private static class WrapperRuntimeException extends RuntimeException {
        
        private static final long serialVersionUID = 1L;

        public WrapperRuntimeException(Throwable cause) {
            super(cause);
        }
    }
    
}
