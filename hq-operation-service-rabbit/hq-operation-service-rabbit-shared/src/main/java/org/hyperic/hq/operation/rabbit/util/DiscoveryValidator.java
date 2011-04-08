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
package org.hyperic.hq.operation.rabbit.util;

import org.hyperic.hq.operation.OperationDiscoveryException;
import org.hyperic.hq.operation.OperationEndpointException;

import java.lang.reflect.Method;

/**
 * @author Helena Edelson
 */
public class DiscoveryValidator {

    public void validateReturnType(Method candidateMethod, Object candidate) throws OperationDiscoveryException {
        if (void.class.equals(candidateMethod.getReturnType())) {
            throw new OperationEndpointException(String.format(
                    "Found illegal operation method '%s' on '%s'. @Operation annotated methods must have a non-void return type.",
                        candidateMethod, candidate));
        }
    }

   public void validateParameterTypes(Method candidateMethod, Object candidate) throws OperationDiscoveryException {
        if (candidateMethod.getParameterTypes().length != 1) {
            throw new OperationEndpointException(String.format(
                    "Found illegal operation method '%s' on '%s'. @Operation annotated methods must have exactly one parameter",
                        candidateMethod, candidate));
        }
    }

    public boolean validArguments(String operationName, String exchangeName, String value) {
        return operationName == null || exchangeName == null || value == null;
    }
}
