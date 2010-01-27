package org.hyperic.hq.appdef.shared;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.ServerType;
import org.hyperic.hq.product.MeasurementInfos;
import org.hyperic.hq.product.ServerTypeInfo;
import org.hyperic.hq.product.ServiceType;
import org.hyperic.hq.product.ServiceTypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.springframework.stereotype.Component;

/**
 * Constructs a {@link ServiceType} from an {@link AIServiceTypeValue}
 * 
 * @author jhickey
 * 
 */
@Component
public class ServiceTypeFactory {

	private Log log = LogFactory.getLog(ServiceTypeFactory.class);

	/**
	 * 
	 * @param serviceTypeValue
	 *            The {@link AIServiceTypeValue} to convert from
	 * @param serverType
	 *            The type of the server the service type belongs to
	 * @return The created {@link ServiceType}
	 */
	public ServiceType create(AIServiceTypeValue serviceTypeValue,
			ServerType serverType) {
		final ServerTypeInfo server = new ServerTypeInfo();
		server.setName(serverType.getName());
		server.setDescription(serverType.getDescription());
		ServiceTypeInfo serviceTypeInfo = new ServiceTypeInfo(serviceTypeValue
				.getName(), serviceTypeValue.getDescription(), server);
		final ServiceType serviceType = new ServiceType(serviceTypeValue
				.getServiceName(), serviceTypeValue.getProductName(),serviceTypeInfo);
		String[] controlActions = serviceTypeValue.getControlActions();
		if (controlActions != null) {
			serviceType.setControlActions(new HashSet(Arrays
					.asList(controlActions)));
		}
		try {
			byte[] customProps = serviceTypeValue.getCustomProperties();
			if (customProps != null) {
				serviceType.setCustomProperties(ConfigSchema
						.decode(customProps));
			}
			serviceType.setPluginClasses(ConfigResponse.decode(serviceTypeValue
					.getPluginClasses()));
			serviceType.setProperties(ConfigResponse.decode(serviceTypeValue
					.getProperties()));
			byte[] measurements = serviceTypeValue.getMeasurements();
			if (measurements != null) {
				serviceType.setMeasurements(MeasurementInfos
						.decode(measurements));
			}
		} catch (EncodingException e) {
			log.error("Error decoding values from AIServiceTypeValue.  Cause: "
					+ e.getMessage());
		}
		return serviceType;
	}
}
