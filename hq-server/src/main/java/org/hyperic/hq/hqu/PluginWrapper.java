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
package org.hyperic.hq.hqu;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.hqu.server.session.Attachment;
import org.hyperic.hq.hqu.server.session.UIPlugin;
import org.hyperic.util.Runnee;
import org.hyperic.util.file.FileUtil;

/**
 * Basically a wrapper around the classloader and associated groovy artifacts.
 * 
 * This class sets up the rendit_sys directory as a member of the classloader
 * hierarchy. In addition, any jars contained in pluginDir/lib will be added to
 * the classloader.
 */
public class PluginWrapper {

    private static final Log _log = LogFactory.getLog(PluginWrapper.class);

    private File _pluginDir;
    private final GroovyClassLoader _loader;
    private IDispatcher _dispatcher;

    /**
     * @return A unique temporary directory for embedded jar deployments.
     */
    private File getTempDir() {
        String tmp = System.getProperty("java.io.tmpdir");

        File tmpHquDir = new File(tmp, "hqu");

        if (!tmpHquDir.exists()) {
            if (!tmpHquDir.mkdirs()) {
                throw new RuntimeException("Unable to create temporary directory " + tmpHquDir.getAbsolutePath());
            }
        }

        return tmpHquDir;
    }

    PluginWrapper(File pluginDir, ClassLoader parentLoader) {
        GroovyClassLoader groovyLoader = new GroovyClassLoader(parentLoader);

        _pluginDir = pluginDir;

        try {
            File tmpDir = getTempDir();
            File libDir = new File(_pluginDir, "lib");

            if (libDir.isDirectory()) {
                File[] files = libDir.listFiles();

                for (int i = 0; i < files.length; i++) {
                    if (files[i].isFile() && files[i].getName().endsWith(".jar")) {
                        String prefix = pluginDir.getName() + "-" +
                                        files[i].getName().substring(0, files[i].getName().length() - 4);
                        File tmpJar = File.createTempFile(prefix, ".jar", tmpDir);
                        FileUtil.copyFile(files[i], tmpJar);
                        tmpJar.deleteOnExit();
                        groovyLoader.addURL(tmpJar.toURL());
                        _log.info("Added url [" +tmpJar.toURL() + "] to plugin [" + pluginDir + "]");
                    }
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        _loader = groovyLoader;
    }

    private Object doInContext(Runnee runnable) throws Exception {
        Thread curThread = Thread.currentThread();
        ClassLoader oldLoader = curThread.getContextClassLoader();

        try {
            curThread.setContextClassLoader(_loader);
            return runnable.run();
        } finally {
            curThread.setContextClassLoader(oldLoader);
        }
    }

    void loadDispatcher() throws Exception {
        doInContext(new Runnee() {
            public Object run() throws Exception {
                try {
                    Class c = _loader.loadClass("org.hyperic.hq.hqu.rendit.Dispatcher");
                    _dispatcher = (IDispatcher) c.newInstance();
                } catch (Exception e) {
                    _log.error("Unable to load groovy class: org.hyperic.hq.hqu.rendit.Dispatcher", e);
                }
                return null;
            }
        });
    }

    Properties loadPlugin() {
        try {
            return (Properties) doInContext(new Runnee() {
                public Object run() {
                    return _dispatcher.loadPlugin(_pluginDir);
                }
            });
        } catch (Exception e) {
            throw new PluginLoadException("Unable to load plugin", e);
        }
    }

    void handleRequest(final Object request) {
        try {
            doInContext(new Runnee() {
                public Object run() {
                    _dispatcher.handleRequest(request);
                    return null;
                }
            });
        } catch (Exception e) {
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
        } catch (Exception e) {
            _log.warn("Error deploying from " + _pluginDir, e);
        }
    }

    AttachmentDescriptor getAttachmentDescriptor(final Attachment a, final Resource r, final AuthzSubject u) {
        try {
            return (AttachmentDescriptor) doInContext(new Runnee() {
                public Object run() {
                    return _dispatcher.getAttachmentDescriptor(a, r, u);
                }
            });
        } catch (Exception e) {
            _log.warn("Error getting attachment descriptor for " + _pluginDir, e);
            throw new SystemException(e);
        }
    }

    Object invokeMethod(final InvokeMethodInvocationBindings b) {
        try {
            return doInContext(new Runnee() {
                public Object run() {
                    return _dispatcher.invokeMethod(b);
                }
            });
        } catch (Exception e) {
            _log.warn("Error invoking method from " + _pluginDir, e);
            throw new SystemException(e);
        }
    }

    File getPluginDir() {
        return _pluginDir;
    }
}
