import java.text.DateFormat
import org.hyperic.hq.hqu.rendit.BaseController
import org.hyperic.hq.application.HQApp
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
        boolean debug = params.getOne('debug')?.toBoolean() == true
            
        log.info "Params is ${params}"
        executeCode(params.getOne('code'), debug)
    }
    
    private Map executeCode(code, debug) {
        log.info "Requested to execute code\n${code}\n"
		File tmp = File.createTempFile('gcon', null)
        log.info "Writing tmp file: ${tmp.absolutePath}"
		tmp.withWriter { writer -> 
			writer.write(code)
		}
		
		def eng = new GroovyScriptEngine('.', 
		                                 Thread.currentThread().contextClassLoader)
		def res
		def hiberStats = ''
		try {
			def script
			if (GenericPlugin.isWin32()) {
				//'file:/' spec required for windows
				script = 'file:/' + tmp.absolutePath
			} else {
				script = tmp.absolutePath
			}
			
			def runnee = [run: {res = eng.run(script, new Binding())}] as Runnee
			if (debug) {
			    def logger = new LoggingChainer()
                HQApp.instance.getHibernateLogManager().log(logger, runnee)
                hiberStats = createHtmlFromLog(logger)
			} else {
			    runnee.run()
			}

			log.info "Result: [${res}]"
		} catch(Throwable e) {
		    log.info "Exception thrown", e
		    def sw = new StringWriter()
		    def pw = new PrintWriter(sw)
		    e.printStackTrace(pw)
		    pw.flush()
		    res = sw.toString()
		}
		
        [result: "${res}".toHtml(), 
         hiberStats: hiberStats]
    }
    
    private String createHtmlFromLog(LoggingChainer logger) {
        Map stats = logger.getStats()
        List logs = logger.getLogs()
        
        StringWriter sw = new StringWriter()
        render([action: 'hiberStats', output: sw, 
                locals: [stats:stats, logs:logs, dateFormat: df]])
        return sw.toString()
    }
}
