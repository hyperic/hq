/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.util.unittest.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.server.MBeanUtil;


/**
 * This class manages the Hyperic Server lifecycle in a unit testing environment. 
 * It is assumed that the local jboss deployment has already been prepared 
 * using the Ant target <code>prepare-jboss</code>. In addition we are assuming 
 * that this is a clean instance of jboss that does not include an HQ EAR 
 * deployment and there is no other instance of the Hyperic server currently 
 * running on the host.
 */
public class ServerLifecycle {
    
    private static final Log _log = LogFactory.getLog(ServerLifecycle.class);
    
    /**
     * See org.jboss.system.server.ServerConfig.HOME_DIR
     */
    private static final String HOME_DIR = "jboss.home.dir";
    
    /**
     * See org.jboss.system.server.ServerConfig.SERVER_NAME
     */
    private static final String SERVER_NAME = "jboss.server.name";
    
    /**
     * See org.jboss.system.server.ServerConfig.BLOCKING_SHUTDOWN
     */
    private static final String BLOCKING_SHUTDOWN = "jboss.server.blockingshutdown";    
   
    private final Object _lifecycleLock = new Object();
    
    private final String _jbossHomeDir;
    
    private final String _configuration;
            
    private boolean _isStarted;
   
    
    /**
     * Create an instance using the "default" configuration in the jboss home dir.
     * 
     * @param jbossHomeDir The path to the local jboss deployment.
     * @throws NullPointerException if the jboss home dir is <code>null</code>.
     * @throws IllegalArgumentException if the jboss home dir does not exist.
     * @throws IOException if the jboss home dir path cannot be resolved.
     */
    public ServerLifecycle(File jbossHomeDir) throws IOException {
        this(jbossHomeDir, "default");
    }
    
    /**
     * Create an instance using the specified configuration in the jboss home dir.
     * 
     * @param jbossHomeDir The path to the local jboss deployment.
     * @param configuration The configuration name.
     * @throws NullPointerException if the jboss home dir or configuration name 
     *                              is <code>null</code>.
     * @throws IllegalArgumentException if the jboss home dir does not exist.
     * @throws IOException if the jboss home dir path cannot be resolved.
     */
    public ServerLifecycle(File jbossHomeDir, String configuration) throws IOException {
        if (jbossHomeDir == null) {
            throw new NullPointerException("jboss home dir is null");
        }
        
        if (configuration == null) {
            throw new NullPointerException("configuration is null");
        }
                
        if (!jbossHomeDir.exists()) {
            throw new IllegalArgumentException("jboss home dir does not exist:"+
                                               jbossHomeDir.getCanonicalPath());
        }
        
        _jbossHomeDir = jbossHomeDir.getCanonicalPath();
        _configuration = configuration;        
    }
    
    /**
     * @return <code>true</code> if the server is started; 
     *         <code>false</code> otherwise.
     */
    public boolean isStarted() {
        synchronized (_lifecycleLock) {
            return _isStarted;            
        }
    }
    
    /**
     * Start the server. This is a blocking operation.
     * 
     * @throws Exception if the server fails to start.
     */
    public void startServer() throws Exception {
        synchronized (_lifecycleLock) {
            if (!_isStarted) {
                System.setProperty(HOME_DIR, _jbossHomeDir);
                System.setProperty(SERVER_NAME, _configuration);
                
                // we will block on server shutdown
                System.setProperty(BLOCKING_SHUTDOWN, Boolean.TRUE.toString());
                        
                _log.debug("Starting server at: "+_jbossHomeDir+"; " +
                           "configuration="+_configuration);

                bootJboss();
                
                setVMNotExitOnServerShutdown();
                
                _isStarted = true;            
            }            
        }
    }
        
    /**
     * Stop the server iff it is already started.
     */
    public void stopServer() {
        _log.debug("Stopping server at: "+_jbossHomeDir+"; " +
                   "configuration="+_configuration);
        
        synchronized (_lifecycleLock) {
            if (_isStarted) {
                shutdownJboss();
                _isStarted = false;
            }   
        }        
    }
        
    /**
     * Deploy the package represented by the URL.
     * 
     * @param url The URL representing the package to deploy.
     * @throws IllegalStateException if the server is not started.
     * @throws Exception if the package cannot be deployed.
     */
    public void deploy(final URL url) throws Exception {
        synchronized (_lifecycleLock) {
            if (!_isStarted) {
                throw new IllegalStateException("the server is not started.");
            }
            
            deployOrUnDeploy(true, url);                    
        }
    }
        
