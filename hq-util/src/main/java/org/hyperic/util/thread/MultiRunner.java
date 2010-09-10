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

package org.hyperic.util.thread;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.MDC;
import org.hyperic.util.ArrayUtil;

/**
 * A class which is able to spawn off multiple threads, each within their
 * own classloader. 
 */
public class MultiRunner {
    public static final String PROP_CLONE_SPAWN_SLEEP = "clone.spawn.sleep";
    public static final String PROP_CLONE_CLASSPATH   = "clone.classpath";
    public static final String PROP_CLONE_NUM         = "clone.number";
    public static final String PROP_CLONE_CLASS       = "clone.class";
    
    private Properties _props;
    
    public MultiRunner(Properties p) {
        _props = p;
    }
    
    private static class TargetRunnable implements Runnable {
        private ClassLoader   _loader;
        private MultiRunnable _target;
        private int           _threadNo;
        private Properties    _props;
        
        TargetRunnable(ClassLoader cl, MultiRunnable target, int threadNo,
                       Properties props) 
        {
            _loader   = cl;
            _target   = target;
            _threadNo = threadNo;
            _props    = props;
        }
        
        public void run() {
            try {
                Thread.currentThread().setContextClassLoader(_loader);
                _target.configure(_threadNo, _props);
                _target.run();
            } catch(Throwable e) {
                e.printStackTrace();
            }
        }
    }
    
    private ClassLoader createThreadClassLoader() throws Exception {
        String classPath = _props.getProperty(PROP_CLONE_CLASSPATH);
        String[] split = classPath.split(":");
        URL[] urls = new URL[split.length];
        
        for (int i=0; i<split.length; i++) {
            File f = new File(split[i].trim());
            if (!f.canRead()) {
                System.out.println("Can't read [" + f.getAbsolutePath() + "]");
            }
            urls[i] = f.toURL();
        }
        
        return new URLClassLoader(urls,
                                  Thread.currentThread().getContextClassLoader());
    }
    
    private void startThread(int threadNo) throws Exception {
        ClassLoader cl = createThreadClassLoader();
        String className = _props.getProperty(PROP_CLONE_CLASS);
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
        
        try {
            Thread.currentThread().setContextClassLoader(cl);
            Class c = cl.loadClass(className);
            MultiRunnable r = (MultiRunnable)c.newInstance();
            Properties subProps = new Properties();
            subProps.putAll(_props);
            MDC.put("cloneId", "clone-" + threadNo);
            Logger.getLogger(MultiRunner.class).info("Initializing log for clone-" + threadNo);
            TargetRunnable tr = new TargetRunnable(cl, r, threadNo, subProps);
            Thread t = new Thread(tr);
            t.start();
        } finally {
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }
    
    public void runThreads() throws Exception {
        String cloneSleepStr = _props.getProperty(PROP_CLONE_SPAWN_SLEEP);
        long cloneSleep = Long.parseLong(cloneSleepStr);
        String numClonesStr = _props.getProperty(PROP_CLONE_NUM);
        int numClones = Integer.parseInt(numClonesStr);
        
        for (int i=0; i< numClones; i++) {
            startThread(i);
            Thread.sleep(cloneSleep);
        }
    }
    
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        File in = new File(args[0]);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(in);
            p.load(fis);
        } finally {
            fis.close(); 
        }

        BasicConfigurator.configure();
        PropertyConfigurator.configure(args[0]);
        
        Logger.getLogger(MultiRunner.class).info("MultiRunner.class");
        MultiRunner m = new MultiRunner(p);
        m.runThreads();
    }
}
