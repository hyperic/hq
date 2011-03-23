/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.hqu.rendit.render

import org.codehaus.groovy.runtime.MethodClosure
import java.lang.reflect.Modifier
import org.hyperic.hq.hqu.rendit.html.DojoUtil
import org.hyperic.hq.hqu.rendit.html.HQUWebUtil
import org.hyperic.hq.hqu.rendit.html.HtmlUtil
import org.hyperic.hq.hqu.rendit.html.FormGenerator
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
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
 *   contentType:  Allows the caller to explicitly specify the content type.
 *                 Defaults to text/html
 *   output:  If specified, is a Writer to which the output of the rendering
 *            will be written.  Defaults to the request output stream
 *   template:  If specified, provides a template to render.  The body
 *              of the template will be specified by the rest of the 
 *              arguments (action, etc.).  You can also use setTemplate()
 *              in the constructor of your controller to always have a 
 *              default template
 *   
 * Internal arguments:
 *   setContentType:  A closure that will set the content type
 *   createDefaultOutput:  A closure which will return a Writer that can be
 *                         used to send the response.  
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
    Log log = LogFactory.getLog(RenderFrame)
    private static final Map<String, Map> _templates = [:]
     
    private Map    opts
    private Writer output
    private        controller
    private        parent
      
    RenderFrame(opts, controller) {
        this(opts, controller, null)
    }
    
    RenderFrame(opts, controller, parent) {
        this.opts       = opts
        this.controller = controller
        this.parent     = parent

        if (opts.locals == null)
            opts.locals = [:]
            
        if (opts.locals.PAGE == null) {
            opts.locals.PAGE = [getOutput:{output}]
        }
        def locals = new HashMap(opts['locals'])
        locals.putAll(getImplicitLocals(locals))
        this.opts = new HashMap(opts)
        this.opts['locals'] = locals
    }

    private Map staticMethodsToMap(varMap, clazz) {
        def res = [:]
        for (m in clazz.declaredMethods) {
            if (m.modifiers & Modifier.STATIC && m.name != 'class$') {
                def meth   = new MethodClosure(clazz, m.name)
                def params = m.parameterTypes
                    
                if (params.size() && params[0] == Binding) {
                    meth = meth.curry(new Binding(varMap))
                }
                res[m.name] = meth
            }
        }
        res
    }

    /**
     * Get a map of implicit locals which should be included in all 
     * generated scripts.  This primarily contains static methods in
     * the *Util classes.
     *
     * Static methods imported from *Util classes get special treatment if
     * their first parameter is a Binding.  In such a case, the method will
     * get a binding of the variable map passed into the render method.
     */
    private Map getImplicitLocals(varMap) {
        def res = [formFor : this.&formFor, 
                   l       : controller.localeBundle]

        res += staticMethodsToMap(varMap, DojoUtil)        
        res += staticMethodsToMap(varMap, HtmlUtil)        
        res += staticMethodsToMap(varMap, HQUWebUtil)        
        
        // Override general methods which generate links so that URLs can
        // be re-written
        res.urlFor   = controller.&urlFor
        res.buttonTo = controller.&buttonTo
        res.linkTo   = controller.&linkTo
        res
    }

    private def formFor(formOpts, formClosure) {
        def form = new FormGenerator(formOpts)
        form.write(output, formClosure)
    }
    
    /**
     * Create new rendering frame in a stack, merging in locals, output, etc. 
     * defined by this renderframe.
     */
    RenderFrame createNewFrame(opts) {
        new RenderFrame(this.opts + opts, controller)
    }
    
    def render() {
        def contentType    = opts.get('contentType', 'text/html')
        def ignoreDefTempl = opts.get('ignoreDefaultTemplate', false)
        def partial        = opts.partial
        def createOutput   = opts.createDefaultOutput
            output         = opts.output
        def templ          = opts.template
        def actionArg      = opts.get('action', controller.action)
        def contArg        = opts.get('controller', controller.controllerName)
		def locals         = opts.get('locals', [:])
        def subViewDir     = new File(controller.viewDir, contArg)
        def partialDir     = opts.get('partialDir', subViewDir)

        opts['setContentType'](contentType)
        
        if (!output)
            output = createOutput()

        // Merge locals with parent, if we have one
        if (parent) {
            def parentLocals = new HashMap(parent.opts.get('locals', [:]))
            parentLocals.putAll(locals)
            locals = parentLocals
        }
        locals = new HashMap(locals)
        
        try {
            def gspFile
    	    if (opts.inline != null) {
                output.write(opts.inline, 0, opts.inline.length())
                output.flush()
                return
    	    } else if(partial) { 
                gspFile = new File(partialDir, "_${partial}.gsp")
    	    } else if(templ) {
    	        // When rendering a template, we initialize the 'body' part
                // with the original body
                def templateDir = new File(controller.viewDir, 'templates')
                gspFile = new File(templateDir, "${templ}.gsp")
                    
                // Recurse, and render the body to insert into the template
                def body = new StringWriter()
    	        def passOpts = new HashMap(opts)
    	        passOpts.remove('template')
    	        passOpts['ignoreDefaultTemplate'] = ignoreDefTempl
    	        passOpts['output'] = body
                def frame = new RenderFrame(passOpts, controller, this)
    	        frame.render()
                
                // And finally, provide it as a local to the template which
                // we will be rendering
                locals['template'] = ['body' : body.toString() ]
                partialDir = templateDir
    	    } else {
                gspFile = new File(subViewDir, "${actionArg}.gsp")
    	    }
    	    
            long mtime = gspFile.lastModified()
            def t;
            synchronized(_templates) {
                Map tmplMap = _templates.get(gspFile)
                if (!tmplMap || tmplMap.mtime != mtime) {
                    log.info("Creating template for [${gspFile.absolutePath}]")
                    gspFile.withReader("UTF-8") { reader ->
                        def eng = new SimpleTemplateEngine(controller.dumpScripts)
                        t = eng.createTemplate(reader)
                        _templates[gspFile] = [mtime: mtime, template : t]
                    }
                } else {
                    t = tmplMap.template
                }
            }
            if (t) {
                locals['render'] = { subOpts ->
                    // Setup a new closure for 'render' so that the context
                    // gets copied around.  Also merge parent rendering options
                    // with sub-options
                    def passOpts = new HashMap(opts)
                    passOpts.putAll(subOpts)
                    if (!passOpts['output']) {
                        passOpts['output'] = output
                    }
                    passOpts['partialDir'] = partialDir                        
                    new RenderFrame(passOpts, controller, this).render()
                }
                t.make(locals).writeTo(output)
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
}