    /**
     * Undeploy the package represented by the URL iff the server is started.
     * 
     * @param url The URL representing the package to undeploy.
     * @throws Exception if the package cannot be undeployed.
     */
    public void undeploy(URL url) throws Exception {
        synchronized (_lifecycleLock) {
            if (_isStarted) {
                deployOrUnDeploy(false, url);
            }            
        }        
    }
    
    private void bootJboss() throws Exception {
        // We want to register an uncaught exception handler to try 
        // and prevent the host vm from being shutdown in case jboss 
        // fails on boot (since we may want to run other unit tests 
        // in the vm).
        ExceptionHandlingThreadGroup group = 
            new ExceptionHandlingThreadGroup("jboss-boot-group");
        
        group.setUncaughtExceptionAction(new Runnable() {
            public void run() {
                try {
                    setVMNotExitOnServerShutdown();
                } catch (Exception e) {
                    _log.error("Setting the vm to not exit on server shutdown failed.", e);
                }
            }
            
        });
        
        // When booting jboss within a junit framework, we don't want jboss 
        // to boot off of the classpath where the junit framework classes 
        // reside. Instead, jboss should boot off of classes and resources 
        // from the jboss home dir. Therefore, we will set up our own class 
        // loader hierarchy for jboss, using our own system classloader that 
        // delegates to the *parent* of the default system classloader.
        Thread bootThread = new Thread(group, "jboss-main") {
            public void run() {
                IsolatingDefaultSystemClassLoader cl = 
                    (IsolatingDefaultSystemClassLoader)ClassLoader.getSystemClassLoader();
                
                cl.setIsolateDefaultSystemClassloader();
                
                try {
                    URL runJar = new URL("file:"+_jbossHomeDir+"/bin/run.jar");
                    
                    cl.addURL(runJar);
                    
                    Thread.currentThread().setContextClassLoader(cl);
                                        
                    Object main = cl.loadClass("org.jboss.Main").newInstance();
                    
                    // the boot operation is blocking
                    Method method = main.getClass().getMethod("boot", new Class[] {String[].class});
                    
                    method.invoke(main, new Object[]{new String[0]});    
                } catch (Exception e) {
                    throw new RuntimeException("jboss boot did not succeed", e);
                }

            }
        };
        
        bootThread.start();
        bootThread.join();
        
        if (group.getUncaughtException() != null) {
            throw new Exception(group.getUncaughtException());
        }
    }
    
    private void shutdownJboss() {
        Thread shutdownThread = new Thread("jboss-shutdown") {
            public void run() {
                IsolatingDefaultSystemClassLoader cl = 
                    (IsolatingDefaultSystemClassLoader)ClassLoader.getSystemClassLoader();
                
                cl.setIsolateDefaultSystemClassloader();
                
                Thread.currentThread().setContextClassLoader(cl);
                                    
                MBeanServer server = MBeanUtil.getMBeanServer();
                
                try {
                    server.invoke(new ObjectName("jboss.system:type=Server"), 
                                  "shutdown", new Object[0], new String[0]);
                } catch (Exception e) {
                    _log.error("Error shutting down server.", e);
                }  
            }
        };
        
        shutdownThread.start();
        
        try {
            shutdownThread.join();
        } catch (InterruptedException e) {
            // swallow
        }
    }
        
    /**
     * Deploy or undeploy a package at the give URL.
     * 
     * @param deploy <code>true</code> to deploy; <code>false</code> to undeploy.
     * @param url The URL.
     * @throws Exception if the deploy/undeploy fails.
     */
    private void deployOrUnDeploy(final boolean deploy, final URL url) throws Exception {
        ExceptionHandlingThreadGroup group = 
            new ExceptionHandlingThreadGroup("jboss-deploy-group");
        
        Thread deployThread = new Thread(group, "jboss-deploy") {
            public void run() {
                IsolatingDefaultSystemClassLoader cl = 
                    (IsolatingDefaultSystemClassLoader)ClassLoader.getSystemClassLoader();
                
                cl.setIsolateDefaultSystemClassloader();
                
                Thread.currentThread().setContextClassLoader(cl);
                
                if (deploy) {
                    loadSigar(url, cl);                    
                }
                
                deployURL(deploy, url);
                
                if (deploy) {
                    deployPlugins();                    
                }
            }
        };
        
        deployThread.start();
        deployThread.join();    
        
        if (group.getUncaughtException() != null) {
            throw new Exception(group.getUncaughtException());
        }
    }
        
