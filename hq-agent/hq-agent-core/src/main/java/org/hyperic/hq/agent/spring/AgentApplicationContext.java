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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * todo: exceptions, logging
 * @author Helena Edelson
 */
public final class AgentApplicationContext implements SmartLifecycle {

    private static final Log logger = LogFactory.getLog(AgentApplicationContext.class);

    private static AnnotationConfigApplicationContext ctx;
 
    public static AnnotationConfigApplicationContext create(String[] basePackages, Class<?>... annotatedClasses) {
        if (ctx != null) return ctx;
  
        try {
            ctx = new AnnotationConfigApplicationContext(annotatedClasses);
            ctx.registerShutdownHook();
            ctx.scan(basePackages);


        }
        catch (BeansException e) {
            shutdown();
        } catch (IllegalStateException e) {
            //TODO
            logger.error(e.getCause() + " " + e.getMessage());
        }
         
        return ctx;
    }

    public static <T> T getBean(Class<T> requiredType) throws RuntimeException {
        try {
            return ctx.getBean(requiredType);
        } catch (NoSuchBeanDefinitionException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Do graceful shutdown and close on demand if needed.
     */
    public static void shutdown() {
        if (ctx != null) {
            ctx.destroy();
        }
    }

    public boolean isAutoStartup() {
        return true;
    }

    public void stop(Runnable callback) {
       stop();
    }

    public void start() {

    }

    public void stop() {
        shutdown();
    }

    public boolean isRunning() {
        return false;
    }

    public int getPhase() {
        return 0;
    }
}
