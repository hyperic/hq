package org.hyperic.hq.ui.rendit

import org.hyperic.hq.ui.rendit.InvocationBindings
import org.hyperic.hq.ui.rendit.PluginLoadException
import org.hyperic.hq.ui.rendit.PluginLoadInfo
import org.hyperic.hq.ui.rendit.metaclass.MapCategory

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * The Dispatcher is the direct invocation target called from the HQ
 * RenditServer.  
 *
 * It uses the InvocationBindings to determine what type of action to take
 * (load a plugin, request a page, etc.)
 */
class Dispatcher {
    final int API_MAJOR = 0
    final int API_MINOR = 1
    
    private Log log = LogFactory.getLog(Dispatcher.class);

    private InvocationBindings invokeArgs
	
    def Dispatcher(invokeArgs) {
        this.invokeArgs = invokeArgs
    }
	
    def invoke() {
        switch (invokeArgs.type) {
        case 'request':
            return invokeRequest()
        case 'load':
            return loadPlugin()
        default:
            throw new IllegalStateException("Unhandled invocation type " +
                                            "${invokeArgs.type}")
        }
    }
    
    def loadPlugin() {
        def eng     = new GroovyScriptEngine(invokeArgs.pluginDir.absolutePath)
        def binding = new Binding()
        def pinfo   = new PluginLoadInfo()
        binding.setVariable("plugin", pinfo)
        eng.run('init.groovy', binding)
        
        if (pinfo.apiMajor != API_MAJOR) {
            throw new PluginLoadException("Plugin API version " +  
                             "${pinfo.apiMajor}.${pinfo.apiMinor} is " + 
                             "incompatable with HQU API version ${API_MAJOR}" +
                             ".${API_MINOR}")
        }
		return pinfo
    }
    
    /**
     * Attempt to invoke a controller.  If the path looks like:
     *   plugin/controller/action
     * then attempt to locate the controller and associated action.
     */
    def invokeRequest() {
        def dispatcher = new DefaultControllerDispatcher()
        def pluginInfo = loadPlugin()
		
        use (MapCategory){
        	return dispatcher.invoke(pluginInfo, invokeArgs)
        }
     }
}

new Dispatcher(invokeArgs).invoke()
