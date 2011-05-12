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

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.server.AgentLifecycleService;
import org.hyperic.hq.agent.server.AgentService;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author Helena Edelson
 */
public final class AgentApplicationContext {

    private static final Log logger = LogFactory.getLog(AgentApplicationContext.class);

    private static final AtomicBoolean running = new AtomicBoolean(false);

    //RabbitOperationServiceConfiguration
    private static final String[] basePackages = {"org.hyperic.hq.agent"};

    private static final AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();


    public static void start() throws RuntimeException {
        if (running.get()) return;

        logger.debug("Configuring Spring context.");
        try {
            ctx.scan(basePackages);
            ctx.registerShutdownHook();
            running.set(true);
            ctx.refresh();
            logger.debug("Context configured.");
        }
        catch (UnsatisfiedDependencyException e) {
            // ignore - thrown from integration tests 
        }
        catch (Throwable t) {
            stop();
            throw new RuntimeException(t);
        }
    }

    /**
     * Not exposing the context - just a call
     * to get the primary bean.
     * @return
     */
    public static AgentService getAgentService() {
        return getBean(AgentLifecycleService.class);
    }

    /**
     * Returns a bean by type. On the agent there are no prototypes.
     * @param type the type
     * @param <T>
     * @return the bean instance if it exists
     * @throws RuntimeException if the type is not in context
     */
    private static <T> T getBean(Class<T> type) throws RuntimeException {
        if (!running.get()) throw new IllegalStateException("Application context is not running.");

        try {
            return ctx.getBean(type);
        } catch (NoSuchBeanDefinitionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Do graceful shutdown and close on demand if needed.
     */
    public static void stop() {
        running.set(false);
        ctx.destroy();
    }
}
