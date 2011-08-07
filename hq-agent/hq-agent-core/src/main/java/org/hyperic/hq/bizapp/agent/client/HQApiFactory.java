/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010-2011], VMware, Inc.
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

package org.hyperic.hq.bizapp.agent.client;

import java.net.URI;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentKeystoreConfig;
import org.hyperic.hq.agent.server.AgentDaemon;
import org.hyperic.hq.bizapp.agent.CommandsAPIInfo;
import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.product.PluginException;

public class HQApiFactory {

	private static final Log _log = LogFactory.getLog(HQApiFactory.class.getName());

	private static final String HQ_USER = "agent.setup.camLogin";
	private static final String HQ_PASS = "agent.setup.camPword";
	private static final String JAVA_SSL_KEYSTORE = "javax.net.ssl.keyStore";
	private static final String JAVA_SSL_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";

	public static HQApi getHQApi(AgentDaemon agent, Properties bootProps)
		throws PluginException {

		try {
			ProviderInfo providerInfo = CommandsAPIInfo.getProvider(agent.getStorageProvider());
			URI uri = new URI(providerInfo.getProviderAddress());
			String user = bootProps.getProperty(HQ_USER, "hqadmin");
			String pass = bootProps.getProperty(HQ_PASS, "hqadmin");

			if (uri.getScheme().equalsIgnoreCase("https")) {
				configureSSLKeystore();
			}
			
			HQApi api = new HQApi(uri, user, pass);

			if (_log.isDebugEnabled()) {
				_log.debug("Using HQApi at " + uri.getScheme() + "://"
							+ uri.getHost() + ":" + uri.getPort());
			}

			return api;

		} catch (Exception e) {
			throw new PluginException("Could not get HQApi connection: "
					+ e.getMessage(), e);
		}
	}
	
	private static void configureSSLKeystore() {
		// TODO: Refactor later so that Java system properties do not need to be set.
		// Ideally, we should be able to pass the DefaultSSLProviderImpl to HQApi.

		AgentKeystoreConfig keystoreConfig = new AgentKeystoreConfig();

		if (!keystoreConfig.isAcceptUnverifiedCert()) {
	    	String keyStorePath = System.getProperty(JAVA_SSL_KEYSTORE);
	    	String keyStorePassword = System.getProperty(JAVA_SSL_KEYSTORE_PASSWORD);
	    	
	    	if (keyStorePath == null || keyStorePassword == null) {
	    		String filePath = keystoreConfig.getFilePath();
	    		System.setProperty(JAVA_SSL_KEYSTORE, filePath);
	    		System.setProperty(JAVA_SSL_KEYSTORE_PASSWORD, keystoreConfig.getFilePassword()); 
	    		
	    		if (_log.isDebugEnabled()) {
	    			_log.debug("Setting Java system property " + JAVA_SSL_KEYSTORE
	    							+ " to " + filePath);
	    		}
	    	}
		}
	}

}
