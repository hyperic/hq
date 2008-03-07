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
import java.net.URLClassLoader;

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
    
    /**
     * The system property to set the path to the log4j configuration file 
     * that the jboss server will use.
     */
    private static final String LOG4J_CONFIGURATION = "log4j.configuration";
    
    private static final String JAVA_ENDORSED_DIRS = "java.endorsed.dirs";
    
    private final String _jbossHomeDir;
    
    private final String _configuration;
            
    private volatile boolean _isStarted;
    
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
        return _isStarted;
    }
    
    /**
     * Start the server. This is a blocking operation.
     * 
     * @throws Exception if the server fails to start.
     */
    public void startServer() throws Exception {
        System.setProperty(HOME_DIR, _jbossHomeDir);
        System.setProperty(SERVER_NAME, _configuration);
        
        // we will block on server shutdown
        System.setProperty(BLOCKING_SHUTDOWN, Boolean.TRUE.toString());
        
        // set java.endorsed.dirs
        System.setProperty(JAVA_ENDORSED_DIRS, _jbossHomeDir+"/lib/endorsed/");
        
        // set the log4j config file if it's not set already
//        if (System.getProperty(LOG4J_CONFIGURATION) == null) {
//            System.setProperty(LOG4J_CONFIGURATION, 
//                    _jbossHomeDir+"/server/"+_configuration+"/conf/log4j.xml");            
//        }

        _log.debug("Starting server at: "+_jbossHomeDir+"; " +
        		   "configuration="+_configuration);
        _log.debug("java.endorsed.dirs="+
                    System.getProperty(JAVA_ENDORSED_DIRS));

        // When running junit within an IDE environment, we don't want jboss 
        // to boot off of classes from the IDE environment. Instead, jboss 
        // should boot off of classes from the jboss home dir.
        // Therefore, we will set up our own class loader hierarchy for jboss, 
        // rooted directly off the bootstrap loader.
        URI runJar = new URI("file:"+_jbossHomeDir+"/bin/run.jar");
                
        ClassLoader jbossLoader = new URLClassLoader(new URL[] {runJar.toURL()}, null);
        
        Thread.currentThread().setContextClassLoader(jbossLoader);
        
        Object main = Class.forName("org.jboss.Main", true, jbossLoader).newInstance();        
  
        // the boot operation is blocking
        Method method = main.getClass().getMethod("boot", new Class[] {String[].class});
        
        method.invoke(main, new Object[]{new String[0]});
        
        _isStarted = true;
    }
    
    /**
     * Stop the server iff it is already started.
     */
    public void stopServer() {
        _log.debug("Stopping server at: "+_jbossHomeDir+"; " +
                   "configuration="+_configuration);

        
        if (_isStarted) {
            shutdownJboss();
                     
//            System.clearProperty(HOME_DIR);
//            System.clearProperty(SERVER_NAME);
//            System.clearProperty(BLOCKING_SHUTDOWN);
//            System.clearProperty(JAVA_ENDORSED_DIRS);
            
            _isStarted = false;
        }
    }
    
    /**
     * Deploy the package represented by the URL.
     * 
     * @param url The URL representing the package to deploy.
     * @throws IllegalStateException if the server is not started.
     * @throws Exception if the package cannot be deployed.
     */
    public void deploy(URL url) throws Exception {
        if (!_isStarted) {
            throw new IllegalStateException("the server is not started.");
        }
        
        deployOrUnDeploy(true, url);
    }
    
    /**
     * Undeploy the package represented by the URL iff the server is started.
     * 
     * @param url The URL representing the package to undeploy.
     * @throws Exception if the package cannot be undeployed.
     */
    public void undeploy(URL url) throws Exception {
        if (_isStarted) {
            deployOrUnDeploy(false, url);
        }
    }
    
    private void shutdownJboss() {
        MBeanServer server = MBeanUtil.getMBeanServer();
        
        try {
            server.invoke(new ObjectName("jboss.system:type=Server"), 
                          "shutdown", new Object[0], new String[0]);
        } catch (Exception e) {
            _log.error("Error shutting down server.", e);
        }
    }
    
    private void deployOrUnDeploy(boolean deploy, URL url) throws Exception {
        String methodName;
        
        if (deploy) {
            methodName = "deploy";
        } else {
            methodName = "undeploy";
        }
        
        MBeanServer server = MBeanUtil.getMBeanServer();
        
        server.invoke(new ObjectName("jboss.system:service=MainDeployer"), 
                      methodName, new Object[]{url}, new String[0]);          
    }

}
