/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.txsnatch.TxSnatch;
import org.hyperic.util.callback.CallbackDispatcher;
import org.hyperic.util.thread.ThreadWatchdog;
import org.jboss.ejb.Interceptor;
import org.jboss.invocation.Invocation;


/**
 * This class represents the central concept of the Hyperic HQ application.  
 * (not the Application resource)
 */
public class HQApp { 
    private static final HQApp INSTANCE = new HQApp(); 
    private static final Log _log = LogFactory.getLog(HQApp.class);

    private static Map         _txSynchs       = new HashMap();
    private ThreadLocal        _txListeners    = new ThreadLocal();
    private List               _startupClasses = new ArrayList();
    private CallbackDispatcher _callbacks;
    private ShutdownCallback   _shutdown;
    private File               _restartStorage;
    private File               _resourceDir;
    private File               _webAccessibleDir;
    private ThreadWatchdog     _watchdog;
    
    private final Object       STAT_LOCK = new Object();
    private long               _numTx;
    private long               _numTxErrors;
    
    static {
        TxSnatch.setSnatcher(new Snatcher());
    }
    
    private HQApp() {
        _callbacks = new CallbackDispatcher();
        _shutdown = (ShutdownCallback)
            _callbacks.generateCaller(ShutdownCallback.class);
        _watchdog = new ThreadWatchdog("ThreadWatchdog");
        
        _watchdog.initialize();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                _log.info("Running shutdown hooks");
                _shutdown.shutdown();
                _log.info("Done running shutdown hooks");
            }
        });
    }

    public ThreadWatchdog getWatchdog() {
        synchronized (_watchdog) {
            return _watchdog;
        }
    }
    
    public void setRestartStorageDir(File dir) {
        synchronized (_startupClasses) {
            _restartStorage = dir;
        } 
    }
    
    /**
     * Get a directory which can have files placed into it which will carry
     * over for a restart.  This should not be used to place files for
     * extensive periods of time.
     */
    public File getRestartStorageDir() {
        synchronized (_startupClasses) {
            return _restartStorage;
        } 
    }
    
    public void setResourceDir(File dir) {
        synchronized (_startupClasses) {
            _resourceDir = dir;
        }
    }
    
    /**
     * Get a directory which contains resources that various parts of the
     * application may need (templates, reports, license files, etc.)
     */
    public File getResourceDir() {
        synchronized (_startupClasses) {
            return _resourceDir;
        }
    }

    public void setWebAccessibleDir(File dir) {
        synchronized(_startupClasses) {
            _webAccessibleDir = dir;
        }
    }

    /**
     * Get the directory which represents the URL root for the application
     */
    public File getWebAccessibleDir() {
        synchronized(_startupClasses) {
            return _webAccessibleDir;
        }
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
    
    /**
     * Adds a class to the list of classes to invoke when the application has
     * started.
     */
    public void addStartupClass(String className) {
        synchronized (_startupClasses) {
            _startupClasses.add(className);
        }
    }
    
    void incrementTxCount(boolean txFailed) {
        synchronized (STAT_LOCK) {
            _numTx++;
            if (txFailed)
                _numTxErrors++;
        }
    }

    /**
     * Get the # of transactions which have been run since the start of the
     * application
     */
    public long getTransactions() {
        synchronized (STAT_LOCK) {
            return _numTx;
        }
    }
    
    /**
     * Get the # of transactions which have failed since the start of the
     * application
     */
    public long getTransactionsFailed() {
        synchronized (STAT_LOCK) {
            return _numTxErrors;
        }
    }
    
    private static class TxSynch implements Synchronization, Serializable {
        private javax.transaction.Transaction _me;
        
        private TxSynch(javax.transaction.Transaction me) {
            _me   = me;
        }
        
        public void afterCompletion(int status) {
            synchronized (_txSynchs) {
                if (_txSynchs.remove(_me) == null) {
                    _log.error("Strange.  I was a registered synchronization " +
                               "but can't find myself.  Where am I?");
                }
            }
        
            if (status != Status.STATUS_COMMITTED) {
                HQApp.getInstance().incrementTxCount(true);
                if (_log.isTraceEnabled()) {
                    _log.trace("Transaction [" + _me + "] failed!");
                }
                // Failed Tx -- kill the session.
                SessionManager.cleanupSession(false);
            } else {
                HQApp.getInstance().incrementTxCount(false);
            }
        }

        public void beforeCompletion() {
        }
    }
    
    private static class Snatcher implements TxSnatch.Snatcher  {
        private void attemptRegisterSynch(javax.transaction.Transaction tx,
                                          Session s) 
        {
            boolean newSynch = false;

            synchronized (_txSynchs) {
                if (_txSynchs.containsKey(tx))
                    return;
            
                newSynch = true;
                _txSynchs.put(tx, s);
            }
            if (newSynch) {
                try {
                    tx.registerSynchronization(new TxSynch(tx));
                } catch(Exception e) {
                    _log.error("Unable to register synchronization!", e);
                }
            }
        }
        
        private Object invokeNextBoth(Interceptor next, 
                                      org.jboss.proxy.Interceptor proxyNext,                                      
                                      Invocation v, boolean isHome) 
            throws Throwable
        {
            Method meth           = v.getMethod();
            String methName       = meth.getName();
            Class c               = meth.getDeclaringClass();
            String className      = c.getName();
            boolean readWrite     = false;
            boolean flush         = true;
            boolean sessCreated   = SessionManager.setupSession(methName);
            
            if (sessCreated && _log.isDebugEnabled()) {
                _log.debug("Created session, executing [" + methName + 
                           "] on [" + className + "]");
            }
                                                  
            try {
                if (_log.isTraceEnabled()) {
                    _log.trace("invokeNext: tx=" + v.getTransaction() + 
                               " meth=" + methName);
                }
                if (v.getTransaction() != null) {
                    attemptRegisterSynch(v.getTransaction(), 
                                         SessionManager.currentSession());
                }

                if (!methIsReadOnly(methName)) {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Upgrading session, due to [" + methName + 
                                   "] on [" + className + "]");
                    }
                    readWrite = true;
                    SessionManager.setSessionReadWrite();
                }
                
                if (proxyNext != null) 
                    return proxyNext.invoke(v);
                if (isHome)
                    return next.invokeHome(v);
                else
                    return next.invoke(v);
            } catch(Throwable e) { 
                flush = false;
                throw e;
            } finally { 
                if (sessCreated) {
                    if (!readWrite && _log.isDebugEnabled()) {
                        _log.debug("Successfully ran read-only transaction " + 
                                   "for [" + methName + "] on [" + 
                                   className + "]");
                    }
                    SessionManager.cleanupSession(flush);
                }
            }
        }
        
        private boolean methIsReadOnly(String methName) {
            return methName.startsWith("get") ||
                   methName.startsWith("find") ||
                   methName.startsWith("is") ||
                   methName.startsWith("check") ||
                   methName.equals("create"); /* 'create' is part of EJB session
                                                 bean creation */
        }

        public Object invokeProxyNext(org.jboss.proxy.Interceptor next, 
                                      Invocation v) 
            throws Throwable 
        {
            return invokeNextBoth(null, next, v, false);
        }

        public Object invokeNext(Interceptor next, Invocation v) 
            throws Exception 
        {
            try {
                return invokeNextBoth(next, null, v, false);
            } catch(Exception e) {
                throw e;
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
            
        }
        
        public Object invokeHomeNext(Interceptor next, Invocation v) 
            throws Exception
        {
            try {
                return invokeNextBoth(next, null, v, true);
            } catch(Exception e) {
                throw e;
            } catch(Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }
    
    /**
     * Execute the registered startup classes.
     */
    public void runStartupClasses() {
        List classNames;
        
        synchronized (_startupClasses) {
            classNames = new ArrayList(_startupClasses);
        }
        
        for (Iterator i=classNames.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            
            try {
                Class c = Class.forName(name);
                StartupListener l = (StartupListener)c.newInstance();
     
                _log.info("Executing startup: " + name);
                l.hqStarted();
            } catch(Exception e) {
                _log.warn("Error executing startup listener [" + name + "]", e);
            }
        }
    }
    
    private void scheduleCommitCallback() {
        Transaction t = 
            Util.getSessionFactory().getCurrentSession().getTransaction();
        final long commitNo = getTransactions();
        final boolean debug = _log.isDebugEnabled();
        
        if (debug) {
            _log.debug("Scheduling commit callback " + commitNo);
        }
        t.registerSynchronization(new Synchronization() {
            public void afterCompletion(int status) {
                if (debug) {
                    _log.debug("Running post-commit for commitNo: " + commitNo);
                }
                runPostCommitListeners(status == Status.STATUS_COMMITTED);
            }

            public void beforeCompletion() {
                if (debug) {
                    _log.debug("Running pre-commit for commitNo: " + commitNo);
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
    
    public static HQApp getInstance() {
        return INSTANCE;
    }
}
