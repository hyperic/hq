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
     * resource: If set, will call resource.urlFor to tack on the link to
     *           the resource.  Some resource urls have additional context
     *           which point at different pages (for instance, an appdef
     *           resource can have a resourceContext of 'alert' which will
     *           link to that resource's alert tab.  See the documentation for
     *           the resource's urlFor for more information.
     * resourceContext:  If specified, will be passed to the resource's urlFor  
     *                   to provide a finer-grained control of the link.   
     *
     * Any additional values in the map will be passed as query parameters
     */
    static String urlFor(opts) {
        opts = opts + [:]
        def res = ''
        def qparams = [:]
                       
        if (opts.absolute) {
            res = "${opts.absolute}/"
            opts.remove('absolute')
        }

        if (opts.asset) {
            res += "public/${opts['asset']}"
            opts.remove('asset')
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
        
        if (opts['resource']) {
            def resourceContext
            if (opts.containsKey('resourceContext')) {
                resourceContext = opts.resourceContext
                opts.remove('resourceContext')
            }
            res += opts['resource'].urlFor(resourceContext)
            opts.remove('resource')
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
     * opts:  Options for the link (see urlFor).  In addition, the following
     *        options may be used:
     *        
     *        rawLabel: true    - Will not escape the text of the link 
     *                            (useful for putting HTML and images inside)
     */
    static String linkTo(text, opts) {
        def rawLabel  = opts.rawLabel
        if (rawLabel) {
            opts.remove('rawLabel')
        }
        
        def passOpts  = opts + [:]
        def useUrlFor = HtmlUtil.&urlFor
        if (passOpts.urlFor) {
            useUrlFor = passOpts.urlFor
            passOpts.remove('urlFor')
        }
        
        if (rawLabel) {
            return "<a href='${useUrlFor(passOpts)}'>${text}</a>"
        } else {
            return "<a href='${useUrlFor(passOpts)}'>${escapeHtml(text)}</a>"
        }
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
	    def passOpts = opts + [:]
	    def confirmOpt = ""
	    
	    if (passOpts.confirm) {
	        def eMsg = escapeHtml(opts.confirm)
			confirmOpt = "onclick=\"return confirm('${eMsg}')\""
			passOpts.remove('confirm')
	    }
	    
	    def useUrlFor = HtmlUtil.&urlFor
	    if (passOpts.urlFor) {
	        useUrlFor = passOpts.urlFor
	        passOpts.remove('urlFor')
	    }
	    
		"<form method='post' action='${useUrlFor(passOpts)}'>" + 
		"  <div>" + 
		"    <input ${confirmOpt} value='${escapeHtml(text)}' type='submit'/>" + 
		"  </div>" +
		"</form>"
	}

	/**
	 * Create a <select> list with <options>.
	 * 
	 * @param vals  A list of objects to fill out the select box.  For
	 *              each element, e.getCode() will be called to fill in the
	 *              value for each <option>, and e.getValue() will be called
	 *              to fill out the text for the option.
	 *
	 * @param htmlOpts A map, specifying key=value pairs to place as options
	 *                 on the returned <select> tag
	 */
	static String selectList(vals, htmlOpts) {
        def res = "<select "
        
        for (i in htmlOpts) {
            res += "${i.key}='${i.value}' "
        }
        res += ">\n"
        
        for (i in vals) {
            res += "<option value='${i.code}'>${escapeHtml(i.value)}</option>\n"    
        }
        
        res += "</select>\n"
        res
    }

    /**
	 * Create a string of key='value' pairs, usable for tag parameters
	 * in HTML
	 *
	 * @param opts A map of key/val pairs
	 */
	static htmlOptions(opts) {
	    def res = ""
	    opts.each {k, v -> res += "${k}='${v}' "}
	    res
	}
}
