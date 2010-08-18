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

package org.hyperic.hq.product.jmx;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.MeasurementInfo;
import org.hyperic.hq.product.MeasurementInfos;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceType;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.filter.TokenReplacer;

/**
 * Constructs a {@link ServiceType} object from a JMX {@link ModelMBeanInfo}
 *
 * @author jhickey
 *
 */
public class ServiceTypeFactory {

	private static final Set VALID_CATEGORIES = new HashSet(Arrays
			.asList(MeasurementConstants.VALID_CATEGORIES));

	private static final Set VALID_UNITS = new HashSet(Arrays
			.asList(MeasurementConstants.VALID_UNITS));

	private Log log = LogFactory.getLog(ServiceTypeFactory.class);

	private void addControlActions(final ServiceType serviceType,
			ModelMBeanInfo serviceInfo) {
		Set actions = new HashSet();
		final MBeanOperationInfo[] operations = serviceInfo.getOperations();
		for (int i = 0; i < operations.length; i++) {
			actions.add(operations[i].getName());
		}
		serviceType.setControlActions(actions);
	}

	private void addCustomProperties(final ServiceType serviceType,
			ModelMBeanInfo serviceInfo) {
		final ConfigSchema propertiesSchema = new ConfigSchema();
		final MBeanAttributeInfo[] attributes = serviceInfo.getAttributes();
		for (int i = 0; i < attributes.length; i++) {
			if (!isMetric(attributes[i])) {
				String description = attributes[i].getDescription();
				if(description == null) {
					description = "";
				}
				propertiesSchema.addOption(new StringConfigOption(attributes[i]
						.getName(), description));
				// custom properties are currently modified by adding a "set"
				// control action
				if (attributes[i].isWritable()) {
					serviceType.addControlAction(
							"set" + attributes[i].getName());
				}
			}
		}
		serviceType.setCustomProperties(propertiesSchema);
	}

	private void addFilter(String key, Properties properties,
			TokenReplacer replacer) {
		String val = properties.getProperty(key);
		if (val != null) {
			replacer.addFilter(key, val);
		}
	}

	private void addMeasurements(final ServiceType serviceType,
			final ProductPlugin productPlugin, ModelMBeanInfo serviceInfo) {
		MeasurementInfos measurements = new MeasurementInfos();
		final MBeanAttributeInfo[] attributes = serviceInfo.getAttributes();
		for (int i = 0; i < attributes.length; i++) {
			if (isMetric(attributes[i])) {
				measurements.addMeasurementInfo(createMeasurementInfo(
						serviceType, productPlugin,
						(ModelMBeanAttributeInfo) attributes[i]));
			}
		}
		measurements.addMeasurementInfo(createAvailabilityMeasurement(
				serviceType, productPlugin));
		serviceType.setMeasurements(measurements);
	}

	private void addMeasurementTemplate(Properties measurementProperties,
			ProductPlugin productPlugin, ServiceType serviceType) {
		TokenReplacer replacer = new TokenReplacer();
		final String objectName = serviceType.getProperties().getValue(
				serviceType.getInfo().getName() + ".OBJECT_NAME");
		addFilter(MeasurementInfo.ATTR_ALIAS, measurementProperties, replacer);
		addFilter(MeasurementInfo.ATTR_NAME, measurementProperties, replacer);
		replacer.addFilter("OBJECT_NAME", objectName);
		final String template = filter(productPlugin.getPluginProperty(MeasurementInfo.ATTR_TEMPLATE), productPlugin,
				replacer);
		measurementProperties.put(MeasurementInfo.ATTR_TEMPLATE, template);
	}

	private void addPlugins(final ServiceType serviceType,
			final ProductPlugin productPlugin) {
		final ConfigResponse pluginClasses = new ConfigResponse();
		pluginClasses.setValue("measurement", productPlugin.getPluginProperty("measurement-class"));
		pluginClasses.setValue("control", productPlugin.getPluginProperty("control-class"));
		serviceType.setPluginClasses(pluginClasses);
	}

