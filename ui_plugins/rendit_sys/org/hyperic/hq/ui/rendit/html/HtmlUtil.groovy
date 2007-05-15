package org.hyperic.hq.ui.rendit.html

import org.apache.commons.lang.StringEscapeUtils

class HtmlUtil {
    static String escapeHtml(o) {
    	StringEscapeUtils.escapeHtml(o.toString())    
    }
    
    static String url_for(opts) {
        println "Ops are ${opts}"
        def res = ''
        if (opts['action']) {
        	res += opts['action'] + '.hqu'    
        }
        res
    }
}
