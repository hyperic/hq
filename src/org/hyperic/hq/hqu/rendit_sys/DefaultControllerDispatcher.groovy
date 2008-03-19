package org.hyperic.hq.hqu.rendit

import org.hyperic.hq.hqu.rendit.RequestInvocationBindings
import org.apache.catalina.Globals
import org.hyperic.hq.hqu.rendit.i18n.BundleMapFacade
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
	
	def invoke(HQUPlugin p, RequestInvocationBindings invokeArgs) {
        def path = invokeArgs.requestURI.split('/')[-3..-1]

        if (log.isDebugEnabled())
            log.debug "Request: ${path}"
            
        if (path.size() < 3)
            return false
        
        def controllerName = path[1].capitalize() + "Controller"

        def pluginDir = p.pluginDir
        def appDir    = new File(pluginDir, "app")
        def etcDir    = new File(pluginDir, "etc")
        def contFile  = new File(appDir, controllerName + ".groovy")
        if (!contFile.isFile()) {
            log.warn "Unable to find controller: ${controllerName} for " +
                     "path = ${path}"
            throw new Exception("Invalid request path")
        }
        
        def loader = this.class.classLoader
        loader.addURL(appDir.toURL())
        loader.addURL(etcDir.toURL())
        def controller = Class.forName(controllerName, true, 
                                       loader).newInstance()

        try {
            def b = ResourceBundle.getBundle("${p.name}_i18n", 
                                             invokeArgs.request.locale, loader)
            controller.setLocaleBundle(new BundleMapFacade(b))
        } catch(MissingResourceException e) {
            log.warn "Unable to find resource bundle for " + 
                     "${pluginInfo.name}_i18n"
        }

        def action = path[2][0..-5]  // Strip out the .hqu
        controller.setAction(action)
        controller.setControllerName(path[1])
        controller.setPluginDir(p.pluginDir)
        controller.setInvokeArgs(invokeArgs)
        controller.setPlugin(p)
        
        return controller.dispatchRequest()
	}
}
