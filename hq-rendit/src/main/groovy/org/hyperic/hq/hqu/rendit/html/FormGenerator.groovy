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