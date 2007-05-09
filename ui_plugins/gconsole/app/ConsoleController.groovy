import org.hyperic.hq.ui.rendit.BaseController

class ConsoleController extends BaseController { 
    def index = { params ->
    	def r = [:]
    
    	if (params['code_input'] && params['code_input'].size() == 1) {
		    r['last_code']   = params['code_input'][0]
			r['last_result'] = executeCode(r['last_code']) 
		} else {
			r['last_code']   = 'nada'
			r['last_result'] = 'nada'
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
