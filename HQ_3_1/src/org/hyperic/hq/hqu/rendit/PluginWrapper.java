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
package org.hyperic.hq.hqu.rendit;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.runtime.InvokerHelper;

/**
 * Basically a wrapper around the classloader and associated groovy 
 * artifacts.
 * 
 * This class sets up the rendit_sys directory as a member of the classloader
 * hierarchy.  In addition, any jars contained in pluginDir/lib will be
 * added to the classloader.
 */
public class PluginWrapper {
    private static final Log _log = LogFactory.getLog(PluginWrapper.class);
    
    private       File              _pluginDir;
    private final File              _sysDir;
    private final GroovyClassLoader _loader;

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
    
    File getPluginDir() {
        return _pluginDir;
    }
    
    Object run(String script, Binding b) throws Exception {
        Thread curThread = Thread.currentThread();
        ClassLoader oldLoader = curThread.getContextClassLoader();

        try {
            curThread.setContextClassLoader(_loader);
            Class c = _loader.parseClass(new File(_sysDir, script));
            Script s = InvokerHelper.createScript(c, b);
            return s.run();
        } finally {
            curThread.setContextClassLoader(oldLoader);
        }
    }
}