	private void addProperties(final ServiceType serviceType,
			ObjectName objectName) {
		final ConfigResponse properties = new ConfigResponse();
		properties.setValue(serviceType.getInfo().getName() + ".NAME",
				serviceType.getServiceName());
		properties.setValue(serviceType.getInfo().getName() + ".OBJECT_NAME",
				getObjectNameProperty(objectName));
		serviceType.setProperties(properties);
	}

	/**
	 * Creates a Set of ServiceTypes from a Set of Services, ignoring multiple
	 * services of the same ServiceType (determined by fully qualified service
	 * type name)
	 *
	 * @param productPlugin
	 *            The plugin of the product containing this service type
	 * @param serverType
	 *            The type of service containing this service type
	 * @param mServer
	 * @param serviceInfo
	 *            The unique info of the service type
	 * @param objectNames
	 *            The {@link ObjectName}s of the associated services whose
	 *            metadata is to be inspected
	 * @return A Set of created {@link ServiceType}s created
	 * @throws InstanceNotFoundException
	 * @throws IntrospectionException
	 * @throws ReflectionException
	 * @throws IOException
	 */
	public Set create(ProductPlugin productPlugin, ServerTypeInfo serverType,
			MBeanServerConnection mServer, Set objectNames)
			throws InstanceNotFoundException, IntrospectionException,
			ReflectionException, IOException {
		final Set serviceTypes = new HashSet();
		for (Iterator iterator = objectNames.iterator(); iterator.hasNext();) {
			final ObjectName objectName = (ObjectName) iterator.next();
			final MBeanInfo serviceInfo = mServer.getMBeanInfo(objectName);
			if (serviceInfo instanceof ModelMBeanInfo) {
				ServiceType identityType = getServiceType(productPlugin.getName(),serverType,
						(ModelMBeanInfo) serviceInfo, objectName);
				if (identityType != null && !serviceTypes.contains(identityType)) {
					final ServiceType serviceType = create(productPlugin,
							serverType, (ModelMBeanInfo) serviceInfo,
							objectName);
					if (serviceType != null) {
						serviceTypes.add(serviceType);
					}
				}
			}
		}
		return serviceTypes;
	}

	/**
	 *
	 * @param productPlugin
	 *            The plugin of the product containing this service type
	 * @param serverType
	 *            The type of service containing this service type
	 * @param serviceInfo
	 *            The unique info of the service type
	 * @param objectName
	 *            The {@link ObjectName} of the associated service whose
	 *            metadata is to be inspected
	 * @return The created {@link ServiceType} or null if it could not be
	 *         created
	 */
	public ServiceType create(ProductPlugin productPlugin,
			ServerTypeInfo serverType, ModelMBeanInfo serviceInfo,
			ObjectName objectName) {
		ServiceType serviceType = getServiceType(productPlugin.getName(),serverType, serviceInfo,
				objectName);
		if(serviceType == null) {
			return null;
		}
		addControlActions(serviceType, serviceInfo);
		addCustomProperties(serviceType, serviceInfo);
		addProperties(serviceType, objectName);
		addPlugins(serviceType, productPlugin);
		addMeasurements(serviceType, productPlugin, serviceInfo);
		return serviceType;
	}

	private MeasurementInfo createAvailabilityMeasurement(
			final ServiceType serviceType, final ProductPlugin productPlugin) {
		Properties measurementProperties = new Properties();
		measurementProperties.put(MeasurementInfo.ATTR_UNITS,
				MeasurementConstants.UNITS_PERCENTAGE);
		measurementProperties.put(MeasurementInfo.ATTR_NAME, Metric.ATTR_AVAIL);
		measurementProperties
				.put(MeasurementInfo.ATTR_ALIAS, Metric.ATTR_AVAIL);
		measurementProperties.put(MeasurementInfo.ATTR_COLLECTION_TYPE,
				"dynamic");
		measurementProperties.put(MeasurementInfo.ATTR_CATEGORY,
				MeasurementConstants.CAT_AVAILABILITY);
		measurementProperties.put(MeasurementInfo.ATTR_INDICATOR, "true");

		measurementProperties.put(MeasurementInfo.ATTR_DEFAULTON, "true");
		measurementProperties.put(MeasurementInfo.ATTR_INTERVAL, "600000");
		addMeasurementTemplate(measurementProperties, productPlugin,
				serviceType);
		return createMeasurementInfo(measurementProperties);
	}

