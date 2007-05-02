package org.hyperic.hq.ui.rendit

import org.hyperic.hq.ui.rendit.util.UserUtil
import org.hyperic.hq.ui.rendit.metaclass.AppdefAppdefCategory
import org.hyperic.hq.ui.rendit.metaclass.AppdefLiveDataCategory
import org.hyperic.hq.ui.rendit.metaclass.AppdefMetricCategory
import org.hyperic.hq.ui.rendit.metaclass.CategoryInfo
import org.hyperic.hq.ui.rendit.metaclass.MetricMetricCategory
import org.hyperic.hq.ui.rendit.PluginLoadException
import org.hyperic.hq.ui.rendit.PluginLoadInfo
import org.hyperic.hq.appdef.shared.AppdefEntityID

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * The Dispatcher is the direct invocation target called from the HQ
 * RenditServer.  It has the responsibility of locating the controllers,
 * setting up the environment, and invoking the request.  
 */
class Dispatcher {
    final int API_MAJOR = 0
    final int API_MINOR = 1
    
    private static final CATEGORIES = [
        AppdefAppdefCategory,                               
        AppdefLiveDataCategory,
        AppdefMetricCategory,
        MetricMetricCategory,
    ]
    private Log log = LogFactory.getLog(Dispatcher.class);

    private List   path
    private def    invokeArgs
	
    private String capitalize(String s) {
        if (s.length() == 0) 
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    
    def Dispatcher(invokeArgs) {
        this.invokeArgs = invokeArgs
    }
    
    def invoke() {
        switch (invokeArgs.type) {
        case 'request':
            return invokeController()
        case 'load':
            return loadPlugin()
        }
    }
    
    def loadPlugin() {
        def eng = new GroovyScriptEngine(invokeArgs.pluginDir.absolutePath)
        def binding = new Binding()
        def plugin  = new PluginLoadInfo()
        binding.setVariable("plugin", plugin)
        eng.run('init.groovy', binding)
        
        if (plugin.apiMajor != API_MAJOR) {
            throw new PluginLoadException("Plugin API version " +  
                             "${plugin.apiMajor}.${plugin.apiMinor} is " + 
                             "incompatable with HQU API version ${API_MAJOR}" +
                             ".${API_MINOR}")
        }
		return plugin
    }
    
    /**
     * Attempt to invoke a controller.  If the path looks like:
     *   plugin/controller/action
     * then attempt to locate the controller and associated action.
     *
     * Returns false if the controller wasn't found or the path was 
     * incorrect (i.e. not likely a controller request, maybe a .css or .html
     * request)                  
     */
    def invokeController() {
        def req      = invokeArgs.request
        def servPath = req.servletPath
        def reqUri   = req.requestURI
        def path     = reqUri[(servPath.length() + 1)..-1].split('/')

        if (path.size() < 3)
            return false
        
        def controllerName = capitalize(path[1]) + "Controller"

        def pluginDir = invokeArgs.pluginDir
        def appDir    = new File(pluginDir, "app")
        def contFile  = new File(appDir, controllerName + ".groovy")
        if (!contFile.isFile())
            return false
        
        def loader    = this.class.classLoader
        loader.addURL(appDir.toURL())
        def controller = Class.forName(controllerName, true, 
                                       loader).newInstance() 

        def action = path[2]
        controller.setAction(action)
        controller.setControllerName(path[1])
        controller.setPluginDir(invokeArgs.pluginDir)
        controller.setInvokeArgs(invokeArgs)
        
        def runner = controller."$action"
        if (runner == null)
            throw new IllegalArgumentException("Unknown action [$action]")
        	
        def start = System.currentTimeMillis()

        try {
            CategoryInfo.setUser(UserUtil.getUser(invokeArgs))
            use (*CATEGORIES) {
                runner(invokeArgs.request.parameterMap)
            }
        } finally {
            CategoryInfo.setUser(null)
        }
		
        log.info "Executed $controllerName:$action in " +   
                 "${System.currentTimeMillis() - start} ms"
        return true
    }
}

new Dispatcher(invokeArgs).invoke()
