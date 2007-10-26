import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.application.HQApp
import org.hyperic.hq.product.GenericPlugin

class ConsoleController extends BaseController { 
	def ConsoleController() {
        setTemplate('standard')
        addBeforeFilter({ 
            if (!user.isSuperUser()) {
                render(inline: "Unauthorized")
                return true
            }
            return false
        })
	}
	
    private def getTemplateDir() {
		new File(HQApp.instance.resourceDir, "gconsoleTemplates")    
	}
	
	private def getTemplates() {
	    def res = []
	    for (f in templateDir.listFiles()) {
	        if (!f.name.endsWith('.groovy'))
	            continue
	           
	        def fname = f.name[0..-8]
	        res << fname
	    }
	    res
	}
	
    def index(params) {
    	def r = [:]
    
    	if (params.hasOne('code_input')) {
		    r['last_code']   = params.getOne('code_input')
			r['last_result'] = executeCode(r['last_code']) 
		} else {
			r['last_code']   = '1 + 2'
			r['last_result'] = '3'
		}
    	
    	render(action:'index', locals:[r:r, templates:templates])
    }
    
    def chooseTemplate(params) {
        def template = params.getOne('template')
        def tmplCode = ""
        
        if (templates.contains(template)) {
            new File(templateDir, "${template}.groovy").withReader { r ->
				tmplCode = r.text
            }
        }
            
        index(['code_input' : [tmplCode]])
    }
    
    private def executeCode(code) {
        log.info "Requested to execute code\n${code}\n"
		File tmp = File.createTempFile('gcon', null)
        log.info "Writing tmp file: ${tmp.absolutePath}"
		tmp.withWriter { writer -> 
			writer.write(code)
		}
		
		def eng = new GroovyScriptEngine('.', 
		                                 Thread.currentThread().contextClassLoader)
		def res
		try {
			def script
			if (GenericPlugin.isWin32()) {
				//'file:/' spec required for windows
				script = 'file:/' + tmp.absolutePath
			}
			else {
				script = tmp.absolutePath
			}
			res = eng.run(script, new Binding())
			log.info "Result: [${res}]"
		} catch(Exception e) {
		    log.info "Exception thrown", e
		    def sw = new StringWriter()
		    def pw = new PrintWriter(sw)
		    e.printStackTrace(pw)
		    pw.flush()
		    res = sw.toString()
		}
		res
    }
}
