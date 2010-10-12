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

package org.hyperic.hq.bizapp.shared.lather;

import org.hyperic.hq.appdef.shared.AIServiceTypeValue;
import org.hyperic.lather.LatherKeyNotFoundException;
import org.hyperic.lather.LatherRemoteException;

/**
 * Represents a service type in data types that can be transferred from agent to
 * server via Lather, as would be defined by a <service> XML tag
 * 
 * @author jhickey
 * 
 */
public class AiServiceTypeLatherValue extends AiLatherValue {
	private static final String PROP_CUSTOM_PROPERTIES = "customProperties";

	private static final String PROP_PROPERTIES = "properties";

	private static final String PROP_PLUGIN_CLASSES = "pluginClasses";

	private static final String PROP_DESCRIPTION = "description";

	private static final String PROP_NAME = "name";

	private static final String PROP_SERVICE_NAME = "serviceName";

	private static final String PROP_SERVER_ID = "serverId";

	private static final String PROP_MEASUREMENTS = "measurements";

	private static final String PROP_CONTROL_ACTIONS = "controlActions";
	
	private static final String PROP_PRODUCT_NAME = "productName";

	/**
	 * Default constructor. Needed by lather implementation
	 */
	public AiServiceTypeLatherValue() {
		super();
	}

	/**
	 * Constructs a new {@link AiServiceTypeLatherValue} from the specified
	 * service type
	 * 
	 * @param serviceType
	 *            The service type to obtain information from
	 */
	public AiServiceTypeLatherValue(AIServiceTypeValue serviceType) {
		super();
		setByteAValue(PROP_CUSTOM_PROPERTIES, serviceType.getCustomProperties());
		setByteAValue(PROP_PLUGIN_CLASSES, serviceType.getPluginClasses());
		setByteAValue(PROP_PROPERTIES, serviceType.getProperties());
		setStringValue(PROP_DESCRIPTION, serviceType.getDescription());
		setStringValue(PROP_NAME, serviceType.getName());
		setStringValue(PROP_SERVICE_NAME, serviceType.getServiceName());
		setStringValue(PROP_PRODUCT_NAME,serviceType.getProductName());
		setIntValue(PROP_SERVER_ID, serviceType.getServerId());
		setByteAValue(PROP_MEASUREMENTS, serviceType.getMeasurements());
		String[] controlActions = serviceType.getControlActions();
		for (int i = 0; i < controlActions.length; i++) {
			addStringToList(PROP_CONTROL_ACTIONS, controlActions[i]);
		}
	}

	/**
	 * Constructs an {@link AIServiceTypeValue} from this object
	 * 
	 * @return An {@link AIServiceTypeValue} containing properties from this
	 *         class
	 */
	public AIServiceTypeValue getAIServiceTypeValue() {
		final AIServiceTypeValue serviceType = new AIServiceTypeValue();
		try {
			serviceType.setDescription(this.getStringValue(PROP_DESCRIPTION));
		} catch (LatherKeyNotFoundException exc) {
		}

		serviceType.setName(this.getStringValue(PROP_NAME));
		serviceType.setServerId(this.getIntValue(PROP_SERVER_ID));
		serviceType.setServiceName(this.getStringValue(PROP_SERVICE_NAME));
		serviceType.setProductName(this.getStringValue(PROP_PRODUCT_NAME));
		try {
			serviceType.setCustomProperties(this
					.getByteAValue(PROP_CUSTOM_PROPERTIES));
		} catch (LatherKeyNotFoundException exc) {
		}

		serviceType.setProperties(this.getByteAValue(PROP_PROPERTIES));
		serviceType.setPluginClasses(this.getByteAValue(PROP_PLUGIN_CLASSES));
		try {
			serviceType.setMeasurements(this.getByteAValue(PROP_MEASUREMENTS));
		} catch (LatherKeyNotFoundException exc) {
		}
		try {
			serviceType.setControlActions(this
					.getStringList(PROP_CONTROL_ACTIONS));
		} catch (LatherKeyNotFoundException exc) {
		}
		return serviceType;
	}

	public void validate() throws LatherRemoteException {
		// required by interface
	}

}
