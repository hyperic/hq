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
package org.hyperic.hq.operation.rabbit.api;

import org.hyperic.hq.operation.rabbit.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.annotation.OperationEndpoint;
import org.hyperic.hq.operation.rabbit.util.OperationToRoutingMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public interface RoutingRegistry {
  
    /**
     * Registers the routing mapping for the given operation
     * @param method The operation to map
     * @param dispatcher the annotation
     */
    void register(Method method, OperationDispatcher dispatcher);

    /**
     * Registers the routing mapping for the given operation
     * @param method The operation to map
     * @param endpoint  the annotation
     */
    void register(Method method, OperationEndpoint endpoint);
 
    /**
     * Returns the routing data by operation name
     * @param operationName The operation's name
     * @param annotation the annotation to construct the key
     * @return the specific mapping for a given operation
     */
    OperationToRoutingMapping map(String operationName, Class<?extends Annotation> annotation);

    /**
     * @param operationName the operation
     * @param annotation the annotation to construct the key
     * @return true if the operation is registered
     */
    boolean supports(String operationName, Class<?extends Annotation> annotation);
 
}
