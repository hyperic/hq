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

package org.hyperic.hq.bizapp.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.ConfigFetchException;
import org.hyperic.hq.appdef.shared.ConfigManager;
import org.hyperic.hq.appdef.shared.InvalidConfigException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.control.shared.ControlManager;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.TopNManager;
import org.hyperic.hq.measurement.shared.TrackerManager;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("configValidator")
public class ConfigValidatorImpl implements ConfigValidator {
    protected MeasurementManager measurementManager;
    protected TrackerManager trackerManager;
    protected ConfigManager configManager;
    private final ControlManager controlManager;
    private final TopNManager topNManager;

    @Autowired
    public ConfigValidatorImpl(ConfigManager configManager, MeasurementManager measurementManager,
            TrackerManager trackerManager, ControlManager controlManager, TopNManager topNManager) {
        this.configManager = configManager;
        this.measurementManager = measurementManager;
        this.trackerManager = trackerManager;
        this.controlManager = controlManager;
        this.topNManager = topNManager;
    }

    public void validate(AuthzSubject subject, String type, AppdefEntityID[] ids) throws PermissionException,
        EncodingException, ConfigFetchException, AppdefEntityNotFoundException, InvalidConfigException {
        if (type.equals(ProductPlugin.TYPE_PRODUCT) || type.equals(ProductPlugin.TYPE_MEASUREMENT)) {
            updateMeasurementConfigs(subject, ids);
        }

        if (type.equals(ProductPlugin.TYPE_PRODUCT) || type.equals(ProductPlugin.TYPE_CONTROL)) {
            updateControlConfigs(subject, ids);
        }
    }

    /**
     * Tell the measurement subsystem that we have updated configuration for a
     * number of entities.
     */
    private void updateMeasurementConfigs(AuthzSubject subject, AppdefEntityID[] ids) throws EncodingException,
        PermissionException, AppdefEntityNotFoundException, InvalidConfigException {
        ConfigResponse[] responses;

        responses = new ConfigResponse[ids.length];

        for (int i = 0; i < ids.length; i++) {
            try {
                responses[i] = configManager.getMergedConfigResponse(subject, ProductPlugin.TYPE_MEASUREMENT, ids[i],
                    true);

                measurementManager.checkConfiguration(subject, ids[i], responses[i], true);
            } catch (ConfigFetchException exc) {
                responses[i] = null;
            }
        }

        for (int i = 0; i < ids.length; i++) {
            if (responses[i] == null) {
                // This is OK, since this resource may not have been
                // configured for measurement (or at all)
                continue;
            }

            // Metric configuration has been validated, check if we need
            // to enable or disable log and config tracking.
            try {
                trackerManager.toggleTrackers(subject, ids[i], responses[i]);
            } catch (PluginException e) {
                throw new InvalidConfigException("Unable to modify config " + "track config: " + e.getMessage(), e);
            }

            topNManager.scheduleTopNCollection(ids[i], responses[i]);
        }
    }

    private void updateControlConfigs(AuthzSubject subject, AppdefEntityID[] ids) throws PermissionException,
        ConfigFetchException, EncodingException, InvalidConfigException {

        for (AppdefEntityID id : ids) {
            try {
                controlManager.checkControlEnabled(subject, id);
            } catch (PluginException e) {
                // Not configured for control
                continue;
            }

            try {
                controlManager.configureControlPlugin(subject, id);
            } catch (Exception e) {
                throw new InvalidConfigException(e.getMessage());
            }
        }
    }
}
