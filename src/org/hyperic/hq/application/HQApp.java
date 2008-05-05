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
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hyperic.hibernate.HibernateInterceptorChain;
import org.hyperic.hibernate.HypericInterceptor;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.hibernate.SessionManager;
import org.hyperic.hq.transport.AgentProxyFactory;
import org.hyperic.hq.transport.ServerTransport;
import org.hyperic.txsnatch.TxSnatch;
import org.hyperic.util.callback.CallbackDispatcher;
import org.hyperic.util.thread.ThreadWatchdog;
import org.jboss.invocation.Invocation;


/**
 * This class represents the central concept of the Hyperic HQ application.  
 * (not the Application resource)
 */
public class HQApp { 
    private static final Log _log = LogFactory.getLog(HQApp.class);
    private static final HQApp INSTANCE = new HQApp(); 

    private static Map         _txSynchs       = new HashMap();
    private ThreadLocal        _txListeners    = new ThreadLocal();
    private List               _startupClasses = new ArrayList();
    private CallbackDispatcher _callbacks;
    private ShutdownCallback   _shutdown;
    private File               _restartStorage;
    private File               _resourceDir;
    private File               _webAccessibleDir;
    private ThreadWatchdog     _watchdog;
    private final Scheduler    _scheduler;
    private final ServerTransport _serverTransport;
    
    private final Object       STAT_LOCK = new Object();
    private long               _numTx;
    private long               _numTxErrors;

    private long               _methWarnTime;

    private Map _methInvokeStats      = new HashMap();
    private AtomicBoolean _collectMethStats = new AtomicBoolean();
    
    private StartupFinishedCallback _startupFinished;
    
    private final HQHibernateLogger         _hiberLogger;
    
    
    static {
        TxSnatch.setSnatcher(new Snatcher());
    }
    
