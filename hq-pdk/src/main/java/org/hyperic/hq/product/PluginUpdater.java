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

package org.hyperic.hq.product;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hyperic.hq.product.pluginxml.PluginData;
import org.hyperic.util.config.ConfigResponse;

/**
 * Updates a {@link ProductPlugin} on server and agent side with data needed to
 * deal with new or updated {@link ServiceType}s
 * 
 * @author jhickey
 * 
 */
public class PluginUpdater {

	private void addControlActions(ProductPlugin productPlugin,
			ServiceType serviceType) {
		productPlugin.getPluginData().removeControlActions(
				serviceType.getInfo().getName());
		productPlugin.getPluginData().addControlActions(
				serviceType.getInfo().getName(),
				new ArrayList(serviceType.getControlActions()));
	}

	private void addCustomProperties(ProductPlugin productPlugin,
			ServiceType serviceType) {
		productPlugin.getPluginData().removeCustomPropertiesSchema(
				serviceType.getInfo().getName());
		if (serviceType.getCustomProperties() != null) {
			productPlugin.getPluginData().addCustomPropertiesSchema(
					serviceType.getInfo().getName(),
					serviceType.getCustomProperties());
		}
	}

	private void addMetrics(ProductPlugin productPlugin, ServiceType serviceType) {
		productPlugin.getPluginData().removeMetrics(
				serviceType.getInfo().getName());
		Set measurementInfos = serviceType.getMeasurements().getMeasurements();
		for (Iterator iterator = measurementInfos.iterator(); iterator
				.hasNext();) {
			final MeasurementInfo measurement = (MeasurementInfo) iterator
					.next();
			productPlugin.getPluginData().addMetric(
					serviceType.getInfo().getName(), measurement);
		}
	}

	private void addPlugins(ProductPlugin productPlugin, ServiceType serviceType) {
		productPlugin.getPluginData().removePlugins(
				serviceType.getInfo().getName());
		ConfigResponse pluginClasses = serviceType.getPluginClasses();
		for (Iterator pluginKeys = pluginClasses.getKeys().iterator(); pluginKeys
				.hasNext();) {
			String pluginName = (String) pluginKeys.next();
			productPlugin.getPluginData().addPlugin(pluginName,
					serviceType.getInfo().getName(),
					pluginClasses.getValue(pluginName));
		}
		productPlugin.getPluginData().addServiceInventoryPlugin(
				serviceType.getInfo().getServerName(),
				serviceType.getInfo().getName(), null);
	}

	private void addProperties(final ServiceType serviceType) {
		// TODO for now, since property names are prepended with serviceType
		// name, we may be leaving some unused properties in Map if some are
		// removed on service type update...
		ConfigResponse properties = serviceType.getProperties();
		for (Iterator propertyKeys = properties.getKeys().iterator(); propertyKeys
				.hasNext();) {
			String propertyName = (String) propertyKeys.next();
			PluginData.getGlobalProperties().put(propertyName,
					properties.getValue(propertyName));
		}
	}

	private void updatePluginData(ProductPlugin productPlugin,
			ServiceType serviceType) {
		addProperties(serviceType);
		addPlugins(productPlugin, serviceType);
		addControlActions(productPlugin, serviceType);
		addCustomProperties(productPlugin, serviceType);
		boolean isServer = productPlugin.getManager().getRegisterTypes();
		if (isServer) {
			addMetrics(productPlugin, serviceType);
		}
	}

	/**
	 * Updates the specified {@link ProductPlugin} with the new or changed service types.
	 * Will completely wipe out old data related to existing ServiceTypes in order to apply possible
	 * updates.
	 * 
	 * @param productPlugin
	 *            The {@link ProductPlugin} to update
	 * @param serviceTypes
	 *            The service types to update
	 * @throws PluginException
	 */
	public void updateServiceTypes(ProductPlugin productPlugin, Set<ServiceType> serviceTypes)
			throws PluginException {
		
		final List typeInfos = new ArrayList(serviceTypes.size());
		for (Iterator iterator = serviceTypes.iterator(); iterator.hasNext();) {
			ServiceType serviceType = (ServiceType) iterator.next();
			updatePluginData(productPlugin, serviceType);
			typeInfos.add(serviceType.getInfo());
		}
		final TypeInfo[] typeInfosArray = (TypeInfo[]) typeInfos
				.toArray(new TypeInfo[typeInfos.size()]);
		
		//TODO possibly remove unused TypeInfos in the future.  If the service was pre-existing, it's TypeInfo should remain unchanged,so call
		//to removeTypes is just to avoid adding duplicates to the TypeInfo[].  We still have existing TypeInfos hanging out.
		productPlugin.getPluginData().removeTypes(typeInfosArray);
		productPlugin.getPluginData().addTypes(typeInfosArray);
		
		//Plugin types could have changed with call to addPlugins (which replaces plugin class names).  Remove existing plugins for each type
		//and re-register.  TODO could possibly be made more efficient
		productPlugin.getManager().removePluginTypes(typeInfos);
		productPlugin.getManager()
				.addPluginTypes(typeInfosArray, productPlugin);
	}
}
