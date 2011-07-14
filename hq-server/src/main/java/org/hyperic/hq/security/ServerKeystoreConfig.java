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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.bizapp.shared.ConfigBoss;
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
    public ServerKeystoreConfig(ConfigBoss configBoss,
                                @Value("#{securityProperties['server.keystore.path']}") String keystore,
                                @Value("#{securityProperties['server.keystore.password']}") String keypass) throws ConfigPropertyException{
        super();
        if(StringUtils.hasText(keystore)){
            super.setAlias("hq");
            super.setFilePath(keystore);
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
