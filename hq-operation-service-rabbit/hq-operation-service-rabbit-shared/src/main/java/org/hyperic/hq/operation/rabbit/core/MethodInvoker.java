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
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 */

package org.hyperic.hq.operation.rabbit.core;

import org.hyperic.hq.operation.Converter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Checks whether the operation is supported, if so returns the value
 * from the map for invocation.
 */
public final class MethodInvoker {

    private final Converter<Object, String> converter;

    private final Method method;

    private final Object instance;

    private final String operationName;

    public MethodInvoker(Method method, Object instance, Converter<Object, String> converter) {
        this.method = method;
        this.instance = instance;
        this.converter = converter;
        this.operationName = method.getName();
    }

    /**
     * Reads the String content to create the specified data Object
     * and invokes the given method with that object
     * @param content the json content
     * @return the result of dispatching the method represented by this object
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     *
     */
    public Object invoke(String content) throws IllegalAccessException, InvocationTargetException {
        Object data = converter.read(content, method.getParameterTypes()[0]);
        return method.invoke(instance, data);
    }

    public Object getReturnType() {
        return method.getReturnType();
    }

    public boolean operationHasReturnType() {
        return !void.class.equals(method.getReturnType());
    }

    @Override
    public String toString() {
        return new StringBuilder("operationName=").append(operationName).append(" method=").append(method)
                .append(" instance=").append(instance).append(" converter=").append(converter).toString();
    }
}
