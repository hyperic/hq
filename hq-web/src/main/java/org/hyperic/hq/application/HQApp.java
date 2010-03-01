/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.HibernateInterceptorChain;
import org.hyperic.hibernate.HypericInterceptor;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.util.thread.ThreadWatchdog;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

/**
 * This class represents the central concept of the Hyperic HQ application. (not
 * the Application resource)
 */
@Component("hqApp")
public class HQApp implements ServletContextAware {
 

   

    private File _restartStorage;

   

    private ThreadWatchdog _watchdog;

    private ServletContext servletContext;

    private final HQHibernateLogger _hiberLogger;

    public HQApp() {

        _watchdog = new ThreadWatchdog("ThreadWatchdog");
        _watchdog.initialize();

        _hiberLogger = new HQHibernateLogger();
    }

    @PostConstruct
    public void init() {
        if (servletContext == null) {
            return;
        }
        String war = servletContext.getRealPath("/");
        File warDir = new File(war);
        String restartStorageDir = new File(warDir.getParent()).getParent();
        setRestartStorageDir(new File(restartStorageDir));
    }

    public ThreadWatchdog getWatchdog() {
        synchronized (_watchdog) {
            return _watchdog;
        }
    }

    public void setRestartStorageDir(File dir) {

        _restartStorage = dir;

    }

    /**
     * Get a directory which can have files placed into it which will carry over
     * for a restart. This should not be used to place files for extensive
     * periods of time.
     */
    public File getRestartStorageDir() {
        return _restartStorage;
    }

 
    public Properties getTweakProperties() throws IOException {
        return readTweakProperties();
    }

    private static Properties readTweakProperties() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("tweak.properties");
        Properties res = new Properties();

        if (is == null)
            return res;

        try {
            res.load(is);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        return res;
    }

  

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    /**
     * Get an interceptor to process hibernate lifecycle methods.
     * 
     * This method is used by {@link HypericInterceptor}
     */
    public HibernateInterceptorChain getHibernateInterceptor() {
        return _hiberLogger;
    }

    /**
     * Get the hibernate log manager, which allows the caller to execute code
     * within the context of a logging hibernate interceptor.
     */
    public HibernateLogManager getHibernateLogManager() {
        return _hiberLogger;
    }

    public static HQApp getInstance() {
        return Bootstrap.getBean(HQApp.class);
    }
}
