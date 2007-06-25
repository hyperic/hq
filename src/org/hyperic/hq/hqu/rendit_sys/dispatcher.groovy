package org.hyperic.hq.hqu.rendit

import org.hyperic.hq.hqu.rendit.InvocationBindings
import org.hyperic.hq.hqu.rendit.PluginLoadException
import org.hyperic.hq.hqu.rendit.metaclass.AuthzSubjectCategory
import org.hyperic.hq.hqu.rendit.metaclass.AlertCategory
import org.hyperic.hq.hqu.rendit.metaclass.MapCategory
import org.hyperic.hq.hqu.rendit.metaclass.StringCategory
import org.hyperic.hq.hqu.UIPluginDescriptor

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
    
    private final CATEGORIES = [AuthzSubjectCategory, AlertCategory,
                                MapCategory, StringCategory]  
    
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
        	case 'invokeMethod':
        	    return invokeMethod()
        	default:
            	throw new IllegalStateException("Unhandled invocation type " +
                                            	"${invokeArgs.type}")
        }
    }
    
    def invokeMethod() {
        def dispatcher = new InvokeMethodDispatcher()
        dispatcher.invoke(invokeArgs)
    }
    
    def loadPlugin() {
        def eng     = new GroovyScriptEngine(invokeArgs.pluginDir.absolutePath)
        def binding = new Binding()
        def pinfo   = new UIPluginDescriptor()
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
		
		use (*CATEGORIES) {
        	return dispatcher.invoke(pluginInfo, invokeArgs)
        }
     }
}

new Dispatcher(invokeArgs).invoke()
