package org.hyperic.hq.ui.rendit

import org.hyperic.hq.ui.rendit.html.FormGenerator
import org.hyperic.hq.ui.rendit.html.HtmlUtil
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.hyperic.hq.ui.rendit.PluginLoadInfo

import groovy.text.SimpleTemplateEngine

/**
 * The base controller is invoked by the dispatcher when it detects that
 * a controller method is being requested.
 */
abstract class BaseController { 
    Log log = LogFactory.getLog(this.getClass())
    
    String             action          // Current action being executed
    File               pluginDir       // Directory of plugin containing us
    String             controllerName  // Name of the controller
    Writer             output
    PluginLoadInfo     pluginInfo
    
    private invokeArgs           // Info about the request
    private File    viewDir      // Path to plugin/app/views
    private boolean rendered     // Have we already performed a rendering op?
    private String  templ        // Template to use (or null)
    
    private void setControllerName(String name) {
        this.controllerName = name
    }
    
    protected setInvokeArgs(args) {
        this.invokeArgs = args
    }
    
    protected void setTemplate(String templ) {
        this.templ = templ
    }
    
    protected String getTemplate() {
        this.templ
    }

    def getInvokeArgs() { invokeArgs }
    
    void setPluginDir(File pluginDir) {
        this.pluginDir = pluginDir
        viewDir = new File(pluginDir, "views")
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
    protected getAddIns() {
		[form_for : this.&form_for,
		 url_for  : HtmlUtil.&url_for,
		 h        : HtmlUtil.&escapeHtml]
    }
        
    
    private def form_for(opts, form_closure) {
        def form = new FormGenerator(formOpts:opts)
        form.write(output, form_closure)
    }
    
    def use_template(arg) {
        log.info arg
    }
    
    /**
     * Render output to the browser.  This method takes a map of named 
     * arguments:
     *   
     *   action:  Specifies the action to render.  Defaults to the action,
     *            currently being executed
     *   locals:  Provides a map of variables to make accessable to the
     *            .gsp which is being rendered
     *   controller:  Specifies a controller (other than the current one) to
     *                render the view for
     *   type:    Allows the caller to explicitly specify the content type.
     *            Defaults to text/html
     *   output:  If specified, is a Writer to which the output of the rendering
     *            will be written.  Defaults to the request output stream
     *   template:  If specified, provides a template to render.  The body
     *              of the template will be specified by the rest of the 
     *              arguments (action, etc.).  You can also use setTemplate()
     *              in the constructor of your controller to always have a 
     *              default template
     *
     * Examples:
     *   
     * render([action : 'list'])
     *   - Renders the 'list' view for the current controller
     *
     * render([locals: [foo:3] ])
     *   - Renders the currently executed action, providing 3 as a value for
     *     the variable 'foo', which is available to the .gsp
     *
     * render([inline : 'Hello world'])
     *   - Writes the text to the browser
     */
    def render(args) {
        args = (args == null) ? [:] : args
        rendered = true

        def useOut         = args.get('output') 
        def ignoreDefTempl = args.get('ignoreDefaultTemplate', false)
        def templ          = args.get('template')
        def actionArg      = args.get('action', action)
        def contArg        = args.get('controller', controllerName)
		def locals         = args.get('locals', [:])
        def subViewDir     = new File(viewDir, contArg)

        if (useOut == null) {
    		def outStream = invokeArgs.response.outputStream
    		def outWriter = new OutputStreamWriter(outStream)

            useOut = outWriter            
        }
        
		if (args['inline']) {
		    return render_inline(args['inline'], useOut)
		}
		
        if (templ == null && !ignoreDefTempl) {
		    templ = getTemplate()
		    ignoreDefTempl = true
        }

        invokeArgs.response.setContentType(args.get('type', 'text/html'))
		locals.putAll(addIns)
        try {
            def gspFile
            			  
            if (templ == null) {
                gspFile = new File(subViewDir, actionArg + '.gsp')
            } else {
                // When rendering a template, we initialize the 'body' part
                // with the original body
                gspFile = new File(viewDir, 'templates')
                gspFile = new File(gspFile, templ + '.gsp')
                    
                // Recurse, and render the body to insert into the template
                def body = new StringWriter()
                render([action : actionArg, output : body, 
                        ignoreDefaultTemplate : ignoreDefTempl, 
                        locals : locals])
                
                // And finally, provide it as a local to the template which
                // we will be rendering
                locals['template'] = ['body' : body.toString() ]
            }
                          
            gspFile.withReader { reader ->
	        	setOutput(useOut)
				def eng = new SimpleTemplateEngine(pluginInfo.dumpScripts)
				def template = eng.createTemplate(reader)
				template.make(locals).writeTo(useOut)
				useOut.flush()
			}
        } catch(Exception e) {
            if (output) {
            	def pw = new PrintWriter(output)
            	e.printStackTrace(pw)
				pw.flush()
            	output.flush()
            }
            throw e
        } finally {
            output = null
        }
    }
    
    private def render_inline(text, writer) {
        writer.write(text, 0, text.length())
        writer.flush()
    }
}
