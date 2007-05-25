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
    static String url_for(opts) {
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
     * opts:  Options for the link (see url_for)
     */
    static def link_to(text, opts) {
        "<a href='${url_for(opts)}'>${escapeHtml(text)}</a>"
    }
}
