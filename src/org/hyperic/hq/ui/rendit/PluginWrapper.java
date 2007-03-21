package org.hyperic.hq.ui.rendit;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Basically a wrapper around the classloader and associated groovy 
 * artifacts.
 */
public class PluginWrapper {
    private final File               _pluginDir;
    private final GroovyScriptEngine _engine;

    PluginWrapper(File pluginDir, File sysDir, ClassLoader parentLoader) {
        URLClassLoader urlLoader = new URLClassLoader(new URL[0], 
                                                      parentLoader);
        URL[] u;

        _pluginDir = pluginDir;
        
        try {
            u = new URL[] {
                sysDir.toURL(),
                _pluginDir.toURL(),
            };
        } catch(MalformedURLException e) {
            throw new RuntimeException(e);
        }
        _engine = new GroovyScriptEngine(u, urlLoader);
    }
    
    File getPluginDir() {
        return _pluginDir;
    }
    
    Object run(String script, Binding b) throws Exception {
        Thread curThread = Thread.currentThread();
        ClassLoader oldLoader = curThread.getContextClassLoader();

        try {
            curThread.setContextClassLoader(_engine.getParentClassLoader());
            return _engine.run(script, b);
        } finally {
            curThread.setContextClassLoader(oldLoader);
        }
    }
    
    public static boolean isValidPlugin(File f) {
        return f.getName().startsWith("hqu_");
    }
}
