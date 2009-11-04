package org.hyperic.hq.hqu.rendit.i18n

/**
 * This class wraps a resource bundle and implements methods which make it
 * usable with the subscript operator (getAt) and also as object
 * properties (i.e. f['someKey'] or f.someKey)
 */
class BundleMapFacade {
    private ResourceBundle bundle
    
    BundleMapFacade(bundle) {
        this.bundle = bundle
    }
    
    def getAt(String o) {
        try {
            return bundle.getString(o)
        } catch(MissingResourceException e) {
            return "MISSING i18n KEY: ${o.toString()}"
        }
    }
    
    def getProperty(String propName) {
        getAt(propName)
    }
}
