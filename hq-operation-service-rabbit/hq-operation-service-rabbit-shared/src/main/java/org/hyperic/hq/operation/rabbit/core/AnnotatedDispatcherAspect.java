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
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.hyperic.hq.operation.OperationFailedException;
import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.rabbit.annotation.OperationDispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Executes when a matched method execution returns normally
 * @author Helena Edelson
 */
@Aspect
@Component
public class AnnotatedDispatcherAspect implements OperationDispatcherService {

        private final Log logger = LogFactory.getLog(this.getClass());

        private final OperationService operationService;

        @Autowired
        public AnnotatedDispatcherAspect(OperationService operationService) {
            this.operationService = operationService;
        }

        /**
         * Executes when a matched method execution returns successfully,
         * dispatching data to the Rabbit Operation Service
         * @param dispatcher the @OperationDispatcher decorating a pointcut method
         * @param jp the AspectJ JoinPoint
         * @param object the pointcut method's return value
         * @throws OperationFailedException
         * if an error occurs in execution
         */
        @AfterReturning(pointcut = "@annotation(dispatcher)", returning = "object", argNames = "jp, object, dispatcher")
        public void dispatch(JoinPoint jp, Object object, OperationDispatcher dispatcher) throws OperationFailedException {
            operationService.perform(jp.getSignature().getName(), object, OperationDispatcher.class);
            logger.debug("performed operation=" + jp.getSignature().getName() + " on " + object + " dispatcher=" + dispatcher);
        }

}
