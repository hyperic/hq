package org.hyperic.plugin.vcenter;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

import java.util.ArrayList;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.hq.product.TypeInfo;

public class Discovery extends DaemonDetector {

    private static final Log log = LogFactory.getLog(Discovery.class);

    /**
     * solve a weird bug with services on the whole plugin
     * @param config
     * @return
     * @throws PluginException 
     */
    @Override
    protected List discoverServices(ConfigResponse config)
        throws PluginException {

        //e.g. qmail plugin has 1 instance of each service
        String hasBuiltinServices =
            getTypeProperty("HAS_BUILTIN_SERVICES");

        if (!"true".equals(hasBuiltinServices)) {
            return super.discoverServices(config);    
        }

        List services = new ArrayList();
        TypeInfo[] types = getPluginData().getTypes();

        for (int i=0; i<types.length; i++) {
            TypeInfo type = types[i];
            if (type.getType() != TypeInfo.TYPE_SERVICE) {
                continue;
            }
            if (!type.getName().startsWith(getTypeInfo().getName())) {
                continue;
            }
            if (!this.getTypeInfo().getVersion().equals(type.getVersion())) {
                continue;
            }

            log.error("====> "+type.getName());
            log.error("====> "+getTypeInfo().getName());
            ServiceResource service = new ServiceResource();
            service.setType(type.getName());
            String name = getTypeNameProperty(type.getName());
            service.setServiceName(name);
            //try the defaults
            setProductConfig(service, new ConfigResponse());
            setMeasurementConfig(service, new ConfigResponse());
            services.add(service);
        }

        return services;
    }
}
