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

import org.springframework.core.io.Resource;

import org.hyperic.hq.context.Bootstrap;

import java.text.DateFormat
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.product.GenericPlugin
import org.hyperic.util.Runnee

class ConsoleController extends BaseController {
    private final DateFormat df = 
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG)
    
	def ConsoleController() {
        addBeforeFilter({ 
            if (!user.isSuperUser()) {
                render(inline: "Unauthorized")
                return true
            }
            return false
        })
        setJSONMethods(['execute', 'getTemplate'])
	}
	
    private def getTemplateDir() {
		Resource templateResource = Bootstrap.getResource("WEB-INF/gconsoleTemplates");
		if(! templateResource.exists()) {
			return null;
		}
		return templateResource.getFile(); 
	}
	
	private def getTemplates() {
	    def res = []
	    if(templateDir == null) {
	    	return res;
	    }
	    for (f in templateDir.listFiles()) {
	        if (!f.name.endsWith('.groovy'))
	            continue
	           
	        def fname = f.name[0..-8]
	        res << fname
	    }
	    res.sort()
	}
	
    def index(params) {
        render(action:'index', locals:[templates:templates])
    }
    
    def getTemplate(params) {
        def template = params.getOne('template')
        def tmplCode = ""
        
        if (templates.contains(template)) {
            new File(templateDir, "${template}.groovy").withReader("UTF-8") { r ->
				tmplCode = r.text
            }
        }

        [result: tmplCode]
    }
    
    def execute(params) {            
        log.info "Params is ${params}"
        def data = executeCode(params.getOne('code'))
        
        // generate a new CSRF token for subsequent requests if needed
        def opts = [action:"execute", encodeUrl:true]
        data["actionToken"] = urlFor(opts)
        
        return data
    }
    
    private Map executeCode(code) {
        log.info "Requested to execute code\n${code}\n"
		File tmp = File.createTempFile('gcon', null)
        log.info "Writing tmp file: ${tmp.absolutePath}"
		tmp.withWriter("UTF-8") { writer -> 
			writer.write(code)
		}
		
		def eng = new GroovyScriptEngine('.', 
		                                 Thread.currentThread().contextClassLoader)
		def res
		long start = now()
		try {
			def script
			if (GenericPlugin.isWin32()) {
				//'file:/' spec required for windows
				script = 'file:/' + tmp.absolutePath
			} else {
				script = tmp.absolutePath
			}
			
			def runnee = [run: {res = eng.run(script, new Binding())}] as Runnee
			
			runnee.run()


			log.info "Result: [${res}]"
		} catch(Throwable e) {
		    log.info "Exception thrown", e
		    def sw = new StringWriter()
		    def pw = new PrintWriter(sw)
		    e.printStackTrace(pw)
		    pw.flush()
		    res = sw.toString()
		}
		
		long end = now()
        [result: "${res}".toHtml(),
         timeStatus: "Executed in ${end - start} ms"]
    }
}
