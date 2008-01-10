import org.hyperic.hq.hqu.rendit.html.HtmlUtil
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
        setJSONMethods(['execute', 'getTemplate'])
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
	    res.sort()
	}
	
    def index(params) {
        render(action:'index', locals:[templates:templates])
    }
    
    def getTemplate(params) {
        def template = params.getOne('template')
        def tmplCode = ""
        
        if (templates.contains(template)) {
            new File(templateDir, "${template}.groovy").withReader { r ->
				tmplCode = r.text
            }
        }

        [result: tmplCode]
    }
    
    def execute(params) {
        def code = params.getOne('code')
        [result: HtmlUtil.escapeHtml(executeCode(code))]
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
		} catch(Throwable e) {
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