	private MeasurementInfo createMeasurementInfo(
			Properties measurementProperties) {
		MeasurementInfo metric = new MeasurementInfo();
		try {
			metric.setAttributes(measurementProperties);
		} catch (Exception e) {
			log.warn("Error setting metric attributes.  Cause: "
					+ e.getMessage());
		}
		metric.setCategory(metric.getCategory().toUpperCase());
		return metric;
	}

	private MeasurementInfo createMeasurementInfo(
			final ServiceType serviceType, final ProductPlugin productPlugin,
			ModelMBeanAttributeInfo attribute) {
		Properties measurementProperties = new Properties();

		Descriptor descriptor = attribute.getDescriptor();
		String units = (String) descriptor
				.getFieldValue(MeasurementInfo.ATTR_UNITS);
		if ("s".equals(units)) {
			units = MeasurementConstants.UNITS_SECONDS;
		}
		if (!VALID_UNITS.contains(units)) {
			measurementProperties.put(MeasurementInfo.ATTR_UNITS, "none");
		} else {
			measurementProperties.put(MeasurementInfo.ATTR_UNITS, units);
		}
		final String displayName = (String) descriptor
				.getFieldValue("displayName");
		// Not likely to be null, as JMX impl currently populates it with
		// attribute name if not set
		if (displayName == null) {
			measurementProperties.put(MeasurementInfo.ATTR_NAME, attribute
					.getName());
		} else {
			measurementProperties.put(MeasurementInfo.ATTR_NAME, displayName);
		}
		measurementProperties.put(MeasurementInfo.ATTR_ALIAS, attribute
				.getName());
		String metricType = (String) descriptor.getFieldValue("metricType");
		if(metricType ==  null || !("COUNTER".equals(metricType.toUpperCase()))) {
			//GAUGE
			measurementProperties.put(MeasurementInfo.ATTR_COLLECTION_TYPE,"dynamic");
			measurementProperties.put(MeasurementInfo.ATTR_INTERVAL, "300000");
		}
		else {
			//COUNTER
			measurementProperties.put(MeasurementInfo.ATTR_COLLECTION_TYPE,
					"trendsup");
			String rate = (String) descriptor.getFieldValue("rate");
			if(rate != null) {
			    measurementProperties.put(MeasurementInfo.ATTR_RATE, rate);
			}else {
			    measurementProperties.put(MeasurementInfo.ATTR_RATE, "none");
			}
			measurementProperties.put(MeasurementInfo.ATTR_INTERVAL, "600000");
		}
		String collectionInterval = (String) descriptor.getFieldValue("collectionInterval");
		if(collectionInterval != null) {
		    try {
		        Long.valueOf(collectionInterval);
		        measurementProperties.put(MeasurementInfo.ATTR_INTERVAL, collectionInterval);
		    }catch(NumberFormatException e) {
		        log.warn("Specified collection interval " + collectionInterval + " is not numeric.  Default value will be used instead.");
		    }
		}
		
		String category = (String) descriptor.getFieldValue("metricCategory");
		if (category == null
				|| !VALID_CATEGORIES.contains(category.toUpperCase())) {
			measurementProperties.put(MeasurementInfo.ATTR_CATEGORY,
					MeasurementConstants.CAT_UTILIZATION);
		} else {
			measurementProperties.put(MeasurementInfo.ATTR_CATEGORY, category
					.toUpperCase());
		}
		String indicator = (String) descriptor.getFieldValue("indicator");
		if(indicator == null || "true".equals(indicator.toLowerCase())) {
			//indicator is not in Spring 3.0 @ManagedMetric.  Turn measurement on and make indicator by default
			measurementProperties.put(MeasurementInfo.ATTR_INDICATOR, "true");
			measurementProperties.put(MeasurementInfo.ATTR_DEFAULTON, "true");
		} else {
			measurementProperties.put(MeasurementInfo.ATTR_INDICATOR, "false");
			measurementProperties.put(MeasurementInfo.ATTR_DEFAULTON, "false");
		}
		String defaultOn = (String) descriptor.getFieldValue("defaultOn");
        if(defaultOn != null) {
            if("true".equals(defaultOn.toLowerCase()) || "false".equals(defaultOn.toLowerCase())) {
                measurementProperties.put(MeasurementInfo.ATTR_DEFAULTON, defaultOn.toLowerCase());
            } else{
                log.warn("Invalid value of " + defaultOn + " specified for defaultOn.  Default value will be used instead.");
            }
        }
		addMeasurementTemplate(measurementProperties, productPlugin,
				serviceType);
		return createMeasurementInfo(measurementProperties);
	}

