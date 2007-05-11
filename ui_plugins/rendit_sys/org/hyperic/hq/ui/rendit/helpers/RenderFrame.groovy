package org.hyperic.hq.ui.rendit.helpers

import groovy.text.SimpleTemplateEngine


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
class RenderFrame {
    private Map            opts
    private controller
    
    RenderFrame(opts, controller) {
        this.opts       = opts
        this.controller = controller
    }
    
    Writer getOutput() {
        opts.output
    }
    
    /**
     * Create new rendering frame in a stack, merging in locals, output, etc. 
     * defined by this renderframe.
     */
    RenderFrame createNewFrame(opts) {
        new RenderFrame(this.opts + opts, controller)
    }
    
    def render() {
        def ignoreDefTempl = opts.get('ignoreDefaultTemplate', false)
        def templ          = opts.template
        def actionArg      = opts.get('action', controller.action)
        def contArg        = opts.get('controller', controller.controllerName)
		def locals         = opts.get('locals', [:])
        def subViewDir     = new File(controller.viewDir, contArg)

        if (templ == null && !ignoreDefTempl) {
		    templ = controller.template
		    ignoreDefTempl = true
        }

/*
        if (!contentTypeSet) {
            invokeArgs.response.setContentType(args.get('type', 'text/html'))
            contentTypeSet = true
        }
*/
        
        // Merge previously specified locals with new ones
        def useLocals = new HashMap(controller.addIns)
        useLocals.putAll(locals)
        
        try {
            def gspFile
            /*
            if (partial) {
                // If we're in a template context, use that dir
                if (templ != null) {
                    gspFile = new File(viewDir, 'templates')
                    gspFile = new File(gspFile, "_${partial}.gsp")
                } else {
                    gspFile = new File(subViewDir, "_${partial}.gsp")
                }
            } else if (args['inline']) {
    		    return render_inline(args['inline'], output)
    		} else 
    		if (templ == null) {
    		    */ 
                gspFile = new File(subViewDir, "${actionArg}.gsp")
    		    /*
            } else {
                // When rendering a template, we initialize the 'body' part
                // with the original body
                gspFile = new File(controller.viewDir, 'templates')
                gspFile = new File(gspFile, templ + '.gsp')
                    
                // Recurse, and render the body to insert into the template
                def body = new StringWriter()
                //log.info "Locals are ${renderLocals}"
                
                render([action : actionArg, output : body, 
                        ignoreDefaultTemplate : ignoreDefTempl, 
                        locals : renderLocals])
                
                // And finally, provide it as a local to the template which
                // we will be rendering
                renderLocals['template'] = ['body' : body.toString() ]
            }
*/
            gspFile.withReader { reader ->
				def eng = new SimpleTemplateEngine(controller.pluginInfo.dumpScripts)
				def template = eng.createTemplate(reader)
				template.make(useLocals).writeTo(output)
				output.flush()
			}
        } catch(Exception e) {
            def pw = new PrintWriter(output)
            e.printStackTrace(pw)
			pw.flush()
            output.flush()
            throw e
        }
    }
    
    private def render_inline(text, writer) {
        writer.write(text, 0, text.length())
        writer.flush()
    }
}
