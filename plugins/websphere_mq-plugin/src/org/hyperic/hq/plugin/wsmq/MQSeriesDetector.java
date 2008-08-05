/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.plugin.wsmq;

import java.io.File;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.FileServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RegistryServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;

import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.util.config.ConfigResponse;

import com.ibm.mq.constants.MQConstants;

public class MQSeriesDetector extends ServerDetector implements FileServerDetector, RegistryServerDetector, AutoServerDetector {

	public static final String PROP_MGR_NAME = "queue.manager.name";
	public static final String PROP_QUE_NAME = "queue.name";
	public static final String PROP_CHL_NAME = "channel.name";
	public static final String PROP_LISTENER = "process.query";

	private static final String listener_PQTL = "State.Name.eq=runmqlsr,Args.*.eq={0}";

	private Log log = LogFactory.getLog(MQSeriesDetector.class);

	// XXX no vale para nada
	private List getServerList(String path) {
		log.debug("getServerList(" + path + ")");

		List serverList = new ArrayList();

		try {
			List productServices = MQSeriesMgrService.findServices();
			Iterator it = productServices.iterator();
			while (it.hasNext()) {
				String qmanager = (String) it.next();
				String type = getTypeInfo().getName();
				log.debug("qmanager = '" + qmanager + "' (" + type + ")");

				ServerResource server = new ServerResource();

				server.setType(type);
				server.setName(getPlatformName() + " " + type + " " + qmanager);
				server.setInstallPath("foo_" + path); // XXX
				server.setIdentifier(server.getInstallPath() + " " + qmanager);

				ConfigResponse conf = new ConfigResponse();
				conf.setValue(PROP_MGR_NAME, qmanager);
				server.setProductConfig(conf);

				server.setMeasurementConfig();

				serverList.add(server);
			}
		} catch (PluginException e) { // XXX revisar el porque de esta captura. Àes necesaria?
			e.printStackTrace();
		}

		return serverList;
	}

	public List getServerResources(ConfigResponse platformConfig) throws PluginException {
		if (isWin32()) {
			return null; // registry scan will pick it up.
		}

		final String[] dirs = MQSeriesProductPlugin.DEFAULT_UNIX_INST;
		for (int i = 0; i < dirs.length; i++) {
			String path = dirs[i];
			if (new File(path).exists()) {
				return getServerList(path);
			}
		}
		return null;
	}

	public List getServerResources(ConfigResponse platformConfig, String path) throws PluginException {
		File filePath = new File(path);

		if (!filePath.exists()) {
			throw new PluginException("Error detecting MQSeries " + "server in: " + path);
		}

		// loose bin/runmqlsr
		path = new File(path).getParentFile().getParentFile().getAbsolutePath();

		return getServerList(path);
	}

	public List getServerResources(ConfigResponse platformConfig, String path, RegistryKey current) throws PluginException {

		// XXX check CurrentVersion\\MQServer{Release,Version}
		return getServerList(path);
	}

	protected List discoverServices(ConfigResponse serverConfig) throws PluginException {
		List services = new ArrayList();
		services.addAll(discoverServicesQueue(serverConfig));
		services.addAll(discoverServicesListener(serverConfig));
		services.addAll(discoverServicesChannels(serverConfig));
		return services;
	}

	private List discoverServicesChannels(ConfigResponse serverConfig) throws PluginException {
		log.debug("discoverServicesChannels(" + serverConfig + ")");
		List services = new ArrayList();

		String qmanager = serverConfig.getValue(PROP_MGR_NAME);
		String type = getTypeInfo().getName();

		Iterator channels = MQSeriesChannelService.findChannels(serverConfig.getValue(PROP_MGR_NAME)).iterator();
		while (channels.hasNext()) {
			MQSeriesChannelService channel = (MQSeriesChannelService) channels.next();
			ServiceResource service = new ServiceResource();
			String serviceName;
			
			switch (channel.getType()) {
			case MQConstants.MQCHT_SENDER:
				serviceName="Sender Channel";
				break;
			case MQConstants.MQCHT_RECEIVER:
				serviceName="Receiver Channel";
				break;
			default:
				serviceName="Channel";
				break;
			}
			
			service.setType(type + " "+serviceName);
			service.setServiceName(serviceName+" " + channel.getName());

			ConfigResponse conf = new ConfigResponse();
			conf.setValue(PROP_MGR_NAME, qmanager);
			conf.setValue(PROP_CHL_NAME, channel.getName());
			service.setProductConfig(conf);

			service.setMeasurementConfig();

			services.add(service);
		}
		return services;

	}

	private List discoverServicesListener(ConfigResponse serverConfig) throws PluginException {
		log.debug("discoverServicesListener(" + serverConfig + ")");
		List services = new ArrayList();
		String type = getTypeInfo().getName();
		String qmanager[] = new String[] { serverConfig.getValue(PROP_MGR_NAME) };

		String query = MessageFormat.format(listener_PQTL, qmanager);
		if (getPids(query).length > 0) {
			ServiceResource service = new ServiceResource();
			service.setType(type + " Listener");
			service.setServiceName("Listener");

			ConfigResponse conf = new ConfigResponse();
			conf.setValue(PROP_LISTENER, query);
			service.setProductConfig(conf);

			service.setMeasurementConfig();

			services.add(service);
		}
		return services;
	}

	private List discoverServicesQueue(ConfigResponse serverConfig) throws PluginException {
		log.debug("discoverServicesQueue(" + serverConfig + ")");
		List services = new ArrayList();

		String qmanager = serverConfig.getValue(PROP_MGR_NAME);
		String type = getTypeInfo().getName();

		List queues = MQSeriesQueueService.findQueues(qmanager);
		Iterator it = queues.iterator();
		while (it.hasNext()) {
			String queue = (String) it.next();
			String servName = (queue.startsWith("SYSTEM") ? "SystemQueue" : "Queue");

			ServiceResource service = new ServiceResource();
			service.setType(type + " " + servName);
			service.setServiceName(servName + " " + queue);

			ConfigResponse conf = new ConfigResponse();
			conf.setValue(PROP_MGR_NAME, qmanager);
			conf.setValue(PROP_QUE_NAME, queue);
			service.setProductConfig(conf);

			service.setMeasurementConfig();

			services.add(service);
		}

		return services;
	}
	// protected List discoverServices(ConfigResponse serverConfig) throws PluginException {
	// List productServices = MQSeriesMgrService.findServices(getTypeInfo().getName());
	//
	// List services = new ArrayList(productServices.size());
	//
	// for (int i = 0; i < productServices.size(); i++) {
	// MQSeriesService mqsvc = (MQSeriesService) productServices.get(i);
	//
	// getLog().debug("discovered: " + mqsvc.getFullName());
	//
	// ServiceResource service = new ServiceResource();
	// service.setType(mqsvc.getTypeName());
	// service.setServiceName(mqsvc.getFullName());
	//
	// ConfigResponse config = new ConfigResponse(mqsvc.getProductConfig());
	// service.setProductConfig(config);
	//
	// service.setMeasurementConfig();
	//
	// services.add(service);
	// }
	//
	// return services;
	// }
}
