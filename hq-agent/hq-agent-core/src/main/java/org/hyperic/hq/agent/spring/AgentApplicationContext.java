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

package org.hyperic.hq.agent.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

/**  todo: exceptions, logging
 * @author Helena Edelson
 */
public final class AgentApplicationContext {

    private static AbstractApplicationContext applicationContext;

    public AgentApplicationContext(Class<?>... annotatedClasses) {
        if (applicationContext == null) {
            applicationContext = create(annotatedClasses);
        }
    }

    public static <T> T getBean(Class<T> requiredType) throws RuntimeException {
        try {
            return applicationContext.getBean(requiredType);
        } catch (NoSuchBeanDefinitionException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static AbstractApplicationContext create(Class<?>... annotatedClasses) {
        if (applicationContext != null) return applicationContext;

        AnnotationConfigApplicationContext ctx = null;

        try {
            ctx = new AnnotationConfigApplicationContext(annotatedClasses);
            //ctx.registerShutdownHook();
            ctx.scan("org.hyperic.hq.bizapp.client", "org.hyperic.hq.operation");
        }
        catch (BeansException e) {
            shutdown();
        } catch (IllegalStateException e) {
            System.out.println(e.getCause() + " " + e.getMessage());
        }
        for (Object bean: ctx.getBeanDefinitionNames()) {
            System.out.println(bean);
        }
        return ctx;
    }


    public static AbstractApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Do graceful shutdown and close on demand if needed.
     */
    public static void shutdown() {
        if (applicationContext != null) applicationContext.close();
    }
}
