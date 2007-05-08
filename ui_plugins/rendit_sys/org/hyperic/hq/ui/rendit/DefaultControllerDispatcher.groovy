package org.hyperic.hq.ui.rendit

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Called by the dispatcher to execute controller methods.  
 *
 * This class is used to perform a dispatch if the plugin itself does not
 * define its own controller dispatcher.
 */
class DefaultControllerDispatcher {
	Log log = LogFactory.getLog(DefaultControllerDispatcher.class)
	
    private String capitalize(String s) {
        if (s.length() == 0) 
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    
	def invoke(invokeArgs) {
        def req  = invokeArgs.request
        def path = req.requestURI.split('/')[-3..-1]

        log.info "Path is ${path}"
        if (path.size() )
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

        def action = path[2][0..-5]  // Strip out the .hqu
        controller.setAction(action)
        controller.setControllerName(path[1])
        controller.setPluginDir(invokeArgs.pluginDir)
        controller.setInvokeArgs(invokeArgs)
        
        return controller.dispatchRequest()
	}
}
