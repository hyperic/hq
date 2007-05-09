package org.hyperic.hq.ui.rendit

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import groovy.text.SimpleTemplateEngine

abstract class BaseController { 
    Log log = LogFactory.getLog(this.getClass())
    
    String             action
    File               pluginDir
    String             controllerName
    OutputStreamWriter output
    
    private invokeArgs
    private File viewDir
    private boolean rendered
    
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
    
    /**
     * Called by the dispatcher when an controller action is dispatched.
     *
     * If the execution of the action does not explicitly perform any 
     * rendering, the view of the current action will be displayed.
     */
    def dispatchRequest() {
    	def runner = this."$action"
    	if (runner == null)
        	throw new IllegalArgumentException("Unknown action [$action]")
    	
	    def start = System.currentTimeMillis()

	    rendered = false
	    
	    try {
    		runner(invokeArgs.request.parameterMap)
	
    		if (!rendered)
    		    render([action : action])
	    } finally {
    		log.info "Executed $controllerName:$action in " +   
	        	     "${System.currentTimeMillis() - start} ms"
	    }
    }
    
    def url_for(opts) {
        println opts
        def res = ''
        if (opts['action']) {
        	res += opts['action'] + '.hqu'    
        }
        res
    }
    
    def form_for(opts, form_closure) {
        //<textarea name='MY_THING', rows='20', cols='80'></textarea>
        
        def form = new Expando()
        form.text_area = { '<textarea>' }
        output.write("<form action='${url_for(opts)}' method='post'>")
        form_closure(form)
        output.write("</form>")
        output.flush()
    }
    
    /**
     * Render output to the browser.  This method takes a map of named 
     * arguments:
     *   
     *   action:  Specifies the action to render.  Defaults to the action,
     *            currently being executed
     *   args:    Provides a map of variables to make accessable to the
     *            .gsp which is being rendered
     *   controller:  Specifies a controller (other than the current one) to
     *                render the view for
     *   type:    Allows the caller to explicitly specify the content type.
     *            Defaults to text/html
     *
     * Examples:
     *   
     * render([action : 'list'])
     *   - Renders the 'list' view for the current controller
     *
     * render([args : [foo:3] ])
     *   - Renders the currently executed action, providing 3 as a value for
     *     the variable 'foo', which is available to the .gsp
     *
     * render([inline : 'Hello world'])
     *   - Writes the text to the browser
     */
    def render(args) {
		def ADD_INS = [form_for : this.&form_for,
		               url_for  : this.&url_for]
		              	
		    
        args = (args == null) ? [:] : args
        rendered = true
		def outStream = invokeArgs.response.outputStream
		def outWriter = new OutputStreamWriter(outStream)
		                
        invokeArgs.response.contentType = args.get('type', 'text/html')
		if (args['inline']) {
		    return render_inline(args['inline'], outWriter)
		} 

        def actionArg  = args.get('action', action)
        def contArg    = args.get('controller', controllerName)
		def gspArgs    = args.get('args', [:])
        def subViewDir = new File(viewDir, contArg)

        gspArgs.putAll(ADD_INS)
        log.info "gspArgs = [${gspArgs}]"
        try {
        	new File(subViewDir, actionArg + '.gsp').withReader { reader ->
	        	output = outWriter
				def eng = new SimpleTemplateEngine(true)
				def template = eng.createTemplate(reader)
				template.make(gspArgs).writeTo(outWriter)
				outWriter.flush()
			}
        } finally {
            output = null
        }
    }
    
    private def render_inline(text, writer) {
        writer.write(text, 0, text.length())
        writer.flush()
    }
}
