package org.hyperic.hq.rendit

import java.io.OutputStreamWriter

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import org.hyperic.hq.authz.server.session.AuthzSubject
import org.hyperic.hq.ui.util.ContextUtils
import org.hyperic.hq.ui.util.RequestUtils

import groovy.text.SimpleTemplateEngine
import java.io.File

public abstract class BaseController 
	extends Expando
{
    Log     log = LogFactory.getLog(this.getClass())
    String  action
    File    pluginDir
    def     invokeArgs
    private AuthzSubject user
    
    private void setAction(String action) { 
        this.action = action
    }
    
    def setInvokeArgs(def args) {
        this.invokeArgs = args
    }
    
    def setPluginDir(File pluginDir) {
        this.pluginDir = pluginDir
    }

    /**
     * Retreives the currently logged-in user
     */
    protected AuthzSubject getUser() {
        if (this.user != null)
            return this.user
        
		def sessId = RequestUtils.getSessionId(invokeArgs.request)
		def ctx    = invokeArgs.request.session.servletContext

		this.user = ContextUtils.getAuthzBoss(ctx).getCurrentSubject(sessId)
    }

    /**
     * Render a .gsp.
     *
     * This method takes a map of arguments.  Valid arguments include:
     *    file:  The file to render.  If not specified, the name of the
     *           current action will be used
     *    args:  A map of key/value pairs to send to the .gsp to use when
     *           rendering
     */
    protected def render(args) {
        args = (args == null) ? [:] : args
		def gspArgs = args.remove("args")
		def gspFile = args.remove("file")
		def useAction
		
		if (gspFile == null)
		    useAction = action
		else
			useAction = gspFile

		def targFile = new File(pluginDir, useAction + ".gsp")
		targFile.withReader { reader ->
			def eng       = new SimpleTemplateEngine(false)
		    def template  = eng.createTemplate(reader)
		    def outStream = invokeArgs.response.outputStream
		    def outWriter = new OutputStreamWriter(outStream) 
		    template.make(gspArgs).writeTo(outWriter)
		}
	}
}
