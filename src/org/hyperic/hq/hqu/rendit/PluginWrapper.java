/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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
package org.hyperic.hq.hqu.rendit;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.hqu.AttachmentDescriptor;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.hyperic.util.Runnee;

/**
 * Basically a wrapper around the classloader and associated groovy 
 * artifacts.
 * 
 * This class sets up the rendit_sys directory as a member of the classloader
 * hierarchy.  In addition, any jars contained in pluginDir/lib will be
 * added to the classloader.
 */
public class PluginWrapper {
    public static final String DISPATCH_PATH = 
        "org/hyperic/hq/hqu/rendit/dispatcher.groovy";
    
    private static final Log _log = LogFactory.getLog(PluginWrapper.class);
    
    private       File              _pluginDir;
    private final File              _sysDir;
    private final GroovyClassLoader _loader;
    private       IDispatcher       _dispatcher;

    PluginWrapper(File pluginDir, File sysDir, ClassLoader parentLoader) {
        URLClassLoader urlLoader;
        List urls = new ArrayList();
        URL[] u;

        _sysDir    = sysDir;
        _pluginDir = pluginDir;

        try {
            File libDir = new File(_pluginDir, "lib");
            
            if (libDir.isDirectory()) {
                File[] files = libDir.listFiles();
                
                for (int i=0; i<files.length; i++) {
                    if (files[i].isFile() && 
                        files[i].getName().endsWith(".jar"))
                    {
                        urls.add(files[i].toURL());
                    }
                }
            }
            
            urls.add(sysDir.toURL());
            u = (URL[])urls.toArray(new URL[urls.size()]);
        } catch(MalformedURLException e) {
            throw new RuntimeException(e);
        }

        _log.info("Loading plugin in [" + pluginDir + "] with loaders for: " +
                  urls);
        urlLoader = URLClassLoader.newInstance(u, parentLoader);
        _loader = new GroovyClassLoader(urlLoader);
    }
    
    private Object doInContext(Runnee c) throws Exception {
        Thread curThread = Thread.currentThread();
        ClassLoader oldLoader = curThread.getContextClassLoader();

        try {
            curThread.setContextClassLoader(_loader);
            return c.run();
        } finally {
            curThread.setContextClassLoader(oldLoader);
        }
    }
    
    void loadDispatcher() throws Exception {
        doInContext(new Runnee() {
            public Object run() throws Exception {
                Class c = _loader.parseClass(new File(_sysDir, DISPATCH_PATH));
                _dispatcher = (IDispatcher)c.newInstance();
                return null;
            }
        });
    }
    
    Properties loadPlugin() {
        try {
            return (Properties)doInContext(new Runnee() {
                public Object run() {
                    return _dispatcher.loadPlugin(_pluginDir);
                }
            });
        } catch(Exception e) {
            throw new PluginLoadException("Unable to load plugin", e);
        }
    }
    
    void handleRequest(final RequestInvocationBindings b) {
        try {
            doInContext(new Runnee() {
                public Object run() {
                    _dispatcher.handleRequest(b);
                    return null;
                }
            });
        } catch(Exception e) {
            _log.warn("Error handling request from " + _pluginDir, e);
            throw new SystemException(e);
        }
    }
    
    void deploy(final UIPlugin p) {
        try {
            doInContext(new Runnee() {
                public Object run() {
                    _dispatcher.deploy(p);
                    return null;
                }
            });
        } catch(Exception e) {
            _log.warn("Error deploying from " + _pluginDir, e);
        }
    }
    
    AttachmentDescriptor getAttachmentDescriptor(final Attachment a, 
                                                 final Resource r,
                                                 final AuthzSubject u)
    {
        try {
            return (AttachmentDescriptor)doInContext(new Runnee() {
                public Object run() {
                    return _dispatcher.getAttachmentDescriptor(a, r, u);
                }
            });
        } catch(Exception e) {
            _log.warn("Error getting attachment descriptor for " + _pluginDir, 
                      e);
            throw new SystemException(e);
        }
    }

    void invokeMethod(final InvokeMethodInvocationBindings b) {
        try {
            doInContext(new Runnee() {
                public Object run() {
                    _dispatcher.invokeMethod(b);
                    return null;
                }
            });
        } catch(Exception e) {
            _log.warn("Error invoking method from " + _pluginDir, e);
            throw new SystemException(e);
        }
    }
    
    File getPluginDir() {
        return _pluginDir;
    }
}