    private HQApp() {
        _callbacks = new CallbackDispatcher();
        _shutdown = (ShutdownCallback)
            _callbacks.generateCaller(ShutdownCallback.class);
        _startupFinished = (StartupFinishedCallback)
            _callbacks.generateCaller(StartupFinishedCallback.class);
        
        _watchdog = new ThreadWatchdog("ThreadWatchdog");
        _watchdog.initialize();
        
        _scheduler = new Scheduler(4);
        this.registerCallbackListener(ShutdownCallback.class, _scheduler);
                
        try {
            _serverTransport = new ServerTransport(4);
            _serverTransport.start();
            this.registerCallbackListener(ShutdownCallback.class, _serverTransport);
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
        
        try {
            Properties p = HQApp.readTweakProperties();
            String prop = p.getProperty("hq.methodWarn.time");
            if (prop == null) {
                _log.warn("Failed to read tweak properties.  Setting method " + 
                          "warn time to 60000");
                _methWarnTime = 60 * 1000;
            } else { 
                _methWarnTime = Long.parseLong(prop);
            }
        } catch(Exception e) {
            _log.error("Unable to read tweak properties", e);
            _methWarnTime = 60 * 1000;
        }
    
        _hiberLogger = new HQHibernateLogger();
    }

    public void setMethodWarnTime(long warnTime) {
        synchronized (STAT_LOCK) {
            _methWarnTime = warnTime;
        }
    }
    
    public long getMethodWarnTime() {
        synchronized (STAT_LOCK) {
            return _methWarnTime;
        }
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

    public Properties getTweakProperties() throws IOException {
        return readTweakProperties();
    }
    
    private static Properties readTweakProperties() throws IOException { 
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = 
            loader.getResourceAsStream("META-INF/tweak.properties");
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

    private void warnMethodTooLong(long total, long warnTime,
                                   Invocation v, Object methodRes,
                                   boolean failed, String methName,
                                   String className)
    {
        Map txPayload = v.getTransientPayload();
        
        if (txPayload.containsKey("hq.methodWarned"))
            return;
        
        txPayload.put("hq.methodWarned", Boolean.TRUE);
        StringBuffer warn = new StringBuffer("Method ran a long time.\n");
        warn.append("Class:   ")
            .append(className)
            .append("\nMethod:  ")
            .append(methName)
            .append("\nRunTime: ")
            .append(total)
            .append("\n");
        
        if (!_log.isDebugEnabled()) {
            _log.warn(warn);
            return;
        }
        
        // if debug is enabled, log a lot more verbose stuff
        if (methodRes == null) {
            warn.append("Result:  null");
        } else {
            warn.append("Result:  (")
                .append(methodRes.getClass())
                .append(") ")
                .append(methodRes.toString());
        }
        warn.append("\nArguments: \n");
        
        Object[] args = v.getArguments();
        for (int i=0; i<args.length; i++) {
            warn.append("    Arg[")
                .append(i)
                .append("]: (")
                .append(args[i].getClass())
                .append(") ")
                .append(args[i].toString())
                .append("\n");
        }
        _log.warn(warn);
    }
    

    private String makeMethodStatKey(Class c, Method meth) {
        StringBuffer key = new StringBuffer();
        key.append(c.getName())
           .append("#")
           .append(meth.getName());
        Class[] params = meth.getParameterTypes();
        for (int i=0; i<params.length; i++) {
            key.append(params[i].getName());
        }
        return key.toString();
    }

    public void setCollectMethodStats(boolean enable) {
        _collectMethStats.set(enable);
    }
    
    public boolean isCollectingMethodStats() {
        return _collectMethStats.get();
    }
    
    public void clearMethodStats() {
        synchronized (STAT_LOCK) {
            _methInvokeStats.clear();
        }
    }
    
    private void updateMethodStats(Class c, Method meth, long total,
                                   boolean txFailed) 
    { 
        if (!_collectMethStats.get()) {
            return;
        }
        
        String key = makeMethodStatKey(c, meth);
        
        MethodStats stats;
        synchronized (STAT_LOCK) {
            stats = (MethodStats)_methInvokeStats.get(key);
            if (stats == null) {
                stats = new MethodStats(c, meth);
                _methInvokeStats.put(key, stats);
            }
        }
        if (stats != null)
            stats.update(total, txFailed);
    }
    
    public List getMethodStats() {
        synchronized (STAT_LOCK) {
            return new ArrayList(_methInvokeStats.values());
        }
    }
    
    private TxSynch createTxSynch(javax.transaction.Transaction tx) {
        return new TxSynch(tx);
    }

    private class TxSynch implements Synchronization, Serializable {
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
                incrementTxCount(true);
                if (_log.isTraceEnabled()) {
                    _log.trace("Transaction [" + _me + "] failed!");
                }
                // Failed Tx -- kill the session.
                SessionManager.cleanupSession(false);
            } else {
                incrementTxCount(false);
            }
        }

        public void beforeCompletion() {
        }
    }
    
    private static class Snatcher implements TxSnatch.Snatcher  {
        private final Object SNATCH_LOCK = new Object();
        private HQApp _app;
        
        private HQApp getAppInstance() {
            synchronized (SNATCH_LOCK) {
                if (_app == null)
                    _app = HQApp.getInstance();
                return _app;
            }
        }
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
                    HQApp app = getAppInstance();
                    tx.registerSynchronization(app.createTxSynch(tx));
                } catch(Exception e) {
                    synchronized (_txSynchs) {
                        _txSynchs.remove(tx);
                    }
                    
                    _log.error("Unable to register synchronization!", e);
                }
            }
        }
        
        private Object invokeNextBoth(org.jboss.ejb.Interceptor next, 
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
                if (v.getTransaction() != null && 
                    (v.getTransaction().getStatus() == Status.STATUS_ACTIVE ||
                     v.getTransaction().getStatus() == Status.STATUS_PREPARING)) {
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

                long startTime = System.currentTimeMillis();
                Object res = null;
                boolean failed = true;
                try {
                    if (proxyNext != null) 
                        res = proxyNext.invoke(v);
                    else if (isHome)
                        res = next.invokeHome(v);
                    else
                        res = next.invoke(v);
                    failed = false;
                } finally {
                    HQApp app = getAppInstance();
                    long total = System.currentTimeMillis() - startTime;
                    long warnTime = app.getMethodWarnTime();
                    
                    app.updateMethodStats(c, meth, total, failed);
                    if (warnTime != -1 && total > warnTime) {
                        try {
                            app.warnMethodTooLong(total, warnTime, v, res,
                                                  failed, methName,
                                                  className);
                        } catch(Throwable t) {
                            _log.warn("Error while warning.  Ugly", t);
                        }
                    }
                }
                return res;
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

        public Object invokeNext(org.jboss.ejb.Interceptor next, Invocation v) 
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
        
        public Object invokeHomeNext(org.jboss.ejb.Interceptor next, 
                                     Invocation v) 
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
            } catch(Throwable e) {
                _log.warn("Error executing startup listener [" + name + "]", e);
                if (e instanceof Error)
                    throw (Error)e;
                if (e instanceof RuntimeException)
                    throw (RuntimeException)e;
            } 
        }
        
        try {
            _startupFinished.startupFinished();
        } catch(Throwable t) {
            _log.error("Error calling startup finish listener", t);
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
        return INSTANCE;
    }
}
