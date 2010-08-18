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

package org.hyperic.hq.hqu.rendit.i18n

import java.text.MessageFormat 

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
