package org.hyperic.hq.appdef.server.session;

import java.rmi.RemoteException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.authz.server.session.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.integrien.alive.common.adapter3.ResourceKey;
import com.integrien.alive.common.adapter3.config.ResourceIdentifierConfig;
import com.integrien.alive.common.adapter3.openapi.OpenDataImporter3;
import com.integrien.alive.common.adapter3.openapi.OpenDataImporterFactory;
import com.integrien.alive.common.adapter3.openapi.ResourceData;

@SuppressWarnings("deprecation")
@Service
public class AimPlatformBridgeImpl implements AimPlatformBridge {

    private static final String HYPERIC_RESOURCE_KIND = "HypericResource";

    public static final String SOURCE_ADAPTER_KIND = "OPEN_API";

    protected final Log log = LogFactory.getLog(AimPlatformBridgeImpl.class.getName());
    private final OpenDataImporter3 openDataImporter;
    private final String host;
    private final int port;

    @Autowired
    public AimPlatformBridgeImpl(@Value("#{AimPlatformProperties['aim.platform.host']}") String host,
            @Value("#{AimPlatformProperties['aim.platform.port']}") int port) {
        this.host = host;
        this.port = port;
        this.openDataImporter = getOpenDataImporter();
    }

    private OpenDataImporter3 getOpenDataImporter() {
        OpenDataImporter3 openDataImporter = null;
        log.info(String.format("Creating OpenDataImporter. Hostname = %s, Port = %d ", host, port));
        for (int numTries = 1; numTries <= 3; numTries++) {
            try {
                openDataImporter = OpenDataImporterFactory.getOpenDataImporter3(host, port);
            } catch (final RemoteException e) {
                log.fatal(String.format("Could not obtain OpenDataImporter. Hostname = %s, Port = %d ", host,
                        port), e);
                break;
            }
            if (openDataImporter != null) {
                break;
            }
            try {
                log.info("Trying rmi connection..." + numTries);
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
            }
        }
        return openDataImporter;
    }

    public void resourceCreated(Resource newResource, Resource parentResource) {
        log.info(String.format("Adding new resource. name = %s, parent resource name = %s ", newResource.getName(),
                (parentResource == null ? "no parent" : parentResource.getName())));

        ResourceKey resourceKey = getResourceKey(newResource);
        ResourceData resourceData = new ResourceData(resourceKey);

        try {
            openDataImporter.addResourceData(SOURCE_ADAPTER_KIND, Arrays.asList(resourceData));
        } catch (Exception ex) {
            log.error("could not add new resources - ", ex);
            return;
        }

        if (null != parentResource) {
            ResourceKey parentResourceKey = getResourceKey(newResource);

            // set relationships for this parent resource
            try {
                openDataImporter.addRelationships(SOURCE_ADAPTER_KIND, parentResourceKey, Arrays.asList(resourceKey));
            } catch (Exception ex) {
                log.error("could not set parent child relationship between '" + parentResource.getName() + "' and '"
                        + newResource.getName() + "'", ex);
            }
        }


    }

    private ResourceKey getResourceKey(Resource resource) {

        ResourceKey resourceKey = new ResourceKey(resource.getName(), HYPERIC_RESOURCE_KIND, SOURCE_ADAPTER_KIND);

        resourceKey.addIdentifier(new ResourceIdentifierConfig("HypericID", String.valueOf(resource.getId())));
        resourceKey.addIdentifier(new ResourceIdentifierConfig("Name", resource.getName()));
        resourceKey.addIdentifier(new ResourceIdentifierConfig("Type", resource.getPrototype().getName()));
        return resourceKey;
    }

}
