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
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.security.KeystoreConfig;

/**
 * This class will get the keystore property in agent's default property file (usually it's agent.properties)
 * and create a keystoreConfig for SSL communication (Should only be used for agent side code).
 */
public class AgentKeystoreConfig extends KeystoreConfig {
    private static final String DEFAULT_SSL_KEYSTORE_ALIAS = AgentConfig.DEFAULT_SSL_KEYSTORE_ALIAS;
    private static final String SSL_KEYSTORE_ALIAS = AgentConfig.SSL_KEYSTORE_ALIAS;
    private Log log = LogFactory.getLog(AgentKeystoreConfig.class);
    private boolean acceptUnverifiedCert;
    public AgentKeystoreConfig(){
        AgentConfig cfg;
        final String propFile = System.getProperty(AgentConfig.PROP_PROPFILE,AgentConfig.DEFAULT_PROPFILE);
        try {
            cfg = AgentConfig.newInstance(propFile);
        } catch(IOException exc){
            log.error("Error: " + exc, exc);
            return ;
        } catch(AgentConfigException exc){
            log.error("Agent Properties error: " + exc.getMessage(), exc);
            return ;
        }
        super.setFilePath(cfg.getBootProperties().getProperty(AgentConfig.SSL_KEYSTORE_PATH));
        super.setFilePassword(cfg.getBootProperties().getProperty(AgentConfig.SSL_KEYSTORE_PASSWORD));
        super.setAlias(cfg.getBootProperties().getProperty(SSL_KEYSTORE_ALIAS, DEFAULT_SSL_KEYSTORE_ALIAS));
        super.setHqDefault(AgentConfig.PROP_KEYSTORE_PATH[1].equals(getFilePath()));
        String prop = cfg.getBootProperties().getProperty(AgentConfig.SSL_KEYSTORE_ACCEPT_UNVERIFIED_CERT);
        this.acceptUnverifiedCert = Boolean.parseBoolean(prop);
        String address = "";
        try {
            address = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            log.error(e,e);
        }
        super.setKeyCN("Hyperic Agent_"+address);
    }
    public boolean isAcceptUnverifiedCert() {
        return acceptUnverifiedCert;
    }
}
