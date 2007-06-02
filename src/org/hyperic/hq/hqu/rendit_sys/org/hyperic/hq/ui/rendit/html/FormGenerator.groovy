package org.hyperic.hq.hqu.rendit.html

/**
 * This class is used to generate form elements.
 *
 * Most tags take the following options:
 *   htmlOpts: a map of key/vals to add to the generated HTML elements
 *
 * Example:
 *   text_area([ name:'myArea', htmlOpts[id:3] ])   
 */
class FormGenerator {
    Map formOpts
    
    FormGenerator(formOpts) {
        this.formOpts = formOpts
    }
    
    void write(output, form_closure) {
    	output.write("<form action='${HtmlUtil.urlFor(formOpts)}' method='post'>")
    	form_closure(this)
    	output.write("</form>")
    	output.flush()
    }
    
    /**
     * Generate a <textarea> element.
     *
     * Arguments:
     *   name: Name of the text area, which will also be the name of the field
     *         in the submitted form parameters
     *   value (optional):  Text to place in the text area
     *   rows  (optional):  # rows
     *   cols  (optional):  # columns
     *
     * Example:
     *   text_area([ name:'myArea', htmlOpts[id:3] ])   
     */
	def text_area(opts) {
	    assert opts
	    def name = opts['name']
	    assert name
	    def value = opts.get('value', '')
	    def rows = opts.get('rows', 20)
	    def cols = opts.get('cols', 80)
		"<textarea name='${name}' cols='${cols}' rows='${rows}'" + 
			gen_html_opts(opts) + ">${HtmlUtil.escapeHtml(value)}</textarea>"
	}
    
    /**
     * Generates a submit button, ala <input type="submit">
     *
     * Arguments:
     *   label:  Label for the button    
     * 
     * Example:
     *   submit_button([label:'Push me!'])
     */
	def submit_button(opts) {
	    assert opts
	    def label = opts['label']
		assert label
		"<input type='submit' value='${label}'/>"
	}
	
    /**
     * Generates an input tag
     *
     * Arguments:
     *   name:  Name used in submitted form param    
     *   size (optional):  Width of the input
     *   type (optional):  <input type= value   
     */
    def input_tag(opts) {
	    assert opts
	    def name = opts['name']
	    assert name
	    def len  = opts.get('size', '10')
	    def type = opts.get('type', 'text')
	    def htmlOpts = opts.get('htmlOpts', [:])
	    "<input type='${type}' name='${name}' size='${len}' " +
	    	gen_html_opts(opts) + "/>"
	}
	
    /**
     * Converts the htmlOpts passed as a map, to the key='val' 
     */
	private gen_html_opts(opts) {
	    def res = ""
	    opts.get('htmlOpts', [:]).each {k, v -> res += "${k}='${v}' "}
	    res
	}
}