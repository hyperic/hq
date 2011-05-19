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

import org.hyperic.hq.operation.OperationDiscoverer;
import org.hyperic.hq.operation.OperationDiscoveryException;
import org.hyperic.hq.operation.OperationRegistry;
import org.hyperic.hq.operation.rabbit.annotation.Operation;
import org.hyperic.hq.operation.rabbit.annotation.OperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
@Component
public class AnnotatedOperationDiscoverer implements OperationDiscoverer {

    private final OperationRegistry operationRegistry;

    @Autowired
    public AnnotatedOperationDiscoverer(OperationRegistry operationRegistry) {
        this.operationRegistry = operationRegistry;
    }

    /**
     * Load-time discovery of candidates. Delegates to the OperationRegistry
     * for full registration.
     * @param candidate the dispatcher candidate class
     * @throws OperationDiscoveryException
     */
    public void discover(Object candidate) throws OperationDiscoveryException {
        Class<?> candidateClass = candidate.getClass();
        if (candidateClass.isAnnotationPresent(OperationService.class)) {
            for (Method method : candidateClass.getDeclaredMethods()) {
                for (Annotation a : method.getAnnotations()) {
                    if (a.annotationType().isAnnotationPresent(Operation.class)) {
                        if (!method.isAccessible()) method.setAccessible(true);
                        operationRegistry.register(method, candidate);
                    }
                }
            }
        }
    }
}
