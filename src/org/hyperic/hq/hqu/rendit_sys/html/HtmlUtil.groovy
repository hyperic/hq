package org.hyperic.hq.hqu.rendit.html

import org.apache.commons.lang.StringEscapeUtils

class HtmlUtil {
    static String escapeHtml(o) {
    	StringEscapeUtils.escapeHtml(o.toString())    
    }
    
    /**
     * Create a URL specified by some options (a map).  Opts can contain:
     *
     * action:   The target action within the current controller to execute
     * id:       Either an integer, or an object which answers to .getId(), 
     *           which will pass an id= query parameter
     * absolute: If set, will re-write the URL with the value as the prefix
     *           of the rest of the URL;  
     *           E.g. absolute:'http://localhost:7080/web'
     *
     * Any additional values in the map will be passed as query parameters
     */
    static String urlFor(opts) {
        opts = new HashMap(opts) 
        def res = ''
        def qparams = [:]
                       
        if (opts.absolute) {
            res = "${opts.absolute}/"
            opts.remove('absolute')
        }
        
        if (opts['action']) {
        	res += opts['action'] + '.hqu'
        	opts.remove('action')
        }
        
        if (opts['id'] != null) {
            def id = opts['id']

            if (!(id instanceof Number))
                id = id.id  // Call 'getId' on the object if it's not a number
            qparams['id'] = "${id}"
            opts.remove('id')
        }
        
        qparams += opts
        def addedParam = false
        for (o in qparams) {
            if (!addedParam)
                res += '?'
            res += "${o.key}=${escapeHtml(o.value)}&"
            addedParam = true
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
