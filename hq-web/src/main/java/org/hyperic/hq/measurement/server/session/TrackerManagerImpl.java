/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.measurement.server.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.agent.AgentConnectionException;
import org.hyperic.hq.agent.AgentRemoteException;
import org.hyperic.hq.appdef.shared.AgentNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.PlatformManager;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClient;
import org.hyperic.hq.measurement.agent.client.MeasurementCommandsClientFactory;
import org.hyperic.hq.measurement.shared.TrackerManager;
import org.hyperic.hq.product.ConfigTrackPlugin;
import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The tracker manager handles sending agents add and remove operations
 * for the log and config track plugins.
 */
@Service
// Not transactional
public class TrackerManagerImpl implements TrackerManager {
    private final Log log = LogFactory.getLog(TrackerManagerImpl.class);
    private PlatformManager platformManager;
    private MeasurementCommandsClientFactory measurementCommandsClientFactory;

    @Autowired
    public TrackerManagerImpl(PlatformManager platformManager, MeasurementCommandsClientFactory measurementCommandsClientFactory) {
        this.platformManager = platformManager;
        this.measurementCommandsClientFactory = measurementCommandsClientFactory;
    }

    private MeasurementCommandsClient getClient(AppdefEntityID aid)
        throws PermissionException, AgentNotFoundException {
        return measurementCommandsClientFactory.getClient(aid); 
    }

    /**
     * Enable log or config tracking for the given resource
     */
    private void trackPluginAdd(AuthzSubject subject, AppdefEntityID id,
                                String pluginType, ConfigResponse response)
        throws PermissionException, PluginException {
        try {
            MeasurementCommandsClient client = getClient(id);
            String resourceName = platformManager.getPlatformPluginName(id);

            client.addTrackPlugin(id.getAppdefKey(), pluginType,
                                  resourceName, response);
        } catch (AppdefEntityNotFoundException e) {
            throw new PluginException("Entity not found: " +
                                      e.getMessage());
        } catch (AgentNotFoundException e) {
            throw new PluginException("Agent error: " + e.getMessage(), e);
        } catch (AgentConnectionException e) {
            throw new PluginException("Agent error: " + e.getMessage(), e);
        } catch (AgentRemoteException e) {
            throw new PluginException("Agent error: " + e.getMessage(), e);
        }
    }

    /**
     * Disable log or config tracking for the given resource
     */
    private void trackPluginRemove(AuthzSubject subject, AppdefEntityID id,
                                   String pluginType)
        throws PermissionException,
        PluginException {
        try {
            MeasurementCommandsClient client = getClient(id);
            client.removeTrackPlugin(id.getAppdefKey(), pluginType);
        } catch (AgentNotFoundException e) {
            log.warn("Agent not found while removing track plugins " +
                     "(this is ok).  Exception Message:" + e.getMessage());
        } catch (AgentConnectionException e) {
            throw new PluginException("Agent error: " + e.getMessage(), e);
        } catch (AgentRemoteException e) {
            throw new PluginException("Agent error: " + e.getMessage(), e);
        }
    }

    /**
     * Enable log and config tracking for a resource if it has been enabled.
     */
    public void enableTrackers(AuthzSubject subject, AppdefEntityID id,
                               ConfigResponse config)
        throws PermissionException, PluginException {
        if (LogTrackPlugin.isEnabled(config, id.getType())) {
            trackPluginAdd(subject, id, ProductPlugin.TYPE_LOG_TRACK, config);
        }

        if (ConfigTrackPlugin.isEnabled(config, id.getType())) {
            trackPluginAdd(subject, id, ProductPlugin.TYPE_CONFIG_TRACK, config);
        }
    }

    /**
     * Disable log and config tracking for a resource.
     */
    public void disableTrackers(AuthzSubject subject, AppdefEntityID id,
                                ConfigResponse config)
        throws PermissionException, PluginException {
        if (LogTrackPlugin.isEnabled(config, id.getType())) {
            trackPluginRemove(subject, id, ProductPlugin.TYPE_LOG_TRACK);
        }

        if (ConfigTrackPlugin.isEnabled(config, id.getType())) {
            trackPluginRemove(subject, id, ProductPlugin.TYPE_CONFIG_TRACK);
        }
    }

    /**
     * Toggle log and config tracking for the resource.
     */
    public void toggleTrackers(AuthzSubject subject, AppdefEntityID id,
                               ConfigResponse config)
        throws PermissionException, PluginException {
        if (LogTrackPlugin.isEnabled(config, id.getType())) {
            trackPluginAdd(subject, id, ProductPlugin.TYPE_LOG_TRACK, config);
        } else {
            trackPluginRemove(subject, id, ProductPlugin.TYPE_LOG_TRACK);
        }

        if (ConfigTrackPlugin.isEnabled(config, id.getType())) {
            trackPluginAdd(subject, id, ProductPlugin.TYPE_CONFIG_TRACK, config);
        } else {
            trackPluginRemove(subject, id, ProductPlugin.TYPE_CONFIG_TRACK);
        }
    }
}
