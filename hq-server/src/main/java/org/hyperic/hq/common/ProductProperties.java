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

package org.hyperic.hq.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.context.Bootstrap;




public class ProductProperties {
    
    private static final Log  _log = 
        LogFactory.getLog(ProductProperties.class);

    public static Object getPropertyInstance(String key) {
        String beanName = org.hyperic.hq.common.shared.ProductProperties.getProperty(key);
        if (beanName != null) {
            try {
                _log.debug("Property " + key + " implemented by " + beanName);
                //TODO get rid of this whole thing and use app context to instantiate and inject
                return Bootstrap.getBean(beanName);
            } catch (Exception e) {
                _log.error("Unable to instantiate bean " + beanName +
                           " for property " + key, e);
            }
        }
        return null;
    }
}
