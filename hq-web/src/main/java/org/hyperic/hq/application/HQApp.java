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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Transaction;
import org.hyperic.hibernate.HibernateInterceptorChain;
import org.hyperic.hibernate.HypericInterceptor;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.transport.AgentProxyFactory;
import org.hyperic.hq.transport.ServerTransport;
import org.hyperic.util.callback.CallbackDispatcher;
import org.hyperic.util.thread.ThreadWatchdog;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;


/**
 * This class represents the central concept of the Hyperic HQ application.
 * (not the Application resource)
 */
@Component("hqApp")
public class HQApp  implements ServletContextAware {
    private static final Log _log = LogFactory.getLog(HQApp.class);
   

    
    private ThreadLocal        _txListeners    = new ThreadLocal();
   
    private CallbackDispatcher _callbacks;
    private ShutdownCallback   _shutdown;
    private File               _restartStorage;
   
    private File               _webAccessibleDir;
    private ThreadWatchdog     _watchdog;
    private final Scheduler    _scheduler;
    private final ServerTransport _serverTransport;

   
    private final Object initLock = new Object();
   
    private ServletContext servletContext;
    

    

    private final HQHibernateLogger         _hiberLogger;

    
    public HQApp() {
        _callbacks = new CallbackDispatcher();
        _shutdown = (ShutdownCallback)
            _callbacks.generateCaller(ShutdownCallback.class);
      

        _watchdog = new ThreadWatchdog("ThreadWatchdog");
        _watchdog.initialize();

        _scheduler = new Scheduler(10);
        this.registerCallbackListener(ShutdownCallback.class, _scheduler);

        try {
            _serverTransport = new ServerTransport(4);
            _serverTransport.start();
            _callbacks.registerListener(ShutdownCallback.class, _serverTransport);
        } catch (Exception e) {
            throw new RuntimeException("Unable to start server transport", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                _log.info("Running shutdown hooks");
                _shutdown.shutdown();
                _log.info("Done running shutdown hooks");
            }
        });
        _hiberLogger = new HQHibernateLogger();
    }

   
    @PostConstruct
    public void init() {
        if (servletContext == null) {
            return;
        }
        String war = servletContext.getRealPath("/");
        File warDir = new File(war);
        setWebAccessibleDir(warDir);
        String restartStorageDir = new File(warDir.getParent()).getParent();
        setRestartStorageDir(new File(restartStorageDir));
    }

   
    public ThreadWatchdog getWatchdog() {
        synchronized (_watchdog) {
            return _watchdog;
        }
    }

    public AgentProxyFactory getAgentProxyFactory() {
        return _serverTransport.getAgentProxyFactory();
    }

    public Scheduler getScheduler() {
        return _scheduler;
    }

    public void setRestartStorageDir(File dir) {
        synchronized (initLock) {
            _restartStorage = dir;
        }
    }

    /**
     * Get a directory which can have files placed into it which will carry
     * over for a restart.  This should not be used to place files for
     * extensive periods of time.
     */
    public File getRestartStorageDir() {
        synchronized (initLock) {
            return _restartStorage;
        }
    }

    public void setWebAccessibleDir(File dir) {
        synchronized(initLock) {
            _webAccessibleDir = dir;
        }
    }

    /**
     * Get the directory which represents the URL root for the application
     */
    public File getWebAccessibleDir() {
        synchronized(initLock) {
            return _webAccessibleDir;
        }
    }

    public Properties getTweakProperties() throws IOException {
        return readTweakProperties();
    }

    private static Properties readTweakProperties() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is =
            loader.getResourceAsStream("tweak.properties");
        Properties res = new Properties();

        if (is == null)
            return res;

        try {
            res.load(is);
        } finally {
            try {is.close();} catch(IOException e) {}
        }
        return res;
    }

 
    /**
     * @see CallbackDispatcher#generateCaller(Class)
     */
    public Object registerCallbackCaller(Class iFace) {
        return _callbacks.generateCaller(iFace);
    }

    /**
     * @see CallbackDispatcher#registerListener(Class, Object)
     */
    public void registerCallbackListener(Class iFace, Object listener) {
        _callbacks.registerListener(iFace, listener);
    }

   
    private void scheduleCommitCallback() {
        Transaction t =
            Util.getSessionFactory().getCurrentSession().getTransaction();
       
        final boolean debug = _log.isDebugEnabled();

        if (debug) {
            _log.debug("Scheduling commit callback ");
        }
        t.registerSynchronization(new Synchronization() {
            public void afterCompletion(int status) {
                if (debug) {
                    _log.debug("Running post-commit");
                }
                runPostCommitListeners(status == Status.STATUS_COMMITTED);
            }

            public void beforeCompletion() {
                if (debug) {
                    _log.debug("Running pre-commit");
                }
                runPreCommitListeners();
            }
        });
    }

    /**
     * Register a listener to be called after a tx has been committed.
     */
    public void addTransactionListener(TransactionListener listener) {
        List listeners = (List)_txListeners.get();

        if (listeners == null) {
            listeners = new ArrayList(1);
            _txListeners.set(listeners);
            scheduleCommitCallback();
        }

        listeners.add(listener);

        // Unfortunately, it seems that the Tx synchronization will get called
        // before Hibernate does its flush.  This wasn't the behaviour before,
        // and looks like it will be fixed up again in 3.3.. :-(
        Util.getSessionFactory().getCurrentSession().flush();
    }

    /**
     * Execute all the pre-commit listeners registered with the current thread.
     */
    private void runPreCommitListeners() {
        List list = (List)_txListeners.get();

        if (list == null)
            return;

        for (Iterator i=list.iterator(); i.hasNext(); ) {
            TransactionListener l = (TransactionListener)i.next();

            try {
                l.beforeCommit();
            } catch(Exception e) {
                _log.warn("Error running pre-commit listener [" + l + "]", e);
            }
        }
    }

    /**
     * Execute all the post-commit listeners registered with the current thread
     */
    private void runPostCommitListeners(boolean success) {
        List list = (List)_txListeners.get();

        if (list == null)
            return;

        try {
            for (Iterator i=list.iterator(); i.hasNext(); ) {
                TransactionListener l = (TransactionListener)i.next();

                try {
                    l.afterCommit(success);
                } catch(Exception e) {
                    _log.warn("Error running post-commit listener [" + l + "]", e);
                }
            }
        } finally {
            _txListeners.set(null);
        }
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
     * Get the hibernate log manager, which allows the caller to execute
     * code within the context of a logging hibernate interceptor.
     */
    public HibernateLogManager getHibernateLogManager() {
        return _hiberLogger;
    }

    public static HQApp getInstance() {
        return Bootstrap.getBean(HQApp.class);
    }
}
