import groovy.text.SimpleTemplateEngine


/**
 * This plugin provides basic .gsp template rendering support.  It is
 * primarily used through the RenditServer.renderTemplate() method
 */
class Renderer {
    def render(File gspFile, Map params, Writer output) {
        gspFile.withReader { reader ->
			def eng = new SimpleTemplateEngine()
		    def template = 
		        eng.createTemplate(Thread.currentThread().contextClassLoader,
		                           reader)
			template.make(params).writeTo(output)
		}
		output.flush()
    }
}
