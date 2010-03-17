package org.hyperic.hq.hqu.rendit.i18n

import java.text.MessageFormat;

/**
 * This class wraps a resource bundle and implements methods which make it
 * usable with the subscript operator (getAt) and also as object
 * properties (i.e. f['someKey'] or f.someKey)
 */
class BundleMapFacade {
    private ResourceBundle bundle
    private MessageFormat formatter
	
    BundleMapFacade(bundle) {
        this.bundle = bundle
		this.formatter = new MessageFormat("");
		
		this.formatter.setLocale(bundle.locale);
    }
    
    def getAt(String o) {
        try {
            return this.bundle.getString(o)
        } catch(MissingResourceException e) {
            return "MISSING i18n KEY: ${o.toString()}"
        }
    }
    
    def getProperty(String propName) {
        getAt(propName)
    }
	
	def getFormattedMessage(String key, Object... arguments) {
		try {
			this.formatter.applyPattern(this.bundle.getString(key));
			
		    return this.formatter.format(arguments);
		} catch(MissingResourceException e) {
		    return "MISSING i18n KEY: ${o.toString()}"
		}
	}
}