	private String filter(String val, ProductPlugin productPlugin,
			TokenReplacer replacer) {
		return replacer.replaceTokens(val);
	}

	private String getObjectNameProperty(ObjectName objectName) {
		final StringBuffer objectNameProperty = new StringBuffer(objectName
				.getDomain()).append(':');
		Hashtable keyProperties = objectName.getKeyPropertyList();
		for (Iterator iterator = keyProperties.entrySet().iterator(); iterator
				.hasNext();) {
			Map.Entry keyProperty = (Map.Entry) iterator.next();
			objectNameProperty.append(keyProperty.getKey()).append('=');
			// for now, recognize only type and subtype - replace all others
			// with variable placeholders
			if ("type".equals(keyProperty.getKey())
					|| "subtype".equals(keyProperty.getKey())) {
				objectNameProperty.append(keyProperty.getValue());
			} else {
				objectNameProperty.append('%').append(keyProperty.getKey())
						.append('%');
			}
			objectNameProperty.append(',');
		}
		objectNameProperty.deleteCharAt(objectNameProperty.length() - 1);
		return objectNameProperty.toString();
	}

	/**
	 * Returns a ServiceType containing ONLY the properties needed at construction time (the ones that guarantee uniqueness)
	 * @param productName The name of the product containing the service
	 * @param serverType The name of the server containing the service
	 * @param serviceInfo Info about the service
	 * @param objectName The {@link ObjectName} of the discovered MBean representing the service instance
	 * @return A ServiceType containing ONLY the properties needed at construction time (the ones that guarantee uniqueness)
	 */
	public ServiceType getServiceType(String productName, ServerTypeInfo serverType,
			ModelMBeanInfo serviceInfo, ObjectName objectName) {
		String serviceTypeName = objectName.getKeyProperty("type");
		final String subType = objectName.getKeyProperty("subtype");
		if (subType != null) {
			serviceTypeName = serviceTypeName + " " + subType;
		}
		try {
			Descriptor serviceDescriptor = serviceInfo.getMBeanDescriptor();
			if ("false".equals(serviceDescriptor.getFieldValue("export"))) {
				return null;
			}
			String serviceType = (String) serviceDescriptor
					.getFieldValue("typeName");
			if (serviceType != null) {
				serviceTypeName = serviceType;
			}
		} catch (Exception e) {
			log
					.warn("Error obtaining MBeanInfo descriptor field values.  Default values will be used.  Cause: "
							+ e.getMessage());
		}
		return new ServiceType(serviceTypeName, productName, new ServiceTypeInfo(serverType
				.getName()
				+ ' ' + serviceTypeName, serviceInfo.getDescription(),
				serverType));
	}

	private boolean isMetric(MBeanAttributeInfo attribute) {
		if (attribute instanceof ModelMBeanAttributeInfo) {
			String attributeType = (String) ((ModelMBeanAttributeInfo) attribute)
					.getDescriptor().getFieldValue("attributeType");
			//attributeType not in Spring 3.0 @ManagedMetric.  If it's there from instrumented 2.5.6, process it
			if(attributeType != null) {
				if ("Metric".equals(attributeType)) {
					return true;
				}else {
					return false;
				}
			}
			String metricType = (String) ((ModelMBeanAttributeInfo) attribute)
			.getDescriptor().getFieldValue("metricType");
			//this field is required for metrics - assume it's not a metric if not present
			if(metricType != null) {
				return true;
			}

		}
		return false;
	}
}
