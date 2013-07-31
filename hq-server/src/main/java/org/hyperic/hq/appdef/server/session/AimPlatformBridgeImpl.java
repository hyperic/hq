package org.hyperic.hq.appdef.server.session;

import java.rmi.RemoteException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.integrien.alive.common.adapter3.openapi.OpenDataImporter3;
import com.integrien.alive.common.adapter3.openapi.OpenDataImporterFactory;

@SuppressWarnings("deprecation")
@Service
public class AimPlatformBridgeImpl implements AimPlatformBridge {

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
}
