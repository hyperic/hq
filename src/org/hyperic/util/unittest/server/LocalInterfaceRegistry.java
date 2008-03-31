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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A registry for looking up EJB local interfaces. This registry is necessary 
 * (as opposed to JNDI local lookups) since JNDI local lookups don't work unless 
 * the current classloader is the EJB deployer classloader.
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
     * @return The proxy that can be cast to the local interface.
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
        
        Object ejbProxy = getOne.invoke(ejb, new Object[0]);
        
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
            
            return ejbMethod.invoke(_ejbProxy, args);
        }
        
    }
 
}
