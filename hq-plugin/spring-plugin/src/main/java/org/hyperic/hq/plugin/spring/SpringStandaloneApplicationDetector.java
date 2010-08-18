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

package org.hyperic.hq.plugin.spring;

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.jmx.MxServerDetector;
import org.hyperic.hq.product.jmx.MxUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

/**
 * Extension of MxServerDetector that discovers Standalone Spring Applications by looking for
 * -Dspring.managed.application.name. Default behavior is overriden here so that server instances can include the value
 * of "spring.managed.application.name" in the Server name (to uniquely identify 2 different applications running on the
 * same machine)
 * @author Jennifer Hickey
 * 
 */
public class SpringStandaloneApplicationDetector extends MxServerDetector {

	public List getServerResources(ConfigResponse platformConfig) throws PluginException {
		List servers = new ArrayList();
		List procs = getServerProcessList();

		for (int i = 0; i < procs.size(); i++) {
			MxProcess process = (MxProcess) procs.get(i);
			String dir = process.getInstallPath();

			if (!isInstallTypeVersion(dir)) {
				continue;
			}

			ConfigResponse config = new ConfigResponse();
			ConfigSchema schema = getConfigSchema(getTypeInfo().getName(), ProductPlugin.CFGTYPE_IDX_PRODUCT);

			if (schema != null) {
				ConfigOption option = schema.getOption(PROP_PROCESS_QUERY);

				if (option != null) {
					// Configure process.query
					String query = PROC_JAVA + ",Args.*.eq=-D" + getProcHomeProperty() + "=" + dir;
					config.setValue(option.getName(), query);
				}
			}

			if (process.getURL() != null) {
				config.setValue(MxUtil.PROP_JMX_URL, process.getURL());
			} else {
				String[] args = process.getArgs();
				for (int j = 0; j < args.length; j++) {
					if (configureMxURL(config, args[j])) {
						break;
					}
				}
			}

			// Create the server resource
			// <platform name> <application name> Spring Application
			final String fqn = platformConfig.getValue("platform.name") + " " + dir + " Spring Application";
			ServerResource server = new ServerResource();
			server.setType(getTypeInfo().getName());
			server.setInstallPath(dir);
			server.setName(fqn);
			server.setIdentifier(fqn);
			// default anything not auto-configured
			setProductConfig(server, config);
			server.setMeasurementConfig();
			servers.add(server);
		}
		return servers;
	}

}
