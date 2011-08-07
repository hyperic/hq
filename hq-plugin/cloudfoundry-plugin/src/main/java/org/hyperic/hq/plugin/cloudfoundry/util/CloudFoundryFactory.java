/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], VMware, Inc.
 * This file is part of Hyperic.
 * 
 * Hyperic is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.plugin.cloudfoundry.util;

import java.net.MalformedURLException;
import java.util.Properties;

import org.hyperic.hq.product.PluginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.client.lib.CloudFoundryClient;

public class CloudFoundryFactory {

    private static final Log _log = LogFactory.getLog(CloudFoundryFactory.class);
    
    private static final String EMAIL = "email";
    private static final String PASSWORD = "password";
    private static final String URI = "uri";
    
    public static CloudFoundryClient getCloudFoundryClient(Properties config) 
    	throws PluginException {
    	
        String email = config.getProperty(EMAIL);
        String pwd = config.getProperty(PASSWORD);
        String cloudControllerUrl = config.getProperty(URI);
        CloudFoundryClient cf = null;
        
        if (email.length() > 0 
        		&& pwd.length() > 0
        		&& cloudControllerUrl.length() > 0) {
        	try {
        		cf = new CloudFoundryClient(email, pwd, cloudControllerUrl);
        	} catch (MalformedURLException e) {
                _log.info(e.getMessage());
                throw new PluginException(e);
            }
        } else {
            throw new PluginException("Missing Cloud Foundry account information");
        }
        
        return cf;
    }
}
