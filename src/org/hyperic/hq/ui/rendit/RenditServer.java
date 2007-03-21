package org.hyperic.hq.ui.rendit;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import groovy.lang.Binding;

public class RenditServer {
    private static final RenditServer INSTANCE = new RenditServer();
    private static final Log _log = LogFactory.getLog(RenditServer.class);
    private final Object CFG_LOCK = new Object();
    private Map  _plugins = new HashMap();
    private File _sysDir;
    
    private RenditServer() {}

    private ClassLoader getUseLoader() {
        return RenditServer.class.getClassLoader();
    }
    
    public File getSysDir() {
        synchronized (CFG_LOCK) {
            return _sysDir;
        }
    }
    
    /**
     * Set the system directory which contains groovy support classes.
     * Since this object is a singleton, we rely on someone to call this
     * before they start adding plugins.
     */
    public void setSysDir(File sysDir) {
        synchronized (CFG_LOCK) { 
            Map oldPlugins = new HashMap(_plugins);
                
            _sysDir = sysDir;
            // Re-create all the plugins with the new system directory
            _plugins.clear();
            for (Iterator i=oldPlugins.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry ent = (Map.Entry)i.next();
                String pluginName = (String)ent.getKey();
                PluginWrapper plugin = (PluginWrapper)ent.getValue();
                PluginWrapper newWrapper;
                
                newWrapper = new PluginWrapper(plugin.getPluginDir(), _sysDir,
                                               getUseLoader());
                
                _plugins.put(pluginName, newWrapper);
            }
        }
    }
    
    public void addPluginDir(String pluginName, File path) {
        _log.info("Adding plugin [" + pluginName + "] as [" + path + "]");
        synchronized (CFG_LOCK) {
            _plugins.put(pluginName, 
                         new PluginWrapper(path, _sysDir, getUseLoader())); 
                                           
        }
    }
    
    public void removePluginDir(String pluginName) {
        _log.info("Removing plugin [" + pluginName + "]");
        synchronized (CFG_LOCK) {
            _plugins.remove(pluginName);
        }
    }

    public void handleRequest(List path, HttpServletRequest req, 
                              HttpServletResponse resp) 
        throws Exception
    {
        String pluginName     = (String)path.get(0);
        PluginWrapper plugin;
        
        synchronized (CFG_LOCK) {
            plugin = (PluginWrapper)_plugins.get(pluginName);
        }
        
        if (plugin == null) {
            throw new IllegalArgumentException("Unknown plugin [" + 
                                               pluginName + "]");
        }
        
        Binding b = new Binding();
        b.setVariable("invokeArgs", 
                      new InvocationBindings(path, plugin.getPluginDir(), 
                                             req, resp));
        plugin.run("org/hyperic/hq/rendit/dispatcher.groovy", b);
    }
    
    public static final RenditServer getInstance() {
        return INSTANCE;
    }
}
