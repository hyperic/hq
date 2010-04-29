import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory


/**
 * This plugin provides basic .gsp template rendering support.  It is
 * primarily used through the RenditServer.renderTemplate() method
 */
class Renderer {
    // We cache the templates here in a static context, because each
    // creation of a template results in a new class -- and that's
    // bad if classloaders aren't being cleaned up correctly.
    private static final Map<String, Map> _templates = [:]
    private final Log _log = LogFactory.getLog(Renderer.class.name)

    private Template getTemplate(File gspFile) {
        if (!gspFile.isFile()) {
            throw new RuntimeException("File [${gspFile.absolutePath}] not readable or not found");
        }
        
        long mtime = gspFile.lastModified()
        synchronized(_templates) {
            Map tmplMap = _templates.get(gspFile)
            
            if (!tmplMap || tmplMap.mtime != mtime) {
                _log.info("Creating template for [${gspFile.absolutePath}]")
                gspFile.withReader { reader ->
                    def eng = new SimpleTemplateEngine(Thread.currentThread().contextClassLoader)
                    Template t = eng.createTemplate(reader)
                    _templates[gspFile] = [mtime: mtime, template : t]
                    return t
                }
            } else {
                return tmplMap.template
            }
        }
    }

    def render(File gspFile, Map params, Writer output) {
        getTemplate(gspFile).make(params).writeTo(output)
        output.flush()
    }
}

