package org.hyperic.hq.ui.rendit

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

abstract class BaseController { 
    Log     log = LogFactory.getLog(this.getClass())
    String  action
    File    pluginDir
    String  controllerName
    
    private invokeArgs
    private File viewDir
    
    private void setControllerName(String name) {
        this.controllerName = name
    }
    
    protected setInvokeArgs(args) {
        this.invokeArgs = args
    }
    
    def getInvokeArgs() { invokeArgs }
    
    void setPluginDir(File pluginDir) {
        this.pluginDir = pluginDir
        viewDir = new File(pluginDir, "views")
    }
}
