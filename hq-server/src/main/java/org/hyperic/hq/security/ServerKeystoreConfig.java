/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.security;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.security.KeystoreConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ServerKeystoreConfig
    extends KeystoreConfig {
    private static final Log log = LogFactory.getLog(ServerKeystoreConfig.class);
    
    @Autowired
    public ServerKeystoreConfig(@Value("#{securityProperties['server.keystore.path']}") String keystore,
                                @Value("#{securityProperties['server.keystore.password']}") String keypass) throws ConfigPropertyException{
        super();
        if(StringUtils.hasText(keystore)){
            super.setAlias("hq");
            
            File keystoreFile = new File(keystore);
            
            if (!keystoreFile.isAbsolute()) {
            	// ...assume we're relative to the user.dir...
            	StringBuilder basePath = new StringBuilder(System.getProperty("catalina.base"));

           		if (basePath.length() == 0) {
           			throw new ConfigPropertyException("Cannot determine base path using catalina.base");
           		}
            	
            	// ...append the keystore path value...
            	basePath.append("/").append(keystore);
            	
            	keystoreFile = new File(basePath.toString());
            }

            // ...make sure this exists
        	if (!keystoreFile.exists()) {
        		throw new ConfigPropertyException("The keystore path [" + keystoreFile.getPath() + "] does not exist. If setting a relative path, it must be relative to the server's hq-server directory.");
        	}
            
        	super.setFilePath(keystoreFile.getPath());
            super.setFilePassword(keypass);
            //The server should never generate the keystore. It should already have a keystore running.
            //As a result, it's "custom" setting.
            super.setHqDefault(false);
            super.setKeyCN("Hyperic Server");
        }else{
            throw new ConfigPropertyException("Keystore path (server.keystore.path) is empty");
        }
    }
}
