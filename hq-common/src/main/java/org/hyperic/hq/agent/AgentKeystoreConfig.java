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
package org.hyperic.hq.agent;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.security.KeystoreConfig;

/**
 * @author achen
 *
 */
public class AgentKeystoreConfig
    extends KeystoreConfig {
    private Log log;
    public AgentKeystoreConfig(){
        this.log = LogFactory.getLog(AgentKeystoreConfig.class);
        AgentConfig cfg;
        final String propFile = System.getProperty(AgentConfig.PROP_PROPFILE,AgentConfig.DEFAULT_PROPFILE);
        try {
            cfg = AgentConfig.newInstance(propFile);
        } catch(IOException exc){
            System.err.println("Error: " + exc);
            return ;
        } catch(AgentConfigException exc){
            System.err.println("Agent Properties error: " + exc.getMessage());
            return ;
        }
        super.setFilePath(cfg.getBootProperties().getProperty(AgentConfig.SSL_KEYSTORE_PATH));
        super.setFilePassword(cfg.getBootProperties().getProperty(AgentConfig.SSL_KEYSTORE_PASSWORD));
        super.setAlias(cfg.getBootProperties().getProperty(AgentConfig.SSL_KEYSTORE_ALIAS));
        super.setHqDefault(AgentConfig.PROP_KEYSTORE_PATH[1].equals(getFilePath()));
        super.setAcceptUnverifiedCert("true".equals(cfg.getBootProperties().getProperty(AgentConfig.SSL_KEYSTORE_ACCEPT_UNVERIFIED_CERT)));
    }
}
