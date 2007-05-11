import org.hyperic.hq.ui.rendit.BaseController

class ConsoleController extends BaseController { 
	def ConsoleController() {
	    setTemplate('standard')
	}
	
    def index = { params ->
    	def r = [:]
    
    	if (params.hasOne('code_input')) {
		    r['last_code']   = params.getOne('code_input')
			r['last_result'] = executeCode(r['last_code']) 
		} else {
			r['last_code']   = '1 + 2'
			r['last_result'] = '3'
		}
    	
    	render(locals:[r:r])
    }
    
    private def executeCode(code) {
        log.info "Requested to execute code\n${code}\n"
		File tmp = File.createTempFile('gcon', null)
        log.info "Writing tmp file: ${tmp.absolutePath}"
		tmp.withWriter { writer -> 
			writer.write(code)
		}
		
		def eng = new GroovyScriptEngine('.')
		def res
		try {
			res = eng.run(tmp.absolutePath, new Binding())
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
