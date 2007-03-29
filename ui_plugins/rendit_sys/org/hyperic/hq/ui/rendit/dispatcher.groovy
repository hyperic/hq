package org.hyperic.hq.ui.rendit

import org.hyperic.hq.ui.rendit.util.UserUtil
import org.hyperic.hq.ui.rendit.metaclass.AppdefAppdefCategory
import org.hyperic.hq.ui.rendit.metaclass.AppdefLiveDataCategory
import org.hyperic.hq.ui.rendit.metaclass.CategoryInfo
import org.hyperic.hq.appdef.shared.AppdefEntityID

import org.codehaus.groovy.runtime.InvokerHelper
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * The Dispatcher is the direct invocation target called from the HQ
 * RenditServer.  It has the responsibility of locating the controllers,
 * setting up the environment, and invoking the request.  
 */
class Dispatcher {
    private static final CATEGORIES = [
        AppdefAppdefCategory,                               
        AppdefLiveDataCategory,                                       
    ]
    private Log log = LogFactory.getLog(Dispatcher.class);

    private String controllerName
    private String action
    private List   path
    private def    invokeArgs
	
    private String capitalize(String s) {
        if (s.length() == 0) 
            return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
    
    def Dispatcher(invokeArgs) {
        def req      = invokeArgs.request
        def servPath = req.servletPath
        def reqUri   = req.requestURI

        path = reqUri[(servPath.length() + 1)..-1].split('/')
        this.invokeArgs = invokeArgs
    }
    
    def invoke() {
        if (invokeController())
            return
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
        if (path.size() < 3)
            return false
        
        controllerName = capitalize(path[1]) + "Controller"

        def pluginDir = invokeArgs.pluginDir
        def appDir    = new File(pluginDir, "app")
        def contFile  = new File(appDir, controllerName + ".groovy")
        if (!contFile.isFile())
            return false
        
        def loader    = this.class.classLoader
        loader.addURL(appDir.toURL())
        def controller = Class.forName(controllerName, true, 
                                       loader).newInstance() 

        action = path[2]
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
