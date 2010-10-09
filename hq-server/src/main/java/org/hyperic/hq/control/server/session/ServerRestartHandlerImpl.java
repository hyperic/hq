/* Copyright 2009 SpringSource Inc. All Rights Reserved. */

package org.hyperic.hq.control.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.ServerManager;
import org.hyperic.hq.autoinventory.shared.AutoinventoryManager;
import org.hyperic.hq.measurement.shared.TrackerManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles a server restarted event by re-enabling Config/Log tracking of the
 * server and its services (to re-subscribe JMX NotificationListeners). This
 * handler will also perform a runtime scan (auto-discovery) for services in the
 * server, as there may be new ones after server is restarted (like in the case
 * of a tc Server config change that results in a new DataSource)
 * 
 * 
 * @author Jennifer Hickey
 * 
 */
@Component("restartHandler")
public class ServerRestartHandlerImpl implements ServerRestartHandler {

    private final Log log = LogFactory.getLog(ServerRestartHandlerImpl.class.getName());

    private final ServerManager serverManager;

    private final ConfigManager configManager;

    private final AutoinventoryManager autoInvManager;

    private final TrackerManager trackerManager;

    private long startDelay = 15l;

    @Autowired
    public ServerRestartHandlerImpl(ServerManager serverManager,
                                ConfigManager configManager,
                                AutoinventoryManager autoInvManager,
                                TrackerManager trackerManager)
    {
        this.serverManager = serverManager;
        this.configManager = configManager;
        this.autoInvManager = autoInvManager;
        this.trackerManager = trackerManager;
    }

    private void autoDiscover(final AppdefEntityID serverId) throws Exception {
        try {
            autoInvManager.toggleRuntimeScan(null, serverId, true);
        } catch (Exception e) {
            throw new Exception("Failure triggering auto-discovery", e);
        }
    }

    // Re-enable log tracking to re-subscribe MxNotificationPlugins now that
    // server is back up
    private void enableTrackers(final AppdefEntityID id) throws Exception {
        try {
            ConfigResponse response = configManager.getMergedConfigResponse(null,
                                                                            ProductPlugin.TYPE_MEASUREMENT,
                                                                            id,
                                                                            true);
            trackerManager.enableTrackers(null, id, response);
        } catch (Exception e) {
            log.error("Failure re-enabling log/config tracking for entity " + id, e);
        }
    }

    /**
     * Handles a server restarted event
     * 
     * @param serverId The id of the server that was started
     * @throws Exception
     */
    @Transactional
    public void serverRestarted(AppdefEntityID serverId) throws Exception {
        Server server = serverManager.findServerById(serverId.getId());
        try {
            autoDiscover(serverId);
        } finally {
            // TODO some other way to delay b/w server start and MBean
            // availability. This is bad.
            try {
                Thread.sleep(startDelay * 1000);
            } catch (InterruptedException e) {
                // do nothing
            }
            enableTrackers(serverId);
            for (Service service:server.getServices()) {
                enableTrackers(service.getEntityId());
            }
        }
    }

    /**
     * 
     * @param startDelay The amount of time in seconds to wait for server to be
     *        started before re-enabling its JMX subscriptions
     */
    public void setStartDelay(long startDelay) {
        this.startDelay = startDelay;
    }

}
