package org.hyperic.hq.ui.rendit.html

import org.apache.commons.lang.StringEscapeUtils

class HtmlUtil {
    static String escapeHtml(o) {
    	StringEscapeUtils.escapeHtml(o.toString())    
    }
    
    /**
     * Create a URL specified by some options (a map).  Opts can contain:
     *
     * action:  The target action within the current controller to execute
     * id:      Either an integer, or an object which answers to .getId(), which
     *          will pass an id= query parameter
     * 
     * TODO:  Add support for arbitrary URL query params
     */
    static String urlFor(opts) {
        def res = ''
        if (opts['action']) {
        	res += opts['action'] + '.hqu'    
        }
        if (opts['id'] != null) {
            def id = opts['id']

            if (!(id instanceof Number))
                id = id.id  // Call 'getId' on the object if it's not a number
            res += "?id=$id"
        }
        res
    }
     
    /**
     * Create a text link.
     *
     * text:  The text for the link, which will be HTML escaped
     * opts:  Options for the link (see urlFor)
     */
    static String linkTo(text, opts) {
        "<a href='${urlFor(opts)}'>${escapeHtml(text)}</a>"
    }
        
	/**
	 * Display a simple button inside a form.  This is useful for creating
	 * buttons which do destructive things (POSTs to delete objects, for
	 * instance).  
	 *
	 * text:  Text within the button
	 * opts:  Options for the link (see urlFor).  If the opts contains a
	 *        key called 'confirm', clicking the button will also open a 
	 *        confirmation dialog with the value of the confirm.
	 */
	static String buttonTo(text, opts) {
	    def confirmOpt = ""
	    
	    if (opts.confirm) {
	        def eMsg = escapeHtml(opts.confirm)
			confirmOpt = "onclick=\"return confirm('${eMsg}')\""
	    }
	    
		"<form method='post' action='${urlFor(opts)}'>" + 
		"  <div>" + 
		"    <input ${confirmOpt} value='${escapeHtml(text)}' type='submit'/>" + 
		"  </div>" +
		"</form>"
	}
}
