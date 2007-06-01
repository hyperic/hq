package org.hyperic.hq.ui.rendit

import org.hyperic.hq.ui.rendit.i18n.BundleMapFacade
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
    
	def invoke(pluginInfo, invokeArgs) {
        def req  = invokeArgs.request
        def path = req.requestURI.split('/')[-3..-1]

        log.info "Request: ${path}"
        if (path.size() < 3)
            return false
        
        def controllerName = capitalize(path[1]) + "Controller"

        def pluginDir = invokeArgs.pluginDir
        def appDir    = new File(pluginDir, "app")
        def etcDir    = new File(pluginDir, "etc")
        def contFile  = new File(appDir, controllerName + ".groovy")
        if (!contFile.isFile())
            return false
        
        def loader = this.class.classLoader
        loader.addURL(appDir.toURL())
        loader.addURL(etcDir.toURL())
        def controller = Class.forName(controllerName, true, 
                                       loader).newInstance()

        try {
            def b = ResourceBundle.getBundle("${pluginInfo.name}_i18n", 
                                             req.locale, loader)
            controller.setLocaleBundle(new BundleMapFacade(b))
        } catch(MissingResourceException e) {
            log.warn "Unable to find resource bundle for " + 
                     "${pluginInfo.name}_i18n"
        }

        def action = path[2][0..-5]  // Strip out the .hqu
        controller.setAction(action)
        controller.setControllerName(path[1])
        controller.setPluginDir(invokeArgs.pluginDir)
        controller.setInvokeArgs(invokeArgs)
        controller.setPluginInfo(pluginInfo)
        
        return controller.dispatchRequest()
	}
}
