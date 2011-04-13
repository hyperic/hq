/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2009-2010], VMware, Inc.
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
package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.Converter;
import org.hyperic.hq.operation.OperationDiscoveryException;
import org.hyperic.hq.operation.OperationNotSupportedException;
import org.hyperic.hq.operation.OperationRegistry;
import org.hyperic.hq.operation.annotation.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Helena Edelson
 */
@Component("operationDiscoverer")
public class OperationMethodInvokingRegistry implements OperationRegistry {

    /**
     * Will consist of either dispatchers or endpoints
     */
    protected final Map<String, MethodInvoker> operationMappings = new ConcurrentHashMap<String, MethodInvoker>();

    protected final RoutingRegistry routingRegistry;

    protected final Converter<Object, String> converter;
 
    @Autowired
    public OperationMethodInvokingRegistry(RoutingRegistry routingRegistry, Converter<Object, String> converter) {
        this.converter = converter;
        this.routingRegistry = routingRegistry;
    }
 
    /**
     * Discovers, evaluates, validates and registers candidates.
     * @param candidate  the dispatcher candidate class
     * @param annotation the annotation to test
     * @throws org.hyperic.hq.operation.OperationDiscoveryException
     *
     */
    public void discover(Object candidate, Class<? extends Annotation> annotation) throws OperationDiscoveryException {
        Class<?> candidateClass = candidate.getClass();
        if (candidateClass.isAnnotationPresent(annotation)) {
            for (Method method : candidateClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Operation.class)) {
                    if (!method.isAccessible()) method.setAccessible(true);
                    System.out.println("\n\n*****************discovered bean " + candidate + " has " + annotation);
                    register(method, candidate, annotation);
                }
            }
        }
    }

    /**
     * Registers a method as operation with a Registry for future dispatch.
     * Registers dispatchers and delegates to RoutingRegistry for further work.
     * @param method    the method
     * @param candidate the candidate instance
     */
    public void register(Method method, Object candidate, Class<? extends Annotation> annotation) {
        if (!this.operationMappings.containsKey(method.getAnnotation(Operation.class).operationName())) {
            this.operationMappings.put(method.getAnnotation(Operation.class).operationName(), new MethodInvoker(method, candidate, this.converter));
            this.routingRegistry.register(method.getAnnotation(Operation.class));
            System.out.println("\n\n*****************registered bean " + candidate + " has " + annotation);
        }
    }

    /**
     * Checks whether the operation is supported, if so returns the value
     * from the map for invocation.
     * @param operationName the potential key
     * @return if supported, returns the MethodInvoker of type OperationDispatcher or OperationEndpoint
     * @throws org.hyperic.hq.operation.OperationNotSupportedException if method not supported
     */
    public MethodInvoker map(String operationName) throws OperationNotSupportedException {
        if (!this.operationMappings.containsKey(operationName)) throw new OperationNotSupportedException(operationName);
        return this.operationMappings.get(operationName);
    }

    public Map<String, MethodInvoker> getOperationMappings() {
        return operationMappings;
    }

    /**
     *
     */
    public static final class MethodInvoker {

        private final Converter<Object, String> converter;

        private final Method method;

        private final Object instance;

        private final String operationName;

        public MethodInvoker(Method method, Object instance, Converter<Object, String> converter) {
            this.method = method;
            this.instance = instance;
            this.converter = converter;
            this.operationName = method.getAnnotation(Operation.class).operationName();
        }

        /**
         * Reads the String content to create the specified data Object
         * and invokes the given method with that object
         * @param content the json content
         * @return the result of dispatching the method represented by this object
         * @throws IllegalAccessException
         * @throws InvocationTargetException
         */
        public Object invoke(String content) throws IllegalAccessException, InvocationTargetException {
            Object data = this.converter.read(content, this.method.getParameterTypes()[0]);
            return this.method.invoke(this.instance, data);
        }
        
        public Object getReturnType() {
            return this.method.getReturnType();
        }

        public boolean operationHasReturnType() {
            return !void.class.equals(this.method.getReturnType());
        }

        @Override
        public String toString() {
            return new StringBuilder("operationName=").append(this.operationName).append(" method=").append(this.method)
                    .append(" instance=").append(this.instance).append(" converter=").append(this.converter).toString();
        }
    } 
}
