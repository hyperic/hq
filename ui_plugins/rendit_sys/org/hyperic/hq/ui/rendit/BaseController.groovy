package org.hyperic.hq.ui.rendit

import org.hyperic.hq.ui.rendit.html.FormGenerator
import org.hyperic.hq.ui.rendit.html.HtmlUtil
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hyperic.hq.ui.rendit.PluginLoadInfo
import org.hyperic.hq.ui.rendit.helpers.RenderFrame

/**
 * The base controller is invoked by the dispatcher when it detects that
 * a controller method is being requested.
 */
abstract class BaseController { 
    Log log = LogFactory.getLog(this.getClass())
    
    String             action          // Current action being executed
    File               pluginDir       // Directory of plugin containing us
    String             controllerName  // Name of the controller
    PluginLoadInfo     pluginInfo
    
    private invokeArgs           // Info about the request
    private File    viewDir      // Path to plugin/app/views
    private boolean rendered     // Have we already performed a rendering op?
    private String  templ        // Template to use (or null)
    private List    renderStack = []  // Stack of RenderFrames
    private boolean contentTypeSet = false

    protected RenderFrame getRenderFrame() {
        renderStack.empty ? null : renderStack[-1]
    }

    private void pushRenderFrame(RenderFrame frame) {
        renderStack << frame
    }
    
    private RenderFrame popRenderFrame() {
        renderStack.pop()
    }
    
    private void setControllerName(String name) {
        this.controllerName = name
    }
    
    protected setInvokeArgs(args) {
        this.invokeArgs = args
    }
    
    protected void setTemplate(String templ) {
        this.templ = templ
    }
    
    public String getTemplate() {
        this.templ
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
		[form_for : this.&form_for,
		 url_for  : HtmlUtil.&url_for,
		 h        : HtmlUtil.&escapeHtml]
    }
    
    protected void render(opts) {
        rendered = true
        opts = (opts == null) ? [:] : opts

        def frame
        if (!renderFrame)  {
            // Setup the base frame
            // TODO:  We may want to move getting the outputStream into the
            //        actual renderframe, since we don't know exactly if it
            //        needs to set the content type, etc.
            if (!opts.output) {
                def outStream = invokeArgs.response.outputStream
                def outWriter = new OutputStreamWriter(outStream)
                opts.output = outWriter
            }
            
            frame = new RenderFrame(opts, this)
        } else {
            frame = renderFrame.createNewFrame(opts)
        }
        
        try {
            pushRenderFrame(frame)
            rendered = true
            frame.render()
        } finally {
            popRenderFrame()
        }
    }
    
    private def form_for(opts, form_closure) {
        assert renderFrame
        def form = new FormGenerator(formOpts:opts)
        form.write(renderFrame.output, form_closure)
    }
}
