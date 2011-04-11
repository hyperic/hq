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
package org.hyperic.hq.operation.rabbit;

import org.hyperic.hq.operation.OperationDiscoverer;
import org.hyperic.hq.operation.annotation.OperationDispatcher;
import org.hyperic.hq.operation.rabbit.core.AnnotatedRabbitOperationService;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author Helena Edelson
 */
public class OperationDispatcherDiscovererBeanPostProcessor implements BeanPostProcessor {

    private final OperationDiscoverer operationDiscoverer;
  
    public OperationDispatcherDiscovererBeanPostProcessor() {
        this(new AnnotatedRabbitOperationService());
    }

    OperationDispatcherDiscovererBeanPostProcessor(OperationDiscoverer operationDiscoverer) {
        this.operationDiscoverer = operationDiscoverer;
    }
 
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    public Object postProcessAfterInitialization(Object bean, String beanName) {
        try {
            this.operationDiscoverer.discover(bean, OperationDispatcher.class);
        } catch (Exception e) {
            throw new FatalBeanException("Unable to scan bean for annotations", e);
        }
        return bean;
    }
}
