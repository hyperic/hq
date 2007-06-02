package org.hyperic.hq.hqu.rendit

import java.text.SimpleDateFormat
import org.hyperic.hq.hqu.UIPluginDescriptor
import org.hyperic.hq.hqu.rendit.render.RenderFrame
import org.hyperic.hq.hqu.rendit.html.FormGenerator
import org.hyperic.hq.hqu.rendit.html.HtmlUtil

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * The base controller is invoked by the dispatcher when it detects that
 * a controller method is being requested.
 */
abstract class BaseController { 
    Log log = LogFactory.getLog(this.getClass())
    
    String             action          // Current action being executed
    File               pluginDir       // Directory of plugin containing us
    String             controllerName  // Name of the controller
    UIPluginDescriptor pluginInfo      // The results of init.groovy
    String             template        // Default template when rendering
    
    private invokeArgs                 // Info about the request
    private File    viewDir            // Path to plugin/app/views
    private boolean rendered           // Have we already performed a render?
    private         localeBundle = [:] // l10n bundle, must support getAt()

    def getLocaleBundle() {
        localeBundle
    }
    
    void setLocaleBundle(newBundle) {
        this.localeBundle = newBundle
    }
    
    private void setControllerName(String name) {
        this.controllerName = name
    }
    
    protected setInvokeArgs(args) {
        this.invokeArgs = args
    }
    
    def getInvokeArgs() { invokeArgs }
    
    void setPluginDir(File pluginDir) {
        this.pluginDir = pluginDir
        this.viewDir = new File(pluginDir, "views")
    }
    
    File getViewDir() {
        this.viewDir
    }
    
    /**
     * Called by the dispatcher when a controller action is dispatched.
     *
     * If the execution of the action does not explicitly perform any 
     * rendering, the view of the current action will be displayed.
     */
    def dispatchRequest() {
		def params = invokeArgs.request.parameterMap

		def runner = this."$action"
    	if (runner == null)
        	throw new IllegalArgumentException("Unknown action [$action]")
    	
	    def start = System.currentTimeMillis()

	    rendered = false
	    
	    try {
    	    runner(params)
    		if (!rendered) {
    		    render([action : action])
	        }
	    } finally {
    		log.info "Executed $controllerName:$action in " +   
	        	     "${System.currentTimeMillis() - start} ms"
	    }
    }
    
    /**
     * Specifies additional methods available to rendered scripts.  
     * Controllers may override this method if they want to add additional
     * commands (but will likely also want to merge it with the results
     * from this base method)
     */
    def getAddIns() {
		[
		 url_for     : HtmlUtil.&urlFor,
		 h           : HtmlUtil.&escapeHtml,
		 link_to     : HtmlUtil.&linkTo,
		 button_to   : HtmlUtil.&buttonTo,
		 format_date : this.&formatDate,
		]
    }
    
    String formatDate(String fmt, Date d) {
        SimpleDateFormat df = new SimpleDateFormat(fmt, 
                                                   invokeArgs.request.locale)
        df.format(d)
    }
    
	protected void redirectTo(opts) {
	    def response = invokeArgs.response

	    rendered = true
	    def targetUrl = response.encodeRedirectURL(HtmlUtil.urlFor(opts))
		response.sendRedirect(targetUrl)
	}
    
    protected void render(opts) {
        opts = (opts == null) ? [:] : opts

        if (rendered) {
            throw new IllegalArgumentException("Only able to render once " + 
                                               "per controller call")
        }
        rendered = true
        opts['createDefaultOutput'] = {
            def outStream = invokeArgs.response.outputStream
            new OutputStreamWriter(outStream)
        }
        opts['setContentType'] = { contentType ->
            invokeArgs.response.setContentType(contentType)
        }
        def locals    = opts.get('locals', [:])
        def newLocals = new HashMap(addIns)
        newLocals.putAll(locals)
        opts = new HashMap(opts) // Clone
        opts['locals'] = newLocals
        if (!opts['template'])
            opts['template'] = template
        if (!opts['partialDir'])
            opts['partialDir'] = new File(viewDir, controllerName) 
                
        new RenderFrame(opts, this).render()
    }
}