    /**
     * Force sigar to load the correct dlls.
     * 
     * @param url The URL for the deployment package.
     * @param cl The isolating system classloader.
     */
    private void loadSigar(final URL url, IsolatingDefaultSystemClassLoader cl) {
        try {
            
            File deployDir = new File(new URI(url.toString()));

            URL sigarJar = new URL("file:"+deployDir.getCanonicalPath()+"/lib/sigar.jar");

            cl.addURL(sigarJar);                        

            File sigarBinDir = new File(deployDir, "sigar_bin/lib/");

            System.setProperty("org.hyperic.sigar.path", sigarBinDir.getCanonicalPath());

            Class clazz = cl.loadClass("org.hyperic.sigar.OperatingSystem");

            Method method = clazz.getMethod("getInstance", new Class[0]);

            method.invoke(clazz, new Object[0]);            
        } catch (Exception e) {
            throw new RuntimeException("Failed to load sigar", e);
        }
    }
    
    /**
     * Deploy or undeploy a package at the give URL.
     * 
     * @param deploy <code>true</code> to deploy; <code>false</code> to undeploy.
     * @param url The URL.
     */
    private void deployURL(boolean deploy, URL url) {
        String methodName;
        
        if (deploy) {
            methodName = "deploy";
        } else {
            methodName = "undeploy";
        }
        
        MBeanServer server = MBeanUtil.getMBeanServer();
        
        try {                    
            server.invoke(new ObjectName("jboss.system:service=MainDeployer"), 
                          methodName, new Object[]{url}, new String[]{"java.net.URL"});
        } catch (Exception e) {
            throw new RuntimeException("Could not "+methodName+
                                       " package at: "+url, e); 
        }
    }
    
    /**
     * Deploy the HQ plugins.
     */
    private void deployPlugins() {
        MBeanServer server = MBeanUtil.getMBeanServer();
        
        try {
            server.invoke(new ObjectName("hyperic.jmx:type=Service,name=ProductPluginDeployer"), 
                          "handleNotification", new Object[]{null, null}, 
                          new String[]{"javax.management.Notification", "java.lang.Object"});
        } catch (Exception e) {
            throw new RuntimeException("Could not deploy hq plugins", e); 
        }        
    }
    
    /**
     * There is a system property to not exit the vm on server shutdown, but 
     * the jboss Main class has been hardcoded to exit the vm. We can override 
     * this setting with a jmx call to the server configuration mbean.
     * 
     * See org.jboss.system.server.ServerConfig.EXIT_ON_SHUTDOWN
     */
    private void setVMNotExitOnServerShutdown() throws Exception {        
        MBeanServer server = MBeanUtil.getMBeanServer();
        
        server.setAttribute(new ObjectName("jboss.system:type=ServerConfig"), 
                new Attribute("ExitOnShutdown", Boolean.FALSE));    
    }
    
    /**
     * A thread group that a client may use for registering an action to perform 
     * if an uncaught exception is handled. The uncaught exception may also be 
     * retrieved.
     */
    private static class ExceptionHandlingThreadGroup extends ThreadGroup {
        
        private final Object _lock = new Object();
        private Runnable _runnable;
        private Throwable _uncaughtException;
        
        
        public ExceptionHandlingThreadGroup(String name) {
            super(name);
        }
        
        /**
         * @return The uncaught exception or <code>null</code>.
         */
        public Throwable getUncaughtException() {
            synchronized (_lock) {
                return _uncaughtException;
            }
        }
        
        /**
         * Set an action to execute in the uncaught exception handler.
         * 
         * @param runnable The runnable representing the action to execute.
         */
        public void setUncaughtExceptionAction(Runnable runnable) {
            synchronized (_lock) {
                _runnable = runnable;
            }
        }
        
        public void uncaughtException(Thread t, Throwable e) {
            synchronized (_lock) {
                _uncaughtException = e;
                
                if (_runnable != null) {
                    _runnable.run();
                }
            }
        }        
    }    

}
