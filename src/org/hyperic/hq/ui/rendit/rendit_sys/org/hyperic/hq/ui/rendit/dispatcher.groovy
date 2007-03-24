package org.hyperic.hq.ui.rendit

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * The Dispatcher is the direct invocation target called from the HQ
 * RenditServer.  It has the responsibility of locating the controllers,
 * setting up the environment, and invoking the request.
 */
class Dispatcher {
    private Log log = LogFactory.getLog(Dispatcher.class);

    private File   pluginDir
    private String controllerName
    private String action
	private def    invokeArgs
	
    private String capitalize(String s) {
        if (s.length() == 0) 
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    
    def Dispatcher(def invokeArgs) {
        def path = invokeArgs.requestPath

        if (path.size() < 3) {
            throw new IllegalArgumentException("Path must have at least 3 " + 
                                               "components");
        }
        
        pluginDir       = invokeArgs.pluginDir
        controllerName  = capitalize(path[1]) + "Controller"
        action          = path[2]
        this.invokeArgs = invokeArgs                              
    }
    
    def invoke() {
		def controller = Class.forName(controllerName, true, 
		                               this.class.classLoader).newInstance() 

		controller.setAction(action)
        controller.setPluginDir(pluginDir)
		controller.setInvokeArgs(invokeArgs) 
        
        def runner = controller."$action"
        if (runner == null) {
        	throw new IllegalArgumentException("Unknown action [$action]")
        }
        	
		def start = System.currentTimeMillis()
		runner(invokeArgs.request.parameterMap)
		log.info "Executed $controllerName:$action in " +   
		         "${System.currentTimeMillis() - start} ms"
    }
}

new Dispatcher(invokeArgs).invoke()
