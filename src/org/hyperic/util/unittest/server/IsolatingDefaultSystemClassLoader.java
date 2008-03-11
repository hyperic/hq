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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * This classloader is used by the unit test framework to isolate jboss from 
 * the unit test framework classpath so that jboss may boot off its own resources 
 * instead of those residing on the classpath. This is achieved by setting this 
 * classloader as the system classloader with the <code>java.system.class.loader</code> 
 * system property. When the unit test framework vm is started, this classloader 
 * is substituted for the default system classloader containing the classpath. 
 * If set to isolate the default system classloader, this classloader will 
 * delegate all class/resource lookups to the parent of the default system 
 * classloader, effectively isolating the default system classloader from the 
 * jboss boot process. 
 */
public class IsolatingDefaultSystemClassLoader extends URLClassLoader {
    
    public static final ThreadLocal SHOULD_ISOLATE = 
        new InheritableThreadLocal()  {
            protected synchronized Object initialValue() {
                return Boolean.FALSE;
            }
    };
    
    private final ClassLoader _defaultSystemClassLoader;
    
    
    /**
     * This constructor is required when registering this classloader as the 
     * system classloader.
     *
     * @param parent The parent classloader. Should be the default system classloader.
     */
    public IsolatingDefaultSystemClassLoader(ClassLoader parent) {
        super(new URL[0], parent.getParent());
        
        if (parent.getParent() == null) {
            throw new IllegalArgumentException("we are expecting that the parent " +
            		                "has its own parent we use for delegation");
        }

        _defaultSystemClassLoader = parent;
    }
    
    /**
     * Appends the specified URL to the list of URLs to search for
     * classes and resources.
     *
     * @param url the URL to be added to the search path of URLs
     */
    public void addURL(URL url) {
        super.addURL(url);
    }
    
    /**
     * Set the current thread and all child threads to isolate the default 
     * system classloader by delegating class/resource lookups to the parent 
     * of the default system classloader.
     */
    public void setIsolateDefaultSystemClassloader() {
        SHOULD_ISOLATE.set(Boolean.TRUE);
    }
    
    /**
     * If the current thread is set to isolate the default system classloader, 
     * then delegate resource lookups to the parent of the default system 
     * classloader before looking in this classloader for the resource. Otherwise, 
     * use the standard delegation algorithm that looks in the parent classloader 
     * first.
     * 
     * @see java.lang.ClassLoader#getResource(java.lang.String)
     */
    public URL getResource(String resName) {        
        boolean isolate = ((Boolean)SHOULD_ISOLATE.get()).booleanValue();
        
        URL url;
        
        if (isolate) {
            url = getParent().getResource(resName);
        } else {
            url = _defaultSystemClassLoader.getResource(resName);
        }
        
        if (url == null) {
            url = findResource(resName);
        }
        
        return url;        
    }
        
    /**
     * If the current thread is set to isolate the default system classloader, 
     * then delegate class loads to the parent of the default system 
     * classloader before loading the class from this classloader. Otherwise, 
     * use the standard delegation algorithm that loads from the parent 
     * classloader first.
     * 
     * @see java.lang.ClassLoader#loadClass(java.lang.String, boolean)
     */
    protected synchronized Class loadClass(String className, boolean resolveClass) 
        throws ClassNotFoundException  {
        
        boolean isolate = ((Boolean)SHOULD_ISOLATE.get()).booleanValue();

        // Ask the VM to look in its cache.
        Class loadedClass = findLoadedClass(className);
        
        
        if (loadedClass == null) {
            if (isolate) {
                // Delegate to the parent (which is actually the grand parent)
                try {
                    loadedClass = getParent().loadClass(className);
                } catch (ClassNotFoundException e) {
                    // don't do anything.  Catching this exception is the normal protocol for
                    // parent classloaders telling us they couldn't find a class.
                }

                // not findLoadedClass or by getParent().loadClass, try locally
                if (loadedClass == null) {
                    loadedClass = findClass(className);
                }
            } else {
                // Delegate to the default system classloader
                loadedClass = _defaultSystemClassLoader.loadClass(className);
            }    
        }
                
        // resolve if required
        if (resolveClass) {
            resolveClass(loadedClass);
        }
        
        return loadedClass;
    }

}
