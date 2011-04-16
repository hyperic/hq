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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.operation.OperationDiscoveryException;
import org.hyperic.hq.operation.OperationRegistry;
import org.hyperic.hq.operation.rabbit.annotation.Operation;
import org.hyperic.hq.operation.rabbit.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.api.OperationDispatcherDiscoverer;
import org.hyperic.hq.operation.rabbit.api.OperationDispatcherRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
@Component
public class AnnotatedOperationDispatcherDiscoverer implements OperationDispatcherDiscoverer {

    private final Log logger = LogFactory.getLog(AnnotatedOperationDispatcherDiscoverer.class);

    private final OperationRegistry dispatcherRegistry;

    @Autowired
    public AnnotatedOperationDispatcherDiscoverer(OperationDispatcherRegistry dispatcherRegistry) {
        this.dispatcherRegistry = dispatcherRegistry;
    }

    /**
     * Discovers, evaluates, validates and registers candidates
     * @param candidate  the dispatcher candidate class
     * @throws org.hyperic.hq.operation.OperationDiscoveryException
     */
    public void discover(Object candidate) throws OperationDiscoveryException {
        Class<?> candidateClass = candidate.getClass();
        if (candidateClass.isAnnotationPresent(OperationDispatcher.class)) {
            for (Method method : candidateClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Operation.class)) {
                    if (!method.isAccessible()) method.setAccessible(true);
                    logger.info("discovered " + candidate);
                    this.dispatcherRegistry.register(method, candidate);
                }
            }
        }
    } 
}
